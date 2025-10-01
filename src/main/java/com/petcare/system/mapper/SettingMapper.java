package com.petcare.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcare.system.model.Setting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SettingMapper extends BaseMapper<Setting> {
    
    @Update("UPDATE settings SET value = #{value}, updated_at = #{updatedAt} WHERE \"key\" = #{key}")
    int updateSetting(Setting setting);
    
    @Select("SELECT \"key\", value, updated_at FROM settings WHERE \"key\" = #{key}")
    Setting selectByKey(String key);
}