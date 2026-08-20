package com.bemo.hr.verticals;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.verticals.api.SpecializedVerticalsApi.*;
import com.bemo.hr.verticals.application.SpecializedVerticalsService;
import com.bemo.hr.verticals.domain.*;
import com.bemo.hr.verticals.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecializedVerticalsServiceTests {

    @Mock
    private StudentEnrollmentRepository studentRepo;
    @Mock
    private TuitionInvoiceRepository invoiceRepo;
    @Mock
    private TourismBookingRepository tourismRepo;
    @Mock
    private CustomsDeclarationRepository customsRepo;
    @Mock
    private ThreePlContractRepository threePlRepo;

    private SpecializedVerticalsService service;

    @BeforeEach
    void setUp() {
        service = new SpecializedVerticalsService(studentRepo, invoiceRepo, tourismRepo, customsRepo, threePlRepo);
    }

    @Test
    void registerStudent_calculatesTotalAndGeneratesInstallments() {
        when(studentRepo.findByStudentCode("STU-1001")).thenReturn(Optional.empty());
        when(studentRepo.save(any(StudentEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepo.save(any(TuitionInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterStudentPayload payload = new RegisterStudentPayload(
                "STU-1001",
                "Ahmed Mohamed",
                "Grade 5",
                "2026/2027",
                "Mohamed Ali",
                "01012345678",
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(3000),
                3
        );

        StudentEnrollmentResponse res = service.registerStudent(payload);

        assertThat(res.studentCode()).isEqualTo("STU-1001");
        assertThat(res.totalTuitionFee()).isEqualByComparingTo("30000");
        assertThat(res.transportFee()).isEqualByComparingTo("5000");
        assertThat(res.booksFee()).isEqualByComparingTo("3000");
        assertThat(res.totalAnnualDue()).isEqualByComparingTo("38000");
        assertThat(res.installmentInvoices()).hasSize(3);

        verify(invoiceRepo, times(3)).save(any(TuitionInvoice.class));
    }

    @Test
    void registerStudent_duplicateCodeThrowsException() {
        when(studentRepo.findByStudentCode("STU-DUPLICATE")).thenReturn(Optional.of(new StudentEnrollment()));

        RegisterStudentPayload payload = new RegisterStudentPayload(
                "STU-DUPLICATE",
                "Ahmed",
                "Grade 1",
                "2026/2027",
                "Father",
                null,
                BigDecimal.valueOf(10000),
                null,
                null,
                1
        );

        assertThatThrownBy(() -> service.registerStudent(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Student code already exists");
    }

    @Test
    void createTourismBooking_computesDirectCostAndMargin() {
        when(tourismRepo.findByBookingCode("TB-2026-001")).thenReturn(Optional.empty());
        when(tourismRepo.save(any(TourismBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingPayload payload = new CreateBookingPayload(
                "TB-2026-001",
                "Sara Group",
                "Sharm Luxury 5D/4N",
                "Sharm El Sheikh",
                1755600000000L,
                1756000000000L,
                4,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(4000)
        );

        TourismBookingResponse res = service.createTourismBooking(payload);

        assertThat(res.bookingCode()).isEqualTo("TB-2026-001");
        assertThat(res.sellingPrice()).isEqualByComparingTo("50000");
        assertThat(res.totalDirectCost()).isEqualByComparingTo("36000");
        assertThat(res.grossMargin()).isEqualByComparingTo("14000");
        assertThat(res.grossMarginPercentage()).isEqualByComparingTo("28.00");
    }

    @Test
    void openCustomsDeclaration_computesTotalInvoiceAmount() {
        when(customsRepo.findByFileNumber("CUST-2026-99")).thenReturn(Optional.empty());
        when(customsRepo.save(any(CustomsDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenDeclarationPayload payload = new OpenDeclarationPayload(
                "CUST-2026-99",
                "Cairo Import Export",
                "Alexandria Sea Port",
                "BL-MAERSK-987654",
                "CERT-46-12345",
                BigDecimal.valueOf(150000),
                BigDecimal.valueOf(18000),
                BigDecimal.valueOf(12000)
        );

        CustomsDeclarationResponse res = service.openCustomsDeclaration(payload);

        assertThat(res.fileNumber()).isEqualTo("CUST-2026-99");
        assertThat(res.dutyDisbursementAmount()).isEqualByComparingTo("150000");
        assertThat(res.portHandlingAmount()).isEqualByComparingTo("18000");
        assertThat(res.clearanceServiceFee()).isEqualByComparingTo("12000");
        assertThat(res.totalInvoiceAmount()).isEqualByComparingTo("180000");
    }

    @Test
    void create3plContract_calculatesEstimatedMonthlyRevenue() {
        when(threePlRepo.findByContractCode("3PL-2026-LOG")).thenReturn(Optional.empty());
        when(threePlRepo.save(any(ThreePlContract.class))).thenAnswer(inv -> inv.getArgument(0));

        Create3plContractPayload payload = new Create3plContractPayload(
                "3PL-2026-LOG",
                "Global Pharma",
                "6th October Central Cold Hub",
                500,
                BigDecimal.valueOf(350),
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(25),
                "MONTHLY"
        );

        ThreePlContractResponse res = service.create3plContract(payload);

        assertThat(res.contractCode()).isEqualTo("3PL-2026-LOG");
        assertThat(res.palletCapacity()).isEqualTo(500);
        assertThat(res.ratePerPalletMonthly()).isEqualByComparingTo("350");
        assertThat(res.estimatedMonthlyRevenue()).isEqualByComparingTo("175000");
    }

    @Test
    void getVerticalsSummary_aggregatesAcrossAllVerticals() {
        StudentEnrollment stu = new StudentEnrollment();
        stu.setTotalTuitionFee(BigDecimal.valueOf(20000));
        stu.setTransportFee(BigDecimal.valueOf(4000));
        stu.setBooksFee(BigDecimal.valueOf(2000));
        when(studentRepo.findAll()).thenReturn(List.of(stu));

        TourismBooking tour = new TourismBooking();
        tour.setSellingPrice(BigDecimal.valueOf(100000));
        tour.setGrossMargin(BigDecimal.valueOf(25000));
        when(tourismRepo.findAll()).thenReturn(List.of(tour));

        CustomsDeclaration cust = new CustomsDeclaration();
        cust.setDutyDisbursementAmount(BigDecimal.valueOf(80000));
        when(customsRepo.findAll()).thenReturn(List.of(cust));

        ThreePlContract contract = new ThreePlContract();
        contract.setPalletCapacity(300);
        when(threePlRepo.findAll()).thenReturn(List.of(contract));

        VerticalsSummaryResponse summary = service.getVerticalsSummary();

        assertThat(summary.totalActiveStudents()).isEqualTo(1);
        assertThat(summary.totalTuitionBilled()).isEqualByComparingTo("26000");
        assertThat(summary.totalActiveBookings()).isEqualTo(1);
        assertThat(summary.totalTourismRevenue()).isEqualByComparingTo("100000");
        assertThat(summary.averageTourismMarginPct()).isEqualByComparingTo("25.00");
        assertThat(summary.totalOpenCustomsFiles()).isEqualTo(1);
        assertThat(summary.totalDutyDisbursements()).isEqualByComparingTo("80000");
        assertThat(summary.totalActive3plContracts()).isEqualTo(1);
        assertThat(summary.total3plPalletCapacity()).isEqualTo(300);
    }
}
