package com.petcare.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petcare.finance.mapper.IncomeMapper;
import com.petcare.finance.model.Income;
import com.petcare.finance.service.IncomeService;
import org.springframework.stereotype.Service;

@Service
public class IncomeServiceImpl extends ServiceImpl<IncomeMapper, Income> implements IncomeService {
}


