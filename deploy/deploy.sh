#!/bin/bash
# ============================================================
# HIS System Deployment Script (Git Clone Deployment)
# ============================================================
# This script automates the deployment process on the server
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/YOUR_USERNAME/HIS/main/deploy/deploy.sh | bash
# Or:
#   ./deploy/deploy.sh
# ============================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPO_URL="https://github.com/YOUR_USERNAME/HIS.git"  # REPLACE WITH YOUR REPO
BRANCH="main"
DEPLOY_DIR="/opt/his"
PROJECT_NAME="HIS"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}==>${NC} $1"
}

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    log_error "Please do not run this script as root. Use a regular user with sudo privileges."
    exit 1
fi

# Banner
echo "=========================================="
echo "  $PROJECT_NAME Deployment Script"
echo "=========================================="
echo ""

# Step 1: Check prerequisites
log_step "Checking prerequisites..."

if ! command -v git &> /dev/null; then
    log_error "Git is not installed. Installing..."
    sudo apt update && sudo apt install -y git
fi

if ! command -v docker &> /dev/null; then
    log_warn "Docker is not installed. Please install Docker first:"
    echo "  curl -fsSL https://get.docker.com | sudo sh"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    log_warn "Docker Compose is not installed. Installing..."
    sudo apt install -y docker-compose
fi

log_info "Prerequisites check passed!"
echo ""

# Step 2: Check PostgreSQL
log_step "Checking PostgreSQL..."
if ! command -v psql &> /dev/null; then
    log_error "PostgreSQL is not installed. Please install PostgreSQL first."
    echo "  sudo apt install -y postgresql postgresql-contrib"
    exit 1
fi

if ! sudo systemctl is-active --quiet postgresql; then
    log_error "PostgreSQL is not running. Start it with:"
    echo "  sudo systemctl start postgresql"
    exit 1
fi

log_info "PostgreSQL is running!"
echo ""

# Step 3: Create deployment directory
log_step "Creating deployment directory..."
if [ -d "$DEPLOY_DIR" ]; then
    log_warn "Deployment directory already exists: $DEPLOY_DIR"
    read -p "Do you want to remove it and redeploy? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        sudo rm -rf "$DEPLOY_DIR"
        log_info "Removed old deployment directory."
    else
        log_info "Keeping existing directory. Will run: git pull"
    fi
fi

if [ ! -d "$DEPLOY_DIR" ]; then
    sudo mkdir -p "$DEPLOY_DIR"
    sudo chown $USER:$USER "$DEPLOY_DIR"
    log_info "Created deployment directory: $DEPLOY_DIR"
fi
echo ""

# Step 4: Clone or update repository
log_step "Getting source code..."
if [ -d "$DEPLOY_DIR/.git" ]; then
    log_info "Repository exists. Pulling latest changes..."
    cd "$DEPLOY_DIR"
    git pull origin $BRANCH
else
    log_info "Cloning repository from: $REPO_URL"
    git clone -b $BRANCH --single-branch "$REPO_URL" "$DEPLOY_DIR"
    cd "$DEPLOY_DIR"
fi
echo ""

# Step 5: Check for .env file
log_step "Configuring environment variables..."
if [ ! -f "$DEPLOY_DIR/.env" ]; then
    log_info ".env file not found. Creating from template..."

    # Generate random passwords
    POSTGRES_PASSWORD=$(openssl rand -base64 32)
    JWT_SECRET=$(openssl rand -base64 64)

    # Create .env file
    cat > "$DEPLOY_DIR/.env" << EOF
POSTGRES_DB=his_project
POSTGRES_USER=his_user
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
JWT_SECRET=$JWT_SECRET
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
EOF

    chmod 600 "$DEPLOY_DIR/.env"

    log_warn "=========================================="
    log_warn "IMPORTANT! Save these credentials:"
    log_warn "=========================================="
    echo "Database Password: $POSTGRES_PASSWORD"
    echo "JWT Secret: $JWT_SECRET"
    log_warn "=========================================="
    echo ""
    read -p "Have you saved the credentials? (Y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]] && [ ! -z "$REPLY" ]; then
        log_error "Please save the credentials before continuing."
        exit 1
    fi
else
    log_info ".env file already exists. Skipping."
fi
echo ""

# Step 6: Check PostgreSQL database
log_step "Checking PostgreSQL database..."
DB_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='his_project'")

if [ "$DB_EXISTS" != "1" ]; then
    log_info "Database 'his_project' not found. Creating..."

    # Read password from .env
    POSTGRES_PASSWORD=$(grep POSTGRES_PASSWORD "$DEPLOY_DIR/.env" | cut -d '=' -f2)

    sudo -u postgres psql << EOF
CREATE DATABASE his_project;
CREATE USER his_user WITH ENCRYPTED PASSWORD '$POSTGRES_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE his_project TO his_user;
EOF
    log_info "Database and user created successfully!"
else
    log_info "Database 'his_project' already exists. Skipping."
fi
echo ""

# Step 7: Build Docker image
log_step "Building Docker image..."
cd "$DEPLOY_DIR"
docker-compose build
log_info "Docker image built successfully!"
echo ""

# Step 8: Stop existing containers (if any)
log_step "Stopping existing containers..."
cd "$DEPLOY_DIR"
docker-compose down 2>/dev/null || log_info "No existing containers to stop."
echo ""

# Step 9: Start containers
log_step "Starting containers..."
cd "$DEPLOY_DIR"
docker-compose up -d
log_info "Containers started!"
echo ""

# Step 10: Wait for application to be healthy
log_step "Waiting for application to start..."
sleep 10

# Check health status
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        log_info "Application is healthy!"
        break
    fi

    ATTEMPT=$((ATTEMPT + 1))
    log_info "Waiting... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    log_error "Application failed to start within expected time."
    log_error "Please check logs with: docker-compose logs -f"
    exit 1
fi
echo ""

# Success!
echo "=========================================="
log_info "Deployment completed successfully!"
echo "=========================================="
echo ""
echo "Application is running at: http://localhost:8080"
echo "Health check: http://localhost:8080/actuator/health"
echo ""
echo "Useful commands:"
echo "  View logs:    cd $DEPLOY_DIR && docker-compose logs -f"
echo "  Restart:      cd $DEPLOY_DIR && docker-compose restart"
echo "  Stop:         cd $DEPLOY_DIR && docker-compose down"
echo "  Update:       cd $DEPLOY_DIR && git pull && docker-compose up -d --build"
echo ""
echo "Next steps:"
echo "  1. Configure Nginx reverse proxy (see deploy/nginx-his.conf.example)"
echo "  2. Set up SSL certificate"
echo "  3. Configure backup script (see deploy/backup.sh.example)"
echo ""
