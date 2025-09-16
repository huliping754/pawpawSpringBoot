package com.petcare.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petcare.common.web.ApiResponse;
import com.petcare.finance.mapper.PetIncomeMapper;
import com.petcare.finance.model.Income;
import com.petcare.finance.model.vo.PetIncomeVO;
import com.petcare.finance.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 收入管理接口
 * 提供收入记录的增删改查功能
 */
@Tag(name = "收入管理", description = "收入记录管理接口")
@RestController
@RequestMapping("/api/incomes")
public class IncomeController {
    private final IncomeService incomeService;
    private final PetIncomeMapper petIncomeMapper;

    public IncomeController(IncomeService incomeService, PetIncomeMapper petIncomeMapper) {
        this.incomeService = incomeService;
        this.petIncomeMapper = petIncomeMapper;
    }

    @Operation(summary = "新增收入记录", description = "手动创建收入记录")
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody Income income) {
        return ApiResponse.success(incomeService.save(income));
    }

    @Operation(summary = "更新收入记录", description = "修改收入记录信息")
    @PutMapping
    public ApiResponse<Boolean> update(@RequestBody Income income) {
        return ApiResponse.success(incomeService.updateById(income));
    }

    @Operation(summary = "删除收入记录", description = "根据ID删除收入记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@Parameter(description = "收入记录ID") @PathVariable Long id) {
        return ApiResponse.success(incomeService.removeById(id));
    }

    @Operation(summary = "获取收入详情", description = "根据ID查询单个收入记录")
    @GetMapping("/{id}")
    public ApiResponse<Income> get(@Parameter(description = "收入记录ID") @PathVariable Long id) {
        return ApiResponse.success(incomeService.getById(id));
    }

    @Operation(summary = "分页查询收入", description = "支持按宠物ID分页查询收入记录")
    @GetMapping
    public ApiResponse<Page<Income>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "宠物ID筛选", example = "1710000000001") @RequestParam(required = false) Long petId) {
        LambdaQueryWrapper<Income> qw = new LambdaQueryWrapper<>();
        if (petId != null) qw.eq(Income::getPetId, petId);
        Page<Income> p = incomeService.page(Page.of(page, size), qw);
        return ApiResponse.success(p);
    }

    @Operation(summary = "宠物收入关联查询", description = "分页查询宠物和收入的关联信息，支持多条件筛选")
    @GetMapping("/pet-income")
    public ApiResponse<Page<PetIncomeVO>> getPetIncomePage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "宠物姓名筛选", example = "小豆") @RequestParam(required = false) String petName,
            @Parameter(description = "宠物状态筛选", example = "checkedOut") @RequestParam(required = false) String petStatus,
            @Parameter(description = "开始日期筛选", example = "2025-09-01") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期筛选", example = "2025-09-30") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "是否已结算筛选", example = "false") @RequestParam(required = false) Boolean isSettled) {
        
        // 先查询所有数据，然后在内存中筛选和分页
        List<PetIncomeVO> allData = petIncomeMapper.selectPetIncomeList(petName, petStatus, startDate, endDate, isSettled);
        
        // 内存筛选
        List<PetIncomeVO> filteredData = allData.stream()
                .filter(item -> {
                    if (petName != null && !petName.isEmpty() && !item.getPetName().contains(petName)) {
                        return false;
                    }
                    if (petStatus != null && !petStatus.isEmpty() && !petStatus.equals(item.getPetStatus())) {
                        return false;
                    }
                    if (startDate != null && item.getStartDate().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && item.getEndDate().isAfter(endDate)) {
                        return false;
                    }
                    if (isSettled != null) {
                        boolean itemSettled = item.getSettledAmount() != null && 
                                            item.getTotalAmount() != null && 
                                            item.getSettledAmount().compareTo(item.getTotalAmount()) >= 0;
                        if (isSettled != itemSettled) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // 手动分页
        int start = (int) ((page - 1) * size);
        int end = (int) Math.min(start + size, filteredData.size());
        List<PetIncomeVO> pageData = start < filteredData.size() ? 
                filteredData.subList(start, end) : new java.util.ArrayList<>();
        
        Page<PetIncomeVO> result = new Page<>(page, size, filteredData.size());
        result.setRecords(pageData);
        
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询指定宠物的收入", description = "根据宠物ID查询该宠物的收入记录")
    @GetMapping("/pet/{petId}")
    public ApiResponse<PetIncomeVO> getPetIncome(@Parameter(description = "宠物ID", example = "1710000000001") @PathVariable Long petId) {
        PetIncomeVO result = petIncomeMapper.selectPetIncomeByPetId(petId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新结算状态", description = "更新指定收入记录的已结算金额")
    @PutMapping("/{id}/settle")
    public ApiResponse<Boolean> settleIncome(
            @Parameter(description = "收入记录ID", example = "1710000000001") @PathVariable Long id,
            @Parameter(description = "结算金额", example = "260.00") @RequestParam java.math.BigDecimal amount) {
        
        Income income = incomeService.getById(id);
        if (income == null) {
            return ApiResponse.fail("收入记录不存在");
        }
        
        if (amount.compareTo(income.getTotalAmount()) > 0) {
            return ApiResponse.fail("结算金额不能超过总金额");
        }
        
        income.setSettledAmount(amount);
        boolean success = incomeService.updateById(income);
        return ApiResponse.success(success);
    }
}


