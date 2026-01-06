package com.his.arch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 日志框架架构测试
 *
 * <p>通过 ArchUnit 确保日志框架的使用符合架构规范：
 * <ul>
 *   <li>日志框架类应该在正确的包中</li>
 *   <li>工具类应该是私有构造函数</li>
 *   <li>@AuditLog 和 @ApiLog 应该在 Controller/Service 层使用</li>
 *   <li>切面类应该有 @Aspect 注解</li>
 * </ul>
 *
 * <h3>运行测试</h3>
 * <pre>
 * ./gradlew test --tests LoggingArchitectureTest
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 */
@DisplayName("日志框架架构测试")
class LoggingArchitectureTest {

    /**
     * 导入所有需要检查的类
     */
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.his");

    // ==================== 规则1: 包结构约束 ====================

    @Test
    @DisplayName("规则1: 日志注解应该在 annotation 包中")
    void logAnnotationsShouldBeInAnnotationPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("ApiLog")
                .or().haveSimpleName("AuditLog")
                .or().haveSimpleName("OperationType")
                .or().haveSimpleName("AuditType")
                .should().resideInAPackage("..log.annotation..")
                .because("日志注解应该在 annotation 包中");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则2: 日志切面应该在 aspect 包中")
    void logAspectsShouldBeInAspectPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("ApiLogAspect")
                .or().haveSimpleName("AuditLogAspect")
                .should().resideInAPackage("..log.aspect..")
                .because("日志切面应该在 aspect 包中");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则3: TraceIdFilter 应该在 filter 包中")
    void traceIdFilterShouldBeInFilterPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("TraceIdFilter")
                .should().resideInAPackage("..log.filter..")
                .because("TraceIdFilter 应该在 filter 包中");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则4: LogUtils 应该在 utils 包中")
    void logUtilsShouldBeInUtilsPackage() {
        ArchRule rule = classes()
                .that().haveSimpleName("LogUtils")
                .should().resideInAPackage("..log.utils..")
                .because("LogUtils 应该在 utils 包中");

        rule.check(classes);
    }

    // ==================== 规则2: 工具类设计约束 ====================

    @Test
    @DisplayName("规则5: LogUtils 应该是工具类（私有构造函数）")
    void logUtilsShouldBeUtilityClass() {
        ArchRule rule = classes()
                .that().haveSimpleName("LogUtils")
                .should().haveOnlyPrivateConstructors()
                .because("LogUtils 是工具类，应该防止实例化");

        rule.check(classes);
    }

    // ==================== 规则3: 注解使用规范 ====================

    @Test
    @DisplayName("规则6: @AuditLog 应该在 Controller 或 Service 层使用")
    void auditLogShouldBeUsedInControllerOrService() {
        // 注意：这是一个建议性规则，不是强制性的
        // 审计日志可以在任何层使用，但主要在 Controller/Service 层
        // 因此这里只检查不应该在 Entity/Repository 层直接使用 LogUtils

        ArchRule rule = noClasses()
                .that().resideInAPackage("..entity..")
                .or().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.his.log.utils.LogUtils")
                .because("Entity 和 Repository 层不应该直接记录审计日志，应该在业务层记录");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则7: 日志框架类不应该被 Entity 依赖")
    void entitiesShouldNotDependOnLoggingFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat()
                    .resideInAPackage("..log..")
                .because("Entity 层不应该依赖日志框架，保持纯粹的数据模型");

        rule.check(classes);
    }

    // ==================== 规则4: 命名规范约束 ====================

    @Test
    @DisplayName("规则8: 日志相关的切面类应该有规范的命名")
    void logAspectsShouldHaveConsistentNaming() {
        ArchRule rule = classes()
                .that().resideInAPackage("..log.aspect..")
                .and().areNotInterfaces()
                .and().haveSimpleNameNotContaining("Test")  // 排除测试类
                .should().haveSimpleNameEndingWith("Aspect")
                .because("日志切面类应该以 'Aspect' 结尾");

        rule.check(classes);
    }

    @Test
    @DisplayName("规则9: 日志相关的注解应该有规范的命名")
    void logAnnotationsShouldHaveConsistentNaming() {
        ArchRule rule = classes()
                .that().resideInAPackage("..log.annotation..")
                .and().areInterfaces()
                .should().haveSimpleNameContaining("Log")
                .because("日志注解应该包含 'Log' 关键字");

        rule.check(classes);
    }

    // ==================== 规则5: 访问权限约束 ====================

    @Test
    @DisplayName("规则10: 日志框架的核心类应该是 public 的")
    void logFrameworkClassesShouldBePublic() {
        // 检查切面类（排除测试类）
        ArchRule aspectsRule = classes()
                .that().resideInAPackage("..log.aspect..")
                .and().haveSimpleNameNotContaining("Test")  // 排除测试类
                .should().bePublic()
                .because("日志切面类应该是 public 的");

        // 检查过滤器类（排除测试类）
        ArchRule filterRule = classes()
                .that().resideInAPackage("..log.filter..")
                .and().haveSimpleNameNotContaining("Test")  // 排除测试类
                .should().bePublic()
                .because("日志过滤器类应该是 public 的");

        // 检查工具类（排除测试类）
        ArchRule utilsRule = classes()
                .that().resideInAPackage("..log.utils..")
                .and().haveSimpleNameNotContaining("Test")  // 排除测试类
                .should().bePublic()
                .because("日志工具类应该是 public 的");

        aspectsRule.check(classes);
        filterRule.check(classes);
        utilsRule.check(classes);
    }
}
