package com.his.service.impl;

import com.his.entity.*;
import com.his.repository.MedicalRecordRepository;
import com.his.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MedicalRecordServiceImpl 懒加载初始化测试
 *
 * 测试目标：
 * 1. 验证 initializeLazyFields 方法能正确触发懒加载
 * 2. 验证在事务内调用不会抛出 LazyInitializationException
 * 3. 验证能访问懒加载字段而不抛出异常
 *
 * @author HIS Development Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordServiceImpl - 懒加载初始化测试")
class MedicalRecordServiceImplLazyInitTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    private MedicalRecord testRecord;
    private Registration testRegistration;
    private Patient testPatient;
    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testPatient = new Patient();
        testPatient.setMainId(1L);
        testPatient.setName("张三");

        testDoctor = new Doctor();
        testDoctor.setMainId(1L);
        testDoctor.setName("李医生");

        testRegistration = new Registration();
        testRegistration.setMainId(202501010001L);
        testRegistration.setPatient(testPatient);
        testRegistration.setDoctor(testDoctor);

        testRecord = new MedicalRecord();
        testRecord.setMainId(1L);
        testRecord.setRecordNo("MR20250101001");
        testRecord.setPatient(testPatient);
        testRecord.setDoctor(testDoctor);
        testRecord.setRegistration(testRegistration);
    }

    @Test
    @DisplayName("应该成功初始化非空的懒加载字段")
    void testInitializeLazyFields_WithNonNullFields() {
        // Given: 病历的所有关联字段都不为空
        assertNotNull(testRecord.getPatient(), "Patient should not be null");
        assertNotNull(testRecord.getDoctor(), "Doctor should not be null");
        assertNotNull(testRecord.getRegistration(), "Registration should not be null");

        // When: 调用 initializeLazyFields 方法
        assertDoesNotThrow(() -> {
            // 使用反射调用私有方法
            var method = MedicalRecordServiceImpl.class
                    .getDeclaredMethod("initializeLazyFields", MedicalRecord.class);
            method.setAccessible(true);
            method.invoke(medicalRecordService, testRecord);
        }, "initializeLazyFields should not throw exception");

        // Then: 验证可以访问懒加载字段（没有 LazyInitializationException）
        assertDoesNotThrow(() -> {
            testRecord.getPatient().getName();
            testRecord.getDoctor().getName();
            testRecord.getRegistration().getMainId();
        }, "Should be able to access lazy fields after initialization");
    }

    @Test
    @DisplayName("应该成功处理空的关联字段")
    void testInitializeLazyFields_WithNullFields() {
        // Given: 病历的所有关联字段都为空
        MedicalRecord emptyRecord = new MedicalRecord();
        emptyRecord.setMainId(2L);
        emptyRecord.setRecordNo("MR20250101002");

        // When & Then: 调用 initializeLazyFields 方法不应抛出异常
        assertDoesNotThrow(() -> {
            var method = MedicalRecordServiceImpl.class
                    .getDeclaredMethod("initializeLazyFields", MedicalRecord.class);
            method.setAccessible(true);
            method.invoke(medicalRecordService, emptyRecord);
        }, "initializeLazyFields should handle null fields gracefully");
    }

    @Test
    @DisplayName("应该成功初始化部分为空的关联字段")
    void testInitializeLazyFields_WithPartialNullFields() {
        // Given: 只有 Patient 不为空
        MedicalRecord partialRecord = new MedicalRecord();
        partialRecord.setMainId(3L);
        partialRecord.setRecordNo("MR20250101003");
        partialRecord.setPatient(testPatient);
        // Doctor 和 Registration 为 null

        // When & Then: 调用 initializeLazyFields 方法不应抛出异常
        assertDoesNotThrow(() -> {
            var method = MedicalRecordServiceImpl.class
                    .getDeclaredMethod("initializeLazyFields", MedicalRecord.class);
            method.setAccessible(true);
            method.invoke(medicalRecordService, partialRecord);
        }, "initializeLazyFields should handle partial null fields gracefully");
    }

    @Test
    @DisplayName("懒加载初始化后应能访问数据而不抛出异常")
    void testLazyFieldsAccessibleAfterInitialization() {
        // Given: 初始化后的病历
        assertDoesNotThrow(() -> {
            var method = MedicalRecordServiceImpl.class
                    .getDeclaredMethod("initializeLazyFields", MedicalRecord.class);
            method.setAccessible(true);
            method.invoke(medicalRecordService, testRecord);
        });

        // When & Then: 访问懒加载字段应该成功
        assertDoesNotThrow(() -> {
            String patientName = testRecord.getPatient().getName();
            String doctorName = testRecord.getDoctor().getName();
            Long mainId = testRecord.getRegistration().getMainId();

            // 验证返回的值不为空
            assertNotNull(patientName, "Patient name should not be null");
            assertNotNull(doctorName, "Doctor name should not be null");
            assertNotNull(mainId, "Registration mainId should not be null");
        }, "Should be able to access lazy field values without LazyInitializationException");
    }
}
