# 🏥 Hospital Information System (HIS) - 医院信息管理系统

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)](https://reactjs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## 📖 项目简介 (Introduction)

**HIS (Hospital Information System)** 是一套企业级标准的现代化全栈医疗信息管理解决方案。该系统基于 **Spring Boot 3** 后端与 **React 18** 前端构建，旨在数字化并优化复杂的医院业务流程——从患者挂号、门诊就诊、电子病历书写，到处方开具、收费结算及药房发药，实现了医疗服务的全流程闭环管理。

本项目严格遵循 **基于角色的访问控制 (RBAC)** 模型，为医生、护士、药师、收费员和系统管理员提供了安全、独立且专属的操作界面。

---

## 🏗️ 系统架构 (System Architecture)

![System Architecture](doc/img/architecture.png)

> 系统采用前后端分离架构。后端提供基于 JWT 安全认证的 RESTful API，前端提供现代化的单页应用 (SPA) 体验。

---

## ✨ 核心功能 (Key Features)

### 🔐 安全与权限 (Security & RBAC)
*   **精细化权限控制**：支持 Admin (管理员), Doctor (医生), Nurse (护士), Cashier (收费员), Pharmacist (药师) 五种角色，实现严格的接口级与视图级权限隔离。
*   **安全认证**：基于 **Spring Security 6** 和 **JWT** 实现无状态认证机制。

### 🏥 临床与运营模块

#### 1. 门诊挂号 (护士工作站)
*   **患者建档**：快速建立患者档案。
*   **智能分诊**：支持初诊、复诊及急诊挂号流程。
*   **级联选择**：挂号时支持科室与医生的动态级联选择。

#### 2. 医生工作站 (Doctor Workstation) 👨‍⚕️
*   **混合队列视图**：支持切换 **个人候诊队列** (指定给该医生的患者) 和 **科室候诊队列** (科室公共池)。
*   **电子病历 (EMR)**：标准化的电子病历书写与管理。
*   **智能处方**：开具处方时实时校验药品库存，防止超售。

#### 3. 药房管理 (Pharmacy Management) 💊
*   **发药流程**：严格的业务逻辑控制，仅当处方完成缴费后方可发药。
*   **库存管控**：药品库存预警与退药流程处理。

#### 4. 收费管理 (Financials) 💰
*   **交易处理**：收费员专属界面，处理处方缴费与退费业务，生成交易记录。

---

## 🛠️ 技术栈 (Tech Stack)

### 后端 (Backend)
| 组件 | 技术 | 说明 |
| :--- | :--- | :--- |
| **开发语言** | Java 17+ | 使用 Record, Sealed Classes 等现代 Java 特性 |
| **核心框架** | Spring Boot 3.x | 应用程序核心框架 |
| **安全框架** | Spring Security 6 | 认证与授权 (RBAC) |
| **持久层** | Spring Data JPA | 数据库抽象与 Repository 模式 |
| **数据库** | PostgreSQL | 关系型数据库存储 |
| **API 文档** | Knife4j (Swagger 3) | 接口文档与在线调试 |
| **构建工具** | Gradle | 依赖管理与构建 |

### 前端 (Frontend)
| 组件 | 技术 | 说明 |
| :--- | :--- | :--- |
| **框架** | React 18 | 函数式组件与 Hooks |
| **构建工具** | Vite | 下一代前端构建工具，极速冷启动 |
| **语言** | TypeScript | 类型安全的开发体验 |
| **UI 组件库** | Ant Design | 企业级 UI 设计体系 |
| **HTTP 客户端** | Axios | 基于 Promise 的网络请求库 |

### 运维与工具 (DevOps & Tools)
*   **内网穿透**：`cpolar` (用于远程演示/测试)
*   **包管理器**：`npm` / `yarn`

---

## 📸 界面截图 (UI Snapshots)

| 挂号页面 | 医生工作站 |
| :---: | :---: |
| ![Registration Page](doc/img/registration_mockup.png)<br>_护士挂号看板_ | ![Doctor View](doc/img/doctor_mockup.png)<br>_电子病历与处方界面_ |

---

## 🚀 快速开始 (Getting Started)

按照以下步骤在本地搭建开发环境。

### 前置要求
*   **JDK 17** 或更高版本
*   **Node.js 18+** (推荐 LTS 版本)
*   **PostgreSQL 14+**

### 1. 数据库初始化
创建一个名为 `his_project` 的 PostgreSQL 数据库。
系统配置了 `ddl-auto: update`，首次运行时会自动创建表结构。若需导入初始数据（如测试用户、基础药品库），请执行：

```bash
# 请确保处于项目根目录
psql -U postgres -d his_project -f sql/test_data_sysuser.sql
psql -U postgres -d his_project -f sql/his_design_bigserial.sql
```

### 2. 后端启动
进入项目根目录并运行：

```bash
# 运行测试确保环境正常
./gradlew test

# 启动 Spring Boot 应用
./gradlew bootRun
```
*后端服务默认运行在 `8080` 端口。*

### 3. 前端启动
进入前端项目目录（假设为 `his-frontend`）：

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```
*前端页面默认运行在 `5173` 端口 (Vite 默认)。*

---

## ⚙️ 关键配置 (Configuration)

关键配置项位于 `src/main/resources/application.yml` 或各环境配置文件中。

| 属性配置 | 说明 | 默认值 |
| :--- | :--- | :--- |
| `spring.datasource.url` | 数据库连接 URL | `jdbc:postgresql://...` |
| `jwt.secret` | Token 签名密钥 | *(见 application-common.yml)* |
| `jwt.expiration` | Token 有效期 | `86400` (24 小时) |

---

## 📚 API 文档 (API Documentation)

后端服务启动后，可访问 Knife4j 在线接口文档进行调试：

**访问地址：** `http://localhost:8080/doc.html`

> 💡 **提示：** 您可以使用 `admin` 账号登录获取 Token，然后在文档页面的 "Authorize" 功能中设置 Token，以便调试受保护的接口。

---

## 🤝 贡献指南 (Contributing)

1.  Fork 本仓库。
2.  创建您的特性分支 (`git checkout -b feature/AmazingFeature`)。
3.  提交您的更改 (`git commit -m 'Add some AmazingFeature'`)。
4.  推送到分支 (`git push origin feature/AmazingFeature`)。
5.  提交 Pull Request。

---
© 2025 HIS Development Team. All rights reserved.
