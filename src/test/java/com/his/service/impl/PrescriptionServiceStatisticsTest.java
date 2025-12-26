package com.his.service.impl;

import com.his.dto.PharmacistStatisticsDTO;
import com.his.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceStatisticsTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    void getPharmacistStatistics_returnsDataFromRepository() {
        Long pharmacistId = 100L;
        PharmacistStatisticsDTO expectedStats = new PharmacistStatisticsDTO(5, new BigDecimal("100.00"), 10);

        when(prescriptionRepository.getPharmacistStatistics(eq(pharmacistId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(expectedStats);

        PharmacistStatisticsDTO result = prescriptionService.getPharmacistStatistics(pharmacistId);

        assertThat(result).isNotNull();
        assertThat(result.getDispensedCount()).isEqualTo(5);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getTotalItems()).isEqualTo(10);

        verify(prescriptionRepository).getPharmacistStatistics(eq(pharmacistId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
