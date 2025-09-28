package com.petcare.system.controller;

import com.petcare.common.web.ApiResponse;
import com.petcare.system.mapper.SettingMapper;
import com.petcare.system.model.Setting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 系统配置管理接口
 * 提供系统配置的增删改查功能
 */
@Tag(name = "系统配置", description = "系统配置管理接口")
@RestController
@RequestMapping("/api/settings")
public class SettingController {
    private final SettingMapper settingMapper;

    public SettingController(SettingMapper settingMapper) {
        this.settingMapper = settingMapper;
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
}


