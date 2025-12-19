package com.his.controller;

import com.his.common.JwtUtils;
import com.his.dto.LoginRequest;
import com.his.entity.SysUser;
import com.his.repository.SysUserRepository;
import com.his.vo.LoginVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 登录认证集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("登录认证集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private String testUsername = "test_admin";
    private String testPassword = "test123";
    private String testRealName = "测试管理员";
    private String testRole = "ADMIN";

    @BeforeEach
    void setUp() {
        // 清空用户数据
        sysUserRepository.deleteAll();

        // 创建测试用户
        SysUser testUser = new SysUser();
        testUser.setUsername(testUsername);
        testUser.setPassword(passwordEncoder.encode(testPassword));
        testUser.setRealName(testRealName);
        testUser.setRole(testRole);
        testUser.setStatus(1);
        sysUserRepository.save(testUser);
    }

    @Test
    @Order(1)
    @DisplayName("测试登录成功")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value(testRole))
                .andExpect(jsonPath("$.data.realName").value(testRealName))
                .andExpect(jsonPath("$.data.userId").exists())
                .andReturn();

        // 验证返回的Token是否有效
        String responseBody = result.getResponse().getContentAsString();
        LoginVO loginVO = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(), 
                LoginVO.class
        );
        
        Assertions.assertNotNull(loginVO.getToken());
        Assertions.assertTrue(jwtUtils.validateToken(loginVO.getToken()));
        Assertions.assertEquals(testUsername, jwtUtils.getUsernameFromToken(loginVO.getToken()));
        Assertions.assertEquals(testRole, jwtUtils.getRoleFromToken(loginVO.getToken()));
    }

    @Test
    @Order(2)
    @DisplayName("测试登录失败 - 用户名错误")
    void testLoginFailure_WrongUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("wrong_user");
        request.setPassword(testPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @Order(3)
    @DisplayName("测试登录失败 - 密码错误")
    void testLoginFailure_WrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword("wrong_password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @Order(4)
    @DisplayName("测试登录失败 - 用户名为空")
    void testLoginFailure_EmptyUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword(testPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("测试登录失败 - 密码为空")
    void testLoginFailure_EmptyPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword("");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("测试登录失败 - 账号已停用")
    void testLoginFailure_AccountDisabled() throws Exception {
        // 停用测试账号
        SysUser user = sysUserRepository.findByUsername(testUsername).orElseThrow();
        user.setStatus(0);
        sysUserRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword(testPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("账号已被停用，请联系管理员"));
    }

    @Test
    @Order(7)
    @DisplayName("测试Token验证 - 有效Token")
    void testValidateToken_ValidToken() throws Exception {
        // 先登录获取Token
        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        LoginVO loginVO = objectMapper.readValue(
                objectMapper.readTree(responseBody).get("data").toString(), 
                LoginVO.class
        );

        // 验证Token
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + loginVO.getToken()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("测试Token验证 - 无效Token")
    void testValidateToken_InvalidToken() throws Exception {
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer invalid_token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Token 验证失败"));
    }

    @Test
    @Order(9)
    @DisplayName("测试Token验证 - 缺少Token")
    void testValidateToken_MissingToken() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("测试登出")
    void testLogout() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }

    @Test
    @Order(11)
    @DisplayName("测试JWT工具类 - 生成和解析Token")
    void testJwtUtils() {
        Long userId = 100L;
        String username = "jwt_test";
        String role = "DOCTOR";
        Long relatedId = 200L;

        // 生成Token
        String token = jwtUtils.generateToken(userId, username, role, relatedId);
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isEmpty());

        // 验证Token
        Assertions.assertTrue(jwtUtils.validateToken(token));

        // 解析Token
        Assertions.assertEquals(userId, jwtUtils.getUserIdFromToken(token));
        Assertions.assertEquals(username, jwtUtils.getUsernameFromToken(token));
        Assertions.assertEquals(role, jwtUtils.getRoleFromToken(token));
        Assertions.assertEquals(relatedId, jwtUtils.getRelatedIdFromToken(token));

        // Token未过期
        Assertions.assertFalse(jwtUtils.isTokenExpired(token));
    }

    @Test
    @Order(12)
    @DisplayName("测试完整登录流程")
    void testCompleteLoginFlow() throws Exception {
        // 1. 登录获取Token
        LoginRequest request = new LoginRequest();
        request.setUsername(testUsername);
        request.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        LoginVO loginVO = objectMapper.readValue(
                objectMapper.readTree(loginResponseBody).get("data").toString(), 
                LoginVO.class
        );
        String token = loginVO.getToken();

        // 2. 使用Token验证
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 3. 登出
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("登出成功"));
    }
}
