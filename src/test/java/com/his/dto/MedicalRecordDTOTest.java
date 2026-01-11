package com.his.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MedicalRecordDTO Validation Test
 */
@DisplayName("MedicalRecordDTO Validation Test")
class MedicalRecordDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
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
        dto.setChiefComplaint("a".repeat(501)); // Max 500
        dto.setPresentIllness("a".repeat(2001)); // Max 2000
        dto.setDiagnosis("a".repeat(501)); // Max 500

        Set<ConstraintViolation<MedicalRecordDTO>> violations = validator.validate(dto);
        assertEquals(3, violations.size(), "Should have 3 violations");
    }
}
