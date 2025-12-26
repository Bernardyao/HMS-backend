# Track Plan: Pharmacist Workstation (Backend)

## Phase 1: Core Dispensing Logic
- [x] Task: Implement `getPendingDispenseList` in `PrescriptionService`. 17f8ca1
- [x] Task: Implement `dispense(Long prescriptionId)` in `PrescriptionService`. 17f8ca1
- [x] Task: Unit Tests for Dispensing Logic. 17f8ca1
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Dispensing Logic'

## Phase 2: Medicine Return & Inventory
- [x] Task: Implement `returnMedicine(Long prescriptionId, String reason)` in `PrescriptionService`. 4aad0da
    - Logic: Update status to `RETURNED`, record reason, and restore medicine stock.
- [x] Task: Implement `updateStock(Long medicineId, Integer quantity, String reason)` in `MedicineService`. 0bed442
    - Logic: Manually adjust stock and log the change (if audit logging is implemented).
- [x] Task: Unit Tests for Medicine Return and Stock Update. 4aad0da, 0bed442
- [x] Task: Conductor - User Manual Verification 'Phase 2: Medicine Return & Inventory' [checkpoint: 7c6aa3c]

## Phase 3: Statistics & Reporting
- [x] Task: Implement `getTodayStatistics` for Pharmacists. 8f2cc0e
    - Metrics: Total prescriptions dispensed today, total items, total amount.
- [x] Task: Enhance `PharmacistMedicineController` and `PharmacistPrescriptionController` to use new service methods. a4f3102, b52758e
- [x] Task: Conductor - User Manual Verification 'Phase 3: Statistics & Reporting' [checkpoint: cecda66]

## Phase 4: Integration & Documentation
- [ ] Task: Comprehensive Integration Tests for Pharmacist Workstation.
- [ ] Task: Update API documentation (Knife4j annotations).
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Integration & Documentation'
