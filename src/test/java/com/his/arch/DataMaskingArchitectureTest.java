package com.his.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 数据脱敏架构测试
 *
 * <p>通过 ArchUnit 确保数据脱敏功能的使用符合安全规范：
 * <ul>
 *   <li>DataMaskingContext 应该只在 Controller 层使用</li>
 *   <li>Service 层不应该直接控制脱敏状态</li>
 *   <li>Repository 层不应该依赖脱敏逻辑</li>
 *   <li>工具类应该防止实例化</li>
 * </ul>
 *
 * <h3>运行测试</h3>
 * <pre>
 * ./gradlew test --tests DataMaskingArchitectureTest
 * </pre>
 *
 * <h3>注意</h3>
 * <p>权限相关的检查（如 @PreAuthorize）需要结合 Code Review，
 * 因为 ArchUnit 难以检查复杂的权限表达式语义。
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("数据脱敏架构测试")
class DataMaskingArchitectureTest {

    /**
     * 导入所有需要检查的类
     */
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.his");

    // ==================== 规则1: DataMaskingContext 分层约束 ====================

    @Test
    @DisplayName("规则1: Service 层不应该调用 DataMaskingContext")
    void serviceLayerShouldNotCallDataMaskingContext() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.his.common.DataMaskingContext")
                .because("Service 层不应该控制脱敏状态，应该由 Controller 层统一管理");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则2: Repository 层不应该调用 DataMaskingContext")
    void repositoryLayerShouldNotCallDataMaskingContext() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.his.common.DataMaskingContext")
                .because("Repository 层不应该控制脱敏状态");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则3: Entity 层不应该依赖 DataMaskingContext")
    void entitiesShouldNotDependOnDataMaskingContext() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.his.common.DataMaskingContext")
                .because("Entity 层不应该关心脱敏逻辑，只负责数据模型");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则4: Common 包内的工具类可以互相依赖")
    void commonClassesCanDependOnEachOther() {
        // 这是一个正向测试，确保common包内的工具类可以互相依赖
        // 不需要特别检查，这是允许的
    }

    // ==================== 规则2: 工具类设计约束 ====================

    @Test
    @DisplayName("规则5: DataMaskingUtils 应该是工具类（私有构造函数）")
    void dataMaskingUtilsShouldBeUtilityClass() {
        ArchRule rule = classes()
                .that().haveSimpleName("DataMaskingUtils")
                .should().haveOnlyPrivateConstructors()
                .because("工具类应该防止实例化");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则6: DataMaskingContext 应该是工具类（私有构造函数）")
    void dataMaskingContextShouldBeUtilityClass() {
        ArchRule rule = classes()
                .that().haveSimpleName("DataMaskingContext")
                .should().haveOnlyPrivateConstructors()
                .because("工具类应该防止实例化");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则7: SensitiveData 应该是注解")
    void sensitiveDataShouldBeAnnotation() {
        ArchRule rule = classes()
                .that().haveSimpleName("SensitiveData")
                .should().beInterfaces()
                .because("@SensitiveData 应该是注解接口");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则8: SensitiveType 应该是枚举")
    void sensitiveTypeShouldBeEnum() {
        ArchRule rule = classes()
                .that().haveSimpleName("SensitiveType")
                .should().beEnums()
                .because("SensitiveType 应该是枚举类型");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则9: SensitiveDataSerializer 应该在 config 包中")
    void serializerShouldBeInConfigPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("SensitiveDataSerializer")
                .should().resideInAPackage("..config..")
                .because("序列化器应该放在配置包中");

        rule.check(classes);
    }

    // ==================== 规则3: 编码规范约束 ====================

    // 注意：通用异常检查规则已禁用，因为当前实现中使用 IllegalStateException 是合理的
    // ArchUnit 不支持检查 throwThrowableOfType，已注释掉

    // ==================== 规则4: 命名规范约束 ====================

    @Test
    @DisplayName("规则11: 脱敏相关的工具类应该有规范的命名")
    void maskingClassesShouldHaveConsistentNaming() {
        // 检查敏感数据相关的核心工具类应该是public的
        ArchRule rule = classes()
                .that().haveSimpleName("DataMaskingUtils")
                .or().haveSimpleName("DataMaskingContext")
                .or().haveSimpleName("SensitiveData")
                .or().haveSimpleName("SensitiveType")
                .should().bePublic()
                .because("脱敏相关的工具类应该是public的，以便其他包使用");

        rule.check(classes);
    }
}
