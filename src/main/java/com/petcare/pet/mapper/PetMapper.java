package com.petcare.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcare.pet.model.Pet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PetMapper extends BaseMapper<Pet> {
}


