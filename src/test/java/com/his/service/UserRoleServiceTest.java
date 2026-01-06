package com.his.service;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.his.vo.views.MedicineViews;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * UserRoleService 单元测试
 * <p>
 * 测试角色到视图的映射逻辑，展示重构后的可测试性。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleService 单元测试")
class UserRoleServiceTest {

    @Mock
    private Authentication mockAuthentication;

    @Nested
    @DisplayName("测试角色到视图的映射")
    class RoleMappingTests {

        @Test
        @DisplayName("PHARMACIST 角色应映射到 Pharmacist 视图")
        void testPharmacistRoleMapping() {
            // Arrange
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PHARMACIST")))
                .when(mockAuthentication).getAuthorities();

            // Act: 使用匿名子类注入 mock authentication
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Pharmacist.class, view);
        }

        @Test
        @DisplayName("DOCTOR 角色应映射到 Doctor 视图")
        void testDoctorRoleMapping() {
            // Arrange
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Doctor.class, view);
        }

        @Test
        @DisplayName("ADMIN 角色应映射到 Pharmacist 视图")
        void testAdminRoleMapping() {
            // Arrange
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Pharmacist.class, view);
        }

        @Test
        @DisplayName("未认证用户应映射到 Public 视图")
        void testUnauthenticatedUserMapping() {
            // Arrange - 未认证场景，直接返回null，无需stubbing
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return null; // 模拟未认证场景
                }
            };

            // Act
            Class<?> view = testService.getMedicineViewForCurrentUser();

            // Assert
            assertEquals(MedicineViews.Public.class, view);
        }

        @Test
        @DisplayName("匿名用户应映射到 Public 视图")
        void testAnonymousUserMapping() {
            // Arrange - 匿名用户场景，只stub getPrincipal()即可
            // isAuthenticated()不会被调用，因为getPrincipal()返回"anonymousUser"时会提前返回
            when(mockAuthentication.getPrincipal()).thenReturn("anonymousUser");

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Public.class, view);
        }
    }

    @Nested
    @DisplayName("测试边界情况")
    class EdgeCaseTests {

        @Test
        @DisplayName("未知角色应映射到 Public 视图")
        void testUnknownRoleMapping() {
            // Arrange - 模拟未知角色
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_UNKNOWN")))
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Public.class, view, "未知角色应使用Public视图");
        }

        @Test
        @DisplayName("authorities为空列表应返回 Public 视图")
        void testEmptyAuthorities() {
            // Arrange - 空角色列表
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.emptyList())
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Public.class, view, "空角色列表应使用Public视图");
        }

        @Test
        @DisplayName("authority为null应被忽略")
        void testNullAuthorityInList() {
            // Arrange - 角色列表中包含null
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Arrays.asList(null, new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            Class<?> view = testService.getMedicineViewForCurrentUser();
            assertEquals(MedicineViews.Doctor.class, view, "应忽略null authority，使用DOCTOR角色");
        }
    }

    @Nested
    @DisplayName("测试角色检查")
    class RoleCheckTests {

        @Test
        @DisplayName("hasRole() 应正确识别用户角色")
        void testHasRole() {
            // Arrange
            when(mockAuthentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .when(mockAuthentication).getAuthorities();

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            assertTrue(testService.hasRole("DOCTOR"));
            assertFalse(testService.hasRole("PHARMACIST"));
        }

        @Test
        @DisplayName("未认证用户调用 hasRole() 应返回 false")
        void testHasRoleWhenUnauthenticated() {
            // Arrange
            when(mockAuthentication).thenReturn(null);

            // Act
            UserRoleService testService = new UserRoleService() {
                @Override
                protected Authentication getAuthentication() {
                    return mockAuthentication;
                }
            };

            // Assert
            assertFalse(testService.hasRole("DOCTOR"));
        }
    }
}
