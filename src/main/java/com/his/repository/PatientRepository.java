package com.his.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.his.entity.Patient;

/**
 * 患者 Repository
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    /**
     * 根据病历号查询（未删除）
     */
    Optional<Patient> findByPatientNoAndIsDeleted(String patientNo, Short isDeleted);

    /**
     * 根据身份证号查询（未删除）
     */
    Optional<Patient> findByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 统计指定身份证号的患者数量（未删除）
     */
    long countByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 根据姓名模糊查询（未删除）
     */
    List<Patient> findByNameContainingAndIsDeleted(String name, Short isDeleted);

    /**
     * 根据手机号查询（未删除）
     */
    List<Patient> findByPhoneAndIsDeleted(String phone, Short isDeleted);

    /**
     * 检查病历号是否存在
     */
    boolean existsByPatientNoAndIsDeleted(String patientNo, Short isDeleted);

    /**
     * 检查身份证号是否存在
     */
    boolean existsByIdCardAndIsDeleted(String idCard, Short isDeleted);

    /**
     * 查询所有未删除的患者
     */
    @Query("SELECT p FROM Patient p WHERE p.isDeleted = 0")
    List<Patient> findAllActive();

    /**
     * 根据姓名和手机号查询
     */
    @Query("SELECT p FROM Patient p WHERE p.name = :name AND p.phone = :phone AND p.isDeleted = 0")
    Optional<Patient> findByNameAndPhone(@Param("name") String name, @Param("phone") String phone);

    /**
     * 根据医保卡号查询
     */
    Optional<Patient> findByMedicalCardNoAndIsDeleted(String medicalCardNo, Short isDeleted);

    /**
     * 根据ID列表批量查询
     */
    List<Patient> findByMainIdInAndIsDeleted(List<Long> ids, Short isDeleted);

    /**
     * 组合搜索：根据关键字模糊匹配姓名、身份证号或手机号（护士工作站专用）
     *
     * <p>此方法支持三字段联合搜索，用于患者信息自动补全功能</p>
     *
     * @param keyword 搜索关键字（支持姓名、身份证号、手机号的模糊匹配）
     * @param pageable 分页参数（建议限制前15条）
     * @return 匹配的患者列表，按更新时间降序排列
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "p.isDeleted = 0 AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "p.idCard LIKE CONCAT('%', :keyword, '%') OR " +
           "p.phone LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY p.updatedAt DESC")
    List<Patient> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ========== 编号生成方法 - 使用数据库序列保证线程安全 ==========

    /**
     * 通过数据库原生函数生成患者病历号（线程安全）
     *
     * <p>使用PostgreSQL序列保证唯一性，格式：P + yyyyMMdd + 4位序列</p>
     *
     * @return 唯一的患者病历号
     */
    @Query(value = "SELECT generate_patient_no()", nativeQuery = true)
    String generatePatientNo();
}
