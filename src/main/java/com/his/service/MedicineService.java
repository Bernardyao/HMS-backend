package com.his.service;

import com.his.entity.Medicine;

import java.util.List;

/**
 * 药品服务接口
 */
public interface MedicineService {

    /**
     * 模糊搜索药品（根据名称或编码）
     *
     * @param keyword 关键字（药品名称或编码）
     * @return 药品列表
     */
    List<Medicine> searchMedicines(String keyword);

    /**
     * 根据ID查询药品
     *
     * @param id 药品ID
     * @return 药品信息
     */
    Medicine getById(Long id);

    /**
     * 查询所有启用的药品
     *
     * @return 药品列表
     */
    List<Medicine> getAllActive();

    /**
     * 检查库存是否充足
     *
     * @param medicineId 药品ID
     * @param quantity 需要数量
     * @return true=库存充足, false=库存不足
     */
    boolean checkStock(Long medicineId, Integer quantity);
}
