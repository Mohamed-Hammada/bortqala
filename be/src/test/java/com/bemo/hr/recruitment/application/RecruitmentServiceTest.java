package com.bemo.hr.recruitment.application;

import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.recruitment.api.RecruitmentApi;
import com.bemo.hr.recruitment.domain.*;
import com.bemo.hr.recruitment.infrastructure.ApplicationStageEventRepository;
import com.bemo.hr.recruitment.infrastructure.JobApplicationRepository;
import com.bemo.hr.recruitment.infrastructure.JobOpeningRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock private JobOpeningRepository openingRepository;
    @Mock private JobApplicationRepository applicationRepository;
    @Mock private ApplicationStageEventRepository eventRepository;
    @Mock private EmployeeRepository employeeRepository;
    @InjectMocks private RecruitmentService service;

    private MockedStatic<SecurityContextHolder> securityCtx;

    @BeforeEach
    void setUp() {
        securityCtx = mockStatic(SecurityContextHolder.class);
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test-user");
        when(ctx.getAuthentication()).thenReturn(auth);
        securityCtx.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    @AfterEach
    void tearDown() {
        securityCtx.close();
    }

    @Test
    void createOpeningSavesSuccessfully() {
        RecruitmentApi.CreateOpeningRequest req = new RecruitmentApi.CreateOpeningRequest(
                "مهندس برمجيات", "Software Engineer", "DEPT-1", 2, "desc");
        when(openingRepository.save(any(JobOpening.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RecruitmentApi.OpeningResponse result = service.createOpening(req);

        assertThat(result.titleAr()).isEqualTo("مهندس برمجيات");
        assertThat(result.headcount()).isEqualTo(2);
        verify(eventRepository).save(any());
    }

    @Test
    void createApplicationBlocksWhenOpeningClosed() {
        JobOpening closed = new JobOpening("ar", "en", "d", 1, "d", false);
        closed.close();
        when(openingRepository.findById("op1")).thenReturn(Optional.of(closed));

        RecruitmentApi.CreateApplicationRequest req = new RecruitmentApi.CreateApplicationRequest(
                "op1", "Ali", "01012345678", "ali@test.com", "linkedin", null);

        assertThatThrownBy(() -> service.createApplication(req))
                .hasMessageContaining("closed");
    }

    @Test
    void moveStageRejectsInvalidTransition() {
        JobApplication app = new JobApplication("op1", "Ali", "01012345678", "ali@test.com", "linkedin", null);
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        RecruitmentApi.MoveStageRequest req = new RecruitmentApi.MoveStageRequest(
                ApplicationStage.HIRED, "skip to hired");

        assertThatThrownBy(() -> service.moveStage("app1", req))
                .hasMessageContaining("Invalid");
    }

    @Test
    void moveStageAcceptsValidTransition() {
        JobApplication app = new JobApplication("op1", "Ali", "01012345678", "ali@test.com", "linkedin", null);
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RecruitmentApi.MoveStageRequest req = new RecruitmentApi.MoveStageRequest(
                ApplicationStage.SCREENING, "good resume");

        RecruitmentApi.ApplicationResponse result = service.moveStage("app1", req);

        assertThat(result.stage()).isEqualTo(ApplicationStage.SCREENING);
        verify(eventRepository).save(any());
    }

    @Test
    void convertBlocksSecondConversion() {
        JobApplication app = new JobApplication("op1", "Ali", "01012345678", "ali@test.com", "linkedin", null);
        app.markHired("emp-old");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.convertToEmployee("app1",
                new RecruitmentApi.ConvertToEmployeeRequest("cat1", "DEPT-1")))
                .hasMessageContaining("already converted");
    }

    @Test
    void convertCreatesEmployee() {
        JobApplication app = new JobApplication("op1", "Ali", "01012345678", "ali@test.com", "linkedin", null);
        app.moveToStage(ApplicationStage.OFFER);
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));
        when(employeeRepository.save(any()))
                .thenAnswer(inv -> {
                    com.bemo.hr.employee.domain.Employee e = inv.getArgument(0);
                    // Set ID via reflection for the test
                    try {
                        var f = e.getClass().getDeclaredField("id");
                        f.setAccessible(true);
                        f.set(e, "emp-new-id");
                    } catch (Exception ignored) {}
                    return e;
                });
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RecruitmentApi.ConvertResponse result = service.convertToEmployee("app1",
                new RecruitmentApi.ConvertToEmployeeRequest(null, "DEPT-1"));

        assertThat(result.employeeId()).isNotNull();
        verify(eventRepository).save(any());
    }
}
