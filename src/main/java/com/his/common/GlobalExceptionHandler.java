package com.his.common;

import lombok.extern.slf4j.Slf4j;
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
 * 功能：
 * 1. 统一处理所有Controller抛出的异常
 * 2. 将异常转换为标准的Result响应格式
 * 3. 记录详细的错误日志，便于排查问题
 * 4. 防止敏感信息泄露到客户端
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.his.controller")
public class GlobalExceptionHandler {
    
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
        
        log.warn("参数校验失败: {}, 请求路径: {}", errorMsg, request.getRequestURI());
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
        
        log.warn("参数类型错误: {}, 请求路径: {}, 错误值: {}", 
                errorMsg, request.getRequestURI(), e.getValue());
        
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
        
        log.warn("业务异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
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

    /**     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneralException(
            Exception e,
            HttpServletRequest request) {
        
        log.error("系统异常: {}, 请求路径: {}, 异常类型: {}", 
                e.getMessage(), request.getRequestURI(), e.getClass().getName(), e);
        
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
