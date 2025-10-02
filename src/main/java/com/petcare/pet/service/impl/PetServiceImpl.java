package com.petcare.pet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petcare.finance.model.Income;
import com.petcare.finance.mapper.IncomeMapper;
import com.petcare.pet.mapper.PetMapper;
import com.petcare.pet.model.Pet;
import com.petcare.pet.service.PetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements PetService {

    private final IncomeMapper incomeMapper;

    public PetServiceImpl(IncomeMapper incomeMapper) {
        this.incomeMapper = incomeMapper;
    }

    @Override
    public boolean checkIn(Long id) {
        Pet pet = getById(id);
        if (pet == null) return false;
        pet.setStatus("checkedIn");
        return updateById(pet);
    }

    @Override
    @Transactional
    public boolean checkOutAndCreateIncome(Long id) {
        Pet pet = getById(id);
        if (pet == null) return false;
        // 按过夜天数计算费用，例如：20-23号 = 3个晚上
        long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
        BigDecimal total = pet.getDailyFee().multiply(BigDecimal.valueOf(overnightDays))
                .add(pet.getOtherFee() != null ? pet.getOtherFee() : BigDecimal.ZERO);

        // 如果该宠物已有收入记录，改为更新而不是再次插入，避免重复记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(Income::getPetId, pet.getId());
        Income existing = incomeMapper.selectOne(qw);
        if (existing != null) {
            existing.setDailyFee(pet.getDailyFee());
            existing.setOtherFee(pet.getOtherFee());
            if (pet.getTotalFee() != null) {
                existing.setTotalFee(pet.getTotalFee());
            }
            existing.setDaysStayed((int) overnightDays);
            existing.setTotalAmount(total);
            existing.setRemark(pet.getRemark());
            existing.setUpdatedAt(LocalDateTime.now());
            incomeMapper.updateById(existing);
        } else {
            Income income = new Income();
            income.setId(System.currentTimeMillis());
            income.setPetId(pet.getId());
            income.setDailyFee(pet.getDailyFee());
            income.setOtherFee(pet.getOtherFee());
            income.setTotalFee(pet.getTotalFee());
            income.setDaysStayed((int) overnightDays);
            income.setTotalAmount(total);
            income.setSettledAmount(BigDecimal.ZERO);
            income.setRemark(pet.getRemark());
            income.setCreatedAt(LocalDateTime.now());
            income.setUpdatedAt(LocalDateTime.now());
            incomeMapper.insert(income);
        }
        // 离店：更新宠物状态为 checkedOut
        pet.setStatus("checkedOut");
        updateById(pet);
        return true;
    }

    @Override
    @Transactional
    public boolean savePetWithIncome(Pet pet) {
        // 1. 保存宠物信息
        if (pet.getId() == null) {
            pet.setId(System.currentTimeMillis()); // 生成唯一ID
        }
        pet.setCreatedAt(LocalDateTime.now());
        pet.setUpdatedAt(LocalDateTime.now());
        boolean petSaved = save(pet);
        
        if (!petSaved) {
            return false;
        }

        // 2. 自动计算总金额并创建收入记录（若已存在则不再重复创建，交由后续更新逻辑维护）
        BigDecimal totalAmount;
        long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
        
        // 如果前端传入了totalFee，则使用totalFee + 其他费用
        if (pet.getTotalFee() != null) {
            totalAmount = pet.getTotalFee().add(pet.getOtherFee() != null ? pet.getOtherFee() : BigDecimal.ZERO);
        } else {
            // 否则按过夜天数计算费用，例如：20-23号 = 3个晚上
            totalAmount = pet.getDailyFee().multiply(BigDecimal.valueOf(overnightDays)).add(pet.getOtherFee() != null ? pet.getOtherFee() : BigDecimal.ZERO);
        }

        Income income = new Income();
        income.setId(System.currentTimeMillis() + 1); // 确保与宠物ID不同
        income.setPetId(pet.getId());
        income.setDailyFee(pet.getDailyFee());
        income.setOtherFee(pet.getOtherFee());
        income.setTotalFee(pet.getTotalFee());
        income.setDaysStayed((int) overnightDays);
        income.setTotalAmount(totalAmount);
        income.setSettledAmount(BigDecimal.ZERO); // 初始未结算
        income.setRemark(pet.getRemark());
        income.setCreatedAt(LocalDateTime.now());
        income.setUpdatedAt(LocalDateTime.now());

        // 检查是否已存在该宠物的收入记录，避免重复插入
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(Income::getPetId, pet.getId());
        Income existed = incomeMapper.selectOne(qw);
        if (existed == null) {
            // 仅当不存在时插入
            int result = incomeMapper.insert(income);
            return result > 0;
        }
        return true;
    }

    @Override
    @Transactional
    public boolean updatePetWithIncome(Pet pet) {
        System.out.println("=== updatePetWithIncome 开始 ===");
        System.out.println("宠物ID: " + pet.getId());
        System.out.println("totalFee: " + pet.getTotalFee());
        System.out.println("dailyFee: " + pet.getDailyFee());
        System.out.println("otherFee: " + pet.getOtherFee());
        System.out.println("startDate: " + pet.getStartDate());
        System.out.println("endDate: " + pet.getEndDate());
        
        // 1. 更新宠物信息
        pet.setUpdatedAt(LocalDateTime.now());
        boolean petUpdated = updateById(pet);
        
        if (!petUpdated) {
            return false;
        }

        // 2. 查找并更新对应的收入记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(Income::getPetId, pet.getId());
        List<Income> existingIncomes = incomeMapper.selectList(queryWrapper);

        if (!existingIncomes.isEmpty()) {
            // 如果有多个收入记录，使用第一个（通常应该只有一个）
            Income existingIncome = existingIncomes.get(0);
            // 3. 重新计算总金额并更新收入记录
            // 需求：如果传了totalFee，优先使用；否则用(日均费用 × 天数 + 其他费用)
            // 触发重算的条件：传入了 totalFee 或者 (dailyFee/otherFee 任一非空) 或者 传入了新的起止日期
            boolean hasDateFields = pet.getStartDate() != null && pet.getEndDate() != null;
            boolean hasTotalFee = pet.getTotalFee() != null;
            boolean feeChanged = pet.getDailyFee() != null || pet.getOtherFee() != null;
            System.out.println("条件判断 - hasDateFields: " + hasDateFields + ", hasTotalFee: " + hasTotalFee + ", feeChanged: " + feeChanged);

            if (hasTotalFee || hasDateFields || feeChanged) {
                BigDecimal totalAmount;

                // 选取参与计算的单价与其他费用（优先取前端传入，否则沿用原收入记录的值）
                BigDecimal useDailyFee = pet.getDailyFee() != null ? pet.getDailyFee() : existingIncome.getDailyFee();
                BigDecimal useOtherFee = pet.getOtherFee() != null ? pet.getOtherFee() : existingIncome.getOtherFee();

                // 选取用于计算的天数：若传入新日期则按新日期计算；否则沿用原 daysStayed
                long overnightDays;
                if (hasDateFields) {
                    overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
                } else {
                    overnightDays = existingIncome.getDaysStayed() != null ? existingIncome.getDaysStayed() : 0;
                }

                if (hasTotalFee) {
                    System.out.println("使用totalFee计算: " + pet.getTotalFee() + " + " + (useOtherFee != null ? useOtherFee : BigDecimal.ZERO));
                    totalAmount = pet.getTotalFee().add(useOtherFee != null ? useOtherFee : BigDecimal.ZERO);
                    // 显式记录原始总价
                    existingIncome.setTotalFee(pet.getTotalFee());
                } else {
                    System.out.println("使用日均费用计算: " + useDailyFee + " × " + overnightDays + " + " + (useOtherFee != null ? useOtherFee : BigDecimal.ZERO));
                    totalAmount = (useDailyFee != null ? useDailyFee : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(overnightDays))
                            .add(useOtherFee != null ? useOtherFee : BigDecimal.ZERO);
                }

                System.out.println("最终计算的总金额: " + totalAmount);

                // 回写字段
                if (pet.getDailyFee() != null) {
                    existingIncome.setDailyFee(pet.getDailyFee());
                }
                if (pet.getOtherFee() != null) {
                    existingIncome.setOtherFee(pet.getOtherFee());
                }
                if (hasDateFields) {
                    existingIncome.setDaysStayed((int) overnightDays);
                }
                existingIncome.setTotalAmount(totalAmount);
                // 保障数据库约束：已结算金额不得大于总金额
                if (pet.getInputSettledAmount() == null) {
                    BigDecimal currentSettled = existingIncome.getSettledAmount();
                    if (currentSettled != null && currentSettled.compareTo(totalAmount) > 0) {
                        existingIncome.setSettledAmount(totalAmount);
                        System.out.println("自动纠正已结算金额以满足约束: settled=" + currentSettled + ", total=" + totalAmount);
                    }
                }
                if (pet.getRemark() != null) {
                    existingIncome.setRemark(pet.getRemark());
                }
                existingIncome.setUpdatedAt(LocalDateTime.now());

                if (pet.getInputSettledAmount() != null) {
                    existingIncome.setSettledAmount(pet.getInputSettledAmount());
                    System.out.println("更新已收入金额: " + pet.getInputSettledAmount());
                }
            } else {
                // 不触发重算，仅同步可变更的非关键字段
                if (pet.getDailyFee() != null) {
                    existingIncome.setDailyFee(pet.getDailyFee());
                }
                if (pet.getOtherFee() != null) {
                    existingIncome.setOtherFee(pet.getOtherFee());
                }
                if (pet.getRemark() != null) {
                    existingIncome.setRemark(pet.getRemark());
                }
                existingIncome.setUpdatedAt(LocalDateTime.now());
                if (pet.getInputSettledAmount() != null) {
                    existingIncome.setSettledAmount(pet.getInputSettledAmount());
                    System.out.println("更新已收入金额: " + pet.getInputSettledAmount());
                }
            }

            // 4. 更新收入记录
            int result = incomeMapper.updateById(existingIncome);
            return result > 0;
        } else {
            // 如果没有找到对应的收入记录，创建一个新的
            // 只有当日期字段和费用字段都不为空时才创建收入记录
            if (pet.getStartDate() != null && pet.getEndDate() != null && 
                pet.getDailyFee() != null && pet.getOtherFee() != null) {
                
                BigDecimal totalAmount;
                long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
                
                // 如果前端传入了totalFee，则使用totalFee + 其他费用
                if (pet.getTotalFee() != null) {
                    totalAmount = pet.getTotalFee().add(pet.getOtherFee());
                } else {
                    // 否则按过夜天数计算费用，例如：20-23号 = 3个晚上
                    totalAmount = pet.getDailyFee().multiply(BigDecimal.valueOf(overnightDays)).add(pet.getOtherFee());
                }

                Income newIncome = new Income();
                newIncome.setId(System.currentTimeMillis() + 2); // 确保ID唯一
                newIncome.setPetId(pet.getId());
                newIncome.setDailyFee(pet.getDailyFee());
                newIncome.setOtherFee(pet.getOtherFee());
                newIncome.setDaysStayed((int) overnightDays);
                newIncome.setTotalAmount(totalAmount);
                newIncome.setSettledAmount(BigDecimal.ZERO);
                newIncome.setRemark(pet.getRemark());
                newIncome.setCreatedAt(LocalDateTime.now());
                newIncome.setUpdatedAt(LocalDateTime.now());

                int result = incomeMapper.insert(newIncome);
                return result > 0;
            } else {
                // 如果日期字段为空，无法创建收入记录，但宠物更新成功
                return true;
            }
        }
    }

    @Override
    @Transactional
    public boolean syncAllPetsToIncome() {
        // 1. 查询所有宠物
        java.util.List<Pet> allPets = list();
        
        for (Pet pet : allPets) {
            // 2. 检查是否已存在对应的收入记录
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Income> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            queryWrapper.eq(Income::getPetId, pet.getId());
            Income existingIncome = incomeMapper.selectOne(queryWrapper);

            if (existingIncome == null) {
                // 3. 如果没有收入记录，创建一个新的
                BigDecimal totalAmount;
                long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
                
                // 如果前端传入了totalFee，则使用totalFee + 其他费用
                if (pet.getTotalFee() != null) {
                    totalAmount = pet.getTotalFee().add(pet.getOtherFee() != null ? pet.getOtherFee() : BigDecimal.ZERO);
                } else {
                    // 否则按过夜天数计算费用，例如：20-23号 = 3个晚上
                    totalAmount = pet.getDailyFee().multiply(BigDecimal.valueOf(overnightDays)).add(pet.getOtherFee() != null ? pet.getOtherFee() : BigDecimal.ZERO);
                }

                Income newIncome = new Income();
                newIncome.setId(System.currentTimeMillis() + pet.getId()); // 确保ID唯一
                newIncome.setPetId(pet.getId());
                newIncome.setDailyFee(pet.getDailyFee());
                newIncome.setOtherFee(pet.getOtherFee());
                newIncome.setDaysStayed((int) overnightDays);
                newIncome.setTotalAmount(totalAmount);
                newIncome.setSettledAmount(BigDecimal.ZERO);
                newIncome.setRemark(pet.getRemark());
                newIncome.setCreatedAt(LocalDateTime.now());
                newIncome.setUpdatedAt(LocalDateTime.now());

                incomeMapper.insert(newIncome);
            } else {
                // 4. 如果存在收入记录，重新计算并更新（按过夜收费）
                // 按过夜天数计算费用，例如：20-23号 = 3个晚上
        long overnightDays = Math.max(0, ChronoUnit.DAYS.between(pet.getStartDate(), pet.getEndDate()));
                BigDecimal totalAmount = pet.getDailyFee().multiply(BigDecimal.valueOf(overnightDays)).add(pet.getOtherFee());

                existingIncome.setDailyFee(pet.getDailyFee());
                existingIncome.setOtherFee(pet.getOtherFee());
                existingIncome.setDaysStayed((int) overnightDays);
                existingIncome.setTotalAmount(totalAmount);
                existingIncome.setRemark(pet.getRemark());
                existingIncome.setUpdatedAt(LocalDateTime.now());

                incomeMapper.updateById(existingIncome);
            }
        }
        return true;
    }
}


