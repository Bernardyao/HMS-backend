package com.his.common;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 统一响应结果封装类
 *
 * <p>封装所有API接口的响应结果，包括成功和失败情况，为前端提供统一的响应格式</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>统一响应格式</b>：所有接口返回相同的数据结构，便于前端统一处理</li>
 *   <li><b>泛型支持</b>：支持任意类型的返回数据</li>
 *   <li><b>状态码规范</b>：使用HTTP标准状态码（200=成功，400=客户端错误，500=服务器错误）</li>
 *   <li><b>自动时间戳</b>：每次创建Result对象时自动记录当前时间戳</li>
 *   <li><b>多种响应类型</b>：提供成功、失败、未授权、禁止访问等多种响应方法</li>
 * </ul>
 *
 * <h3>响应结构</h3>
 * <pre>
 * {
 *   "code": 200,           // 响应状态码
 *   "message": "操作成功",  // 响应消息
 *   "data": {...},         // 响应数据（可为null）
 *   "timestamp": 1701417600000  // 时间戳
 * }
 * </pre>
 *
 * <h3>状态码规范</h3>
 * <table border="1">
 *   <tr><th>状态码</th><th>含义</th><th>使用场景</th></tr>
 *   <tr><td>200</td><td>成功</td><td>请求成功处理</td></tr>
 *   <tr><td>400</td><td>客户端错误</td><td>参数错误、业务校验失败</td></tr>
 *   <tr><td>401</td><td>未授权</td><td>未登录或Token失效</td></tr>
 *   <tr><td>403</td><td>禁止访问</td><td>已登录但权限不足</td></tr>
 *   <tr><td>404</td><td>资源未找到</td><td>请求的资源不存在</td></tr>
 *   <tr><td>500</td><td>服务器错误</td><td>系统内部错误</td></tr>
 * </table>
 *
 * <h3>使用示例</h3>
 * <h4>1. 成功响应（无数据）</h4>
 * <pre>
 * {@code @PostMapping("/registrations/{id}/cancel")}
 * public Result{@code <Void>} cancelRegistration(@PathVariable Long id) {
 *     registrationService.cancel(id, "患者要求取消");
 *     return Result.success();  // 返回：{"code":200,"message":"操作成功","data":null}
 * }
 * </pre>
 *
 * <h4>2. 成功响应（带数据）</h4>
 * <pre>
 * {@code @GetMapping("/patients/{id}")}
 * public Result{@code <PatientVO>} getPatient(@PathVariable Long id) {
 *     Patient patient = patientService.getById(id);
 *     return Result.success(convertToVO(patient));  // 返回患者数据
 * }
 * </pre>
 *
 * <h4>3. 失败响应</h4>
 * <pre>
 * {@code @PostMapping("/charges")}
 * public Result{@code <ChargeVO>} createCharge(@RequestBody CreateChargeDTO dto) {
 *     if (dto.getAmount() <= 0) {
 *         return Result.error("收费金额必须大于0");  // 返回：{"code":500,"message":"收费金额必须大于0"}
 *     }
 *     // ...
 * }
 * </pre>
 *
 * <h4>4. 参数错误响应</h4>
 * <pre>
 * {@code @GetMapping("/doctors/{id}")}
 * public Result{@code <DoctorVO>} getDoctor(@PathVariable Long id) {
 *     if (id == null || id <= 0) {
 *         return Result.badRequest("医生ID无效");  // 返回：{"code":400,"message":"医生ID无效"}
 *     }
 *     // ...
 * }
 * </pre>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>Controller层统一使用Result作为返回值类型</li>
 *   <li>成功时使用success()系列方法，失败时使用error()或badRequest()</li>
 *   <li>message字段应使用用户友好的描述，避免技术术语</li>
 *   <li>敏感信息不要放在响应中，使用日志记录</li>
 *   <li>复杂对象的序列化使用VO（View Object）而非Entity</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "响应状态码（200=成功, 400=失败）", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳", example = "1701417600000")
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）
     *
     * <p>用于不需要返回数据的成功操作，如删除、更新、取消等</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>删除操作：删除成功后不需要返回数据</li>
     *   <li>更新操作：更新成功后不需要返回更新后的对象</li>
     *   <li>取消操作：取消挂号、取消处方等</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":200,"message":"操作成功","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @return 成功响应对象，data为null
     * @since 1.0
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     *
     * <p>用于需要返回数据的成功操作，是最常用的响应方法</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>查询操作：查询单个对象或列表</li>
     *   <li>创建操作：创建成功后返回新创建的对象</li>
     *   <li>详情查询：返回完整的详细信息</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":200,"message":"操作成功","data":{...},"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型
     * @param data 响应数据
     * @return 成功响应对象，包含data数据
     * @since 1.0
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息和数据）
     *
     * <p>用于需要自定义成功消息的场景，如"创建成功"、"更新成功"等</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>需要更具体的成功消息（如"挂号成功"、"收费成功"）</li>
     *   <li>需要返回操作相关的提示信息</li>
     *   <li>国际化场景下的自定义消息</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":200,"message":"挂号成功","data":{...},"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型
     * @param message 自定义成功消息
     * @param data 响应数据
     * @return 成功响应对象，包含自定义消息和data数据
     * @since 1.0
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应（默认500状态码）
     *
     * <p>用于服务器内部错误，默认使用500状态码</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>服务器内部处理错误</li>
     *   <li>数据库操作失败</li>
     *   <li>第三方服务调用失败</li>
     *   <li>其他非客户端错误的异常情况</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":500,"message":"操作失败","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param message 错误消息
     * @return 失败响应对象，状态码为500
     * @since 1.0
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败响应（自定义状态码）
     *
     * <p>用于需要自定义错误状态码的场景</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>业务异常需要特定的状态码</li>
     *   <li>与前端约定的特殊错误码</li>
     *   <li>需要区分不同类型的错误</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":1001,"message":"库存不足","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param code 自定义错误状态码
     * @param message 错误消息
     * @return 失败响应对象，包含自定义状态码
     * @since 1.0
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 参数错误响应（400状态码）
     *
     * <p>用于客户端请求参数错误或业务校验失败</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>必填参数缺失</li>
     *   <li>参数格式错误（如手机号、身份证号格式不正确）</li>
     *   <li>参数值不符合业务规则（如年龄为负数）</li>
     *   <li>数据校验失败（如重复挂号、库存不足）</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":400,"message":"患者ID不能为空","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param message 参数错误消息
     * @return 失败响应对象，状态码为400
     * @since 1.0
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    /**
     * 未授权响应（401状态码）
     *
     * <p>用于用户未登录或认证信息无效的场景</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>Token过期或无效</li>
     *   <li>用户未登录</li>
     *   <li>认证信息缺失（如没有Authorization请求头）</li>
     *   <li>Token被吊销</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":401,"message":"未登录，请先登录","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param message 未授权消息
     * @return 失败响应对象，状态码为401
     * @since 1.0
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    /**
     * 禁止访问响应（403状态码）
     *
     * <p>用于用户已登录但权限不足的场景</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>角色权限不足（如护士尝试开处方）</li>
     *   <li>资源访问权限不足（如医生查看其他医生的患者）</li>
     *   <li>操作权限不足（如普通用户尝试管理员操作）</li>
     *   <li>跨资源访问（IDOR防护）</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":403,"message":"权限不足，无法访问","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param message 禁止访问消息
     * @return 失败响应对象，状态码为403
     * @since 1.0
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    /**
     * 资源未找到响应（404状态码）
     *
     * <p>用于请求的资源不存在的场景</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>查询不存在的患者ID</li>
     *   <li>查询不存在的医生ID</li>
     *   <li>访问已删除的数据</li>
     *   <li>请求的URL路径不正确</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{"code":404,"message":"患者不存在，ID: 100","data":null,"timestamp":1701417600000}</pre>
     *
     * @param <T> 数据类型占位符
     * @param message 资源未找到消息
     * @return 失败响应对象，状态码为404
     * @since 1.0
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
}
