package com.petcare.pet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petcare.common.web.ApiResponse;
import com.petcare.finance.model.Income;
import com.petcare.finance.service.IncomeService;
import com.petcare.pet.model.Pet;
import com.petcare.pet.model.vo.PetListResponse;
import com.petcare.pet.service.PetService;
import com.petcare.system.mapper.SettingMapper;
import com.petcare.system.model.Setting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 宠物管理接口
 * 提供宠物的增删改查、入住离店、导出等功能
 */
@Tag(name = "宠物管理", description = "宠物预约、入住、离店等管理接口")
@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;
    private final IncomeService incomeService;
    private final SettingMapper settingMapper;

    public PetController(PetService petService, IncomeService incomeService, SettingMapper settingMapper) {
        this.petService = petService;
        this.incomeService = incomeService;
        this.settingMapper = settingMapper;
    }

    @Operation(summary = "新增宠物预约", description = "创建新的宠物预约记录，支持前端指定状态（booked或checkedIn），自动计算总金额并同步到收入表")
    @PostMapping
    public ApiResponse<Pet> create(@RequestBody Pet pet) {
        // 验证前端传入的状态
        if (pet.getStatus() == null || pet.getStatus().trim().isEmpty()) {
            // 如果前端没有传入状态，默认为booked
            pet.setStatus("booked");
        } else {
            // 验证状态值是否有效
            String status = pet.getStatus().trim();
            if (!"booked".equals(status) && !"checkedIn".equals(status)) {
                return ApiResponse.fail("状态值无效，只支持 'booked' 或 'checkedIn'");
            }
            pet.setStatus(status);
        }
        
        boolean success = petService.savePetWithIncome(pet);
        if (success && pet.getId() != null) {
            // 重新查询宠物信息以确保数据完整性
            Pet savedPet = petService.getById(pet.getId());
            calculateStayDays(savedPet);
            return ApiResponse.success(savedPet);
        }
        return ApiResponse.fail("创建失败");
    }

    @Operation(summary = "更新宠物信息", description = "修改宠物预约或入住信息，自动重新计算总金额并同步到收入表，支持更新已收入金额")
    @PutMapping
    public ApiResponse<Pet> update(@RequestBody Pet pet) {
        boolean success = petService.updatePetWithIncome(pet);
        if (success && pet.getId() != null) {
            // 重新查询宠物信息以确保数据完整性
            Pet updatedPet = petService.getById(pet.getId());
            calculateStayDays(updatedPet);
            return ApiResponse.success(updatedPet);
        }
        return ApiResponse.fail("更新失败");
    }

    @Operation(summary = "删除宠物记录", description = "根据ID删除宠物记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@Parameter(description = "宠物ID") @PathVariable Long id) {
        return ApiResponse.success(petService.removeById(id));
    }

    @Operation(summary = "获取宠物详情", description = "根据ID查询单个宠物信息")
    @GetMapping("/{id}")
    public ApiResponse<Pet> get(@Parameter(description = "宠物ID") @PathVariable Long id) {
        Pet pet = petService.getById(id);
        if (pet != null) {
            calculateStayDays(pet);
        }
        return ApiResponse.success(pet);
    }

    @Operation(summary = "分页查询宠物", description = "支持按状态、日期等条件分页查询宠物列表，包含统计信息")
    @GetMapping
    public ApiResponse<PetListResponse> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "单个状态筛选", example = "booked") @RequestParam(required = false) String status,
            @Parameter(description = "多个状态筛选(逗号分隔)", example = "booked,checkedIn") @RequestParam(required = false) String statuses,
            @Parameter(description = "开始日期筛选", example = "2025-09-01") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期筛选", example = "2025-09-30") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LambdaQueryWrapper<Pet> qw = new LambdaQueryWrapper<Pet>();
        if (status != null && !status.isEmpty()) {
            qw.eq(Pet::getStatus, status);
        }
        if (statuses != null && !statuses.isEmpty()) {
            String[] arr = statuses.split(",");
            qw.in(Pet::getStatus, Arrays.asList(arr));
        }
        if (startDate != null) qw.ge(Pet::getStartDate, startDate);
        if (endDate != null) qw.le(Pet::getEndDate, endDate);
        Page<Pet> p = petService.page(Page.of(page, size), qw);
        
        // 计算每个宠物的寄养天数、总费用和已入账金额
        calculateStayDaysForList(p.getRecords());
        
        // 创建包含统计信息的响应对象
        PetListResponse response = new PetListResponse(p);
        
        return ApiResponse.success(response);
    }

    @Operation(summary = "宠物入住", description = "将预约状态的宠物标记为已入住")
    @PostMapping("/{id}/checkin")
    public ApiResponse<Pet> checkIn(@Parameter(description = "宠物ID") @PathVariable Long id) {
        boolean success = petService.checkIn(id);
        if (success) {
            Pet pet = petService.getById(id);
            calculateStayDays(pet);
            return ApiResponse.success(pet);
        }
        return ApiResponse.fail("入住操作失败");
    }

    @Operation(summary = "宠物离店", description = "宠物离店并自动生成收入记录")
    @PostMapping("/{id}/checkout")
    public ApiResponse<Pet> checkOut(@Parameter(description = "宠物ID") @PathVariable Long id) {
        boolean success = petService.checkOutAndCreateIncome(id);
        if (success) {
            Pet pet = petService.getById(id);
            calculateStayDays(pet);
            return ApiResponse.success(pet);
        }
        return ApiResponse.fail("离店操作失败");
    }


    @Operation(summary = "查询容量状态", description = "查询指定日期的宠物容量使用情况，自动从配置表获取最大容量，包含已入住和已预约的宠物姓名")
    @GetMapping("/capacity")
    public ApiResponse<Map<String, Object>> capacity(
            @Parameter(description = "查询日期", example = "2025-09-15") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        // 从settings表获取最大容量配置
        int maxCapacity = getMaxCapacityFromSettings();
        
        // 规则：入住日区间采用 [start_date, end_date] 闭区间
        // 统计该日预约与在住数量，结束日期当天也算在店内
        LambdaQueryWrapper<Pet> bookedQw = new LambdaQueryWrapper<Pet>()
                .eq(Pet::getStatus, "booked")
                .le(Pet::getStartDate, date)
                .ge(Pet::getEndDate, date);

        LambdaQueryWrapper<Pet> inQw = new LambdaQueryWrapper<Pet>()
                .eq(Pet::getStatus, "checkedIn")
                .le(Pet::getStartDate, date)
                .ge(Pet::getEndDate, date);

        long bookedCount = petService.count(bookedQw);
        long checkedInCount = petService.count(inQw);
        long occupied = bookedCount + checkedInCount;
        long available = Math.max(0, (long) maxCapacity - occupied);

        // 查询已预约的宠物姓名列表
        List<Pet> bookedPets = petService.list(bookedQw);
        List<String> bookedPetNames = bookedPets.stream()
                .map(Pet::getName)
                .collect(Collectors.toList());

        // 查询已入住的宠物姓名列表
        List<Pet> checkedInPets = petService.list(inQw);
        List<String> checkedInPetNames = checkedInPets.stream()
                .map(Pet::getName)
                .collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("date", date.toString());
        resp.put("maxCapacity", maxCapacity);
        resp.put("bookedCount", bookedCount);
        resp.put("checkedInCount", checkedInCount);
        resp.put("availableCount", available);
        resp.put("bookedPetNames", bookedPetNames);
        resp.put("checkedInPetNames", checkedInPetNames);
        return ApiResponse.success(resp);
    }

    @Operation(summary = "查询某月每日容量状态", description = "按月份一次性返回该月每天的容量使用情况，包含预约与在住数量及可用容量")
    @GetMapping("/capacity/month")
    public ApiResponse<Map<String, Object>> capacityByMonth(
            @Parameter(description = "月份，格式 yyyy-MM", example = "2025-10") @RequestParam String month) {

        YearMonth ym = YearMonth.parse(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        int maxCapacity = getMaxCapacityFromSettings();

        // 一次查询出与该月有交集的所有预约与在住订单
        LambdaQueryWrapper<Pet> overlapQw = new LambdaQueryWrapper<Pet>()
                .le(Pet::getStartDate, monthEnd)
                .ge(Pet::getEndDate, monthStart)
                .in(Pet::getStatus, Arrays.asList("booked", "checkedIn"));
        List<Pet> overlappedPets = petService.list(overlapQw);

        // 为该月每天统计容量
        List<Map<String, Object>> days = new java.util.ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate day = ym.atDay(d);

            long bookedCount = overlappedPets.stream()
                    .filter(p -> "booked".equals(p.getStatus()))
                    .filter(p -> !p.getStartDate().isAfter(day) && !p.getEndDate().isBefore(day))
                    .count();

            long checkedInCount = overlappedPets.stream()
                    .filter(p -> "checkedIn".equals(p.getStatus()))
                    .filter(p -> !p.getStartDate().isAfter(day) && !p.getEndDate().isBefore(day))
                    .count();

            long occupied = bookedCount + checkedInCount;
            long available = Math.max(0, (long) maxCapacity - occupied);

            Map<String, Object> item = new HashMap<>();
            item.put("date", day.toString());
            item.put("bookedCount", bookedCount);
            item.put("checkedInCount", checkedInCount);
            item.put("availableCount", available);
            days.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("maxCapacity", maxCapacity);
        result.put("days", days);
        return ApiResponse.success(result);
    }


    /**
     * 计算并设置宠物的寄养天数、总费用和已入账金额（按过夜计算）
     * @param pet 宠物对象
     */
    private void calculateStayDays(Pet pet) {
        if (pet.getStartDate() != null && pet.getEndDate() != null) {
            // 计算寄养天数（按过夜计算）
            long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
            pet.setStayDays((int) overnightDays);
        } else {
            pet.setStayDays(0);
        }
        
        // 查询已入账金额、寄养费用(totalFee)与总金额（从incomes表获取准确数据）
        BigDecimal settledAmount = getSettledAmountByPetId(pet.getId());
        BigDecimal totalAmount = getTotalAmountByPetId(pet.getId());
        BigDecimal totalFee = getTotalFeeByPetId(pet.getId());
        
        pet.setSettledAmount(settledAmount);
        pet.setTotalAmount(totalAmount);
        pet.setTotalFee(totalFee);
    }
    
    /**
     * 根据宠物ID查询已入账金额
     * @param petId 宠物ID
     * @return 已入账金额，如果没有记录则返回0
     */
    private BigDecimal getSettledAmountByPetId(Long petId) {
        if (petId == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 直接查询收入表，避免LEFT JOIN的问题
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            queryWrapper.eq(Income::getPetId, petId);
            List<Income> incomes = incomeService.list(queryWrapper);
            
            if (!incomes.isEmpty()) {
                // 计算所有收入记录的已入账金额总和
                BigDecimal totalSettledAmount = incomes.stream()
                    .map(income -> income.getSettledAmount() != null ? income.getSettledAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return totalSettledAmount;
            }
        } catch (Exception e) {
            // 如果查询失败，返回0
            System.err.println("查询已入账金额失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 根据宠物ID查询总金额
     * @param petId 宠物ID
     * @return 总金额，如果没有记录则返回0
     */
    private BigDecimal getTotalAmountByPetId(Long petId) {
        if (petId == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            // 直接查询收入表，避免LEFT JOIN的问题
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            queryWrapper.eq(Income::getPetId, petId);
            List<Income> incomes = incomeService.list(queryWrapper);
            
            if (!incomes.isEmpty()) {
                // 计算所有收入记录的总金额总和
                BigDecimal totalAmount = incomes.stream()
                    .map(income -> income.getTotalAmount() != null ? income.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return totalAmount;
            }
        } catch (Exception e) {
            // 如果查询失败，返回0
            System.err.println("查询总金额失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 根据宠物ID查询前端传入的寄养费用(totalFee)
     */
    private BigDecimal getTotalFeeByPetId(Long petId) {
        if (petId == null) {
            return null;
        }
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            qw.eq(Income::getPetId, petId);
            List<Income> incomes = incomeService.list(qw);
            if (!incomes.isEmpty()) {
                // 若存在多条，取第一条非空值；也可按时间排序后取最新一条
                for (Income income : incomes) {
                    if (income.getTotalFee() != null) {
                        return income.getTotalFee();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查询totalFee失败: " + e.getMessage());
        }
        return null;
    }
    

    /**
     * 批量计算并设置宠物列表的寄养天数、总费用和已入账金额
     * @param pets 宠物列表
     */
    private void calculateStayDaysForList(List<Pet> pets) {
        if (pets != null) {
            pets.forEach(this::calculateStayDays);
        }
    }
    
    /**
     * 从settings表获取最大容量配置
     * @return 最大容量，如果获取失败则返回默认值10
     */
    private int getMaxCapacityFromSettings() {
        try {
            // 使用自定义查询方法，避免key字段的SQL语法问题
            Setting setting = settingMapper.selectByKey("max_capacity");
            if (setting != null && setting.getValue() != null) {
                return Integer.parseInt(setting.getValue());
            }
        } catch (Exception e) {
            System.err.println("获取最大容量配置失败: " + e.getMessage());
        }
        // 如果获取失败，返回默认值
        return 10;
    }
}


