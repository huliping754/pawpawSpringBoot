package com.petcare.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petcare.common.web.ApiResponse;
import com.petcare.finance.model.Cost;
import com.petcare.finance.service.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成本管理接口
 * 提供成本支出的增删改查功能
 */
@Tag(name = "成本管理", description = "成本支出记录管理接口")
@RestController
@RequestMapping("/api/costs")
public class CostController {
    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @Operation(summary = "新增成本记录", description = "创建新的成本支出记录")
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody Cost cost) {
        return ApiResponse.success(costService.save(cost));
    }

    @Operation(summary = "更新成本记录", description = "修改成本支出记录")
    @PutMapping
    public ApiResponse<Boolean> update(@RequestBody Cost cost) {
        return ApiResponse.success(costService.updateById(cost));
    }

    @Operation(summary = "删除成本记录", description = "根据ID删除成本记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@Parameter(description = "成本记录ID") @PathVariable Long id) {
        return ApiResponse.success(costService.removeById(id));
    }

    @Operation(summary = "获取成本详情", description = "根据ID查询单个成本记录")
    @GetMapping("/{id}")
    public ApiResponse<Cost> get(@Parameter(description = "成本记录ID") @PathVariable Long id) {
        return ApiResponse.success(costService.getById(id));
    }

    @Operation(summary = "查询成本记录", description = "支持按月份查询成本记录")
    @GetMapping
    public ApiResponse<List<Cost>> list(
            @Parameter(description = "成本月份", example = "2025-09") @RequestParam(required = false) String costMonth) {
        LambdaQueryWrapper<Cost> qw = new LambdaQueryWrapper<>();
        if (costMonth != null && !costMonth.trim().isEmpty()) {
            qw.eq(Cost::getCostMonth, costMonth);
        }
        List<Cost> costs = costService.list(qw);
        return ApiResponse.success(costs);
    }
}


