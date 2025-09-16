package com.petcare.finance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.petcare.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收入账单记录实体
 */
@Data
@TableName("incomes")
@Schema(description = "收入记录")
public class Income extends BaseEntity {
    @TableId(type = IdType.INPUT)
    @Schema(description = "收入记录ID", example = "1710000000001")
    private Long id;

    @Schema(description = "关联的宠物ID", example = "1710000000001")
    private Long petId;
    
    @Schema(description = "每日费用", example = "120.00")
    private BigDecimal dailyFee;
    
    @Schema(description = "其他费用", example = "20.00")
    private BigDecimal otherFee;
    
    @Schema(description = "总金额", example = "260.00")
    private BigDecimal totalAmount;
    
    @Schema(description = "入住天数", example = "2")
    private Integer daysStayed;
    
    @Schema(description = "已结算金额", example = "0.00")
    private BigDecimal settledAmount;
    
    @Schema(description = "备注", example = "已完成离店")
    private String remark;
}


