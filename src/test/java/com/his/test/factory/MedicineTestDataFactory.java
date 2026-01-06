package com.his.test.factory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.his.entity.Medicine;

/**
 * 药品测试数据工厂
 * <p>
 * 提供具有业务语义的测试数据创建方法，避免魔法数字和硬编码业务逻辑。
 * 提高测试的可读性、可维护性和业务表达力。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li><b>业务语义</b>：方法名直接表达业务场景（如 createLowStockMedicine）</li>
 *   <li><b>默认值</b>：提供合理的默认值，减少测试代码量</li>
 *   <li><b>可配置</b>：支持 Builder 模式，允许灵活定制</li>
 *   <li><b>类型安全</b>：使用 Integer 而非 Short，避免类型转换</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建普通库存药品
 * Medicine medicine = MedicineTestDataFactory.createNormalStockMedicine();
 *
 * // 创建低库存药品
 * Medicine lowStock = MedicineTestDataFactory.createLowStockMedicine();
 * assertEquals("LOW_STOCK", VoConverter.computeStockStatus(lowStock));
 *
 * // 创建自定义药品
 * Medicine custom = MedicineTestDataFactory.builder()
 *     .name("测试药品")
 *     .stockQuantity(100)
 *     .minStock(50)
 *     .build();
 * }</pre>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 */
public class MedicineTestDataFactory {

    // 默认测试数据常量
    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_CODE = "MED001";
    private static final String DEFAULT_NAME = "阿莫西林胶囊";
    private static final String DEFAULT_CATEGORY = "抗生素";
    private static final BigDecimal DEFAULT_RETAIL_PRICE = new BigDecimal("25.80");
    private static final BigDecimal DEFAULT_PURCHASE_PRICE = new BigDecimal("18.50");
    private static final Integer DEFAULT_STOCK = 100;
    private static final Integer DEFAULT_MIN_STOCK = 50;
    private static final Integer DEFAULT_MAX_STOCK = 500;
    private static final Integer DEFAULT_STATUS = 1; // 启用
    private static final Integer DEFAULT_IS_PRESCRIPTION = 1; // 是处方药

    /**
     * 创建普通库存药品（库存 > 最小库存）
     * <p>适用于测试正常业务场景</p>
     *
     * @return Medicine 实体，stockQuantity=100, minStock=50
     */
    public static Medicine createNormalStockMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode(DEFAULT_CODE)
                .name(DEFAULT_NAME)
                .category(DEFAULT_CATEGORY)
                .retailPrice(DEFAULT_RETAIL_PRICE)
                .purchasePrice(DEFAULT_PURCHASE_PRICE)
                .stockQuantity(DEFAULT_STOCK)
                .minStock(DEFAULT_MIN_STOCK)
                .maxStock(DEFAULT_MAX_STOCK)
                .status(DEFAULT_STATUS)
                .isPrescription(DEFAULT_IS_PRESCRIPTION)
                .build();
    }

    /**
     * 创建低库存药品（库存 <= 最小库存）
     * <p>适用于测试低库存警告、库存查询等场景</p>
     *
     * @return Medicine 实体，stockQuantity=30, minStock=50，库存状态为 LOW_STOCK
     */
    public static Medicine createLowStockMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode(DEFAULT_CODE)
                .name(DEFAULT_NAME)
                .category(DEFAULT_CATEGORY)
                .retailPrice(DEFAULT_RETAIL_PRICE)
                .purchasePrice(DEFAULT_PURCHASE_PRICE)
                .stockQuantity(30) // 低于最小库存
                .minStock(50)
                .maxStock(DEFAULT_MAX_STOCK)
                .status(DEFAULT_STATUS)
                .isPrescription(DEFAULT_IS_PRESCRIPTION)
                .build();
    }

    /**
     * 创建缺货药品（库存 = 0）
     * <p>适用于测试缺货、库存不足等场景</p>
     *
     * @return Medicine 实体，stockQuantity=0，库存状态为 OUT_OF_STOCK
     */
    public static Medicine createOutOfStockMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode(DEFAULT_CODE)
                .name(DEFAULT_NAME)
                .category(DEFAULT_CATEGORY)
                .retailPrice(DEFAULT_RETAIL_PRICE)
                .purchasePrice(DEFAULT_PURCHASE_PRICE)
                .stockQuantity(0) // 缺货
                .minStock(DEFAULT_MIN_STOCK)
                .maxStock(DEFAULT_MAX_STOCK)
                .status(DEFAULT_STATUS)
                .isPrescription(DEFAULT_IS_PRESCRIPTION)
                .build();
    }

    /**
     * 创建非处方药
     * <p>适用于测试非处方药查询场景</p>
     *
     * @return Medicine 实体，isPrescription=0
     */
    public static Medicine createNonPrescriptionMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode("VIT001")
                .name("维生素C片")
                .category("维生素")
                .retailPrice(new BigDecimal("12.00"))
                .purchasePrice(new BigDecimal("8.00"))
                .stockQuantity(200)
                .minStock(100)
                .maxStock(1000)
                .status(1)
                .isPrescription(0) // 非处方药
                .build();
    }

    /**
     * 创建高利润药品（利润率 > 50%）
     * <p>适用于测试药师视图中的利润率计算</p>
     *
     * @return Medicine 实体，零售价30，进货价10，利润率66.67%
     */
    public static Medicine createHighProfitMarginMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode("PROF001")
                .name("测试高利润药品")
                .category("测试分类")
                .retailPrice(new BigDecimal("30.00"))
                .purchasePrice(new BigDecimal("10.00")) // 高利润率
                .stockQuantity(100)
                .minStock(50)
                .maxStock(500)
                .status(1)
                .isPrescription(1)
                .build();
    }

    /**
     * 创建零利润药品（零售价 = 进货价）
     * <p>适用于测试边界场景</p>
     *
     * @return Medicine 实体，零售价=进货价=20，利润率0%
     */
    public static Medicine createZeroProfitMarginMedicine() {
        BigDecimal price = new BigDecimal("20.00");
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode("ZERO001")
                .name("测试零利润药品")
                .category("测试分类")
                .retailPrice(price)
                .purchasePrice(price) // 零利润
                .stockQuantity(100)
                .minStock(50)
                .maxStock(500)
                .status(1)
                .isPrescription(0)
                .build();
    }

    /**
     * 创建已停用药品
     * <p>适用于测试停用、过滤等场景</p>
     *
     * @return Medicine 实体，status=0（停用）
     */
    public static Medicine createInactiveMedicine() {
        return builder()
                .mainId(DEFAULT_ID)
                .medicineCode("INA001")
                .name("已停用药品")
                .category("测试分类")
                .retailPrice(DEFAULT_RETAIL_PRICE)
                .purchasePrice(DEFAULT_PURCHASE_PRICE)
                .stockQuantity(DEFAULT_STOCK)
                .minStock(DEFAULT_MIN_STOCK)
                .maxStock(DEFAULT_MAX_STOCK)
                .status(0) // 停用
                .isPrescription(DEFAULT_IS_PRESCRIPTION)
                .build();
    }

    /**
     * 创建完整信息的药品（包含所有字段）
     * <p>适用于测试 JsonView 字段可见性</p>
     *
     * @return 包含所有字段的 Medicine 实体
     */
    public static Medicine createFullInfoMedicine() {
        Medicine medicine = builder()
                .mainId(999L)
                .medicineCode("FULL001")
                .name("全信息测试药品")
                .genericName("Amoxicillin Capsules")
                .category("抗生素")
                .retailPrice(new BigDecimal("25.80"))
                .purchasePrice(new BigDecimal("18.50"))
                .stockQuantity(100)
                .minStock(50)
                .maxStock(500)
                .status(1)
                .isPrescription(1)
                .specification("0.25g*24粒")
                .unit("盒")
                .dosageForm("胶囊")
                .manufacturer("某某制药有限公司")
                .storageCondition("密闭，在阴凉干燥处保存")
                .approvalNo("国药准字H12345678")
                .expiryWarningDays(90)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 12, 31, 15, 30, 0))
                .build();

        return medicine;
    }

    /**
     * Medicine Builder，支持灵活构建测试数据
     * <p>使用示例：</p>
     * <pre>{@code
     * Medicine medicine = MedicineTestDataFactory.builder()
     *     .name("自定义药品")
     *     .stockQuantity(500)
     *     .build();
     * }</pre>
     *
     * @return MedicineBuilder
     */
    public static MedicineBuilder builder() {
        return new MedicineBuilder();
    }

    /**
     * Medicine Builder 类
     * <p>提供流式 API 构建测试数据</p>
     */
    public static class MedicineBuilder {
        private final Medicine medicine = new Medicine();

        public MedicineBuilder mainId(Long mainId) {
            medicine.setMainId(mainId);
            return this;
        }

        public MedicineBuilder medicineCode(String code) {
            medicine.setMedicineCode(code);
            return this;
        }

        public MedicineBuilder name(String name) {
            medicine.setName(name);
            return this;
        }

        public MedicineBuilder genericName(String genericName) {
            medicine.setGenericName(genericName);
            return this;
        }

        public MedicineBuilder category(String category) {
            medicine.setCategory(category);
            return this;
        }

        public MedicineBuilder retailPrice(BigDecimal price) {
            medicine.setRetailPrice(price);
            return this;
        }

        public MedicineBuilder purchasePrice(BigDecimal price) {
            medicine.setPurchasePrice(price);
            return this;
        }

        public MedicineBuilder stockQuantity(Integer quantity) {
            medicine.setStockQuantity(quantity);
            return this;
        }

        public MedicineBuilder minStock(Integer minStock) {
            medicine.setMinStock(minStock);
            return this;
        }

        public MedicineBuilder maxStock(Integer maxStock) {
            medicine.setMaxStock(maxStock);
            return this;
        }

        public MedicineBuilder status(Integer status) {
            medicine.setStatus(status != null ? status.shortValue() : null);
            return this;
        }

        public MedicineBuilder isPrescription(Integer isPrescription) {
            medicine.setIsPrescription(isPrescription != null ? isPrescription.shortValue() : null);
            return this;
        }

        public MedicineBuilder specification(String specification) {
            medicine.setSpecification(specification);
            return this;
        }

        public MedicineBuilder unit(String unit) {
            medicine.setUnit(unit);
            return this;
        }

        public MedicineBuilder dosageForm(String dosageForm) {
            medicine.setDosageForm(dosageForm);
            return this;
        }

        public MedicineBuilder manufacturer(String manufacturer) {
            medicine.setManufacturer(manufacturer);
            return this;
        }

        public MedicineBuilder storageCondition(String storageCondition) {
            medicine.setStorageCondition(storageCondition);
            return this;
        }

        public MedicineBuilder approvalNo(String approvalNo) {
            medicine.setApprovalNo(approvalNo);
            return this;
        }

        public MedicineBuilder expiryWarningDays(Integer days) {
            medicine.setExpiryWarningDays(days);
            return this;
        }

        public MedicineBuilder createdAt(LocalDateTime createdAt) {
            medicine.setCreatedAt(createdAt);
            return this;
        }

        public MedicineBuilder updatedAt(LocalDateTime updatedAt) {
            medicine.setUpdatedAt(updatedAt);
            return this;
        }

        public Medicine build() {
            // 设置默认时间戳（如果未设置）
            if (medicine.getCreatedAt() == null) {
                medicine.setCreatedAt(LocalDateTime.now());
            }
            if (medicine.getUpdatedAt() == null) {
                medicine.setUpdatedAt(LocalDateTime.now());
            }

            // 设置默认值
            if (medicine.getIsDeleted() == null) {
                medicine.setIsDeleted((short) 0);
            }

            return medicine;
        }
    }

    /**
     * 业务场景枚举，用于参数化测试
     * <p>使用示例：</p>
     * <pre>{@code
     * @ParameterizedTest
     * @EnumSource(MedicineTestDataFactory.StockScenario.class)
     * void testStockStatus(StockScenario scenario) {
     *     Medicine medicine = scenario.create();
     *     String expectedStatus = scenario.getExpectedStockStatus();
     *     assertEquals(expectedStatus, VoConverter.computeStockStatus(medicine));
     * }
     * }</pre>
     */
    public enum StockScenario {
        NORMAL_STOCK(
            "正常库存",
            "stockQuantity > minStock",
            () -> builder().stockQuantity(100).minStock(50).build(),
            "IN_STOCK"
        ),
        LOW_STOCK(
            "低库存",
            "stockQuantity <= minStock",
            () -> builder().stockQuantity(30).minStock(50).build(),
            "LOW_STOCK"
        ),
        OUT_OF_STOCK(
            "缺货",
            "stockQuantity = 0",
            () -> builder().stockQuantity(0).minStock(50).build(),
            "OUT_OF_STOCK"
        ),
        BARELY_IN_STOCK(
            "临界库存",
            "stockQuantity = minStock",
            () -> builder().stockQuantity(50).minStock(50).build(),
            "LOW_STOCK"
        );

        private final String description;
        private final String condition;
        private final java.util.function.Supplier<Medicine> supplier;
        private final String expectedStatus;

        StockScenario(String description, String condition,
                       java.util.function.Supplier<Medicine> supplier, String expectedStatus) {
            this.description = description;
            this.condition = condition;
            this.supplier = supplier;
            this.expectedStatus = expectedStatus;
        }

        public Medicine create() {
            return supplier.get();
        }

        public String getExpectedStatus() {
            return expectedStatus;
        }

        public String getDescription() {
            return description;
        }

        public String getCondition() {
            return condition;
        }
    }

    /**
     * 处方药场景枚举，用于参数化测试
     */
    public enum PrescriptionScenario {
        PRESCRIPTION_MEDICINE("处方药", 1),
        NON_PRESCRIPTION_MEDICINE("非处方药", 0);

        private final String description;
        private final Integer value;

        PrescriptionScenario(String description, Integer value) {
            this.description = description;
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}
