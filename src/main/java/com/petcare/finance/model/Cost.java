package com.petcare.finance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本流水实体
 */
@Data
@TableName("costs")
@Schema(description = "成本支出记录")
public class Cost {
    @TableId(type = IdType.AUTO)
    @Schema(description = "成本记录ID", example = "1")
    private Long id;

    @Schema(description = "水费", example = "100.00")
    private BigDecimal waterFee;
    
    @Schema(description = "电费", example = "200.00")
    private BigDecimal electricityFee;
    
    @Schema(description = "房租", example = "3000.00")
    private BigDecimal rentFee;
    
    @Schema(description = "其他费用", example = "50.00")
    private BigDecimal otherFee;
    
    @Schema(description = "总成本", example = "3350.00")
    private BigDecimal totalCost;
    
    @Schema(description = "成本月份", example = "2025-09")
    private String costMonth;

    @Schema(description = "创建时间", example = "2025-09-01 10:30:00")
    private LocalDateTime createdAt;
}


