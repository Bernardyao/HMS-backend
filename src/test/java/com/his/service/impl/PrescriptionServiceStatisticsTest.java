package com.his.service.impl;

import com.his.dto.PharmacistStatisticsDTO;
import com.his.repository.PrescriptionRepository;
import com.his.test.base.BaseServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionServiceStatisticsTest extends BaseServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    void getPharmacistStatistics_returnsDataFromRepository() {
        Long pharmacistId = 100L;
        PharmacistStatisticsDTO expectedStats = new PharmacistStatisticsDTO(5L, new BigDecimal("100.00"), 10L);

        when(prescriptionRepository.getPharmacistStatistics(eq(pharmacistId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(expectedStats);

        PharmacistStatisticsDTO result = prescriptionService.getPharmacistStatistics(pharmacistId);

        assertThat(result).isNotNull();
        assertThat(result.getDispensedCount()).isEqualTo(5L);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getTotalItems()).isEqualTo(10L);

        verify(prescriptionRepository).getPharmacistStatistics(eq(pharmacistId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
