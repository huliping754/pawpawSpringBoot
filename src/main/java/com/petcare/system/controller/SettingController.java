package com.petcare.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petcare.common.web.ApiResponse;
import com.petcare.system.mapper.SettingMapper;
import com.petcare.system.model.Setting;
import com.petcare.system.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置管理接口
 * 提供系统配置的增删改查功能
 */
@Tag(name = "系统配置", description = "系统配置管理接口")
@RestController
@RequestMapping("/api/settings")
public class SettingController {
    private final SettingService settingService;
    private final SettingMapper settingMapper;

    public SettingController(SettingService settingService, SettingMapper settingMapper) {
        this.settingService = settingService;
        this.settingMapper = settingMapper;
    }

    @Operation(summary = "获取所有配置", description = "获取系统中所有配置项")
    @GetMapping
    public ApiResponse<List<Setting>> list() {
        return ApiResponse.success(settingService.list());
    }

    @Operation(summary = "获取单个配置", description = "根据配置键获取单个配置项")
    @GetMapping("/{key}")
    public ApiResponse<Setting> get(@Parameter(description = "配置键", example = "max_capacity") @PathVariable String key) {
        return ApiResponse.success(settingService.getById(key));
    }

    @Operation(summary = "新增配置", description = "创建新的配置项")
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody Setting setting) {
        return ApiResponse.success(settingService.save(setting));
    }

    @Operation(summary = "更新配置", description = "修改现有配置项，系统自动设置更新时间")
    @PutMapping
    public ApiResponse<Boolean> update(@RequestBody Setting setting) {
        // 自动设置更新时间
        setting.setUpdatedAt(LocalDateTime.now());
        // 使用自定义的更新方法，正确处理key字段
        int result = settingMapper.updateSetting(setting);
        return ApiResponse.success(result > 0);
    }

    @Operation(summary = "删除配置", description = "根据配置键删除配置项")
    @DeleteMapping("/{key}")
    public ApiResponse<Boolean> delete(@Parameter(description = "配置键", example = "max_capacity") @PathVariable String key) {
        return ApiResponse.success(settingService.removeById(key));
    }
}


