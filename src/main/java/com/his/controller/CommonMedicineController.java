package com.his.controller;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.entity.Medicine;
import com.his.service.MedicineService;
import com.his.vo.MedicineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公共接口-药品搜索控制器
 *
 * <p>为各工作站提供药品搜索和查询功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>药品搜索</b>：根据药品名称或编码进行模糊搜索</li>
 *   <li><b>药品详情</b>：根据药品ID查询详细信息</li>
 *   <li><b>启用药品</b>：查询所有启用的药品</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>医生工作站</b>：开具处方时搜索药品</li>
 *   <li><b>药师工作站</b>：查询药品库存和价格</li>
 *   <li><b>收费管理</b>：查询药品价格</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要已认证用户（isAuthenticated()）</p>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicineService
 */
@Tag(name = "公共接口-药品查询", description = "药品搜索和查询接口（医生、护士、药师共用）")
@Slf4j
@RestController
@RequestMapping(value = "/api/common/medicines", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommonMedicineController {

    private final MedicineService medicineService;

    /**
     * 搜索药品（根据名称或编码）
     * 权限：所有认证用户
     *
     * @param keyword 关键字
     * @return 药品列表
     */
    @Operation(summary = "搜索药品", description = "根据药品名称或编码模糊搜索药品信息")
    @GetMapping("/search")
    public Result<List<MedicineVO>> search(
            @Parameter(description = "关键字（药品名称或编码）", example = "阿莫西林")
            @RequestParam(required = false) String keyword) {
        
        log.info("搜索药品，关键字: {}", keyword);
        
        List<Medicine> medicines = medicineService.searchMedicines(keyword);
        List<MedicineVO> voList = medicines.stream()
            .map(VoConverter::toMedicineVO)
            .collect(Collectors.toList());

        return Result.success("查询成功", voList);
    }

    /**
     * 根据ID查询药品
     * 权限：所有认证用户
     *
     * @param id 药品ID
     * @return 药品信息
     */
    @Operation(summary = "查询药品详情", description = "根据药品ID查询详细信息")
    @GetMapping("/{id}")
    public Result<MedicineVO> getById(
            @Parameter(description = "药品ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        
        log.info("查询药品详情，ID: {}", id);
        
        Medicine medicine = medicineService.getById(id);
        MedicineVO vo = VoConverter.toMedicineVO(medicine);

        return Result.success("查询成功", vo);
    }
}
