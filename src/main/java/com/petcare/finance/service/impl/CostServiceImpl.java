package com.petcare.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petcare.finance.mapper.CostMapper;
import com.petcare.finance.model.Cost;
import com.petcare.finance.service.CostService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class CostServiceImpl extends ServiceImpl<CostMapper, Cost> implements CostService {
    
    @Override
    public boolean save(Cost entity) {
        // 设置默认成本月份为当前月份
        if (entity.getCostMonth() == null) {
            entity.setCostMonth(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        // 计算总成本
        if (entity.getTotalCost() == null) {
            BigDecimal totalCost = BigDecimal.ZERO;
            if (entity.getWaterFee() != null) {
                totalCost = totalCost.add(entity.getWaterFee());
            }
            if (entity.getElectricityFee() != null) {
                totalCost = totalCost.add(entity.getElectricityFee());
            }
            if (entity.getRentFee() != null) {
                totalCost = totalCost.add(entity.getRentFee());
            }
            if (entity.getOtherFee() != null) {
                totalCost = totalCost.add(entity.getOtherFee());
            }
            entity.setTotalCost(totalCost);
        }
        return super.save(entity);
    }
}


