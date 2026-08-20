package com.bemo.hr.verticals;

import com.bemo.hr.verticals.api.SpecializedVerticalsApi.*;
import com.bemo.hr.verticals.api.SpecializedVerticalsController;
import com.bemo.hr.verticals.application.SpecializedVerticalsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecializedVerticalsControllerTests {

    @Mock
    private SpecializedVerticalsService verticalsService;

    @InjectMocks
    private SpecializedVerticalsController controller;

    @Test
    void getSummary_delegatesToService() {
        VerticalsSummaryResponse summary = new VerticalsSummaryResponse(
                10,
                BigDecimal.valueOf(250000),
                5,
                BigDecimal.valueOf(80000),
                BigDecimal.valueOf(30),
                4,
                BigDecimal.valueOf(60000),
                3,
                1500
        );

        when(verticalsService.getVerticalsSummary()).thenReturn(summary);

        VerticalsSummaryResponse res = controller.getSummary();

        assertThat(res.totalActiveStudents()).isEqualTo(10);
        assertThat(res.total3plPalletCapacity()).isEqualTo(1500);
        verify(verticalsService).getVerticalsSummary();
    }

    @Test
    void listStudents_delegatesToService() {
        StudentEnrollmentResponse stu = new StudentEnrollmentResponse(
                "s-1",
                "STU-01",
                "Ahmed",
                "Grade 4",
                "2026/2027",
                "Ali",
                "010",
                BigDecimal.valueOf(20000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(20000),
                "ACTIVE",
                1755600000000L,
                List.of()
        );

        when(verticalsService.listStudents()).thenReturn(List.of(stu));

        List<StudentEnrollmentResponse> res = controller.listStudents();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).studentName()).isEqualTo("Ahmed");
        verify(verticalsService).listStudents();
    }
}
