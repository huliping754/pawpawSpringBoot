package com.petcare.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petcare.common.web.ApiResponse;
import com.petcare.finance.model.Cost;
import com.petcare.finance.model.Income;
import com.petcare.finance.service.CostService;
import com.petcare.finance.service.IncomeService;
import com.petcare.pet.model.Pet;
import com.petcare.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 财务统计接口
 * 提供财务数据统计功能
 */
@Tag(name = "财务统计", description = "财务数据统计接口")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    
    private final PetService petService;
    private final IncomeService incomeService;
    private final CostService costService;
    
    public FinanceController(PetService petService, IncomeService incomeService, CostService costService) {
        this.petService = petService;
        this.incomeService = incomeService;
        this.costService = costService;
    }
    
    @Operation(summary = "月度财务统计", description = "统计指定月份的总收入、已入账、待入账、总成本、净利润")
    @GetMapping("/monthly-stats")
    public ApiResponse<MonthlyFinanceStats> getMonthlyStats(
            @Parameter(description = "统计月份", example = "2025-09") @RequestParam String month) {
        
        // 解析月份，获取该月的开始和结束日期
        LocalDate monthStart = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        
        // 1. 计算总收入（处理跨月份订单）
        BigDecimal totalIncome = calculateMonthlyIncome(monthStart, monthEnd);
        
        // 2. 计算已入账金额
        BigDecimal settledAmount = calculateSettledAmount(monthStart, monthEnd);
        
        // 3. 计算待入账金额
        BigDecimal unsettledAmount = totalIncome.subtract(settledAmount);
        
        // 4. 计算总成本
        BigDecimal totalCost = calculateMonthlyCost(month);
        
        // 5. 计算净利润
        BigDecimal netProfit = totalIncome.subtract(totalCost);
        
        MonthlyFinanceStats stats = new MonthlyFinanceStats();
        stats.setMonth(month);
        stats.setTotalIncome(totalIncome);
        stats.setSettledAmount(settledAmount);
        stats.setUnsettledAmount(unsettledAmount);
        stats.setTotalCost(totalCost);
        stats.setNetProfit(netProfit);
        
        return ApiResponse.success(stats);
    }
    
    @Operation(summary = "总体财务统计", description = "统计所有时间段的总体财务数据：总成本、总利润、总收入")
    @GetMapping("/total-stats")
    public ApiResponse<TotalFinanceStats> getTotalStats() {
        
        // 1. 计算总收入（使用incomes表的数据，确保与宠物详情一致）
        BigDecimal totalIncome = calculateTotalIncomeFromIncomes();
        
        // 2. 计算总成本（所有成本记录的总和）
        BigDecimal totalCost = calculateTotalCost();
        
        // 3. 计算总利润
        BigDecimal totalProfit = totalIncome.subtract(totalCost);
        
        TotalFinanceStats stats = new TotalFinanceStats();
        stats.setTotalIncome(totalIncome);
        stats.setTotalCost(totalCost);
        stats.setTotalProfit(totalProfit);
        
        return ApiResponse.success(stats);
    }
    
    
    @Operation(summary = "月度订单统计", description = "统计不同月份的订单数和总收入，跨月订单会在对应月份都计算")
    @GetMapping("/monthly-orders")
    public ApiResponse<List<MonthlyOrderStats>> getMonthlyOrders() {
        
        // 查询所有宠物订单
        List<Pet> pets = petService.list();
        
        // 使用Map来存储每个月的统计数据
        java.util.Map<String, MonthlyOrderStats> monthlyStatsMap = new java.util.HashMap<>();
        
        for (Pet pet : pets) {
            // 获取订单的开始和结束月份
            String startMonth = pet.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String endMonth = pet.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // 计算订单的总收入（按过夜计算）
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate());
            
            // 处理订单跨越的所有月份
            LocalDate currentMonth = LocalDate.parse(startMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate endMonthDate = LocalDate.parse(endMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            while (!currentMonth.isAfter(endMonthDate)) {
                String currentMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                
                // 计算该月在该订单中的实际日期范围
                LocalDate monthStart = currentMonth;
                LocalDate monthEnd = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
                LocalDate orderStartInMonth = pet.getStartDate().isBefore(monthStart) ? monthStart : pet.getStartDate();
                LocalDate orderEndInMonth = pet.getEndDate().isAfter(monthEnd) ? monthEnd : pet.getEndDate();
                
                // 计算在该月的天数（按过夜计算）
                long daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(orderStartInMonth, orderEndInMonth);
                
                // 只有当该月有实际天数时才计算订单数和收入
                if (daysInMonth > 0 && totalDays > 0) {
                    // 获取或创建该月的统计数据
                    MonthlyOrderStats monthStats = monthlyStatsMap.computeIfAbsent(currentMonthStr, k -> {
                        MonthlyOrderStats stats = new MonthlyOrderStats();
                        stats.setMonth(k);
                        stats.setOrderCount(0);
                        stats.setTotalIncome(BigDecimal.ZERO);
                        return stats;
                    });
                    monthStats.setOrderCount(monthStats.getOrderCount() + 1);
                    
                    // 计算该月的收入（每日费用 × 天数 + 按比例分配的其他费用）
                    BigDecimal dailyIncomeInMonth = pet.getDailyFee().multiply(BigDecimal.valueOf(daysInMonth));
                    BigDecimal otherFeeRatio = BigDecimal.valueOf(daysInMonth).divide(BigDecimal.valueOf(totalDays), 4, BigDecimal.ROUND_HALF_UP);
                    BigDecimal proportionalOtherFee = pet.getOtherFee().multiply(otherFeeRatio);
                    BigDecimal monthIncome = dailyIncomeInMonth.add(proportionalOtherFee);
                    
                    monthStats.setTotalIncome(monthStats.getTotalIncome().add(monthIncome));
                }
                
                // 移动到下一个月
                currentMonth = currentMonth.plusMonths(1);
            }
        }
        
        // 计算每个月的总成本和总利润
        for (MonthlyOrderStats stats : monthlyStatsMap.values()) {
            // 计算该月的总成本
            BigDecimal monthCost = calculateMonthlyCost(stats.getMonth());
            stats.setTotalCost(monthCost);
            
            // 计算总利润
            BigDecimal totalProfit = stats.getTotalIncome().subtract(monthCost);
            stats.setTotalProfit(totalProfit);
        }
        
        // 转换为List并按月份排序
        List<MonthlyOrderStats> result = new java.util.ArrayList<>(monthlyStatsMap.values());
        result.sort((a, b) -> a.getMonth().compareTo(b.getMonth()));
        
        return ApiResponse.success(result);
    }
    
    @Operation(summary = "月度订单详情", description = "根据指定月份展示该月的所有宠物订单详情，跨月订单会拆分显示")
    @GetMapping("/monthly-orders-detail")
    public ApiResponse<MonthlyOrderDetailResponse> getMonthlyOrdersDetail(
            @Parameter(description = "统计月份", example = "2025-09") @RequestParam String month) {
        
        // 解析月份，获取该月的开始和结束日期
        LocalDate monthStart = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        
        // 查询所有与指定月份有重叠的宠物订单
        LambdaQueryWrapper<Pet> petQuery = new LambdaQueryWrapper<>();
        petQuery.le(Pet::getStartDate, monthEnd)  // 开始日期在月末之前
                .ge(Pet::getEndDate, monthStart);  // 结束日期在月初之后
        
        List<Pet> pets = petService.list(petQuery);
        
        // 查询该月的所有成本记录
        LambdaQueryWrapper<Cost> costQuery = new LambdaQueryWrapper<>();
        costQuery.eq(Cost::getCostMonth, month);
        List<Cost> monthlyCostList = costService.list(costQuery);
        
        // 计算该月的总成本
        BigDecimal totalMonthlyCost = monthlyCostList.stream()
                .map(cost -> cost.getTotalCost() != null ? cost.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算各项费用的总和
        BigDecimal totalWaterFee = monthlyCostList.stream()
                .map(cost -> cost.getWaterFee() != null ? cost.getWaterFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalElectricityFee = monthlyCostList.stream()
                .map(cost -> cost.getElectricityFee() != null ? cost.getElectricityFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRentFee = monthlyCostList.stream()
                .map(cost -> cost.getRentFee() != null ? cost.getRentFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalOtherCostFee = monthlyCostList.stream()
                .map(cost -> cost.getOtherFee() != null ? cost.getOtherFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 创建月度成本数据
        MonthlyCosts monthlyCosts = new MonthlyCosts();
        monthlyCosts.setWaterFee(totalWaterFee);
        monthlyCosts.setElectricityFee(totalElectricityFee);
        monthlyCosts.setRentFee(totalRentFee);
        monthlyCosts.setOtherCostFee(totalOtherCostFee);
        monthlyCosts.setTotalCost(totalMonthlyCost);
        
        // 创建订单详情列表
        List<OrderDetail> orderDetails = new java.util.ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        
        for (Pet pet : pets) {
            // 计算该订单在指定月份内的实际日期范围
            LocalDate orderStartInMonth = pet.getStartDate().isBefore(monthStart) ? monthStart : pet.getStartDate();
            LocalDate orderEndInMonth = pet.getEndDate().isAfter(monthEnd) ? monthEnd : pet.getEndDate();
            
            // 计算在指定月份内的天数（按过夜计算）
            long daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(orderStartInMonth, orderEndInMonth);
            
            // 计算订单的总天数（按过夜计算）
            long totalOrderDays = java.time.temporal.ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate());
            
            // 计算该月份的收入
            BigDecimal dailyIncome = pet.getDailyFee().multiply(BigDecimal.valueOf(daysInMonth));
            
            // 按比例分配其他费用
            BigDecimal otherFeeRatio = BigDecimal.valueOf(daysInMonth).divide(BigDecimal.valueOf(totalOrderDays), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal proportionalOtherFee = pet.getOtherFee().multiply(otherFeeRatio);
            
            BigDecimal monthIncome = dailyIncome.add(proportionalOtherFee);
            totalIncome = totalIncome.add(monthIncome);
            
            // 创建订单详情
            OrderDetail detail = new OrderDetail();
            detail.setPetId(pet.getId());
            detail.setPetName(pet.getName());
            detail.setStartDate(orderStartInMonth);
            detail.setEndDate(orderEndInMonth);
            detail.setDaysInMonth((int) daysInMonth);
            detail.setDailyFee(pet.getDailyFee());
            detail.setOtherFee(proportionalOtherFee);
            detail.setTotalIncome(monthIncome);
            detail.setOriginalStartDate(pet.getStartDate());
            detail.setOriginalEndDate(pet.getEndDate());
            detail.setIsCrossMonth(!pet.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    .equals(pet.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
            
            orderDetails.add(detail);
        }
        
        // 按宠物ID排序
        orderDetails.sort((a, b) -> a.getPetId().compareTo(b.getPetId()));
        
        // 创建月度汇总
        MonthlySummary summary = new MonthlySummary();
        summary.setTotalIncome(totalIncome);
        summary.setTotalCost(totalMonthlyCost);
        summary.setNetProfit(totalIncome.subtract(totalMonthlyCost));
        
        // 创建响应对象
        MonthlyOrderDetailResponse response = new MonthlyOrderDetailResponse();
        response.setMonth(month);
        response.setMonthlyCosts(monthlyCosts);
        response.setOrders(orderDetails);
        response.setSummary(summary);
        
        return ApiResponse.success(response);
    }
    
    
    /**
     * 从incomes表计算总收入（确保与宠物详情一致）
     */
    private BigDecimal calculateTotalIncomeFromIncomes() {
        // 查询所有收入记录
        List<Income> incomes = incomeService.list();
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        
        for (Income income : incomes) {
            if (income.getTotalAmount() != null) {
                totalIncome = totalIncome.add(income.getTotalAmount());
            }
        }
        
        return totalIncome;
    }
    
    
    /**
     * 计算总成本（所有成本记录）
     */
    private BigDecimal calculateTotalCost() {
        // 查询所有成本记录
        List<Cost> costs = costService.list();
        
        return costs.stream()
                .map(cost -> cost.getTotalCost() != null ? cost.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 计算指定月份的收入（处理跨月份订单）
     */
    private BigDecimal calculateMonthlyIncome(LocalDate monthStart, LocalDate monthEnd) {
        // 查询所有与指定月份有重叠的宠物订单
        LambdaQueryWrapper<Pet> petQuery = new LambdaQueryWrapper<>();
        petQuery.le(Pet::getStartDate, monthEnd)  // 开始日期在月末之前
                .ge(Pet::getEndDate, monthStart);  // 结束日期在月初之后
        
        List<Pet> pets = petService.list(petQuery);
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        
        for (Pet pet : pets) {
            // 计算该订单在指定月份内的天数
            LocalDate orderStart = pet.getStartDate().isBefore(monthStart) ? monthStart : pet.getStartDate();
            LocalDate orderEnd = pet.getEndDate().isAfter(monthEnd) ? monthEnd : pet.getEndDate();
            
            // 计算在指定月份内的天数（按过夜计算）
            long daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(orderStart, orderEnd);
            
            // 计算该订单在指定月份内的收入
            BigDecimal dailyIncome = pet.getDailyFee().multiply(BigDecimal.valueOf(daysInMonth));
            
            // 如果订单跨越月份，需要按比例分配其他费用
            long totalOrderDays = java.time.temporal.ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate());
            BigDecimal otherFeeRatio = BigDecimal.valueOf(daysInMonth).divide(BigDecimal.valueOf(totalOrderDays), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal proportionalOtherFee = pet.getOtherFee().multiply(otherFeeRatio);
            
            totalIncome = totalIncome.add(dailyIncome).add(proportionalOtherFee);
        }
        
        return totalIncome;
    }
    
    /**
     * 计算指定月份的已入账金额
     */
    private BigDecimal calculateSettledAmount(LocalDate monthStart, LocalDate monthEnd) {
        // 查询所有与指定月份有重叠的宠物订单对应的收入记录
        LambdaQueryWrapper<Pet> petQuery = new LambdaQueryWrapper<>();
        petQuery.le(Pet::getStartDate, monthEnd)
                .ge(Pet::getEndDate, monthStart);
        
        List<Pet> pets = petService.list(petQuery);
        
        BigDecimal totalSettled = BigDecimal.ZERO;
        
        for (Pet pet : pets) {
            // 查询该宠物的收入记录（可能有多个）
            LambdaQueryWrapper<Income> incomeQuery = new LambdaQueryWrapper<>();
            incomeQuery.eq(Income::getPetId, pet.getId());
            
            List<Income> incomes = incomeService.list(incomeQuery);
            if (!incomes.isEmpty()) {
                // 计算该订单在指定月份内的天数比例
                LocalDate orderStart = pet.getStartDate().isBefore(monthStart) ? monthStart : pet.getStartDate();
                LocalDate orderEnd = pet.getEndDate().isAfter(monthEnd) ? monthEnd : pet.getEndDate();
                
                long daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(orderStart, orderEnd);
                long totalOrderDays = java.time.temporal.ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate());
                
                BigDecimal ratio = BigDecimal.valueOf(daysInMonth).divide(BigDecimal.valueOf(totalOrderDays), 4, BigDecimal.ROUND_HALF_UP);
                
                // 累加所有收入记录的已入账金额
                for (Income income : incomes) {
                    BigDecimal proportionalSettled = income.getSettledAmount().multiply(ratio);
                    totalSettled = totalSettled.add(proportionalSettled);
                }
            }
        }
        
        return totalSettled;
    }
    
    /**
     * 计算指定月份的总成本
     */
    private BigDecimal calculateMonthlyCost(String month) {
        LambdaQueryWrapper<Cost> costQuery = new LambdaQueryWrapper<>();
        costQuery.eq(Cost::getCostMonth, month);
        
        List<Cost> costs = costService.list(costQuery);
        
        return costs.stream()
                .map(Cost::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 月度财务统计数据
     */
    public static class MonthlyFinanceStats {
        private String month;
        private BigDecimal totalIncome;
        private BigDecimal settledAmount;
        private BigDecimal unsettledAmount;
        private BigDecimal totalCost;
        private BigDecimal netProfit;
        
        // Getters and Setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        
        public BigDecimal getSettledAmount() { return settledAmount; }
        public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
        
        public BigDecimal getUnsettledAmount() { return unsettledAmount; }
        public void setUnsettledAmount(BigDecimal unsettledAmount) { this.unsettledAmount = unsettledAmount; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    }
    
    /**
     * 总体财务统计数据
     */
    public static class TotalFinanceStats {
        private BigDecimal totalIncome;
        private BigDecimal totalCost;
        private BigDecimal totalProfit;
        
        // Getters and Setters
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
    }
    
    /**
     * 月度订单统计数据
     */
    public static class MonthlyOrderStats {
        private String month;
        private Integer orderCount;
        private BigDecimal totalIncome;
        private BigDecimal totalCost;
        private BigDecimal totalProfit;
        
        // Getters and Setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        
        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
        
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
    }
    
    /**
     * 月度订单详情响应数据
     */
    public static class MonthlyOrderDetailResponse {
        private String month;
        private MonthlyCosts monthlyCosts;
        private List<OrderDetail> orders;
        private MonthlySummary summary;
        
        // Getters and Setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        
        public MonthlyCosts getMonthlyCosts() { return monthlyCosts; }
        public void setMonthlyCosts(MonthlyCosts monthlyCosts) { this.monthlyCosts = monthlyCosts; }
        
        public List<OrderDetail> getOrders() { return orders; }
        public void setOrders(List<OrderDetail> orders) { this.orders = orders; }
        
        public MonthlySummary getSummary() { return summary; }
        public void setSummary(MonthlySummary summary) { this.summary = summary; }
    }
    
    /**
     * 月度成本数据
     */
    public static class MonthlyCosts {
        private BigDecimal waterFee;
        private BigDecimal electricityFee;
        private BigDecimal rentFee;
        private BigDecimal otherCostFee;
        private BigDecimal totalCost;
        
        // Getters and Setters
        public BigDecimal getWaterFee() { return waterFee; }
        public void setWaterFee(BigDecimal waterFee) { this.waterFee = waterFee; }
        
        public BigDecimal getElectricityFee() { return electricityFee; }
        public void setElectricityFee(BigDecimal electricityFee) { this.electricityFee = electricityFee; }
        
        public BigDecimal getRentFee() { return rentFee; }
        public void setRentFee(BigDecimal rentFee) { this.rentFee = rentFee; }
        
        public BigDecimal getOtherCostFee() { return otherCostFee; }
        public void setOtherCostFee(BigDecimal otherCostFee) { this.otherCostFee = otherCostFee; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    }
    
    /**
     * 订单详情数据
     */
    public static class OrderDetail {
        private Long petId;
        private String petName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer daysInMonth;
        private BigDecimal dailyFee;
        private BigDecimal otherFee;
        private BigDecimal totalIncome;
        private LocalDate originalStartDate;
        private LocalDate originalEndDate;
        private Boolean isCrossMonth;
        
        // Getters and Setters
        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
        
        public String getPetName() { return petName; }
        public void setPetName(String petName) { this.petName = petName; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public Integer getDaysInMonth() { return daysInMonth; }
        public void setDaysInMonth(Integer daysInMonth) { this.daysInMonth = daysInMonth; }
        
        public BigDecimal getDailyFee() { return dailyFee; }
        public void setDailyFee(BigDecimal dailyFee) { this.dailyFee = dailyFee; }
        
        public BigDecimal getOtherFee() { return otherFee; }
        public void setOtherFee(BigDecimal otherFee) { this.otherFee = otherFee; }
        
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        
        public LocalDate getOriginalStartDate() { return originalStartDate; }
        public void setOriginalStartDate(LocalDate originalStartDate) { this.originalStartDate = originalStartDate; }
        
        public LocalDate getOriginalEndDate() { return originalEndDate; }
        public void setOriginalEndDate(LocalDate originalEndDate) { this.originalEndDate = originalEndDate; }
        
        public Boolean getIsCrossMonth() { return isCrossMonth; }
        public void setIsCrossMonth(Boolean isCrossMonth) { this.isCrossMonth = isCrossMonth; }
    }
    
    /**
     * 月度汇总数据
     */
    public static class MonthlySummary {
        private BigDecimal totalIncome;
        private BigDecimal totalCost;
        private BigDecimal netProfit;
        
        // Getters and Setters
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    }
}
