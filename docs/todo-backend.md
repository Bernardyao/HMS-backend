# 后端 TODO（Java 8 Servlet）— HIS

> 范围：对应当前数据库 9 表与门诊闭环。
> 目标：交付稳定可联调的 API、严格的数据一致性与最小化合规能力。

---

## P0（本迭代必须完成）

### 1) 工程骨架与分层
- 建立后端分层：
  - `web/`（Servlet Controller）
  - `service/`（业务规则、事务边界、状态机）
  - `repo/`（DAO/Repository，SQL 与映射）
  - `domain/`（实体/枚举）
  - `dto/`（请求/响应）
  - `security/`（鉴权/权限）
  - `common/`（错误码、响应封装、分页）
- 统一响应结构：`{ code, message, data, traceId, fieldErrors? }`。

### 2) 鉴权与权限（MVP）
- 登录鉴权（Session 或 Token 二选一，先简单可用）：
  - 登录、登出、会话失效；
  - 基础密码策略与暴力破解防护（最少：失败次数限制/冷却）。
- 角色权限：医生/收费员/管理员最小集合；核心接口按角色拦截。

### 3) API 资源与核心业务（闭环）
- 主数据 API：
  - 科室 `/departments`
  - 医生 `/doctors`
  - 患者 `/patients`
  - 药品 `/medicines`
- 门诊闭环 API：
  - 挂号 `/registrations`（创建/取消/完成就诊/列表查询）
  - 病历 `/medical-records`（按挂号查询/创建/保存草稿/提交/审核）
  - 处方 `/prescriptions`（主从写入/审核/发药）
  - 收费 `/charges`（创建收费单/支付回写/退费）

### 4) 数据一致性：事务、幂等、复算校验
- 事务边界：
  - 创建处方：处方主表 + 明细同一事务。
  - 支付回写：写入/更新收费记录同一事务。
- 金额复算：
  - 处方明细 `subtotal` 与主表 `total_amount/item_count` 后端复算，发现不一致直接拒绝。
  - 收费 `actual_amount` 与折扣/医保/退费关系需定义规则并校验。
- 幂等：支付回写必须以 `transaction_no`（或等价幂等键）保证重复回调不重复入账。

### 5) 参数校验与错误码
- 字段校验：必填/长度/格式/枚举范围（即使 DB 未加 CHECK，也必须应用层兜底）。
- 唯一冲突：对 `dept_code/doctor_no/patient_no/id_card/medicine_code/reg_no/record_no/prescription_no/charge_no` 返回明确错误码（例如 `DUPLICATE_KEY`）并带字段名。
- 状态机非法流转：统一错误码（例如 `INVALID_STATE_TRANSITION`）。

### 6) 审计与日志（MVP）
- 关键操作审计：开方/审核/发药/收费/退费/取消挂号/审核病历。
- 日志脱敏：严禁输出身份证/手机号明文到应用日志。
- `traceId`：请求链路 ID（最少在请求进入处生成并贯穿日志）。

---

## P1（2–3 周内完成）

### 1) 数据库约束与迁移工程化
- 引入迁移工具（Flyway/Liquibase 二选一），将变更改为增量迁移脚本。
- 补齐 DB CHECK 约束（按 docs/backend-db-reference.md 的建议）：
  - `gender/status/visit_type/payment_method/charge_type/...`
  - 金额/数量 `>=0`、`quantity>0` 等。
- 评估时间字段升级：`TIMESTAMP` → `TIMESTAMPTZ` 并设为 `NOT NULL`。

### 2) 并发控制
- 对关键更新增加乐观锁/版本控制（例如处方版本、药品库存版本）。
- 收费回写与退费避免竞态：同一单据状态更新要做“读-校验-写”的原子性保证。

### 3) 自动化测试（最小集合）
- 单元测试：状态机与金额复算规则。
- 接口测试：核心闭环冒烟（可用 Testcontainers 或测试库环境）。

---

## P2（持续优化）

- 增加明细与审计表：收费明细（项目级）、库存流水/发药记录、操作审计表（只追加）。
- 监控告警：错误率、延迟、DB 连接、慢查询基线。
- 安全加固：CSRF/会话固定防护、细粒度权限、敏感数据列级加密（视合规要求）。

---

## 依赖与约定（需要与前端/测试对齐）

- OpenAPI/契约：字段/枚举/分页/错误码必须先定再实现。
- 脱敏规则：哪些角色可以看明文、导出权限与审计规则。
- 状态机：由业务确认后冻结，变更需走评审。

参考：
- docs/backend-db-reference.md
- docs/dev-process-3person.md
