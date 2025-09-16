package com.petcare.system.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
@TableName("settings")
@Schema(description = "系统配置")
public class Setting {
    @TableId
    @TableField("`key`")  // 使用反引号包围key字段，因为key是MySQL保留关键字
    @Schema(description = "配置键", example = "max_capacity")
    private String key;
    
    @Schema(description = "配置值", example = "10")
    private String value;
    
    @Schema(description = "更新时间", example = "2025-09-12 10:30:00")
    private LocalDateTime updatedAt;
}


