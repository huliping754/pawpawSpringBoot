package com.petcare.pet.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.petcare.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物预约/入住记录实体
 */
@Data
@TableName("pets")
@Schema(description = "宠物预约/入住记录")
public class Pet extends BaseEntity {

    @TableId(type = IdType.INPUT)
    @Schema(description = "宠物ID，小程序Date.now()生成", example = "1710000000001")
    private Long id;

    @Schema(description = "宠物姓名", example = "小豆")
    private String name;
    
    @Schema(description = "宠物品种", example = "柯基")
    private String breed;
    
    @Schema(description = "性别", example = "弟弟", allowableValues = {"弟弟", "妹妹", "未知"})
    private String gender;
    
    @Schema(description = "年龄", example = "2")
    private Integer age;
    
    @Schema(description = "绝育状态", example = "未绝育", allowableValues = {"已绝育", "未绝育", "未知"})
    private String neutered;
    
    @Schema(description = "开始日期", example = "2025-09-10")
    private LocalDate startDate;
    
    @Schema(description = "结束日期", example = "2025-09-12")
    private LocalDate endDate;
    
    @Schema(description = "每日费用", example = "120.00")
    private BigDecimal dailyFee;
    
    @Schema(description = "其他费用", example = "20.00")
    private BigDecimal otherFee;
    
    @Schema(description = "备注", example = "首次到店")
    private String remark;
    
    @Schema(description = "状态", example = "booked", allowableValues = {"booked", "checkedIn", "checkedOut"})
    private String status;
    
    // 计算字段：寄养天数（按过夜计算）
    @TableField(exist = false)  // 标记此字段不存在于数据库表中
    @Schema(description = "寄养天数（按过夜计算）", example = "3")
    private Integer stayDays;
    
    // 计算字段：总费用（寄养天数 × 每日费用 + 其他费用）
    @TableField(exist = false)  // 标记此字段不存在于数据库表中
    @Schema(description = "总费用（寄养天数 × 每日费用 + 其他费用）", example = "350.00")
    private BigDecimal totalAmount;
    
    // 计算字段：已入账金额（从incomes表的settledAmount字段查询）
    @TableField(exist = false)  // 标记此字段不存在于数据库表中
    @Schema(description = "已入账金额", example = "200.00")
    private BigDecimal settledAmount;
    
    // 临时字段：前端传入的已收入金额（用于更新settledAmount）
    @TableField(exist = false)  // 标记此字段不存在于数据库表中
    @Schema(description = "前端传入的已收入金额（用于更新settledAmount）", example = "200.00")
    private BigDecimal inputSettledAmount;
    
    // 预期设计是把 totalFee 存在 incomes.total_fee，在返回时再装配回 Pet.totalFee。因此 Pet.totalFee 必须是“非持久化字段”。
    @TableField(exist = false)  // 标记此字段不存在于数据库表中
    @Schema(description = "前端传入的寄养总费用（如果传入则使用此值计算总费用）", example = "300.00")
    private BigDecimal totalFee;
}


