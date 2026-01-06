package com.his.service.impl;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.his.entity.Doctor;
import com.his.entity.Registration;
import com.his.enums.RegStatusEnum;
import com.his.repository.DepartmentRepository;
import com.his.repository.RegistrationRepository;
import com.his.test.base.BaseServiceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DoctorServiceImplTest extends BaseServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Test
    void validateAndUpdateStatus_whenDoctorMatchesAndWaiting_thenUpdatesToCompleted() {
        long regId = 1L;
        long doctorId = 10L;

        Doctor doctor = new Doctor();
        doctor.setMainId(doctorId);

        Registration registration = new Registration();
        registration.setMainId(regId);
        registration.setDoctor(doctor);
        registration.setIsDeleted((short) 0);
        registration.setVisitDate(LocalDate.now());
        registration.setStatus(RegStatusEnum.WAITING.getCode());

        when(registrationRepository.findById(regId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        doctorService.validateAndUpdateStatus(regId, doctorId, RegStatusEnum.COMPLETED);

        verify(registrationRepository, times(2)).findById(regId);
        verify(registrationRepository).save(registration);
        assertThat(registration.getStatus()).isEqualTo(RegStatusEnum.COMPLETED.getCode());
    }

    @Test
    void validateAndUpdateStatus_whenDoctorMismatch_thenThrowsAndDoesNotSave() {
        long regId = 2L;
        long registrationDoctorId = 11L;
        long currentDoctorId = 12L;

        Doctor doctor = new Doctor();
        doctor.setMainId(registrationDoctorId);

        Registration registration = new Registration();
        registration.setMainId(regId);
        registration.setDoctor(doctor);
        registration.setIsDeleted((short) 0);

        when(registrationRepository.findById(regId)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> doctorService.validateAndUpdateStatus(regId, currentDoctorId, RegStatusEnum.COMPLETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权限操作");

        verify(registrationRepository, times(1)).findById(regId);
        verify(registrationRepository, never()).save(any());
    }
}
