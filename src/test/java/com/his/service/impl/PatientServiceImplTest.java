package com.his.service.impl;

import com.his.entity.Patient;
import com.his.repository.PatientRepository;
import com.his.vo.PatientSearchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PatientService 单元测试
 *
 * @author HIS 开发团队
 */
@DisplayName("PatientService 单元测试")
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("搜索患者 - 成功场景")
    void searchPatients_Success() {
        // Given
        String keyword = "张三";
        Patient patient1 = createTestPatient(1L, "张三", "320106199001011234", "13812345678");
        Patient patient2 = createTestPatient(2L, "张三丰", "320106199002022345", "13887654321");

        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(Arrays.asList(patient1, patient2));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("张三");
        assertThat(result.get(0).getIdCard()).isEqualTo("320106199001011234");  // 不脱敏
        assertThat(result.get(0).getPhone()).isEqualTo("13812345678");  // 不脱敏
        assertThat(result.get(1).getName()).isEqualTo("张三丰");

        verify(patientRepository).searchByKeyword(eq(keyword), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索患者 - 按身份证号搜索")
    void searchPatients_ByIdCard() {
        // Given
        String keyword = "320106";
        Patient patient = createTestPatient(1L, "张三", "320106199001011234", "13812345678");

        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(Collections.singletonList(patient));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdCard()).contains("320106");
    }

    @Test
    @DisplayName("搜索患者 - 按手机号搜索")
    void searchPatients_ByPhone() {
        // Given
        String keyword = "138";
        Patient patient = createTestPatient(1L, "张三", "320106199001011234", "13812345678");

        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(Collections.singletonList(patient));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).contains("138");
    }

    @Test
    @DisplayName("搜索患者 - 无结果")
    void searchPatients_NoResult() {
        // Given
        String keyword = "不存在的患者";

        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("搜索患者 - 关键字为空")
    void searchPatients_EmptyKeyword() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            patientService.searchPatients("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            patientService.searchPatients("   ");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            patientService.searchPatients(null);
        });

        verify(patientRepository, never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("搜索患者 - 关键字过短")
    void searchPatients_KeywordTooShort() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            patientService.searchPatients("张");
        });

        verify(patientRepository, never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("搜索患者 - 关键字过长")
    void searchPatients_KeywordTooLong() {
        // Given
        String keyword = "a".repeat(21);  // 21个字符

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            patientService.searchPatients(keyword);
        });

        verify(patientRepository, never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("搜索患者 - 特殊字符过滤")
    void searchPatients_SpecialCharactersFiltered() {
        // Given
        String keyword = "张三%_";
        Patient patient = createTestPatient(1L, "张三", "320106199001011234", "13812345678");

        when(patientRepository.searchByKeyword(eq("张三"), any(Pageable.class)))  // 特殊字符已被过滤
                .thenReturn(Collections.singletonList(patient));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).hasSize(1);
        verify(patientRepository).searchByKeyword(eq("张三"), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索患者 - 限制返回数量")
    void searchPatients_LimitResults() {
        // Given
        String keyword = "张三";  // 使用有效长度的关键字
        List<Patient> patients = createManyPatients(20);  // 创建20个患者

        // 模拟仓库只返回前 15 条（分页限制由 PageRequest 控制）
        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(patients.subList(0, 15));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        // 验证调用时使用了正确的分页参数
        verify(patientRepository).searchByKeyword(
                eq(keyword),
                eq(PageRequest.of(0, 15))  // 限制15条
        );

        // 验证返回结果被正确处理并返回 15 条记录
        assertThat(result).hasSize(15);
    }

    @Test
    @DisplayName("搜索患者 - 性别描述正确转换")
    void searchPatients_GenderDescCorrect() {
        // Given
        String keyword = "张三";  // 使用有效长度的关键字
        Patient patient = createTestPatient(1L, "张三", "320106199001011234", "13812345678");

        when(patientRepository.searchByKeyword(eq(keyword), any(Pageable.class)))
                .thenReturn(Collections.singletonList(patient));

        // When
        List<PatientSearchVO> result = patientService.searchPatients(keyword);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGender()).isEqualTo((short) 1);
        assertThat(result.get(0).getGenderDesc()).isEqualTo("男");
    }

    // ==================== 辅助方法 ====================

    private Patient createTestPatient(Long id, String name, String idCard, String phone) {
        Patient patient = new Patient();
        patient.setMainId(id);
        patient.setPatientNo("P2025010500" + id);
        patient.setName(name);
        patient.setIdCard(idCard);
        patient.setGender((short) 1);
        patient.setAge((short) 34);
        patient.setPhone(phone);
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
        patient.setIsDeleted((short) 0);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        return patient;
    }

    private List<Patient> createManyPatients(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createTestPatient((long) i, "患者" + i, "32010619900101" + String.format("%04d", i), "138000000" + String.format("%02d", i)))
                .toList();
    }
}
