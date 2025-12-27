# Plan: Implement Cashier/Charging Module

This plan outlines the implementation of the core charging and billing functionality for the HIS system.

## Phase 1: Foundation - Data Model & Enums
Focus: Establishing the database schema and state definitions required for charging.

- [ ] Task: Create Database Migration Scripts
    - Write SQL scripts to create `his_charge` and `his_charge_detail` tables.
    - Write SQL scripts to update `his_prescription` status comments/constraints (if applicable).
- [ ] Task: Update `Prescription` status flow and create Enums
    - Add `PAID(5)` status to `Prescription` logic.
    - Ensure `REFUNDED(4)` status consistency.
    - Create `ChargeStatus` and `PaymentMethod` enums.
    - **Test:** Unit tests for Enum state transitions.
- [ ] Task: Create `Charge` and `ChargeDetail` entities and repositories
    - Implement JPA entities.
    - Implement Repositories.
    - **Test:** Repository integration tests (CRUD).
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Foundation' (Protocol in workflow.md)

## Phase 2: Core Business Logic - Charging & Mock Payment
Focus: Implementing the service layer for creating charges and processing payments.

- [ ] Task: Implement `ChargeService.createCharge()`
    - Implement aggregation and validation logic.
    - **Test:** Unit tests for calculation and validation.
- [ ] Task: Refactor `PrescriptionService` for Charging Integration
    - Extract `restoreInventoryOnly()` method.
    - Update `dispense()` to enforce `PAID(5)` status.
    - **Test:** Regression tests for Dispensing flow.
- [ ] Task: Implement `ChargeService.processPayment()` with Mock Provider
    - Implement payment processing, idempotency, and status updates.
    - Implement Mock Payment simulation.
    - **Test:** Unit/Integration tests for Payment flow and Idempotency.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Core Business Logic' (Protocol in workflow.md)

## Phase 3: Advanced Logic - Refunds & Reports
Focus: Implementing refund handling and financial reporting.

- [ ] Task: Implement `ChargeService.processRefund()`
    - Implement refund logic and inventory restoration integration.
    - **Test:** Integration tests for Refund flow (with and without inventory restore).
- [ ] Task: Implement Daily Settlement Report
    - Implement statistics aggregation logic.
    - **Test:** Unit tests for statistics calculation.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Advanced Logic' (Protocol in workflow.md)

## Phase 4: API Layer & Security
Focus: Exposing functionality via REST endpoints and securing them.

- [ ] Task: Implement `ChargeController`
    - Create REST endpoints.
    - Add Swagger annotations.
    - **Test:** Controller slice tests (MockMVC).
- [ ] Task: Apply Role-Based Access Control
    - Secure endpoints (CASHIER/ADMIN).
    - **Test:** Security integration tests (Access Control).
- [ ] Task: Conductor - User Manual Verification 'Phase 4: API Layer & Security' (Protocol in workflow.md)

## Phase 5: Verification & Documentation
Focus: Ensuring quality and completeness.

- [ ] Task: End-to-End Integration Testing
    - Verify complete flow: Register -> Prescribe -> Charge -> Pay -> Dispense -> Refund.
- [ ] Task: Performance Testing
    - Basic load test for Charge Creation and Payment Processing.
- [ ] Task: Finalize Documentation
    - Update module documentation and API references.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Verification & Documentation' (Protocol in workflow.md)
