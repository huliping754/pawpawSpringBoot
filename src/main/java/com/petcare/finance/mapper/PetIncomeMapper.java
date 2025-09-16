package com.petcare.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petcare.finance.model.vo.PetIncomeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 宠物收入关联查询 Mapper
 */
@Mapper
public interface PetIncomeMapper extends BaseMapper<Object> {
    
    /**
     * 分页查询宠物收入关联信息
     * @param page 分页对象
     * @param petName 宠物姓名筛选（可选）
     * @param petStatus 宠物状态筛选（可选）
     * @param startDate 开始日期筛选（可选）
     * @param endDate 结束日期筛选（可选）
     * @param isSettled 是否已结算筛选（可选，null表示全部）
     * @return 分页结果
     */
    @Select("SELECT " +
            "  p.id as petId, " +
            "  p.name as petName, " +
            "  p.breed as petBreed, " +
            "  p.start_date as startDate, " +
            "  p.end_date as endDate, " +
            "  p.status as petStatus, " +
            "  i.id as incomeId, " +
            "  i.daily_fee as dailyFee, " +
            "  i.other_fee as otherFee, " +
            "  i.total_amount as totalAmount, " +
            "  i.days_stayed as daysStayed, " +
            "  i.settled_amount as settledAmount, " +
            "  (i.total_amount - i.settled_amount) as unsettledAmount, " +
            "  i.remark as incomeRemark, " +
            "  i.created_at as incomeCreatedAt " +
            "FROM pets p " +
            "LEFT JOIN incomes i ON p.id = i.pet_id " +
            "ORDER BY i.created_at DESC, p.created_at DESC")
    List<PetIncomeVO> selectPetIncomeList(@Param("petName") String petName,
                                          @Param("petStatus") String petStatus,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("isSettled") Boolean isSettled);
    
    /**
     * 查询指定宠物的收入记录
     * @param petId 宠物ID
     * @return 宠物收入信息
     */
    @Select("SELECT " +
            "  p.id as petId, " +
            "  p.name as petName, " +
            "  p.breed as petBreed, " +
            "  p.start_date as startDate, " +
            "  p.end_date as endDate, " +
            "  p.status as petStatus, " +
            "  i.id as incomeId, " +
            "  i.daily_fee as dailyFee, " +
            "  i.other_fee as otherFee, " +
            "  i.total_amount as totalAmount, " +
            "  i.days_stayed as daysStayed, " +
            "  i.settled_amount as settledAmount, " +
            "  (i.total_amount - i.settled_amount) as unsettledAmount, " +
            "  i.remark as incomeRemark, " +
            "  i.created_at as incomeCreatedAt " +
            "FROM pets p " +
            "LEFT JOIN incomes i ON p.id = i.pet_id " +
            "WHERE p.id = #{petId}")
    PetIncomeVO selectPetIncomeByPetId(@Param("petId") Long petId);
}
