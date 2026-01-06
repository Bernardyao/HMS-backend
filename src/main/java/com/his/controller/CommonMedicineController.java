package com.his.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.service.UserRoleService;
import com.his.vo.MedicineVO;
import com.his.vo.views.MedicineViews;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共接口-药品查询控制器（统一版本，支持JsonView）
 *
 * <p>为所有工作站提供统一的药品查询功能，根据用户角色自动返回不同字段的数据</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品列表查询</b>：支持多条件组合查询（关键字、分类、价格、库存状态等）</li>
 *   <li><b>药品详情查询</b>：根据ID查询药品详细信息</li>
 *   <li><b>角色感知</b>：通过 UserRoleService 根据用户角色自动返回不同字段</li>
 * </ul>
 *
 * <h3>架构改进</h3>
 * <p>重构后使用 {@link UserRoleService} 进行角色到视图的映射：</p>
 * <ul>
 *   <li>✅ 解耦：Controller 不直接依赖 Spring Security Context</li>
 *   <li>✅ 可测试：UserRoleService 可以在单元测试中 Mock</li>
 *   <li>✅ 显式逻辑：使用 null 检查而非异常驱动</li>
 *   <li>✅ 单一职责：角色判断逻辑集中在 Service 层</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生工作站</b>：开具处方时搜索药品</li>
 *   <li><b>药师工作站</b>：查询药品库存、进货价、利润率</li>
 *   <li><b>收费管理</b>：查询药品价格</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要已认证用户（isAuthenticated()）</p>
 *
 * @author HIS 开发团队
 * @version 2.1
 * @since 2.1
 * @see com.his.service.MedicineService
 * @see com.his.service.UserRoleService
 */
@Tag(name = "公共接口-药品查询", description = "统一的药品查询接口（所有认证用户，根据角色返回不同字段）")
@Slf4j
@RestController
@RequestMapping(value = "/api/common/medicines", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommonMedicineController {

    private final MedicineService medicineService;
    private final UserRoleService userRoleService;

    /**
     * 统一药品查询（支持分页、多条件筛选）
     * <p>
     * 根据当前用户角色自动返回不同字段的数据：
     * <ul>
     *   <li>药师：返回所有字段（含进货价、利润率等敏感信息）</li>
     *   <li>医生：返回除进货价外的所有字段（含规格、剂型、库存状态等）</li>
     *   <li>其他：仅返回基础字段（名称、价格、库存等）</li>
     * </ul>
     * </p>
     *
     * <p><b>查询参数：</b></p>
     * <ul>
     *   <li><b>keyword</b>: 关键字搜索（支持名称、编码、通用名）</li>
     *   <li><b>category</b>: 药品分类（如：抗生素、解热镇痛药）</li>
     *   <li><b>isPrescription</b>: 是否处方药（0=否, 1=是）</li>
     *   <li><b>inStock</b>: 是否只显示有货药品（true=是, false=否）</li>
     *   <li><b>stockStatus</b>: 库存状态（"LOW"=低库存, "OUT"=缺货，仅药师可用）</li>
     *   <li><b>manufacturer</b>: 生产厂家（仅药师可用）</li>
     *   <li><b>minPrice</b>: 最低零售价（仅药师可用）</li>
     *   <li><b>maxPrice</b>: 最高零售价（仅药师可用）</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 查询所有药品（药师会看到进货价，医生不会）
     * GET /api/common/medicines
     *
     * # 关键字搜索
     * GET /api/common/medicines?keyword=阿莫西林
     *
     * # 只显示有货的处方药
     * GET /api/common/medicines?isPrescription=1&inStock=true
     *
     * # 药师：查询价格区间（10-50元）的抗生素
     * GET /api/common/medicines?category=抗生素&minPrice=10&maxPrice=50
     *
     * # 药师：查询低库存药品
     * GET /api/common/medicines?stockStatus=LOW
     * }</pre>
     *
     * @param keyword        关键字（可选）
     * @param category       药品分类（可选）
     * @param isPrescription 是否处方药（可选）
     * @param inStock        是否只显示有货药品（可选）
     * @param stockStatus    库存状态（可选，仅药师有效）
     * @param manufacturer   生产厂家（可选，仅药师有效）
     * @param minPrice       最低价格（可选，仅药师有效）
     * @param maxPrice       最高价格（可选，仅药师有效）
     * @param pageable       分页和排序参数
     * @return 药品分页列表（根据角色返回不同字段）
     */
    @GetMapping
    @Operation(
        summary = "查询药品列表",
        description = """
            支持多条件组合查询的分页接口，根据用户角色自动返回不同字段。

            **🔒 字段可见性控制（基于Jackson JsonView）：**

            系统根据当前登录用户的角色自动过滤返回字段，确保敏感信息不被越权访问：

            | 角色 | 视图类型 | 可见字段 | 敏感字段 |
            |------|---------|---------|---------|
            | 护士、收费员、管理员 | Public | id, name, code, category, isPrescription, stockQuantity, retailPrice | ❌ 无 |
            | 医生、药师 | Doctor | 以上字段 + specification, unit, dosageForm, manufacturer, stockStatus | ❌ 进货价 |
            | 药师 | Pharmacist | 所有字段 | ✅ purchasePrice, profitMargin, minStock, maxStock |

            **字段说明：**
            - `purchasePrice`: 进货价（仅药师可见，用于成本核算）
            - `profitMargin`: 利润率（仅药师可见，计算公式：(零售价-进货价)/零售价）
            - `stockStatus`: 库存状态（自动计算：IN_STOCK/LOW_STOCK/OUT_OF_STOCK）

            **查询参数：**
            - keyword: 关键字（名称/编码/通用名）
            - category: 药品分类
            - isPrescription: 是否处方药（0=否, 1=是）
            - inStock: 是否只显示有货药品
            - stockStatus: 库存状态（LOW=低库存, OUT=缺货，仅药师有效）
            - manufacturer: 生产厂家（仅药师有效）
            - minPrice/maxPrice: 价格区间（仅药师有效）
            - page: 页码（默认0）
            - size: 每页大小（默认20）
            - sort: 排序（默认name,asc）

            **请求示例：**
            ```bash
            # 护士查询药品（仅返回基础字段）
            GET /api/common/medicines?keyword=阿莫西林

            # 医生查询处方药（包含规格、剂型等）
            GET /api/common/medicines?isPrescription=1&category=抗生素

            # 药师查询低库存药品（包含进货价、利润率）
            GET /api/common/medicines?stockStatus=LOW&minPrice=10&maxPrice=50
            ```
            """
    )
    public Result<Page<MedicineVO>> search(
        @Parameter(description = "关键字（名称/编码/通用名）", example = "阿莫西林")
        @RequestParam(name = "keyword", required = false) String keyword,

        @Parameter(description = "药品分类", example = "抗生素")
        @RequestParam(name = "category", required = false) String category,

        @Parameter(description = "是否处方药（0=否, 1=是）", example = "1")
        @RequestParam(name = "isPrescription", required = false) Short isPrescription,

        @Parameter(description = "是否只显示有货药品", example = "true")
        @RequestParam(name = "inStock", required = false) Boolean inStock,

        @Parameter(description = "库存状态（LOW=低库存, OUT=缺货，仅药师有效）", example = "LOW")
        @RequestParam(name = "stockStatus", required = false) String stockStatus,

        @Parameter(description = "生产厂家（仅药师有效）", example = "某某制药有限公司")
        @RequestParam(name = "manufacturer", required = false) String manufacturer,

        @Parameter(description = "最低价格（元，仅药师有效）", example = "10")
        @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,

        @Parameter(description = "最高价格（元，仅药师有效）", example = "50")
        @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,

        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
        @Parameter(description = "分页和排序参数")
        Pageable pageable
    ) {
        log.info("【通用】查询药品列表 - keyword: {}, category: {}, isPrescription: {}, inStock: {}, " +
                 "stockStatus: {}, manufacturer: {}, priceRange: {}-{}",
                 keyword, category, isPrescription, inStock, stockStatus, manufacturer, minPrice, maxPrice);

        // 通过 UserRoleService 获取当前用户对应的视图类型
        Class<?> view = userRoleService.getMedicineViewForCurrentUser();

        // 根据用户角色调用不同的Service方法
        Page<Medicine> page;
        if (view == MedicineViews.Pharmacist.class) {
            // 药师：使用高级查询（支持价格、厂家、库存状态等）
            page = medicineService.searchMedicinesForPharmacist(
                keyword, category, isPrescription, stockStatus,
                manufacturer, minPrice, maxPrice, pageable
            );
        } else {
            // 医生和其他角色：使用基础查询
            page = medicineService.searchMedicinesForDoctor(
                keyword, category, isPrescription, inStock, pageable
            );
        }

        // 使用JsonView转换VO
        Page<MedicineVO> voPage = page.map(m -> VoConverter.toMedicineVO(m, view));

        return Result.success(
            String.format("查询成功，共 %d 条记录", voPage.getTotalElements()),
            voPage
        );
    }

    /**
     * 根据ID查询药品详情（支持JsonView）
     * <p>
     * 根据当前用户角色自动返回不同字段的数据：
     * <ul>
     *   <li>药师：返回所有字段（含进货价、利润率等敏感信息）</li>
     *   <li>医生：返回除进货价外的所有字段（含规格、剂型、库存状态等）</li>
     *   <li>其他：仅返回基础字段（名称、价格、库存等）</li>
     * </ul>
     * </p>
     *
     * @param id 药品ID
     * @return 药品详细信息（根据角色返回不同字段）
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "查询药品详情",
        description = """
            根据药品ID查询详细信息，根据用户角色自动返回不同字段。

            **🔒 字段可见性控制（基于Jackson JsonView）：**

            系统根据当前登录用户的角色自动过滤返回字段，确保敏感信息不被越权访问：

            | 角色 | 可见字段示例 |
            |------|------------|
            | 护士、收费员 | `{"id":1,"name":"阿莫西林","retailPrice":25.80,"stockQuantity":100}` |
            | 医生、药师 | `{"specification":"0.25g*24粒","manufacturer":"某某制药"}` |
            | 药师 | `{"purchasePrice":18.50,"profitMargin":28.29,"minStock":50}` |

            **字段说明：**
            - `purchasePrice`: 进货价（仅药师可见）
            - `profitMargin`: 利润率百分比（仅药师可见）

            **请求示例：**
            ```bash
            # 护士查询（无敏感信息）
            GET /api/common/medicines/1
            # 返回：不含进货价、利润率

            # 药师查询（包含敏感信息）
            GET /api/common/medicines/1
            # 返回：包含进货价、利润率、库存阈值
            ```
            """
    )
    public Result<MedicineVO> getById(
        @Parameter(description = "药品ID", required = true, example = "1")
        @PathVariable("id") Long id) {

        log.info("【通用】查询药品详情 - ID: {}", id);

        // 通过 UserRoleService 获取当前用户对应的视图类型
        Class<?> view = userRoleService.getMedicineViewForCurrentUser();

        Medicine medicine = medicineService.getById(id);
        MedicineVO vo = VoConverter.toMedicineVO(medicine, view);

        return Result.success("查询成功", vo);
    }

    /**
     * 搜索药品（根据名称或编码）- 简化版接口
     * <p>
     * 这是一个简化的搜索接口，用于快速搜索药品。
     * 返回List而非Page，不需要分页参数。
     * </p>
     *
     * @param keyword 关键字
     * @return 药品列表（根据角色返回不同字段）
     * @deprecated 建议使用 {@link #search(String, String, Short, Boolean, String, String, BigDecimal, BigDecimal, Pageable)} 代替
     */
    @Deprecated
    @GetMapping("/search")
    @Operation(
        summary = "搜索药品（简化版）",
        description = "根据药品名称或编码模糊搜索药品信息（不加分页，建议使用主查询接口）"
    )
    public Result<List<MedicineVO>> searchSimple(
        @Parameter(description = "关键字（药品名称或编码）", example = "阿莫西林")
        @RequestParam(name = "keyword", required = false) String keyword) {

        log.info("【通用】搜索药品（简化版），关键字: {}", keyword);

        // 通过 UserRoleService 获取当前用户对应的视图类型
        Class<?> view = userRoleService.getMedicineViewForCurrentUser();

        List<Medicine> medicines = medicineService.searchMedicines(keyword);
        List<MedicineVO> voList = medicines.stream()
            .map(m -> VoConverter.toMedicineVO(m, view))
            .collect(Collectors.toList());

        return Result.success("查询成功", voList);
    }
}
