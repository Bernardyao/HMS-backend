/*
 ============================================================================
 HIS System Design for PostgreSQL 14+ (BIGSERIAL Version)
 ============================================================================
 Source Database       : his_system
 Target Server Type    : PostgreSQL
 Target Server Version : 140000
 File Encoding         : UTF-8
 
 Date: 2025-12-12
 
 说明：本脚本为BIGSERIAL主键版本
 1. 数据库命名: his
 2. 表命名: his_entity (模块前缀 his_)
 3. 字段命名: 下划线命名 (snake_case), 主键 main_id
 4. 主键类型: BIGINT + SEQUENCE (GENERATED ALWAYS AS IDENTITY)
 5. 外键类型: BIGINT
 6. 触发器/函数: t_/p_ 前缀
 7. 字段排序: 优化存储
 ============================================================================
*/

-- ============================================
-- 初始化设置
-- ============================================
SET session_replication_role = 'replica';

-- 删除已存在的触发器和函数（清理旧对象）
DROP FUNCTION IF EXISTS p_set_updated_at() CASCADE;

-- 删除已存在的表（按依赖顺序）
DROP TABLE IF EXISTS his_charge CASCADE;
DROP TABLE IF EXISTS his_prescription_detail CASCADE;
DROP TABLE IF EXISTS his_prescription CASCADE;
DROP TABLE IF EXISTS his_medical_record CASCADE;
DROP TABLE IF EXISTS his_registration CASCADE;
DROP TABLE IF EXISTS his_medicine CASCADE;
DROP TABLE IF EXISTS his_patient CASCADE;
DROP TABLE IF EXISTS his_doctor CASCADE;
DROP TABLE IF EXISTS his_department CASCADE;
DROP TABLE IF EXISTS his_sysuser CASCADE;

-- ============================================
-- 公共函数: 自动更新 updated_at
-- ============================================
CREATE OR REPLACE FUNCTION p_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION p_set_updated_at() IS '自动更新 updated_at 时间戳的触发器函数';

-- ============================================
-- 0. 系统用户表 (his_sysuser)
-- ============================================
CREATE TABLE his_sysuser (
    -- 主键使用IDENTITY (PostgreSQL 10+ 推荐方式)
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username            VARCHAR(50)     NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    phone               VARCHAR(20)     DEFAULT NULL,
    email               VARCHAR(100)    DEFAULT NULL,
    status              SMALLINT        NOT NULL DEFAULT 1,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    last_login_time     TIMESTAMP       DEFAULT NULL,
    
    -- 角色和权限相关
    role_code           VARCHAR(50)     NOT NULL DEFAULT 'DOCTOR',
    department_main_id  BIGINT          DEFAULT NULL,
    
    -- 可空/变长字段在后
    avatar              VARCHAR(500)    DEFAULT NULL,
    remark              VARCHAR(500)    DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_sysuser IS '系统用户表';
COMMENT ON COLUMN his_sysuser.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_sysuser.username IS '用户名（登录账号）';
COMMENT ON COLUMN his_sysuser.password IS '密码（加密存储）';
COMMENT ON COLUMN his_sysuser.name IS '真实姓名';
COMMENT ON COLUMN his_sysuser.phone IS '手机号码';
COMMENT ON COLUMN his_sysuser.email IS '电子邮箱';
COMMENT ON COLUMN his_sysuser.status IS '状态（0=停用, 1=启用）';
COMMENT ON COLUMN his_sysuser.is_deleted IS '软删除标记（0=未删除, 1=已删除）';
COMMENT ON COLUMN his_sysuser.created_at IS '创建时间';
COMMENT ON COLUMN his_sysuser.updated_at IS '更新时间';
COMMENT ON COLUMN his_sysuser.last_login_time IS '最后登录时间';
COMMENT ON COLUMN his_sysuser.role_code IS '角色代码（ADMIN=管理员, DOCTOR=医生, NURSE=护士, PHARMACIST=药师, CASHIER=收费员）';
COMMENT ON COLUMN his_sysuser.department_main_id IS '所属科室ID（医生/护士必填）';
COMMENT ON COLUMN his_sysuser.avatar IS '头像URL';
COMMENT ON COLUMN his_sysuser.remark IS '备注信息';
COMMENT ON COLUMN his_sysuser.created_by IS '创建人ID';
COMMENT ON COLUMN his_sysuser.updated_by IS '更新人ID';

-- 创建索引
CREATE UNIQUE INDEX idx_his_sysuser_username ON his_sysuser (username) WHERE is_deleted = 0;
CREATE INDEX idx_his_sysuser_department ON his_sysuser (department_main_id) WHERE is_deleted = 0;
CREATE INDEX idx_his_sysuser_role ON his_sysuser (role_code) WHERE is_deleted = 0;
CREATE INDEX idx_his_sysuser_status ON his_sysuser (status) WHERE is_deleted = 0;

-- ============================================
-- 1. 科室表 (his_department)
-- ============================================
CREATE TABLE his_department (
    -- 主键使用IDENTITY (PostgreSQL 10+ 推荐方式)
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dept_code           VARCHAR(50)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    status              SMALLINT        NOT NULL DEFAULT 1,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    -- 可空/变长字段在后
    parent_id           BIGINT          DEFAULT NULL,
    sort_order          INTEGER         DEFAULT 0,
    description         VARCHAR(500)    DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_department IS '科室信息表';
COMMENT ON COLUMN his_department.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_department.dept_code IS '科室代码';
COMMENT ON COLUMN his_department.name IS '科室名称';
COMMENT ON COLUMN his_department.status IS '状态（0=停用, 1=启用）';
COMMENT ON COLUMN his_department.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_department.created_at IS '创建时间';
COMMENT ON COLUMN his_department.updated_at IS '更新时间';
COMMENT ON COLUMN his_department.parent_id IS '上级科室ID';
COMMENT ON COLUMN his_department.sort_order IS '排序序号';
COMMENT ON COLUMN his_department.description IS '科室描述';
COMMENT ON COLUMN his_department.created_by IS '创建人ID';
COMMENT ON COLUMN his_department.updated_by IS '更新人ID';

-- ============================================
-- 2. 医生表 (his_doctor)
-- ============================================
CREATE TABLE his_doctor (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_main_id  BIGINT          NOT NULL,
    doctor_no           VARCHAR(50)     NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    gender              SMALLINT        NOT NULL,
    status              SMALLINT        NOT NULL DEFAULT 1,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    title               VARCHAR(50)     DEFAULT NULL,
    specialty           VARCHAR(200)    DEFAULT NULL,
    phone               VARCHAR(20)     DEFAULT NULL,
    email               VARCHAR(100)    DEFAULT NULL,
    license_no          VARCHAR(50)     DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_doctor IS '医生信息表';
COMMENT ON COLUMN his_doctor.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_doctor.department_main_id IS '所属科室ID';
COMMENT ON COLUMN his_doctor.doctor_no IS '医生工号';
COMMENT ON COLUMN his_doctor.name IS '医生姓名';
COMMENT ON COLUMN his_doctor.gender IS '性别（0=女, 1=男, 2=未知）';
COMMENT ON COLUMN his_doctor.status IS '状态（0=停用, 1=启用）';
COMMENT ON COLUMN his_doctor.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_doctor.created_at IS '创建时间';
COMMENT ON COLUMN his_doctor.updated_at IS '更新时间';
COMMENT ON COLUMN his_doctor.title IS '职称';
COMMENT ON COLUMN his_doctor.specialty IS '专长';
COMMENT ON COLUMN his_doctor.phone IS '联系电话';
COMMENT ON COLUMN his_doctor.email IS '电子邮箱';
COMMENT ON COLUMN his_doctor.license_no IS '医师执业证号';
COMMENT ON COLUMN his_doctor.created_by IS '创建人ID';
COMMENT ON COLUMN his_doctor.updated_by IS '更新人ID';

-- ============================================
-- 3. 患者表 (his_patient)
-- ============================================
CREATE TABLE his_patient (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_no          VARCHAR(50)     NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    gender              SMALLINT        NOT NULL,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    birth_date          DATE            DEFAULT NULL,
    age                 SMALLINT        DEFAULT NULL,
    phone               VARCHAR(20)     DEFAULT NULL,
    id_card             VARCHAR(18)     DEFAULT NULL,
    medical_card_no     VARCHAR(50)     DEFAULT NULL,
    address             VARCHAR(500)    DEFAULT NULL,
    emergency_contact   VARCHAR(50)     DEFAULT NULL,
    emergency_phone     VARCHAR(20)     DEFAULT NULL,
    blood_type          VARCHAR(10)     DEFAULT NULL,
    allergy_history     TEXT            DEFAULT NULL,
    medical_history     TEXT            DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_patient IS '患者信息表';
COMMENT ON COLUMN his_patient.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_patient.patient_no IS '病历号';
COMMENT ON COLUMN his_patient.name IS '患者姓名';
COMMENT ON COLUMN his_patient.gender IS '性别（0=女, 1=男, 2=未知）';
COMMENT ON COLUMN his_patient.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_patient.created_at IS '创建时间';
COMMENT ON COLUMN his_patient.updated_at IS '更新时间';
COMMENT ON COLUMN his_patient.birth_date IS '出生日期';
COMMENT ON COLUMN his_patient.age IS '年龄';
COMMENT ON COLUMN his_patient.phone IS '联系电话';
COMMENT ON COLUMN his_patient.id_card IS '身份证号';
COMMENT ON COLUMN his_patient.medical_card_no IS '医保卡号';
COMMENT ON COLUMN his_patient.address IS '联系地址';
COMMENT ON COLUMN his_patient.emergency_contact IS '紧急联系人';
COMMENT ON COLUMN his_patient.emergency_phone IS '紧急联系电话';
COMMENT ON COLUMN his_patient.blood_type IS '血型';
COMMENT ON COLUMN his_patient.allergy_history IS '过敏史';
COMMENT ON COLUMN his_patient.medical_history IS '既往病史';
COMMENT ON COLUMN his_patient.created_by IS '创建人ID';
COMMENT ON COLUMN his_patient.updated_by IS '更新人ID';

-- ============================================
-- 4. 药品表 (his_medicine)
-- ============================================
CREATE TABLE his_medicine (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medicine_code       VARCHAR(50)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    retail_price        DECIMAL(10, 4)  NOT NULL,
    stock_quantity      INTEGER         NOT NULL DEFAULT 0,
    status              SMALLINT        NOT NULL DEFAULT 1,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    generic_name        VARCHAR(200)    DEFAULT NULL,
    specification       VARCHAR(100)    DEFAULT NULL,
    dosage_form         VARCHAR(50)     DEFAULT NULL,
    manufacturer        VARCHAR(200)    DEFAULT NULL,
    approval_no         VARCHAR(100)    DEFAULT NULL,
    category            VARCHAR(50)     DEFAULT NULL,
    unit                VARCHAR(20)     DEFAULT NULL,
    purchase_price      DECIMAL(10, 4)  DEFAULT NULL,
    min_stock           INTEGER         DEFAULT 0,
    max_stock           INTEGER         DEFAULT NULL,
    storage_condition   VARCHAR(100)    DEFAULT NULL,
    expiry_warning_days INTEGER         DEFAULT 90,
    is_prescription     SMALLINT        DEFAULT 0,
    version             INTEGER         DEFAULT 1,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_medicine IS '药品信息表';
COMMENT ON COLUMN his_medicine.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_medicine.medicine_code IS '药品编码';
COMMENT ON COLUMN his_medicine.name IS '药品名称';
COMMENT ON COLUMN his_medicine.retail_price IS '零售价格';
COMMENT ON COLUMN his_medicine.stock_quantity IS '库存数量';
COMMENT ON COLUMN his_medicine.status IS '状态（0=停用, 1=启用）';
COMMENT ON COLUMN his_medicine.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_medicine.created_at IS '创建时间';
COMMENT ON COLUMN his_medicine.updated_at IS '更新时间';
COMMENT ON COLUMN his_medicine.generic_name IS '通用名称';
COMMENT ON COLUMN his_medicine.specification IS '规格';
COMMENT ON COLUMN his_medicine.dosage_form IS '剂型';
COMMENT ON COLUMN his_medicine.manufacturer IS '生产厂家';
COMMENT ON COLUMN his_medicine.approval_no IS '批准文号';
COMMENT ON COLUMN his_medicine.category IS '药品分类';
COMMENT ON COLUMN his_medicine.unit IS '单位';
COMMENT ON COLUMN his_medicine.purchase_price IS '进货价格';
COMMENT ON COLUMN his_medicine.min_stock IS '最低库存';
COMMENT ON COLUMN his_medicine.max_stock IS '最高库存';
COMMENT ON COLUMN his_medicine.storage_condition IS '储存条件';
COMMENT ON COLUMN his_medicine.expiry_warning_days IS '过期预警天数';
COMMENT ON COLUMN his_medicine.is_prescription IS '是否处方药（0=否, 1=是）';
COMMENT ON COLUMN his_medicine.version IS '版本号';
COMMENT ON COLUMN his_medicine.created_by IS '创建人ID';
COMMENT ON COLUMN his_medicine.updated_by IS '更新人ID';

-- ============================================
-- 5. 挂号表 (his_registration)
-- ============================================
CREATE TABLE his_registration (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_main_id     BIGINT          NOT NULL,
    doctor_main_id      BIGINT          NOT NULL,
    department_main_id  BIGINT          NOT NULL,
    reg_no              VARCHAR(50)     NOT NULL,
    visit_date          DATE            NOT NULL,
    visit_type          SMALLINT        NOT NULL DEFAULT 1,
    registration_fee    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    status              SMALLINT        NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    appointment_time    TIMESTAMP       DEFAULT NULL,
    queue_no            VARCHAR(20)     DEFAULT NULL,
    cancel_reason       VARCHAR(500)    DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_registration IS '挂号记录表';
COMMENT ON COLUMN his_registration.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_registration.patient_main_id IS '患者ID';
COMMENT ON COLUMN his_registration.doctor_main_id IS '医生ID';
COMMENT ON COLUMN his_registration.department_main_id IS '科室ID';
COMMENT ON COLUMN his_registration.reg_no IS '挂号流水号';
COMMENT ON COLUMN his_registration.visit_date IS '就诊日期';
COMMENT ON COLUMN his_registration.visit_type IS '就诊类型（1=初诊, 2=复诊, 3=急诊）';
COMMENT ON COLUMN his_registration.registration_fee IS '挂号费';
COMMENT ON COLUMN his_registration.status IS '状态（0=待就诊, 1=已就诊, 2=已取消）';
COMMENT ON COLUMN his_registration.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_registration.created_at IS '创建时间';
COMMENT ON COLUMN his_registration.updated_at IS '更新时间';
COMMENT ON COLUMN his_registration.appointment_time IS '预约时间';
COMMENT ON COLUMN his_registration.queue_no IS '排队号';
COMMENT ON COLUMN his_registration.cancel_reason IS '取消原因';
COMMENT ON COLUMN his_registration.created_by IS '创建人ID';
COMMENT ON COLUMN his_registration.updated_by IS '更新人ID';

-- ============================================
-- 6. 病历表 (his_medical_record)
-- ============================================
CREATE TABLE his_medical_record (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_main_id BIGINT         NOT NULL,
    patient_main_id     BIGINT          NOT NULL,
    doctor_main_id      BIGINT          NOT NULL,
    record_no           VARCHAR(50)     NOT NULL,
    status              SMALLINT        NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    visit_time          TIMESTAMP       DEFAULT now(),
    chief_complaint     TEXT            DEFAULT NULL,
    present_illness     TEXT            DEFAULT NULL,
    past_history        TEXT            DEFAULT NULL,
    personal_history    TEXT            DEFAULT NULL,
    family_history      TEXT            DEFAULT NULL,
    physical_exam       TEXT            DEFAULT NULL,
    auxiliary_exam      TEXT            DEFAULT NULL,
    diagnosis           TEXT            DEFAULT NULL,
    diagnosis_code      VARCHAR(50)     DEFAULT NULL,
    treatment_plan      TEXT            DEFAULT NULL,
    doctor_advice       TEXT            DEFAULT NULL,
    version             INTEGER         DEFAULT 1,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_medical_record IS '电子病历表';
COMMENT ON COLUMN his_medical_record.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_medical_record.registration_main_id IS '挂号记录ID';
COMMENT ON COLUMN his_medical_record.patient_main_id IS '患者ID';
COMMENT ON COLUMN his_medical_record.doctor_main_id IS '医生ID';
COMMENT ON COLUMN his_medical_record.record_no IS '病历编号';
COMMENT ON COLUMN his_medical_record.status IS '状态（0=草稿, 1=已提交, 2=已审核）';
COMMENT ON COLUMN his_medical_record.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_medical_record.created_at IS '创建时间';
COMMENT ON COLUMN his_medical_record.updated_at IS '更新时间';
COMMENT ON COLUMN his_medical_record.visit_time IS '就诊时间';
COMMENT ON COLUMN his_medical_record.chief_complaint IS '主诉';
COMMENT ON COLUMN his_medical_record.present_illness IS '现病史';
COMMENT ON COLUMN his_medical_record.past_history IS '既往史';
COMMENT ON COLUMN his_medical_record.personal_history IS '个人史';
COMMENT ON COLUMN his_medical_record.family_history IS '家族史';
COMMENT ON COLUMN his_medical_record.physical_exam IS '体格检查';
COMMENT ON COLUMN his_medical_record.auxiliary_exam IS '辅助检查';
COMMENT ON COLUMN his_medical_record.diagnosis IS '诊断';
COMMENT ON COLUMN his_medical_record.diagnosis_code IS '诊断编码';
COMMENT ON COLUMN his_medical_record.treatment_plan IS '治疗方案';
COMMENT ON COLUMN his_medical_record.doctor_advice IS '医嘱';
COMMENT ON COLUMN his_medical_record.version IS '版本号';
COMMENT ON COLUMN his_medical_record.created_by IS '创建人ID';
COMMENT ON COLUMN his_medical_record.updated_by IS '更新人ID';

-- ============================================
-- 7. 处方主表 (his_prescription)
-- ============================================
CREATE TABLE his_prescription (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_main_id      BIGINT          NOT NULL,
    patient_main_id     BIGINT          NOT NULL,
    doctor_main_id      BIGINT          NOT NULL,
    prescription_no     VARCHAR(50)     NOT NULL,
    prescription_type   SMALLINT        NOT NULL DEFAULT 1,
    total_amount        DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    item_count          INTEGER         NOT NULL DEFAULT 0,
    status              SMALLINT        NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    validity_days       INTEGER         DEFAULT 3,
    review_doctor_main_id BIGINT        DEFAULT NULL,
    review_time         TIMESTAMP       DEFAULT NULL,
    review_remark       VARCHAR(500)    DEFAULT NULL,
    dispense_time       TIMESTAMP       DEFAULT NULL,
    dispense_by         BIGINT          DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_prescription IS '处方主表';
COMMENT ON COLUMN his_prescription.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_prescription.record_main_id IS '病历ID';
COMMENT ON COLUMN his_prescription.patient_main_id IS '患者ID';
COMMENT ON COLUMN his_prescription.doctor_main_id IS '开方医生ID';
COMMENT ON COLUMN his_prescription.prescription_no IS '处方号';
COMMENT ON COLUMN his_prescription.prescription_type IS '处方类型（1=西药, 2=中药, 3=中成药）';
COMMENT ON COLUMN his_prescription.total_amount IS '总金额';
COMMENT ON COLUMN his_prescription.item_count IS '药品数量';
COMMENT ON COLUMN his_prescription.status IS '状态（0=草稿, 1=已开方, 2=已审核, 3=已发药）';
COMMENT ON COLUMN his_prescription.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_prescription.created_at IS '创建时间';
COMMENT ON COLUMN his_prescription.updated_at IS '更新时间';
COMMENT ON COLUMN his_prescription.validity_days IS '有效天数';
COMMENT ON COLUMN his_prescription.review_doctor_main_id IS '审核医生ID';
COMMENT ON COLUMN his_prescription.review_time IS '审核时间';
COMMENT ON COLUMN his_prescription.review_remark IS '审核备注';
COMMENT ON COLUMN his_prescription.dispense_time IS '发药时间';
COMMENT ON COLUMN his_prescription.dispense_by IS '发药人ID';
COMMENT ON COLUMN his_prescription.created_by IS '创建人ID';
COMMENT ON COLUMN his_prescription.updated_by IS '更新人ID';

-- ============================================
-- 8. 处方明细表 (his_prescription_detail)
-- ============================================
CREATE TABLE his_prescription_detail (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prescription_main_id BIGINT         NOT NULL,
    medicine_main_id    BIGINT          NOT NULL,
    medicine_name       VARCHAR(200)    NOT NULL,
    unit_price          DECIMAL(10, 4)  NOT NULL,
    quantity            INTEGER         NOT NULL,
    subtotal            DECIMAL(10, 2)  NOT NULL,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    specification       VARCHAR(100)    DEFAULT NULL,
    unit                VARCHAR(20)     DEFAULT NULL,
    frequency           VARCHAR(50)     DEFAULT NULL,
    dosage              VARCHAR(50)     DEFAULT NULL,
    route               VARCHAR(50)     DEFAULT NULL,
    days                INTEGER         DEFAULT NULL,
    instructions        VARCHAR(500)    DEFAULT NULL,
    sort_order          INTEGER         DEFAULT 0,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_prescription_detail IS '处方明细表';
COMMENT ON COLUMN his_prescription_detail.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_prescription_detail.prescription_main_id IS '处方ID';
COMMENT ON COLUMN his_prescription_detail.medicine_main_id IS '药品ID';
COMMENT ON COLUMN his_prescription_detail.medicine_name IS '药品名称';
COMMENT ON COLUMN his_prescription_detail.unit_price IS '单价';
COMMENT ON COLUMN his_prescription_detail.quantity IS '数量';
COMMENT ON COLUMN his_prescription_detail.subtotal IS '小计';
COMMENT ON COLUMN his_prescription_detail.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_prescription_detail.created_at IS '创建时间';
COMMENT ON COLUMN his_prescription_detail.updated_at IS '更新时间';
COMMENT ON COLUMN his_prescription_detail.specification IS '规格';
COMMENT ON COLUMN his_prescription_detail.unit IS '单位';
COMMENT ON COLUMN his_prescription_detail.frequency IS '用药频率';
COMMENT ON COLUMN his_prescription_detail.dosage IS '用量';
COMMENT ON COLUMN his_prescription_detail.route IS '用药途径';
COMMENT ON COLUMN his_prescription_detail.days IS '用药天数';
COMMENT ON COLUMN his_prescription_detail.instructions IS '用药说明';
COMMENT ON COLUMN his_prescription_detail.sort_order IS '排序序号';
COMMENT ON COLUMN his_prescription_detail.created_by IS '创建人ID';
COMMENT ON COLUMN his_prescription_detail.updated_by IS '更新人ID';

-- ============================================
-- 9. 缴费表 (his_charge)
-- ============================================
CREATE TABLE his_charge (
    main_id             BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_main_id     BIGINT          NOT NULL,
    charge_no           VARCHAR(50)     NOT NULL,
    charge_type         SMALLINT        NOT NULL DEFAULT 1,
    total_amount        DECIMAL(10, 2)  NOT NULL,
    actual_amount       DECIMAL(10, 2)  NOT NULL,
    status              SMALLINT        NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       DEFAULT now(),
    updated_at          TIMESTAMP       DEFAULT now(),
    
    registration_main_id BIGINT         DEFAULT NULL,
    discount_amount     DECIMAL(10, 2)  DEFAULT 0.00,
    insurance_amount    DECIMAL(10, 2)  DEFAULT 0.00,
    payment_method      SMALLINT        DEFAULT NULL,
    transaction_no      VARCHAR(100)    DEFAULT NULL,
    charge_time         TIMESTAMP       DEFAULT NULL,
    cashier_main_id     BIGINT          DEFAULT NULL,
    refund_amount       DECIMAL(10, 2)  DEFAULT 0.00,
    refund_time         TIMESTAMP       DEFAULT NULL,
    refund_reason       VARCHAR(500)    DEFAULT NULL,
    invoice_no          VARCHAR(50)     DEFAULT NULL,
    remark              VARCHAR(500)    DEFAULT NULL,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL
);

COMMENT ON TABLE his_charge IS '缴费记录表';
COMMENT ON COLUMN his_charge.main_id IS '主键ID（自增）';
COMMENT ON COLUMN his_charge.patient_main_id IS '患者ID';
COMMENT ON COLUMN his_charge.charge_no IS '收费单号';
COMMENT ON COLUMN his_charge.charge_type IS '收费类型（1=挂号费, 2=药费, 3=检查费, 4=治疗费）';
COMMENT ON COLUMN his_charge.total_amount IS '应收金额';
COMMENT ON COLUMN his_charge.actual_amount IS '实收金额';
COMMENT ON COLUMN his_charge.status IS '状态（0=未缴费, 1=已缴费, 2=已退费）';
COMMENT ON COLUMN his_charge.is_deleted IS '软删除标记';
COMMENT ON COLUMN his_charge.created_at IS '创建时间';
COMMENT ON COLUMN his_charge.updated_at IS '更新时间';
COMMENT ON COLUMN his_charge.registration_main_id IS '挂号记录ID';
COMMENT ON COLUMN his_charge.discount_amount IS '优惠金额';
COMMENT ON COLUMN his_charge.insurance_amount IS '医保金额';
COMMENT ON COLUMN his_charge.payment_method IS '支付方式（1=现金, 2=银行卡, 3=微信, 4=支付宝, 5=医保）';
COMMENT ON COLUMN his_charge.transaction_no IS '交易流水号';
COMMENT ON COLUMN his_charge.charge_time IS '收费时间';
COMMENT ON COLUMN his_charge.cashier_main_id IS '收费员ID';
COMMENT ON COLUMN his_charge.refund_amount IS '退费金额';
COMMENT ON COLUMN his_charge.refund_time IS '退费时间';
COMMENT ON COLUMN his_charge.refund_reason IS '退费原因';
COMMENT ON COLUMN his_charge.invoice_no IS '发票号';
COMMENT ON COLUMN his_charge.remark IS '备注';
COMMENT ON COLUMN his_charge.created_by IS '创建人ID';
COMMENT ON COLUMN his_charge.updated_by IS '更新人ID';

-- ============================================
-- 10. 唯一索引 (UNIQUE INDEX)
-- ============================================
CREATE UNIQUE INDEX uk_his_department_dept_code ON his_department(dept_code) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_doctor_doctor_no ON his_doctor(doctor_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_patient_patient_no ON his_patient(patient_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_patient_id_card ON his_patient(id_card) WHERE is_deleted = 0 AND id_card IS NOT NULL;
CREATE UNIQUE INDEX uk_his_medicine_medicine_code ON his_medicine(medicine_code) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_registration_reg_no ON his_registration(reg_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_medical_record_record_no ON his_medical_record(record_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_medical_record_registration ON his_medical_record(registration_main_id) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_prescription_prescription_no ON his_prescription(prescription_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_his_charge_charge_no ON his_charge(charge_no) WHERE is_deleted = 0;

-- ============================================
-- 11. 普通索引 (INDEX)
-- ============================================
-- 科室表
CREATE INDEX idx_his_department_parent_id ON his_department(parent_id);
CREATE INDEX idx_his_department_status ON his_department(status) WHERE is_deleted = 0;

-- 医生表
CREATE INDEX idx_his_doctor_department_main_id ON his_doctor(department_main_id);
CREATE INDEX idx_his_doctor_name ON his_doctor(name);
CREATE INDEX idx_his_doctor_status ON his_doctor(status) WHERE is_deleted = 0;

-- 患者表
CREATE INDEX idx_his_patient_name ON his_patient(name);
CREATE INDEX idx_his_patient_phone ON his_patient(phone);

-- 药品表
CREATE INDEX idx_his_medicine_name ON his_medicine(name);
CREATE INDEX idx_his_medicine_category ON his_medicine(category);
CREATE INDEX idx_his_medicine_status ON his_medicine(status) WHERE is_deleted = 0;
CREATE INDEX idx_his_medicine_stock ON his_medicine(stock_quantity) WHERE stock_quantity <= min_stock;

-- 挂号表
CREATE INDEX idx_his_registration_patient_main_id ON his_registration(patient_main_id);
CREATE INDEX idx_his_registration_doctor_main_id ON his_registration(doctor_main_id);
CREATE INDEX idx_his_registration_department_main_id ON his_registration(department_main_id);
CREATE INDEX idx_his_registration_visit_date ON his_registration(visit_date);
CREATE INDEX idx_his_registration_status ON his_registration(status);
CREATE INDEX idx_his_registration_composite ON his_registration(visit_date, status, department_main_id) WHERE is_deleted = 0;

-- 病历表
CREATE INDEX idx_his_medical_record_patient_main_id ON his_medical_record(patient_main_id);
CREATE INDEX idx_his_medical_record_doctor_main_id ON his_medical_record(doctor_main_id);
CREATE INDEX idx_his_medical_record_visit_time ON his_medical_record(visit_time);

-- 处方表
CREATE INDEX idx_his_prescription_record_main_id ON his_prescription(record_main_id);
CREATE INDEX idx_his_prescription_patient_main_id ON his_prescription(patient_main_id);
CREATE INDEX idx_his_prescription_doctor_main_id ON his_prescription(doctor_main_id);
CREATE INDEX idx_his_prescription_status ON his_prescription(status);

-- 处方明细表
CREATE INDEX idx_his_prescription_detail_prescription_main_id ON his_prescription_detail(prescription_main_id);
CREATE INDEX idx_his_prescription_detail_medicine_main_id ON his_prescription_detail(medicine_main_id);

-- 缴费表
CREATE INDEX idx_his_charge_patient_main_id ON his_charge(patient_main_id);
CREATE INDEX idx_his_charge_registration_main_id ON his_charge(registration_main_id);
CREATE INDEX idx_his_charge_status ON his_charge(status);
CREATE INDEX idx_his_charge_charge_time ON his_charge(charge_time);

-- ============================================
-- 12. 外键约束 (FOREIGN KEY)
-- ============================================
-- 系统用户表
ALTER TABLE his_sysuser
    ADD CONSTRAINT fk_sysuser_department
    FOREIGN KEY (department_main_id)
    REFERENCES his_department(main_id)
    ON DELETE SET NULL;

-- 科室表 (自引用)
ALTER TABLE his_department 
    ADD CONSTRAINT fk_his_department_parent 
    FOREIGN KEY (parent_id) REFERENCES his_department(main_id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 医生表
ALTER TABLE his_doctor 
    ADD CONSTRAINT fk_his_doctor_department 
    FOREIGN KEY (department_main_id) REFERENCES his_department(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 挂号表
ALTER TABLE his_registration 
    ADD CONSTRAINT fk_his_registration_patient 
    FOREIGN KEY (patient_main_id) REFERENCES his_patient(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_registration 
    ADD CONSTRAINT fk_his_registration_doctor 
    FOREIGN KEY (doctor_main_id) REFERENCES his_doctor(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_registration 
    ADD CONSTRAINT fk_his_registration_department 
    FOREIGN KEY (department_main_id) REFERENCES his_department(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 病历表
ALTER TABLE his_medical_record 
    ADD CONSTRAINT fk_his_medical_record_registration 
    FOREIGN KEY (registration_main_id) REFERENCES his_registration(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_medical_record 
    ADD CONSTRAINT fk_his_medical_record_patient 
    FOREIGN KEY (patient_main_id) REFERENCES his_patient(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_medical_record 
    ADD CONSTRAINT fk_his_medical_record_doctor 
    FOREIGN KEY (doctor_main_id) REFERENCES his_doctor(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 处方表
ALTER TABLE his_prescription 
    ADD CONSTRAINT fk_his_prescription_record 
    FOREIGN KEY (record_main_id) REFERENCES his_medical_record(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_prescription 
    ADD CONSTRAINT fk_his_prescription_patient 
    FOREIGN KEY (patient_main_id) REFERENCES his_patient(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_prescription 
    ADD CONSTRAINT fk_his_prescription_doctor 
    FOREIGN KEY (doctor_main_id) REFERENCES his_doctor(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_prescription 
    ADD CONSTRAINT fk_his_prescription_review_doctor 
    FOREIGN KEY (review_doctor_main_id) REFERENCES his_doctor(main_id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 处方明细表
ALTER TABLE his_prescription_detail 
    ADD CONSTRAINT fk_his_prescription_detail_prescription 
    FOREIGN KEY (prescription_main_id) REFERENCES his_prescription(main_id)
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE his_prescription_detail 
    ADD CONSTRAINT fk_his_prescription_detail_medicine 
    FOREIGN KEY (medicine_main_id) REFERENCES his_medicine(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 缴费表
ALTER TABLE his_charge 
    ADD CONSTRAINT fk_his_charge_patient 
    FOREIGN KEY (patient_main_id) REFERENCES his_patient(main_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE his_charge 
    ADD CONSTRAINT fk_his_charge_registration 
    FOREIGN KEY (registration_main_id) REFERENCES his_registration(main_id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- ============================================
-- 13. 触发器 (Triggers)
-- ============================================
CREATE TRIGGER t_his_sysuser_updated_at
    BEFORE UPDATE ON his_sysuser
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_department_updated_at
    BEFORE UPDATE ON his_department
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_doctor_updated_at
    BEFORE UPDATE ON his_doctor
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_patient_updated_at
    BEFORE UPDATE ON his_patient
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_medicine_updated_at
    BEFORE UPDATE ON his_medicine
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_registration_updated_at
    BEFORE UPDATE ON his_registration
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_medical_record_updated_at
    BEFORE UPDATE ON his_medical_record
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_prescription_updated_at
    BEFORE UPDATE ON his_prescription
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_prescription_detail_updated_at
    BEFORE UPDATE ON his_prescription_detail
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

CREATE TRIGGER t_his_charge_updated_at
    BEFORE UPDATE ON his_charge
    FOR EACH ROW
    EXECUTE FUNCTION p_set_updated_at();

-- ============================================
-- 14. 初始化数据
-- ============================================

-- 插入默认管理员账户
-- 密码: admin123 (实际使用时应使用加密后的密码，如 BCrypt)
INSERT INTO his_sysuser (username, password, name, phone, email, role_code, status, is_deleted, remark)
VALUES 
    ('admin', '$2a$10$EH/qE.0QE0QE0QE0QE0QEuGKF9kK0QE0QE0QE0QE0QE0QE0QE0QE', '系统管理员', '13800138000', 'admin@his.com', 'ADMIN', 1, 0, '系统默认管理员账户'),
    ('doctor001', '$2a$10$EH/qE.0QE0QE0QE0QE0QEuGKF9kK0QE0QE0QE0QE0QE0QE0QE0QE', '张医生', '13800138001', 'doctor001@his.com', 'DOCTOR', 1, 0, '测试医生账户');

COMMENT ON TABLE his_sysuser IS '系统用户表 - 默认密码为 admin123，建议首次登录后修改';

-- ============================================
-- 恢复外键检查
-- ============================================
SET session_replication_role = 'origin';
