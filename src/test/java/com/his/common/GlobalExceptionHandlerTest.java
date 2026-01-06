package com.his.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.his.exception.BusinessException;
import com.his.monitoring.AlertService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 * 专注于测试新增的数据库约束违例异常处理
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void testHandleDataIntegrityViolationException_DuplicateRegistration() {
        // PostgreSQL 格式的重复挂号约束违例
        String errorMessage = "ERROR: duplicate key value violates unique constraint \"uk_his_registration_patient_doctor_date_status\"";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/registrations");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.DUPLICATE_REGISTRATION.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.DUPLICATE_REGISTRATION.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolationException_DuplicateTransactionNo() {
        // MySQL 格式的重复交易号约束违例
        String errorMessage = "Duplicate entry 'TXN123' for key 'uk_his_charge_transaction_no'";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/charges/payment");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.DUPLICATE_TRANSACTION_NO.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.DUPLICATE_TRANSACTION_NO.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolationException_ForeignKeyViolation() {
        String errorMessage = "ERROR: insert or update on table \"his_charge\" violates foreign key constraint \"fk_his_charge_patient\"";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/charges");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.FOREIGN_KEY_VIOLATION.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.FOREIGN_KEY_VIOLATION.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolationException_NotNullViolation() {
        String errorMessage = "ERROR: null value in column \"charge_no\" violates not-null constraint";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/charges");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.NOT_NULL_VIOLATION.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.NOT_NULL_VIOLATION.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolationException_GenericConstraint() {
        String errorMessage = "ERROR: value too long for type character varying(50)";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/charges");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.CONSTRAINT_VIOLATION.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.CONSTRAINT_VIOLATION.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolationException_NullMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException((String) null);

        request.setRequestURI("/api/charges");
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.CONSTRAINT_VIOLATION.getCode(), response.getBody().getCode());
    }
}
