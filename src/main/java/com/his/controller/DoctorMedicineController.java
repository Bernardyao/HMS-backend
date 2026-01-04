package com.his.controller;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.vo.DoctorMedicineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 医生工作站 - 药品查询控制器
 * <p>
 * 为医生工作站提供药品查询功能，用于开处方时选择药品。
 * </p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品列表查询</b>：支持关键字、分类、处方药、库存状态筛选</li>
 *   <li><b>药品详情查询</b>：根据ID查询药品详细信息</li>
 *   <li><b>按分类查询</b>：根据药品分类快速筛选</li>
 * </ul>
 *
 * <h3>数据权限</h3>
 * <ul>
 *   <li>医生可以查看所有药品信息</li>
 *   <li>不包含进货价等敏感商业信息</li>
 *   <li>显示库存状态，方便开方时选择有货药品</li>
 * </ul>
 *
 * <h3>库存状态说明</h3>
 * <ul>
 *   <li><b>IN_STOCK</b>: 正常库存（库存 > 最低库存）</li>
 *   <li><b>LOW_STOCK</b>: 低库存（库存 <= 最低库存，但 > 0）</li>
 *   <li><b>OUT_OF_STOCK</b>: 缺货（库存 = 0）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicineService
 */
@Tag(name = "医生工作站-药品查询", description = "医生开处方时的药品查询接口")
@Slf4j
@RestController
@RequestMapping("/api/doctor/medicines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class DoctorMedicineController {

    private final MedicineService medicineService;

    /**
     * 药品列表查询（分页、筛选、排序）
     * <p>
     * 支持多条件组合查询，方便医生快速找到需要的药品。
     * </p>
     *
     * <p><b>查询参数：</b></p>
     * <ul>
     *   <li><b>keyword</b>: 关键字搜索（支持名称、编码、通用名）</li>
     *   <li><b>category</b>: 药品分类（如：抗生素、解热镇痛药）</li>
     *   <li><b>isPrescription</b>: 是否处方药（0=否, 1=是）</li>
     *   <li><b>inStock</b>: 是否只显示有货药品（true=是, false=否）</li>
     *   <li><b>page</b>: 页码（从0开始）</li>
     *   <li><b>size</b>: 每页大小（默认20）</li>
     *   <li><b>sort</b>: 排序字段和方向（默认name,asc）</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 查询所有药品
     * GET /api/doctor/medicines
     *
     * # 关键字搜索
     * GET /api/doctor/medicines?keyword=阿莫西林
     *
     * # 只显示有货的处方药
     * GET /api/doctor/medicines?isPrescription=1&inStock=true
     *
     * # 按分类查询，第二页，每页50条，按名称排序
     * GET /api/doctor/medicines?category=抗生素&page=1&size=50&sort=name,asc
     * }</pre>
     *
     * @param keyword        关键字（可选）
     * @param category       药品分类（可选）
     * @param isPrescription 是否处方药（可选）
     * @param inStock        是否只显示有货药品（可选）
     * @param pageable       分页和排序参数
     * @return 药品分页列表
     */
    @GetMapping
    @Operation(
        summary = "查询药品列表",
        description = """
            支持关键字、分类、处方药、库存状态筛选的分页查询。

            **查询参数：**
            - keyword: 关键字（名称/编码/通用名）
            - category: 药品分类
            - isPrescription: 是否处方药（0=否, 1=是）
            - inStock: 是否只显示有货药品
            - page: 页码（默认0）
            - size: 每页大小（默认20）
            - sort: 排序（默认name,asc）

            **库存状态：**
            - IN_STOCK: 正常库存
            - LOW_STOCK: 低库存
            - OUT_OF_STOCK: 缺货
            """
    )
    public Result<Page<DoctorMedicineVO>> searchMedicines(
        @Parameter(description = "关键字（名称/编码/通用名）", example = "阿莫西林")
        @RequestParam(required = false) String keyword,

        @Parameter(description = "药品分类", example = "抗生素")
        @RequestParam(required = false) String category,

        @Parameter(description = "是否处方药（0=否, 1=是）", example = "1")
        @RequestParam(required = false) Short isPrescription,

        @Parameter(description = "是否只显示有货药品", example = "true")
        @RequestParam(required = false) Boolean inStock,

        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
        @Parameter(description = "分页和排序参数")
        Pageable pageable
    ) {
        try {
            log.info("【医生】查询药品列表 - keyword: {}, category: {}, isPrescription: {}, inStock: {}",
                     keyword, category, isPrescription, inStock);

            Page<Medicine> page = medicineService.searchMedicinesForDoctor(
                keyword, category, isPrescription, inStock, pageable
            );

            Page<DoctorMedicineVO> voPage = page.map(VoConverter::toDoctorMedicineVO);

            return Result.success(
                String.format("查询成功，共 %d 条记录", voPage.getTotalElements()),
                voPage
            );
        } catch (Exception e) {
            log.error("查询药品列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询药品详情
     * <p>
     * 根据药品ID查询详细信息，用于开处方时查看药品完整信息。
     * </p>
     *
     * @param id 药品ID
     * @return 药品详细信息
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "查询药品详情",
        description = "根据药品ID查询详细信息，包含库存状态等完整信息。"
    )
    public Result<DoctorMedicineVO> getMedicineDetail(
        @Parameter(description = "药品ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        try {
            log.info("【医生】查询药品详情 - ID: {}", id);

            Medicine medicine = medicineService.getById(id);
            DoctorMedicineVO vo = VoConverter.toDoctorMedicineVO(medicine);

            log.info("查询药品详情成功，药品: {}", medicine.getName());
            return Result.success("查询成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("查询药品详情失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询药品详情系统异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按分类查询药品
     * <p>
     * 根据药品分类快速筛选药品，支持分页和排序。
     * </p>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * # 查询所有抗生素类药品
     * GET /api/doctor/medicines/by-category/抗生素
     *
     * # 查询解热镇痛药，第一页，每页30条
     * GET /api/doctor/medicines/by-category/解热镇痛药?page=0&size=30
     * }</pre>
     *
     * @param category 药品分类
     * @param pageable 分页和排序参数
     * @return 该分类的药品列表
     */
    @GetMapping("/by-category/{category}")
    @Operation(
        summary = "按分类查询药品",
        description = """
            根据药品分类查询药品列表，支持分页和排序。

            **常见分类：**
            - 抗生素
            - 解热镇痛药
            - 心血管药物
            - 消化系统药物
            - 呼吸系统药物
            """
    )
    public Result<Page<DoctorMedicineVO>> getByCategory(
        @Parameter(description = "药品分类", required = true, example = "抗生素")
        @PathVariable String category,

        @PageableDefault(size = 20)
        @Parameter(description = "分页和排序参数")
        Pageable pageable
    ) {
        try {
            log.info("【医生】按分类查询药品 - category: {}", category);

            Page<Medicine> page = medicineService.searchMedicinesForDoctor(
                null, category, null, null, pageable
            );

            Page<DoctorMedicineVO> voPage = page.map(VoConverter::toDoctorMedicineVO);

            return Result.success(
                String.format("查询成功，共 %d 条记录", voPage.getTotalElements()),
                voPage
            );
        } catch (Exception e) {
            log.error("按分类查询药品失败，category: {}", category, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
