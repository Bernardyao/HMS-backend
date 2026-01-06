package com.his.common;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.his.exception.BusinessException;
import com.his.monitoring.AlertService;
import com.his.test.base.BaseServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 覆盖率测试
 * <p>
 * 专注于提升异常处理分支覆盖率，测试各种边界场景
 * </p>
 *
 * @author HIS 开发团队
 * @since 1.0
 */
@DisplayName("GlobalExceptionHandler 覆盖率测试")
class GlobalExceptionHandlerCoverageTest extends BaseServiceTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    // ==================== 单元测试部分 ====================

    @Test
    @DisplayName("应该处理多个字段的校验错误")
    void handleValidationException_MultipleFields() {
        // Given - 模拟多个字段校验失败
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.FieldError fieldError1 = new org.springframework.validation.FieldError(
                "objectName", "field1", "rejectedValue", false,
                new String[]{"errorCode1"}, null, "错误消息1"
        );
        org.springframework.validation.FieldError fieldError2 = new org.springframework.validation.FieldError(
                "objectName", "field2", "rejectedValue", false,
                new String[]{"errorCode2"}, null, "错误消息2"
        );

        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError1, fieldError2));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleValidationException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("field1"));
        assertTrue(response.getBody().getMessage().contains("field2"));
    }

    @Test
    @DisplayName("应该处理参数类型不匹配异常（泛型类型）")
    void handleTypeMismatchException_GenericType() {
        // Given - 参数类型不匹配
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("testParam");
        when(ex.getRequiredType()).thenAnswer(invocation -> Long.class);

        request.setRequestURI("/api/registrations");

        // When
        ResponseEntity<Result<Void>> response = handler.handleTypeMismatchException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("testParam"));
        assertTrue(response.getBody().getMessage().contains("Long"));
    }

    @Test
    @DisplayName("应该处理未认证的访问拒绝异常")
    void handleAccessDeniedException_NotAuthenticated() {
        // Given - 未认证用户访问受保护资源
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        request.setRequestURI("/api/doctor/prescriptions");
        request.setMethod("POST");

        // When
        ResponseEntity<Result<Void>> response = handler.handleAccessDeniedException(ex, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getCode());
    }

    @Test
    @DisplayName("应该处理HTTP方法不支持异常（显示支持的方法）")
    void handleMethodNotSupportedException_SupportedMethods() {
        // Given - 不支持的HTTP方法
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMessage()).thenReturn("Method not supported");
        when(ex.getSupportedHttpMethods()).thenReturn(Collections.singleton(org.springframework.http.HttpMethod.GET));

        request.setMethod("DELETE");
        request.setRequestURI("/api/registrations/123");

        // When
        ResponseEntity<Result<Void>> response = handler.handleMethodNotSupportedException(ex, request);

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("HTTP方法不支持"));
    }

    @Test
    @DisplayName("应该处理消息为null的通用异常")
    void handleGeneralException_NullMessage() {
        // Given - 异常消息为null
        Exception ex = new Exception((String) null);

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleGeneralException(ex, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        // Message should be the generic error message since log.isDebugEnabled() is false in test
        assertTrue(response.getBody().getMessage().contains("系统内部错误")
                || response.getBody().getMessage().contains("未知"));
        verify(alertService).recordError(anyString(), any());
    }

    @Test
    @DisplayName("应该处理超长异常消息")
    void handleGeneralException_VeryLongMessage() {
        // Given - 超长异常消息（1000字符）
        String longMessage = "A".repeat(1000);
        Exception ex = new Exception(longMessage);

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleGeneralException(ex, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
        verify(alertService).recordError(anyString(), any());
    }

    @Test
    @DisplayName("应该处理空指针异常并提取堆栈信息")
    void handleNullPointerException_DeepStack() {
        // Given - 空指针异常，深度调用栈
        NullPointerException ex = new NullPointerException("Null pointer");
        ex.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.his.service.DoctorService", "prescribe",
                        "DoctorService.java", 123),
                new StackTraceElement("com.his.controller.DoctorController", "createPrescription",
                        "DoctorController.java", 45)
        });

        request.setRequestURI("/api/doctor/prescriptions");

        // When
        ResponseEntity<Result<Void>> response = handler.handleNullPointerException(ex, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("空指针异常"));
        assertTrue(response.getBody().getMessage().contains("DoctorService"));
    }

    @Test
    @DisplayName("应该处理Content-Type协商失败异常")
    void handleMediaTypeNotAcceptableException() {
        // Given - Content-Type协商失败
        HttpMediaTypeNotAcceptableException ex = mock(HttpMediaTypeNotAcceptableException.class);
        when(ex.getMessage()).thenReturn("Could not find acceptable representation");
        when(ex.getSupportedMediaTypes()).thenReturn(
                Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON)
        );

        request.setRequestURI("/api/registrations");
        request.addHeader("Accept", "application/xml");

        // When
        ResponseEntity<Result<Void>> response = handler.handleMediaTypeNotAcceptableException(ex, request);

        // Then
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Content-Type"));
    }

    @Test
    @DisplayName("应该处理缺少请求头异常（关键请求头）")
    void handleMissingRequestHeaderException_CriticalHeaders() {
        // Given - 缺少Authorization请求头
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("Authorization");

        request.setRequestURI("/api/doctor/prescriptions");

        // When
        ResponseEntity<Result<Void>> response = handler.handleMissingRequestHeaderException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Authorization"));
        assertTrue(response.getBody().getMessage().contains("请求头"));
    }

    @Test
    @DisplayName("应该处理缺少请求参数异常")
    void handleMissingServletRequestParameterException() {
        // Given - 缺少必填请求参数
        MissingServletRequestParameterException ex = mock(MissingServletRequestParameterException.class);
        when(ex.getParameterName()).thenReturn("deptId");

        request.setRequestURI("/api/registrations");
        request.setMethod("POST");

        // When
        ResponseEntity<Result<Void>> response = handler.handleMissingServletRequestParameterException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("deptId"));
        assertTrue(response.getBody().getMessage().contains("参数"));
    }

    @Test
    @DisplayName("应该处理认证失败异常")
    void handleAuthenticationException() {
        // Given - 认证失败
        AuthenticationException ex = mock(AuthenticationException.class);
        when(ex.getMessage()).thenReturn("Bad credentials");

        request.setRequestURI("/api/auth/login");

        // When
        ResponseEntity<Result<Void>> response = handler.handleAuthenticationException(ex, request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("认证失败"));
    }

    @Test
    @DisplayName("应该处理数据完整性约束违例异常（通用约束）")
    void handleDataIntegrityViolationException_GenericConstraint() {
        // Given - 通用数据约束违例（非特定约束）
        String errorMessage = "ERROR: value too long for type character varying(50)";
        DataIntegrityViolationException ex = new DataIntegrityViolationException(errorMessage);

        request.setRequestURI("/api/registrations");

        // When
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolationException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BusinessException.CONSTRAINT_VIOLATION.getCode(), response.getBody().getCode());
        assertEquals(BusinessException.CONSTRAINT_VIOLATION.getMessage(), response.getBody().getMessage());
    }

    // ==================== 边界条件和特殊场景 ====================

    @Test
    @DisplayName("应该处理类型不匹配异常（未知类型）")
    void handleTypeMismatchException_UnknownType() {
        // Given - 类型不匹配，但目标类型未知（null）
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("unknownParam");
        when(ex.getRequiredType()).thenReturn(null);

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleTypeMismatchException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("未知"));
    }

    @Test
    @DisplayName("应该处理消息为空的通用异常")
    void handleGeneralException_EmptyMessage() {
        // Given - 异常消息为空字符串
        Exception ex = new Exception("");

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleGeneralException(ex, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        verify(alertService).recordError(anyString(), any());
    }

    @Test
    @DisplayName("应该处理空指针异常（无堆栈信息）")
    void handleNullPointerException_NoStackTrace() {
        // Given - 空指针异常，无堆栈信息
        NullPointerException ex = new NullPointerException("Null pointer");
        ex.setStackTrace(new StackTraceElement[0]);

        request.setRequestURI("/api/test");

        // When
        ResponseEntity<Result<Void>> response = handler.handleNullPointerException(ex, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("空指针异常"));
    }

    @Test
    @DisplayName("应该处理非法参数异常")
    void handleIllegalArgumentException() {
        // Given - 非法参数异常
        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter value");

        request.setRequestURI("/api/registrations");

        // When
        ResponseEntity<Result<Void>> response = handler.handleIllegalArgumentException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid parameter value"));
    }

    @Test
    @DisplayName("应该处理非法状态异常")
    void handleIllegalStateException() {
        // Given - 非法状态异常
        IllegalStateException ex = new IllegalStateException("Invalid state transition");

        request.setRequestURI("/api/registrations/123/cancel");

        // When
        ResponseEntity<Result<Void>> response = handler.handleIllegalStateException(ex, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid state transition"));
    }
}
