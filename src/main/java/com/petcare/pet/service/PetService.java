package com.petcare.pet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petcare.pet.model.Pet;

public interface PetService extends IService<Pet> {
    boolean checkIn(Long id);
    boolean checkOutAndCreateIncome(Long id);
    boolean savePetWithIncome(Pet pet);
    boolean updatePetWithIncome(Pet pet);
    boolean syncAllPetsToIncome();
}


