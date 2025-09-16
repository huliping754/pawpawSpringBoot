package com.petcare.finance.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物收入关联查询 VO
 * 用于关联查询 pets 表和 incomes 表的数据
 */
@Data
@Schema(description = "宠物收入关联信息")
public class PetIncomeVO {

    @Schema(description = "宠物ID")
    private Long petId;

    @Schema(description = "宠物姓名")
    private String petName;

    @Schema(description = "宠物品种")
    private String petBreed;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "宠物状态")
    private String petStatus;

    @Schema(description = "收入记录ID")
    private Long incomeId;

    @Schema(description = "日费用")
    private BigDecimal dailyFee;

    @Schema(description = "其他费用")
    private BigDecimal otherFee;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "入住天数")
    private Integer daysStayed;

    @Schema(description = "已结算金额")
    private BigDecimal settledAmount;

    @Schema(description = "未结算金额")
    private BigDecimal unsettledAmount;

    @Schema(description = "收入备注")
    private String incomeRemark;

    @Schema(description = "收入记录创建时间")
    private LocalDateTime incomeCreatedAt;
}