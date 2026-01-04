package com.his.common;

import com.his.exception.BusinessException;
import com.his.exception.BusinessRuntimeException;
import com.his.log.utils.LogUtils;
import com.his.monitoring.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 *
 * <p>统一捕获和处理Controller层抛出的所有异常，将异常转换为标准的Result响应格式</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>统一异常处理</b>：集中处理所有Controller层的异常，避免重复的try-catch代码</li>
 *   <li><b>标准响应格式</b>：将所有异常转换为Result格式，保持API响应的一致性</li>
 *   <li><b>详细日志记录</b>：记录异常堆栈和请求信息，便于问题排查和审计</li>
 *   <li><b>敏感信息保护</b>：避免将详细的异常信息直接暴露给客户端</li>
 *   <li><b>友好错误提示</b>：将技术异常转换为用户可理解的错误消息</li>
 * </ul>
 *
 * <h3>处理范围</h3>
 * <p>仅处理com.his.controller包下的Controller异常，避免干扰Swagger等框架路径</p>
 *
 * <h3>异常分类</h3>
 * <table border="1">
 *   <tr><th>异常类型</th><th>HTTP状态码</th><th>使用场景</th></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>参数校验失败（@Valid）</td></tr>
 *   <tr><td>IllegalArgumentException</td><td>400</td><td>业务参数错误</td></tr>
 *   <tr><td>IllegalStateException</td><td>400</td><td>业务状态错误</td></tr>
 *   <tr><td>AccessDeniedException</td><td>403</td><td>权限不足</td></tr>
 *   <tr><td>AuthenticationException</td><td>401</td><td>认证失败</td></tr>
 *   <tr><td>NoHandlerFoundException</td><td>404</td><td>接口不存在</td></tr>
 *   <tr><td>DataIntegrityViolationException</td><td>400</td><td>数据库约束违例</td></tr>
 *   <tr><td>Exception</td><td>500</td><td>未捕获的系统异常</td></tr>
 * </table>
 *
 * <h3>响应格式</h3>
 * <pre>
 * {
 *   "code": 400,
 *   "message": "用户友好的错误描述",
 *   "data": null,
 *   "timestamp": 1701417600000
 * }
 * </pre>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>@RestControllerAdvice</b>：声明全局异常处理器，拦截所有Controller异常</li>
 *   <li><b>basePackages限制</b>：仅处理controller包，避免干扰框架（如Swagger）</li>
 *   <li><b>异常优先级</b>：最具体的异常处理器优先匹配（子类优先于父类）</li>
 *   <li><b>生产环境保护</b>：生产环境不返回详细堆栈，避免泄露系统内部信息</li>
 *   <li><b>开发环境友好</b>：开发环境返回更多调试信息（通过isDebugEnabled判断）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.his.controller")
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AlertService alertService;

    // 限制异常处理器只处理controller包，避免干扰Swagger等框架路径

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");

        LogUtils.logValidationError("参数", errorMsg, request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.badRequest(errorMsg));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        
        String errorMsg = String.format("参数'%s'类型错误，期望类型: %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");

        LogUtils.logValidationError("参数", errorMsg, request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(Result.badRequest(errorMsg));
    }

    /**
     * 处理业务异常（参数错误等）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request) {

        LogUtils.logValidationError("业务参数", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.badRequest(e.getMessage()));
    }

    /**
     * 处理业务状态异常（状态流转错误等）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalStateException(
            IllegalStateException e,
            HttpServletRequest request) {

        LogUtils.logValidationError("业务状态", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.badRequest(e.getMessage()));
    }

    /**
     * 处理业务运行时异常
     */
    @ExceptionHandler(BusinessRuntimeException.class)
    public ResponseEntity<Result<Void>> handleBusinessRuntimeException(
            BusinessRuntimeException e,
            HttpServletRequest request) {

        log.warn("业务异常: [{}] {}, 请求路径: {}",
                e.getCode(), e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.badRequest(e.getMessage()));
    }

    /**
     * 处理权限不足异常（统一处理Spring Security的AccessDeniedException）
     */
    @ExceptionHandler({
        AccessDeniedException.class,
        org.springframework.security.access.AccessDeniedException.class
    })
    public ResponseEntity<Result<Void>> handleAccessDeniedException(
            Exception e,
            HttpServletRequest request) {
        
        log.warn("权限不足: {}, 请求路径: {}, 用户可能未登录或角色不匹配", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Result.error(403, "访问被拒绝：" + e.getMessage() + "。请先登录并确保具有相应权限。"));
    }

    /**
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        
        log.warn("HTTP方法不支持: {} {}, 支持的方法: {}", 
                request.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error("HTTP方法不支持：" + e.getMessage()));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFoundException(
            NoHandlerFoundException e,
            HttpServletRequest request) {
        
        log.warn("资源未找到: {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error("资源未找到：" + request.getRequestURI()));
    }

    /**
     * 处理Content-Type不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotAcceptableException(
            HttpMediaTypeNotAcceptableException e,
            HttpServletRequest request) {
        
        log.error("Content-Type协商失败: {}, 请求路径: {}, Accept: {}", 
                e.getMessage(), request.getRequestURI(), request.getHeader("Accept"));
        
        // 这是关键错误 - 可能导致返回错误的Content-Type
        log.error("⚠⚠⚠ 警告：Content-Type协商失败可能导致返回错误的响应格式（如Knife4j.txt）");
        log.error("支持的MediaTypes: {}", e.getSupportedMediaTypes());
        
        return ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(Result.error("不支持的Content-Type：" + e.getMessage()));
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException e,
            HttpServletRequest request) {
        
        log.warn("认证失败: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "认证失败：" + e.getMessage() + "。请检查用户名和密码。"));
    }

        /**
         * 处理必填请求头缺失（例如 Authorization）
         */
        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<Result<Void>> handleMissingRequestHeaderException(
                        MissingRequestHeaderException e,
                        HttpServletRequest request) {

                log.warn("缺少请求头: {}, 请求路径: {}", e.getHeaderName(), request.getRequestURI());
                return ResponseEntity
                                .badRequest()
                                .body(Result.badRequest("缺少请求头：" + e.getHeaderName()));
        }

        /**
         * 处理必填请求参数缺失（例如 deptId）
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(
                        MissingServletRequestParameterException e,
                        HttpServletRequest request) {

                log.warn("缺少请求参数: {}, 请求路径: {}", e.getParameterName(), request.getRequestURI());
                return ResponseEntity
                                .badRequest()
                                .body(Result.badRequest("缺少请求参数：" + e.getParameterName()));
        }

    /**
     * 处理数据完整性约束违例异常
     * 包括唯一约束、外键约束、非空约束等
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException e,
            HttpServletRequest request) {

        String constraintName = extractConstraintName(e);
        BusinessException errorCode = getErrorCodeForConstraint(constraintName, e);
        String userMessage = errorCode.getMessage();

        log.warn("数据完整性约束违例: constraint={}, 请求路径: {}, 详细消息: {}",
                constraintName, request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(Result.error(errorCode.getCode(), userMessage));
    }

    /**
     * 从异常中提取约束名称
     * 支持PostgreSQL和MySQL的错误消息格式
     */
    private String extractConstraintName(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message == null) {
            return "unknown";
        }

        // PostgreSQL格式: "duplicate key value violates unique constraint \"uk_his_registration_patient_doctor_date_status\""
        java.util.regex.Pattern postgresPattern = java.util.regex.Pattern.compile("constraint \"(.+?)\"");
        java.util.regex.Matcher postgresMatcher = postgresPattern.matcher(message);
        if (postgresMatcher.find()) {
            return postgresMatcher.group(1);
        }

        // MySQL格式: "Duplicate entry 'xxx' for key 'uk_his_charge_transaction_no'"
        java.util.regex.Pattern mysqlPattern = java.util.regex.Pattern.compile("for key '(.+?)'");
        java.util.regex.Matcher mysqlMatcher = mysqlPattern.matcher(message);
        if (mysqlMatcher.find()) {
            return mysqlMatcher.group(1);
        }

        return "unknown";
    }

    /**
     * 根据约束名称映射到友好的错误码
     */
    private BusinessException getErrorCodeForConstraint(String constraintName, DataIntegrityViolationException exception) {
        if (constraintName == null) {
            return BusinessException.CONSTRAINT_VIOLATION;
        }

        // 挂号唯一约束
        if (constraintName.contains("uk_his_registration")) {
            return BusinessException.DUPLICATE_REGISTRATION;
        }

        // 收费交易号唯一约束
        if (constraintName.contains("uk_his_charge")) {
            return BusinessException.DUPLICATE_TRANSACTION_NO;
        }

        // 外键约束
        if (constraintName.toLowerCase().contains("fk_")
                || messageContainsForeignKeyViolation(exception)) {
            return BusinessException.FOREIGN_KEY_VIOLATION;
        }

        // 非空约束
        if (constraintName.toLowerCase().contains("not null")
                || messageContainsNotNullViolation(exception)) {
            return BusinessException.NOT_NULL_VIOLATION;
        }

        // 默认返回通用约束违例错误
        return BusinessException.CONSTRAINT_VIOLATION;
    }

    /**
     * 检查异常消息是否包含外键违例信息
     */
    private boolean messageContainsForeignKeyViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        return message != null &&
                (message.toLowerCase().contains("foreign key")
                        || message.toLowerCase().contains("外键")
                        || message.contains("violates foreign key constraint"));
    }

    /**
     * 检查异常消息是否包含非空违例信息
     */
    private boolean messageContainsNotNullViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        return message != null &&
                (message.toLowerCase().contains("null value")
                        || message.toLowerCase().contains("not null")
                        || message.contains("cannot be null"));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneralException(
            Exception e,
            HttpServletRequest request) {

        LogUtils.logSystemError("系统", e.getMessage(), e);

        // 触发告警
        alertService.recordError("系统异常: " + e.getClass().getSimpleName(),
                org.slf4j.MDC.get("traceId"));

        // 确保错误信息不为空
        String errorMsg = e.getMessage();
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            errorMsg = "发生了未知错误。异常类型：" + e.getClass().getSimpleName();
        }

        // 生产环境不要返回详细的异常堆栈给客户端
        String responseMsg = "系统内部错误，请联系管理员";

        // 开发环境可以返回更多信息
        if (log.isDebugEnabled()) {
            responseMsg = responseMsg + "：" + errorMsg;
        }

        // 绝对不返回空，确保总有有效的Result对象
        Result<Void> result = Result.error(500, responseMsg);
        if (result == null) {
            result = new Result<>(500, "系统错误", null);
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<Void>> handleNullPointerException(
            NullPointerException e,
            HttpServletRequest request) {
        
        log.error("空指针异常: 请求路径: {}", request.getRequestURI(), e);
        
        // 获取堆栈信息中的关键位置
        String location = "未知位置";
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            StackTraceElement element = e.getStackTrace()[0];
            location = element.getClassName() + "." + element.getMethodName() + 
                      "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
        }
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统错误：空指针异常。错误位置：" + location));
    }
}
