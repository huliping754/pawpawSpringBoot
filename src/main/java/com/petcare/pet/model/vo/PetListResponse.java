package com.petcare.pet.model.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petcare.pet.model.Pet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 宠物列表响应对象
 * 包含分页数据和统计信息
 */
@Data
@Schema(description = "宠物列表响应")
public class PetListResponse {
    
    @Schema(description = "宠物列表")
    private Page<Pet> records;
    
    @Schema(description = "总记录数")
    private Long total;
    
    @Schema(description = "总页数")
    private Long pages;
    
    @Schema(description = "当前页码")
    private Long current;
    
    @Schema(description = "每页大小")
    private Long size;
    
    @Schema(description = "总寄养天数")
    private Integer totalStayDays;
    
    @Schema(description = "总费用")
    private BigDecimal totalAmount;
    
    @Schema(description = "已入账总金额")
    private BigDecimal totalSettledAmount;
    
    @Schema(description = "未入账总金额")
    private BigDecimal totalUnsettledAmount;
    
    public PetListResponse(Page<Pet> page) {
        this.records = page;
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.current = page.getCurrent();
        this.size = page.getSize();
        
        // 计算统计信息
        this.totalStayDays = page.getRecords().stream()
                .mapToInt(pet -> pet.getStayDays() != null ? pet.getStayDays() : 0)
                .sum();
        
        this.totalAmount = page.getRecords().stream()
                .map(pet -> pet.getTotalAmount() != null ? pet.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalSettledAmount = page.getRecords().stream()
                .map(pet -> pet.getSettledAmount() != null ? pet.getSettledAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalUnsettledAmount = this.totalAmount.subtract(this.totalSettledAmount);
    }
}
