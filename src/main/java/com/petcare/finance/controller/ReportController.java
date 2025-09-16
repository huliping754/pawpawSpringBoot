package com.petcare.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petcare.common.web.ApiResponse;
import com.petcare.finance.model.Cost;
import com.petcare.finance.service.CostService;
import com.petcare.pet.model.Pet;
import com.petcare.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报表统计接口
 * 提供收入、成本、利润等统计报表功能
 */
@Tag(name = "报表统计", description = "财务报表和统计分析接口")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PetService petService;
    private final CostService costService;

    public ReportController(PetService petService, CostService costService) {
        this.petService = petService;
        this.costService = costService;
    }

    /**
     * 月度收支统计（跨月订单按天摊算每日费用；other_fee 计入离店所在月份）
     */
    @Operation(summary = "月度收支报表", description = "按月份统计收入、成本、利润，支持跨月订单按天分摊计算，包含宠物收入明细")
    @GetMapping("/monthly")
    public ApiResponse<Map<String, Object>> monthly(
            @Parameter(description = "月份，格式 yyyy-MM", example = "2025-09") @RequestParam String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.plusMonths(1).atDay(1); // 开区间结尾

        // 1) 收入：从 pets 里按日期相交求和
        LambdaQueryWrapper<Pet> overlapQw = new LambdaQueryWrapper<Pet>()
                .lt(Pet::getStartDate, monthEnd)
                .gt(Pet::getEndDate, monthStart);

        List<Pet> pets = petService.list(overlapQw);

        BigDecimal totalIncome = BigDecimal.ZERO;
        List<Map<String, Object>> petDetails = new ArrayList<>();
        for (Pet p : pets) {
            LocalDate stayStart = p.getStartDate();
            LocalDate stayEnd = p.getEndDate(); // 半开区间 [start, end)

            LocalDate calcStart = stayStart.isAfter(monthStart) ? stayStart : monthStart;
            LocalDate calcEnd = stayEnd.isBefore(monthEnd) ? stayEnd : monthEnd;

            long daysInMonth = Math.max(0, ChronoUnit.DAYS.between(calcStart, calcEnd));
            BigDecimal daily = p.getDailyFee() == null ? BigDecimal.ZERO : p.getDailyFee();
            BigDecimal dayPart = daily.multiply(BigDecimal.valueOf(daysInMonth));

            // 其他费用分配：计入离店月（endDate 落在本月区间）
            boolean endInMonth = !stayEnd.isBefore(monthStart) && stayEnd.isBefore(monthEnd);
            BigDecimal otherPart = endInMonth ? (p.getOtherFee() == null ? BigDecimal.ZERO : p.getOtherFee()) : BigDecimal.ZERO;

            BigDecimal totalForPet = dayPart.add(otherPart);
            totalIncome = totalIncome.add(totalForPet);

            Map<String, Object> row = new HashMap<>();
            row.put("petId", p.getId());
            row.put("name", p.getName());
            row.put("breed", p.getBreed());
            row.put("status", p.getStatus());
            row.put("startDate", p.getStartDate());
            row.put("endDate", p.getEndDate());
            row.put("daysInMonth", daysInMonth);
            row.put("dailyFee", daily);
            row.put("dailyPart", dayPart);
            row.put("otherPart", otherPart);
            row.put("total", totalForPet);
            petDetails.add(row);
        }

        // 2) 成本：cost_month 匹配指定月份
        LambdaQueryWrapper<Cost> costQw = new LambdaQueryWrapper<Cost>()
                .eq(Cost::getCostMonth, month);
        List<Cost> costs = costService.list(costQw);
        BigDecimal totalCost = costs.stream()
                .map(c -> c.getTotalCost() == null ? BigDecimal.ZERO : c.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("income", totalIncome);
        data.put("cost", totalCost);
        data.put("profit", totalIncome.subtract(totalCost));
        data.put("petsCount", pets.size());
        data.put("details", petDetails);
        data.put("costItems", costs.size());

        return ApiResponse.success(data);
    }
}


