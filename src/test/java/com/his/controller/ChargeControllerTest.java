package com.his.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.service.ChargeService;
import com.his.vo.ChargeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("收费控制器集成测试")
class ChargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChargeService chargeService;

    @Test
    @DisplayName("测试创建收费单 - 成功")
    @WithMockUser(roles = "CASHIER")
    void testCreateCharge_Success() throws Exception {
        CreateChargeDTO dto = new CreateChargeDTO();
        dto.setRegistrationId(1L);
        dto.setPrescriptionIds(Arrays.asList(10L));

        ChargeVO vo = new ChargeVO();
        vo.setId(100L);
        vo.setChargeNo("CHG001");
        vo.setTotalAmount(new BigDecimal("50.00"));

        when(chargeService.createCharge(any())).thenReturn(vo);

        mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chargeNo").value("CHG001"));
    }

    @Test
    @DisplayName("测试支付 - 成功")
    @WithMockUser(roles = "CASHIER")
    void testPay_Success() throws Exception {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentMethod((short) 3);
        dto.setTransactionNo("TX001");
        dto.setPaidAmount(new BigDecimal("50.00"));

        ChargeVO vo = new ChargeVO();
        vo.setId(100L);
        vo.setStatus((short) 1);

        when(chargeService.processPayment(eq(100L), any())).thenReturn(vo);

        mockMvc.perform(post("/api/cashier/charges/100/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @DisplayName("测试权限 - 医生无法访问收费接口")
    @WithMockUser(roles = "DOCTOR")
    void testAccessDenied_Doctor() throws Exception {
        CreateChargeDTO dto = new CreateChargeDTO();
        dto.setRegistrationId(1L);

        mockMvc.perform(post("/api/cashier/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
