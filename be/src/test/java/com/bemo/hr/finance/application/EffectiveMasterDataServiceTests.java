package com.bemo.hr.finance.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.EffectiveMasterValue;
import com.bemo.hr.finance.infrastructure.EffectiveMasterValueRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectiveMasterDataServiceTests {
    @Test
    void resolvesHistoricalAndCurrentValuesAndRejectsOverlap() {
        EffectiveMasterValueRepository r = mock(EffectiveMasterValueRepository.class);
        EffectiveMasterDataService s = new EffectiveMasterDataService(r, mock(AuditService.class));
        when(r.save(any())).thenAnswer(i -> i.getArgument(0));
        EffectiveMasterValue old = new EffectiveMasterValue("PARTY", "p1", "PAYMENT_TERMS", "30", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), "Initial", "admin");
        EffectiveMasterValue current = new EffectiveMasterValue("PARTY", "p1", "PAYMENT_TERMS", "45", LocalDate.of(2026, 7, 1), null, "Renegotiated", "admin");
        when(r.findByMasterTypeAndMasterIdAndValueKeyOrderByEffectiveFromDesc("PARTY", "p1", "PAYMENT_TERMS")).thenReturn(List.of(current, old));
        assertThat(s.resolve("PARTY", "p1", "PAYMENT_TERMS", LocalDate.of(2026, 3, 1)).getValueText()).isEqualTo("30");
        assertThat(s.resolve("PARTY", "p1", "PAYMENT_TERMS", LocalDate.of(2026, 8, 1)).getValueText()).isEqualTo("45");
        assertThatThrownBy(() -> s.add("PARTY", "p1", "PAYMENT_TERMS", "60", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31), "Overlap", "admin")).hasMessageContaining("OVERLAP");
        assertThat(s.history("PARTY", "p1", "PAYMENT_TERMS")).containsExactly(current, old);
    }
}
