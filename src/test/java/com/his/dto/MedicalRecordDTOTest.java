package com.his.dto;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MedicalRecordDTO Validation Test
 */
@DisplayName("MedicalRecordDTO Validation Test")
class MedicalRecordDTOTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    @DisplayName("Valid DTO should pass validation")
    void testValidDto() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("Headache");
        dto.setPresentIllness("Started yesterday");
        dto.setDiagnosis("Migraine");
        dto.setDiagnosisCode("G43");
        dto.setStatus((short) 1);

        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid DTO should have no violations");
    }

    @Test
    @DisplayName("DTO with null registrationId should fail validation")
    void testNullRegistrationId() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setChiefComplaint("Headache");

        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "DTO with null registrationId should have violations");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("registrationId")),
                   "Should have violation for registrationId");
    }

    @Test
    @DisplayName("DTO with too long fields should fail validation")
    void testTooLongFields() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);

        // Exceed max lengths
        dto.setChiefComplaint("a".repeat(501)); // Max 500
        dto.setPresentIllness("a".repeat(2001)); // Max 2000
        dto.setPastHistory("a".repeat(2001)); // Max 2000
        dto.setPersonalHistory("a".repeat(1001)); // Max 1000
        dto.setFamilyHistory("a".repeat(1001)); // Max 1000
        dto.setPhysicalExam("a".repeat(2001)); // Max 2000
        dto.setAuxiliaryExam("a".repeat(2001)); // Max 2000
        dto.setDiagnosis("a".repeat(501)); // Max 500
        dto.setDiagnosisCode("a".repeat(51)); // Max 50
        dto.setTreatmentPlan("a".repeat(2001)); // Max 2000
        dto.setDoctorAdvice("a".repeat(1001)); // Max 1000

        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        Set<String> violatedFields = violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());

        assertTrue(violatedFields.contains("chiefComplaint"), "Should have violation for chiefComplaint");
        assertTrue(violatedFields.contains("presentIllness"), "Should have violation for presentIllness");
        assertTrue(violatedFields.contains("pastHistory"), "Should have violation for pastHistory");
        assertTrue(violatedFields.contains("personalHistory"), "Should have violation for personalHistory");
        assertTrue(violatedFields.contains("familyHistory"), "Should have violation for familyHistory");
        assertTrue(violatedFields.contains("physicalExam"), "Should have violation for physicalExam");
        assertTrue(violatedFields.contains("auxiliaryExam"), "Should have violation for auxiliaryExam");
        assertTrue(violatedFields.contains("diagnosis"), "Should have violation for diagnosis");
        assertTrue(violatedFields.contains("diagnosisCode"), "Should have violation for diagnosisCode");
        assertTrue(violatedFields.contains("treatmentPlan"), "Should have violation for treatmentPlan");
        assertTrue(violatedFields.contains("doctorAdvice"), "Should have violation for doctorAdvice");
    }

    @Test
    @DisplayName("DTO with invalid status should fail validation")
    void testStatusValidation() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);

        // Test status < 0
        dto.setStatus((short) -1);
        Set<ConstraintViolation<MedicalRecordDTO>> violationsLow = validator.validate(dto);
        assertTrue(violationsLow.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")),
                   "Should have violation for status < 0");

        // Test status > 2
        dto.setStatus((short) 3);
        Set<ConstraintViolation<MedicalRecordDTO>> violationsHigh = validator.validate(dto);
        assertTrue(violationsHigh.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")),
                   "Should have violation for status > 2");

        // Test valid status values (0, 1, 2)
        dto.setStatus((short) 0);
        assertTrue(validator.validate(dto).isEmpty(), "Status 0 should be valid");
        dto.setStatus((short) 1);
        assertTrue(validator.validate(dto).isEmpty(), "Status 1 should be valid");
        dto.setStatus((short) 2);
        assertTrue(validator.validate(dto).isEmpty(), "Status 2 should be valid");

        // Test null status (valid as it's optional)
        dto.setStatus(null);
        assertTrue(validator.validate(dto).isEmpty(), "Null status should be valid");
    }

    @Test
    @DisplayName("Empty strings should be valid for optional text fields")
    void testEmptyStrings() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);
        dto.setChiefComplaint("");
        dto.setPresentIllness("");
        dto.setPastHistory("");
        dto.setPersonalHistory("");
        dto.setFamilyHistory("");
        dto.setPhysicalExam("");
        dto.setAuxiliaryExam("");
        dto.setDiagnosis("");
        dto.setTreatmentPlan("");
        dto.setDoctorAdvice("");

        // diagnosisCode is explicitly NOT set to empty string here because
        // it has @Size(min=1), so empty string would be invalid.
        // See testDiagnosisCodeValidation for that case.
        dto.setDiagnosisCode(null);

        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Empty strings should be allowed for optional fields (except diagnosisCode)");
    }

    @Test
    @DisplayName("Empty string for diagnosisCode should be invalid")
    void testDiagnosisCodeValidation() {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setRegistrationId(1L);

        // Empty string (length 0) should fail min=1 constraint
        dto.setDiagnosisCode("");
        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("diagnosisCode")),
                   "Should have violation for empty diagnosisCode");

        // Valid code
        dto.setDiagnosisCode("A01");
        assertTrue(validator.validate(dto).isEmpty(), "Valid diagnosisCode should pass");
    }
}
