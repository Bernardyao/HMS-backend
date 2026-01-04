package com.his.controller;

import com.his.dto.RegistrationDTO;
import com.his.entity.Department;
import com.his.entity.Doctor;
import com.his.repository.DepartmentRepository;
import com.his.repository.DoctorRepository;
import com.his.repository.PatientRepository;
import com.his.test.base.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 挂号控制器集成测试
 */
@Transactional
@DisplayName("挂号控制器集成测试")
@WithMockUser(roles = "NURSE")
class RegistrationControllerTest extends BaseControllerTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Long testDeptId;
    private Long testDoctorId;

    @BeforeEach
    protected void setUp() {
        // 准备测试数据
        Department department = new Department();
        department.setDeptCode("D001");
        department.setName("内科");
        department.setStatus((short) 1);
        department.setIsDeleted((short) 0);
        department.setSortOrder(1);
        Department savedDept = departmentRepository.save(department);
        testDeptId = savedDept.getMainId();

        Doctor doctor = new Doctor();
        doctor.setDoctorNo("D001");
        doctor.setName("张医生");
        doctor.setGender((short) 1);
        doctor.setDepartment(savedDept);
        doctor.setTitle("主任医师");
        doctor.setStatus((short) 1);
        doctor.setIsDeleted((short) 0);
        Doctor savedDoctor = doctorRepository.save(doctor);
        testDoctorId = savedDoctor.getMainId();
    }

    @Test
    @DisplayName("测试挂号接口 - 新患者")
    void testRegister_NewPatient() throws Exception {
        // 准备请求数据
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("李四");
        dto.setIdCard("320106199501012345");
        dto.setGender((short) 1);
        dto.setAge((short) 28);
        dto.setPhone("13900139000");
        dto.setDeptId(testDeptId);
        dto.setDoctorId(testDoctorId);
        dto.setRegFee(new BigDecimal("15.00"));

        // 发送 POST 请求
        mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("挂号成功"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.patientName").value("李四"))
                .andExpect(jsonPath("$.data.deptId").value(testDeptId))
                .andExpect(jsonPath("$.data.doctorId").value(testDoctorId))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.statusDesc").value("待就诊"))
                .andExpect(jsonPath("$.data.regNo").isNotEmpty())
                .andExpect(jsonPath("$.data.queueNo").isNotEmpty());
    }

    @Test
    @DisplayName("测试挂号接口 - 老患者")
    void testRegister_ExistingPatient() throws Exception {
        // 准备第二个医生（用于老患者第二次挂号）
        Doctor doctor2 = new Doctor();
        doctor2.setDoctorNo("D002");
        doctor2.setName("李医生");
        doctor2.setGender((short) 0);
        doctor2.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
        doctor2.setTitle("副主任医师");
        doctor2.setStatus((short) 1);
        doctor2.setIsDeleted((short) 0);
        Doctor savedDoctor2 = doctorRepository.save(doctor2);
        Long testDoctorId2 = savedDoctor2.getMainId();

        // 第一次挂号（建档）
        RegistrationDTO dto1 = new RegistrationDTO();
        dto1.setPatientName("王五");
        dto1.setIdCard("320106199601013456");
        dto1.setGender((short) 0);
        dto1.setAge((short) 27);
        dto1.setPhone("13800138888");
        dto1.setDeptId(testDeptId);
        dto1.setDoctorId(testDoctorId);
        dto1.setRegFee(new BigDecimal("20.00"));

        mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 第二次挂号（使用相同身份证号，但挂不同医生）
        RegistrationDTO dto2 = new RegistrationDTO();
        dto2.setPatientName("王五");
        dto2.setIdCard("320106199601013456"); // 相同身份证号
        dto2.setGender((short) 0);
        dto2.setAge((short) 27);
        dto2.setPhone("13800138888");
        dto2.setDeptId(testDeptId);
        dto2.setDoctorId(testDoctorId2); // 不同的医生
        dto2.setRegFee(new BigDecimal("20.00"));

        mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.patientName").value("王五"));

        // 验证相同身份证号只创建了一个患者（不受其他测试/初始化数据影响）
        long sameIdCardCount = patientRepository.countByIdCardAndIsDeleted("320106199601013456", (short) 0);
        org.assertj.core.api.Assertions.assertThat(sameIdCardCount).isEqualTo(1);
    }

    @Test
    @DisplayName("测试挂号接口 - 参数缺失")
    void testRegister_MissingParameters() throws Exception {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("赵六");
        // 缺少身份证号

        mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("身份证号不能为空")));
    }

    @Test
    @DisplayName("测试查询挂号记录")
    void testGetById() throws Exception {
        // 先创建挂号记录
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("孙七");
        dto.setIdCard("320106199701014567");
        dto.setGender((short) 1);
        dto.setAge((short) 26);
        dto.setPhone("13700137777");
        dto.setDeptId(testDeptId);
        dto.setDoctorId(testDoctorId);
        dto.setRegFee(new BigDecimal("25.00"));

        String response = mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 解析返回的 ID
        Long registrationId = objectMapper.readTree(response)
                .get("data").get("id").asLong();

        // 查询挂号记录
        mockMvc.perform(get("/api/nurse/registrations/{id}", registrationId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(registrationId))
                .andExpect(jsonPath("$.data.patientName").value("孙七"));
    }

    @Test
    @DisplayName("测试取消挂号")
    void testCancel() throws Exception {
        // 先创建挂号记录
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientName("周八");
        dto.setIdCard("320106199801015678");
        dto.setGender((short) 0);
        dto.setAge((short) 25);
        dto.setPhone("13600136666");
        dto.setDeptId(testDeptId);
        dto.setDoctorId(testDoctorId);
        dto.setRegFee(new BigDecimal("30.00"));

        String response = mockMvc.perform(post("/api/nurse/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long registrationId = objectMapper.readTree(response)
                .get("data").get("id").asLong();

        // 取消挂号
        mockMvc.perform(put("/api/nurse/registrations/{id}/cancel", registrationId)
                        .param("reason", "临时有事"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("挂号已取消"));

        // 验证状态已变更
        mockMvc.perform(get("/api/nurse/registrations/{id}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.statusDesc").value("已取消"));
    }

    @Test
    @DisplayName("测试查询不存在的挂号记录")
    void testGetById_NotFound() throws Exception {
        mockMvc.perform(get("/api/nurse/registrations/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("挂号记录不存在")));
    }
}
