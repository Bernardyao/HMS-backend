package com.his.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.his.entity.Prescription;
import com.his.enums.PrescriptionStatusEnum;
import com.his.repository.PrescriptionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 处方状态机服务实现类测试
 *
 * <p>验证处方状态机的所有状态转换是否正确</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("处方状态机服务测试")
class PrescriptionStateMachineServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private PrescriptionStateMachineServiceImpl prescriptionStateMachine;

    private Prescription prescription;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        prescription = new Prescription();
        prescription.setMainId(1L);
        prescription.setPrescriptionNo("RX2026010800001");
        prescription.setStatus(PrescriptionStatusEnum.DRAFT.getCode());
        prescription.setIsDeleted((short) 0);
    }

    @Test
    @DisplayName("状态转换 DRAFT → ISSUED")
    void testTransition_DraftToIssued() throws Exception {
        // 准备数据
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setPrescriptionNo("RX2026010800001");
        currentPrescription.setStatus(PrescriptionStatusEnum.DRAFT.getCode());
        currentPrescription.setIsDeleted((short) 0);

        // Mock查询 - 返回当前状态的处方
        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        // Mock保存 - 返回更新后的处方
        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        // 执行状态转换
        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.DRAFT,
            PrescriptionStatusEnum.ISSUED,
            1L,
            "张三医生",
            "医生开方"
        );

        // 验证
        assertEquals(PrescriptionStatusEnum.ISSUED.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：DRAFT → ISSUED");
    }

    @Test
    @DisplayName("状态转换 ISSUED → REVIEWED")
    void testTransition_IssuedToReviewed() throws Exception {
        // 准备数据
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());

        // Mock查询
        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        // Mock保存
        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        // 执行状态转换
        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.ISSUED,
            PrescriptionStatusEnum.REVIEWED,
            1L,
            "李药师",
            "药师审核"
        );

        // 验证
        assertEquals(PrescriptionStatusEnum.REVIEWED.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：ISSUED → REVIEWED");
    }

    @Test
    @DisplayName("状态转换 REVIEWED → PAID")
    void testTransition_ReviewedToPaid() throws Exception {
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());

        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.PAID.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.REVIEWED,
            PrescriptionStatusEnum.PAID,
            1L,
            "收费员",
            "患者缴费"
        );

        assertEquals(PrescriptionStatusEnum.PAID.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：REVIEWED → PAID");
    }

    @Test
    @DisplayName("状态转换 PAID → DISPENSED")
    void testTransition_PaidToDispensed() throws Exception {
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.PAID.getCode());

        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.DISPENSED.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.PAID,
            PrescriptionStatusEnum.DISPENSED,
            1L,
            "王药师",
            "药师发药"
        );

        assertEquals(PrescriptionStatusEnum.DISPENSED.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：PAID → DISPENSED");
    }

    @Test
    @DisplayName("状态转换 DISPENSED → REFUNDED")
    void testTransition_DispensedToRefunded() throws Exception {
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.DISPENSED.getCode());

        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.REFUNDED.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.DISPENSED,
            PrescriptionStatusEnum.REFUNDED,
            1L,
            "王药师",
            "退药退费"
        );

        assertEquals(PrescriptionStatusEnum.REFUNDED.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：DISPENSED → REFUNDED");
    }

    @Test
    @DisplayName("状态转换 PAID → REVIEWED（退费）")
    void testTransition_PaidToReviewed() throws Exception {
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.PAID.getCode());

        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        Prescription updatedPrescription = new Prescription();
        updatedPrescription.setMainId(1L);
        updatedPrescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenReturn(updatedPrescription);

        Prescription result = prescriptionStateMachine.transition(
            1L,
            PrescriptionStatusEnum.PAID,
            PrescriptionStatusEnum.REVIEWED,
            1L,
            "收费员",
            "退费"
        );

        assertEquals(PrescriptionStatusEnum.REVIEWED.getCode(), result.getStatus());
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));

        System.out.println("✅ 测试通过：PAID → REVIEWED（退费）");
    }

    @Test
    @DisplayName("非法状态转换应抛出异常")
    void testInvalidTransition_ThrowsException() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> prescriptionStateMachine.transition(
                    1L,
                    PrescriptionStatusEnum.DRAFT,
                    PrescriptionStatusEnum.DISPENSED,
                    1L,
                    "医生",
                    "非法转换"
                )
        );

        assertTrue(exception.getMessage().contains("无效的处方状态转换"));
        assertTrue(exception.getMessage().contains("草稿"));
        assertTrue(exception.getMessage().contains("已发药"));

        System.out.println("✅ 测试通过：非法状态转换抛出异常");
    }

    @Test
    @DisplayName("状态不匹配时应抛出异常")
    void testStatusMismatch_ThrowsException() throws Exception {
        Prescription currentPrescription = new Prescription();
        currentPrescription.setMainId(1L);
        currentPrescription.setStatus(PrescriptionStatusEnum.ISSUED.getCode());

        when(prescriptionRepository.findById(1L))
                .thenReturn(java.util.Optional.of(currentPrescription));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> prescriptionStateMachine.transition(
                    1L,
                    PrescriptionStatusEnum.DRAFT,
                    PrescriptionStatusEnum.ISSUED,
                    1L,
                    "医生",
                    "测试"
                )
        );

        assertTrue(exception.getMessage().contains("处方状态不匹配"));
        assertTrue(exception.getMessage().contains("期望"));
        assertTrue(exception.getMessage().contains("实际"));

        System.out.println("✅ 测试通过：状态不匹配抛出异常");
    }

    @Test
    @DisplayName("验证状态转换是否合法")
    void testIsValidTransition() throws Exception {
        // 合法转换
        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.DRAFT,
                PrescriptionStatusEnum.ISSUED
        ));

        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.ISSUED,
                PrescriptionStatusEnum.REVIEWED
        ));

        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.REVIEWED,
                PrescriptionStatusEnum.PAID
        ));

        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.PAID,
                PrescriptionStatusEnum.DISPENSED
        ));

        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.PAID,
                PrescriptionStatusEnum.REVIEWED
        ));

        assertTrue(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.DISPENSED,
                PrescriptionStatusEnum.REFUNDED
        ));

        // 非法转换
        assertFalse(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.DRAFT,
                PrescriptionStatusEnum.DISPENSED
        ));

        assertFalse(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.DRAFT,
                PrescriptionStatusEnum.DRAFT
        ));

        assertFalse(prescriptionStateMachine.isValidTransition(
                PrescriptionStatusEnum.REFUNDED,
                PrescriptionStatusEnum.DRAFT
        ));

        System.out.println("✅ 测试通过：状态转换合法性验证");
    }
}
