package com.his.repository;

import com.his.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Charge Repository 集成测试")
class ChargeRepositoryTest {

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private ChargeDetailRepository chargeDetailRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @DisplayName("测试 Charge 及其明细的级联保存和查询")
    void testSaveAndFindChargeWithDetails() {
        // 1. 准备患者
        Patient patient = new Patient();
        patient.setPatientNo("P-TEST-001");
        patient.setName("Test Patient");
        patient.setGender((short) 1);
        patient.setIdCard("110101199001011234");
        patient = patientRepository.save(patient);

        // 2. 创建收费主表
        Charge charge = new Charge();
        charge.setPatient(patient);
        charge.setChargeNo("CHG-TEST-001");
        charge.setChargeType((short) 2);
        charge.setTotalAmount(new BigDecimal("100.00"));
        charge.setActualAmount(new BigDecimal("100.00"));
        charge.setStatus((short) 0);
        
        // 3. 创建收费明细
        List<ChargeDetail> details = new ArrayList<>();
        
        ChargeDetail detail1 = new ChargeDetail();
        detail1.setCharge(charge);
        detail1.setItemType("REGISTRATION");
        detail1.setItemId(1L);
        detail1.setItemName("Registration Fee");
        detail1.setItemAmount(new BigDecimal("10.00"));
        details.add(detail1);
        
        ChargeDetail detail2 = new ChargeDetail();
        detail2.setCharge(charge);
        detail2.setItemType("PRESCRIPTION");
        detail2.setItemId(10L);
        detail2.setItemName("Medicine A");
        detail2.setItemAmount(new BigDecimal("90.00"));
        details.add(detail2);
        
        charge.setDetails(details);

        // 4. 保存 (应该级联保存明细)
        Charge savedCharge = chargeRepository.save(charge);
        assertNotNull(savedCharge.getMainId());

        // 5. 查询验证
        Charge foundCharge = chargeRepository.findById(savedCharge.getMainId()).orElseThrow();
        assertThat(foundCharge.getDetails()).hasSize(2);
        assertThat(foundCharge.getDetails()).extracting(ChargeDetail::getItemName)
                .containsExactlyInAnyOrder("Registration Fee", "Medicine A");
        
        // 6. 验证明细 Repository 查询
        List<ChargeDetail> foundDetails = chargeDetailRepository.findByCharge_MainId(savedCharge.getMainId());
        assertThat(foundDetails).hasSize(2);
    }
}
