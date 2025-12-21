package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 护士工作站查询参数 DTO
 */
@Data
@Schema(description = "护士工作站查询参数")
public class NurseWorkstationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "就诊日期（不传默认当天）", example = "2023-12-01", type = "string", format = "date")
    private LocalDate visitDate;

    @Schema(description = "科室ID（不传查询所有科室）", example = "10")
    private Long departmentId;

    @Schema(description = "挂号状态（0=待就诊, 1=已就诊, 2=已取消，不传查询所有状态）", example = "0")
    private Short status;

    @Schema(description = "就诊类型（1=初诊, 2=复诊, 3=急诊，不传查询所有类型）", example = "1")
    private Short visitType;

    @Schema(description = "患者姓名或挂号号模糊查询", example = "张三")
    private String keyword;
}
