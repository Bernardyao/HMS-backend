# Track Specification: Implement Cashier/Charging Module

## 1. Overview
This track implements the core Cashier/Charging module for the HIS system. It bridges the gap between the Doctor/Prescription workflow and the Pharmacy/Dispensing workflow by introducing a financial transaction layer. The module will handle charge slip creation, payment processing (via a mock provider), refund handling with inventory restoration, and daily settlement reporting.

## 2. Goals
- **Enable Financial Transactions:** Allow cashiers to create charge slips for registrations and prescriptions.
- **Payment Processing:** Implement a robust payment flow supporting Cash, Card, and Mobile methods (using a mock provider for non-cash).
- **Inventory Integration:** Ensure refunds automatically trigger inventory restoration by reusing/refactoring existing logic in `PrescriptionService`.
- **Data Integrity:** Guarantee 100% idempotency for payments and atomic transactions for all financial operations.
- **Reporting:** Provide a daily settlement API for financial tracking.
- **State Management:** Explicitly track the "Paid" state in Prescriptions to control dispensing eligibility.

## 3. Functional Requirements

### 3.1 Charge Management
- **Create Charge Slip:**
  - Input: Registration ID (mandatory), Prescription IDs (optional).
  - Validation: Registration must be 'COMPLETED'; Prescriptions must be 'REVIEWED'.
  - Calculation: Sum of Registration Fee + Prescription Costs.
  - Output: Unique Charge ID and Charge Number (CHG...).
- **Query Charges:**
  - Search by Charge No, Patient ID, Date Range, Status.

### 3.2 Payment Processing
- **Process Payment:**
  - Input: Charge ID, Payment Method (CASH/CARD/MOBILE), Transaction No (for non-cash), Amount.
  - Logic:
    - Validate amount matches total (tolerance ±0.01).
    - **Mock Payment:** Simulate success for CARD/MOBILE.
    - **Idempotency:** Prevent duplicate payments using `transaction_no`.
    - Update Charge Status to `PAID`.
    - **Update Prescription Status:** Change status from `REVIEWED (2)` to `PAID (5)`.

### 3.3 Refund Processing
- **Process Refund:**
  - Input: Charge ID, Refund Reason.
  - Logic:
    - Verify Status is `PAID`.
    - Update Charge Status to `REFUNDED`.
    - **Conditional Logic:**
      - If Prescription Status is `PAID (5)` -> Revert to `REVIEWED (2)`. (No stock change)
      - If Prescription Status is `DISPENSED (3)` -> Change to `REFUNDED (4)` AND call `PrescriptionService.restoreInventoryOnly()`.

### 3.4 Settlement
- **Daily Report API:**
  - Return JSON data: Total charges, total amount, breakdown by payment method, refund stats, net collection.

## 4. Technical Implementation Strategy

### 4.1 Data Model Changes
- **New Tables:**
  - `his_charge` (Master): Stores totals, status, payment details.
  - `his_charge_detail` (Detail): Stores line items.
- **State Machine (Prescription):**
  - Current: `0=Draft -> 1=Issued -> 2=Reviewed -> 3=Dispensed -> 4=Returned`
  - **New Flow:** `0=Draft -> 1=Issued -> 2=Reviewed -> 5=PAID (New) -> 3=Dispensed -> 4=REFUNDED (Renamed from Returned)`
  - **Note:** Dispensing logic must be updated to only allow dispensing if status is `PAID (5)`.

### 4.2 API Design
- `POST /api/cashier/charges`: Create.
- `GET /api/cashier/charges/{id}`: Read.
- `GET /api/cashier/charges`: List/Search.
- `POST /api/cashier/charges/{id}/pay`: Pay.
- `POST /api/cashier/charges/{id}/refund`: Refund.
- `GET /api/cashier/charges/statistics/daily`: Settlement Report.

### 4.3 Integration Points
- **PrescriptionService:**
  - **Refactor:** Extract `restoreInventoryOnly()` method for reuse.
  - **Update Logic:** Modify `dispense()` to require status `PAID (5)` instead of `REVIEWED (2)`.
  - **Update Logic:** Add logic to handle status transitions to `PAID (5)` and `REFUNDED (4)`.

## 5. Error Codes (Reference)
| Code | Message |
|------|---------|
| 8001 | Registration ID is required |
| 8005 | Can only charge reviewed prescriptions |
| 8021 | Charge already paid |
| 8023 | Duplicate transaction number |
| 8031 | Can only refund paid charges |
| ...  | (See requirements doc for full list) |

## 6. Testing Strategy
- **Unit Tests:** Service layer logic (creation, payment calculation, state transitions).
- **Integration Tests:** Full flow (Create -> Pay -> Dispense -> Refund).
- **Concurrency Tests:** Simulate simultaneous payment attempts for same charge.

## 7. Mock Payment Behavior
- **Configuration:** No external config required; logic is embedded in Service.
- **Behavior:**
  - If `paymentMethod` is CARD/MOBILE, `transactionNo` is required.
  - System logs "Simulating payment verification with provider..."
  - Always returns success if inputs are valid.

## 8. Out of Scope
- Integration with real payment gateways.
- Physical receipt printing.
- Frontend UI development.
