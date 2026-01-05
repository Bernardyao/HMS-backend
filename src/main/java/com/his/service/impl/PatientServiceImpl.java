package com.his.service.impl;

import com.his.entity.Patient;
import com.his.enums.GenderEnum;
import com.his.repository.PatientRepository;
import com.his.service.PatientService;
import com.his.vo.PatientSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 患者服务实现类
 *
 * @author HIS 开发团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 20;
    private static final int MAX_SEARCH_RESULTS = 15;

    /**
     * 搜索患者信息
     *
     * @param keyword 搜索关键字
     * @return 匹配的患者列表
     */
    @Override
    public List<PatientSearchVO> searchPatients(String keyword) {
        // 1. 关键字校验
        validateKeyword(keyword);

        // 2. 清洗关键字（去除前后空格，过滤特殊字符）
        String sanitizedKeyword = sanitizeKeyword(keyword);

        log.info("患者搜索，关键字: [{}], 清洗后: [{}]", keyword, sanitizedKeyword);

        // 3. 执行搜索
        List<Patient> patients = patientRepository.searchByKeyword(
                sanitizedKeyword,
                PageRequest.of(0, MAX_SEARCH_RESULTS)
        );

        log.info("患者搜索结果数量: {}", patients.size());

        // 4. 转换为 VO（不脱敏）
        return patients.stream()
                .map(this::convertToSearchVO)
                .collect(Collectors.toList());
    }

    /**
     * 校验关键字
     *
     * @param keyword 关键字
     * @throws IllegalArgumentException 当关键字不符合要求时
     */
    private void validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            log.warn("患者搜索失败：关键字为空");
            throw new IllegalArgumentException("搜索关键字不能为空");
        }

        String trimmed = keyword.trim();
        if (trimmed.length() < MIN_KEYWORD_LENGTH) {
            log.warn("患者搜索失败：关键字过短，长度: {}", trimmed.length());
            throw new IllegalArgumentException("搜索关键字至少需要 " + MIN_KEYWORD_LENGTH + " 个字符");
        }

        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            log.warn("患者搜索失败：关键字过长，长度: {}", trimmed.length());
            throw new IllegalArgumentException("搜索关键字不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
    }

    /**
     * 清洗关键字
     *
     * <p>去除前后空格，过滤 SQL 特殊字符和控制字符</p>
     *
     * @param keyword 原始关键字
     * @return 清洗后的关键字
     */
    private String sanitizeKeyword(String keyword) {
        // 去除前后空格
        String trimmed = keyword.trim();

        // 过滤特殊字符：% _ \ 和控制字符
        return trimmed.replaceAll("[%_\\\\\\x00-\\x1F]", "");
    }

    /**
     * 转换为搜索 VO（不脱敏）
     *
     * @param patient 患者实体
     * @return 搜索 VO
     */
    private PatientSearchVO convertToSearchVO(Patient patient) {
        // 解析性别描述
        String genderDesc = "未知";
        try {
            if (patient.getGender() != null) {
                genderDesc = GenderEnum.fromCode(patient.getGender()).getDescription();
            }
        } catch (Exception e) {
            log.warn("无法解析性别代码: {}", patient.getGender());
        }

        return PatientSearchVO.builder()
                .patientId(patient.getMainId())
                .patientNo(patient.getPatientNo())
                .name(patient.getName())
                .idCard(patient.getIdCard())  // 不脱敏
                .gender(patient.getGender())
                .genderDesc(genderDesc)
                .age(patient.getAge())
                .phone(patient.getPhone())  // 不脱敏
                .build();
    }
}
