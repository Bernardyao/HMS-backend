# Plan: Implement Cashier/Charging Module

This plan outlines the implementation of the core charging and billing functionality for the HIS system.

## Phase 1: Foundation - Data Model & Enums [checkpoint: e780486]
Focus: Establishing the database schema and state definitions required for charging.

- [x] Task: Create Database Migration Scripts (901cda7)
    - Write SQL scripts to create `his_charge` and `his_charge_detail` tables.
    - Write SQL scripts to update `his_prescription` status comments/constraints (if applicable).
- [x] Task: Update `Prescription` status flow and create Enums (3e79fdc)
    - Add `PAID(5)` status to `Prescription` logic.
    - Ensure `REFUNDED(4)` status consistency.
    - Create `ChargeStatus` and `PaymentMethod` enums.
    - **Test:** Unit tests for Enum state transitions.
- [x] Task: Create `Charge` and `ChargeDetail` entities and repositories (c6a1c33)
    - Implement JPA entities.
    - Implement Repositories.
    - **Test:** Repository integration tests (CRUD).
- [x] Task: Conductor - User Manual Verification 'Phase 1: Foundation' (Protocol in workflow.md) (e780486)

## Phase 2: Core Business Logic - Charging & Mock Payment [checkpoint: 4e5c4b3]
Focus: Implementing the service layer for creating charges and processing payments.

- [x] Task: Implement `ChargeService.createCharge()` (e43c744)
    - Implement aggregation and validation logic.
    - **Test:** Unit tests for calculation and validation.
- [x] Task: Refactor `PrescriptionService` for Charging Integration (cc36860)
    - Extract `restoreInventoryOnly()` method.
    - Update `dispense()` to enforce `PAID(5)` status.
    - **Test:** Regression tests for Dispensing flow.
- [x] Task: Implement `ChargeService.processPayment()` with Mock Provider (bd33125)
    - Implement payment processing, idempotency, and status updates.
    - Implement Mock Payment simulation.
    - **Test:** Unit/Integration tests for Payment flow and Idempotency.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Core Business Logic' (Protocol in workflow.md) (4e5c4b3)

## Phase 3: Advanced Logic - Refunds & Reports [checkpoint: bf7274f]
Focus: Implementing refund handling and financial reporting.

- [x] Task: Implement `ChargeService.processRefund()` (0f8ae78)
    - Implement refund logic and inventory restoration integration.
    - **Test:** Integration tests for Refund flow (with and without inventory restore).
- [x] Task: Implement Daily Settlement Report (207eacb)
    - Implement statistics aggregation logic.
    - **Test:** Unit tests for statistics calculation.
- [x] Task: Conductor - User Manual Verification 'Phase 3: Advanced Logic' (Protocol in workflow.md) (bf7274f)

## Phase 4: API Layer & Security [checkpoint: 0696943]
Focus: Exposing functionality via REST endpoints and securing them.

- [x] Task: Implement `ChargeController` (93affdd)
    - Create REST endpoints.
    - Add Swagger annotations.
    - **Test:** Controller slice tests (MockMVC).
- [x] Task: Apply Role-Based Access Control (93affdd)
    - Secure endpoints (CASHIER/ADMIN).
    - **Test:** Security integration tests (Access Control).
- [x] Task: Conductor - User Manual Verification 'Phase 4: API Layer & Security' (Protocol in workflow.md) (0696943)

## Phase 5: Verification & Documentation
Focus: Ensuring quality and completeness.

- [ ] Task: End-to-End Integration Testing
    - Verify complete flow: Register -> Prescribe -> Charge -> Pay -> Dispense -> Refund.
- [ ] Task: Performance Testing
    - Basic load test for Charge Creation and Payment Processing.
- [ ] Task: Finalize Documentation
    - Update module documentation and API references.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Verification & Documentation' (Protocol in workflow.md)
