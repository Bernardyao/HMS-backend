package com.his.vo.views;

/**
 * 药品数据的JsonView视图定义
 * <p>
 * 用于控制不同角色可见的药品字段，避免敏感信息泄露
 * </p>
 *
 * <h3>视图层级</h3>
 * <ul>
 *   <li><b>Public</b>: 公共视图 - 所有认证用户可见（基础信息）</li>
 *   <li><b>Doctor</b>: 医生视图 - 医生和药师可见（含用法用量等）</li>
 *   <li><b>Pharmacist</b>: 药师视图 - 仅药师可见（含进货价等敏感信息）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <pre>
 * {@code
 * @GetMapping("/{id}")
 * public Result<MedicineVO> getById(@PathVariable Long id) {
 *     Medicine medicine = medicineService.getById(id);
 *     Class<?> view = getViewForCurrentUser();
 *     MedicineVO vo = VoConverter.toMedicineVO(medicine, view);
 *     return Result.success("查询成功", vo);
 * }
 * }
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 */
public class MedicineViews {

    /**
     * 公共视图 - 所有角色可见的基础信息
     * <p>
     * 包含字段：id, name, code, category, isPrescription, stockQuantity, retailPrice
     * </p>
     */
    public interface Public {
    }

    /**
     * 医生视图 - 医生和药师可见
     * <p>
     * 继承Public视图，额外包含：specification, usage, dosageForm, manufacturer
     * </p>
     * <p>
     * 使用场景：医生开处方时需要查看详细的药品信息
     * </p>
     */
    public interface Doctor extends Public {
    }

    /**
     * 药师视图 - 仅药师可见的敏感信息
     * <p>
     * 继承Doctor视图，额外包含：purchasePrice, minStock, maxStock,
     * storageCondition, approvalNo
     * </p>
     * <p>
     * 使用场景：药师库存管理、采购决策
     * </p>
     * <p>
     * <b>安全说明</b>：进货价等敏感信息仅对药师可见，防止泄露商业机密
     * </p>
     */
    public interface Pharmacist extends Doctor {
    }
}
