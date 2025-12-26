# Track Plan: Pharmacist Workstation (Backend)

## Phase 1: Core Dispensing Logic
- [x] Task: Implement `getPendingDispenseList` in `PrescriptionService`. 17f8ca1
- [x] Task: Implement `dispense(Long prescriptionId)` in `PrescriptionService`. 17f8ca1
- [x] Task: Unit Tests for Dispensing Logic. 17f8ca1
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Core Dispensing Logic'

## Phase 2: Medicine Return & Inventory
- [ ] Task: Implement `returnMedicine(Long prescriptionId, String reason)` in `PrescriptionService`.
    - Logic: Update status to `RETURNED`, record reason, and restore medicine stock.
- [ ] Task: Implement `updateStock(Long medicineId, Integer quantity, String reason)` in `MedicineService`.
    - Logic: Manually adjust stock and log the change (if audit logging is implemented).
- [ ] Task: Unit Tests for Medicine Return and Stock Update.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Medicine Return & Inventory'

## Phase 3: Statistics & Reporting
- [ ] Task: Implement `getTodayStatistics` for Pharmacists.
    - Metrics: Total prescriptions dispensed today, total items, total amount.
- [ ] Task: Enhance `PharmacistMedicineController` and `PharmacistPrescriptionController` to use new service methods.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Statistics & Reporting'

## Phase 4: Integration & Documentation
- [ ] Task: Comprehensive Integration Tests for Pharmacist Workstation.
- [ ] Task: Update API documentation (Knife4j annotations).
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Integration & Documentation'
