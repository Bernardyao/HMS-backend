package com.his.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建收费单 DTO
 */
@Data
@Schema(description = "创建收费单请求对象")
public class CreateChargeDTO {

    @NotNull(message = "挂号记录ID不能为空")
    @Schema(description = "挂号记录ID", requiredMode = RequiredMode.REQUIRED, example = "1")
    private Long registrationId;

    @Schema(description = "处方ID列表", example = "[10, 11]")
    private List<Long> prescriptionIds;
}
