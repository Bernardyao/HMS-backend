package com.his.service.impl;

import com.his.entity.Medicine;
import com.his.repository.MedicineRepository;
import com.his.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 药品服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    /**
     * 模糊搜索药品（根据名称或编码）
     */
    @Override
    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String keyword) {
        log.info("搜索药品，关键字: {}", keyword);
        
        if (!StringUtils.hasText(keyword)) {
            log.warn("搜索关键字为空，返回所有启用的药品");
            return getAllActive();
        }

        // 使用 @Query 自定义查询，支持名称或编码模糊匹配
        List<Medicine> medicines = medicineRepository.searchByKeyword(keyword, (short) 0);
        log.info("搜索到 {} 个药品", medicines.size());
        
        return medicines;
    }

    /**
     * 根据ID查询药品
     */
    @Override
    @Transactional(readOnly = true)
    public Medicine getById(Long id) {
        log.info("查询药品，ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("药品ID不能为空");
        }
        
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("药品不存在，ID: " + id));
        
        if (medicine.getIsDeleted() == 1) {
            throw new IllegalArgumentException("药品已被删除，ID: " + id);
        }
        
        return medicine;
    }

    /**
     * 查询所有启用的药品
     */
    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getAllActive() {
        log.info("查询所有启用的药品");
        return medicineRepository.findAllActive();
    }

    /**
     * 检查库存是否充足
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkStock(Long medicineId, Integer quantity) {
        log.info("检查库存，药品ID: {}, 需要数量: {}", medicineId, quantity);
        
        if (medicineId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数错误");
        }
        
        Medicine medicine = getById(medicineId);
        boolean sufficient = medicine.getStockQuantity() >= quantity;
        
        log.info("库存检查结果: 当前库存={}, 需要数量={}, 是否充足={}", 
                medicine.getStockQuantity(), quantity, sufficient);
        
        return sufficient;
    }
}
