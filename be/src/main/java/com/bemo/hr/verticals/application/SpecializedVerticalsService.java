package com.bemo.hr.verticals.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.verticals.api.SpecializedVerticalsApi.*;
import com.bemo.hr.verticals.domain.*;
import com.bemo.hr.verticals.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SpecializedVerticalsService {

    private final StudentEnrollmentRepository studentRepo;
    private final TuitionInvoiceRepository invoiceRepo;
    private final TourismBookingRepository tourismRepo;
    private final CustomsDeclarationRepository customsRepo;
    private final ThreePlContractRepository threePlRepo;

    public SpecializedVerticalsService(
            StudentEnrollmentRepository studentRepo,
            TuitionInvoiceRepository invoiceRepo,
            TourismBookingRepository tourismRepo,
            CustomsDeclarationRepository customsRepo,
            ThreePlContractRepository threePlRepo
    ) {
        this.studentRepo = studentRepo;
        this.invoiceRepo = invoiceRepo;
        this.tourismRepo = tourismRepo;
        this.customsRepo = customsRepo;
        this.threePlRepo = threePlRepo;
    }

    // --- School & Education Vertical ---

    public StudentEnrollmentResponse registerStudent(RegisterStudentPayload payload) {
        if (studentRepo.findByStudentCode(payload.studentCode()).isPresent()) {
            throw new BusinessRuleException("Student code already exists: " + payload.studentCode(), "DUPLICATE_STUDENT_CODE", org.springframework.http.HttpStatus.CONFLICT);
        }

        long now = Instant.now().toEpochMilli();
        BigDecimal tuition = payload.totalTuitionFee();
        BigDecimal transport = payload.transportFee() != null ? payload.transportFee() : BigDecimal.ZERO;
        BigDecimal books = payload.booksFee() != null ? payload.booksFee() : BigDecimal.ZERO;
        BigDecimal totalAnnual = tuition.add(transport).add(books);

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(UUID.randomUUID().toString());
        enrollment.setStudentCode(payload.studentCode().trim());
        enrollment.setStudentName(payload.studentName().trim());
        enrollment.setGradeLevel(payload.gradeLevel().trim());
        enrollment.setAcademicYear(payload.academicYear().trim());
        enrollment.setGuardianName(payload.guardianName().trim());
        enrollment.setGuardianPhone(payload.guardianPhone() != null ? payload.guardianPhone().trim() : null);
        enrollment.setTotalTuitionFee(tuition);
        enrollment.setTransportFee(transport);
        enrollment.setBooksFee(books);
        enrollment.setStatus("ACTIVE");
        enrollment.setCreatedAt(now);
        enrollment.setUpdatedAt(now);

        StudentEnrollment saved = studentRepo.save(enrollment);

        // Generate Installment Invoices (default 3 installments: 40%, 30%, 30%)
        int count = payload.installmentsCount() != null && payload.installmentsCount() > 0 ? payload.installmentsCount() : 3;
        List<TuitionInvoiceResponse> invoices = new ArrayList<>();

        if (count == 1) {
            TuitionInvoice inv = createInvoice(saved.getId(), saved.getStudentCode() + "-INV-01", "Full Term Annual", now + 86400000L * 15, totalAnnual, now);
            invoices.add(toInvoiceResponse(inv));
        } else {
            BigDecimal part = totalAnnual.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            BigDecimal running = BigDecimal.ZERO;
            for (int i = 1; i <= count; i++) {
                BigDecimal amt = (i == count) ? totalAnnual.subtract(running) : part;
                running = running.add(amt);
                long dueDate = Instant.ofEpochMilli(now).plus(30L * (i - 1), ChronoUnit.DAYS).toEpochMilli();
                TuitionInvoice inv = createInvoice(saved.getId(), saved.getStudentCode() + "-INV-0" + i, "Installment #" + i, dueDate, amt, now);
                invoices.add(toInvoiceResponse(inv));
            }
        }

        return toStudentResponse(saved, invoices);
    }

    private TuitionInvoice createInvoice(String enrollmentId, String invNumber, String name, long dueDate, BigDecimal amount, long now) {
        TuitionInvoice inv = new TuitionInvoice();
        inv.setId(UUID.randomUUID().toString());
        inv.setEnrollmentId(enrollmentId);
        inv.setInvoiceNumber(invNumber);
        inv.setInstallmentName(name);
        inv.setDueDate(dueDate);
        inv.setAmountDue(amount);
        inv.setAmountPaid(BigDecimal.ZERO);
        inv.setStatus("PENDING");
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        return invoiceRepo.save(inv);
    }

    @Transactional(readOnly = true)
    public List<StudentEnrollmentResponse> listStudents() {
        return studentRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> {
                    var invs = invoiceRepo.findByEnrollmentIdOrderByDueDateAsc(s.getId()).stream()
                            .map(this::toInvoiceResponse)
                            .toList();
                    return toStudentResponse(s, invs);
                })
                .toList();
    }

    // --- Tourism & Travel Vertical ---

    public TourismBookingResponse createTourismBooking(CreateBookingPayload payload) {
        if (tourismRepo.findByBookingCode(payload.bookingCode()).isPresent()) {
            throw new BusinessRuleException("Booking code already exists: " + payload.bookingCode(), "DUPLICATE_BOOKING_CODE", org.springframework.http.HttpStatus.CONFLICT);
        }

        long now = Instant.now().toEpochMilli();
        BigDecimal selling = payload.sellingPrice();
        BigDecimal hotel = payload.hotelCost() != null ? payload.hotelCost() : BigDecimal.ZERO;
        BigDecimal flight = payload.flightCost() != null ? payload.flightCost() : BigDecimal.ZERO;
        BigDecimal excursion = payload.excursionCost() != null ? payload.excursionCost() : BigDecimal.ZERO;
        BigDecimal totalCost = hotel.add(flight).add(excursion);
        BigDecimal margin = selling.subtract(totalCost);

        TourismBooking booking = new TourismBooking();
        booking.setId(UUID.randomUUID().toString());
        booking.setBookingCode(payload.bookingCode().trim());
        booking.setCustomerName(payload.customerName().trim());
        booking.setPackageName(payload.packageName().trim());
        booking.setDestination(payload.destination().trim());
        booking.setTravelDate(payload.travelDate());
        booking.setReturnDate(payload.returnDate());
        booking.setTravelersCount(payload.travelersCount());
        booking.setSellingPrice(selling);
        booking.setHotelCost(hotel);
        booking.setFlightCost(flight);
        booking.setExcursionCost(excursion);
        booking.setGrossMargin(margin);
        booking.setStatus("CONFIRMED");
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        return toBookingResponse(tourismRepo.save(booking));
    }

    @Transactional(readOnly = true)
    public List<TourismBookingResponse> listTourismBookings() {
        return tourismRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toBookingResponse)
                .toList();
    }

    // --- Customs Clearance Vertical ---

    public CustomsDeclarationResponse openCustomsDeclaration(OpenDeclarationPayload payload) {
        if (customsRepo.findByFileNumber(payload.fileNumber()).isPresent()) {
            throw new BusinessRuleException("Customs file number already exists: " + payload.fileNumber(), "DUPLICATE_CUSTOMS_FILE", org.springframework.http.HttpStatus.CONFLICT);
        }

        long now = Instant.now().toEpochMilli();
        BigDecimal duty = payload.dutyDisbursementAmount() != null ? payload.dutyDisbursementAmount() : BigDecimal.ZERO;
        BigDecimal port = payload.portHandlingAmount() != null ? payload.portHandlingAmount() : BigDecimal.ZERO;
        BigDecimal serviceFee = payload.clearanceServiceFee();
        BigDecimal totalInvoice = duty.add(port).add(serviceFee);

        CustomsDeclaration decl = new CustomsDeclaration();
        decl.setId(UUID.randomUUID().toString());
        decl.setFileNumber(payload.fileNumber().trim());
        decl.setImporterName(payload.importerName().trim());
        decl.setPortOfEntry(payload.portOfEntry().trim());
        decl.setBillOfLadingNumber(payload.billOfLadingNumber().trim());
        decl.setCustomsCertificateNumber(payload.customsCertificateNumber() != null ? payload.customsCertificateNumber().trim() : null);
        decl.setDutyDisbursementAmount(duty);
        decl.setPortHandlingAmount(port);
        decl.setClearanceServiceFee(serviceFee);
        decl.setTotalInvoiceAmount(totalInvoice);
        decl.setStatus("IN_INSPECTION");
        decl.setCreatedAt(now);
        decl.setUpdatedAt(now);

        return toCustomsResponse(customsRepo.save(decl));
    }

    @Transactional(readOnly = true)
    public List<CustomsDeclarationResponse> listCustomsDeclarations() {
        return customsRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCustomsResponse)
                .toList();
    }

    // --- 3PL Logistics Vertical ---

    public ThreePlContractResponse create3plContract(Create3plContractPayload payload) {
        if (threePlRepo.findByContractCode(payload.contractCode()).isPresent()) {
            throw new BusinessRuleException("Contract code already exists: " + payload.contractCode(), "DUPLICATE_CONTRACT_CODE", org.springframework.http.HttpStatus.CONFLICT);
        }

        long now = Instant.now().toEpochMilli();
        BigDecimal rate = payload.ratePerPalletMonthly();
        int pallets = payload.palletCapacity();
        BigDecimal inRate = payload.handlingInRatePerPallet() != null ? payload.handlingInRatePerPallet() : BigDecimal.ZERO;
        BigDecimal outRate = payload.handlingOutRatePerPallet() != null ? payload.handlingOutRatePerPallet() : BigDecimal.ZERO;

        ThreePlContract contract = new ThreePlContract();
        contract.setId(UUID.randomUUID().toString());
        contract.setContractCode(payload.contractCode().trim());
        contract.setClientName(payload.clientName().trim());
        contract.setWarehouseName(payload.warehouseName().trim());
        contract.setPalletCapacity(pallets);
        contract.setRatePerPalletMonthly(rate);
        contract.setHandlingInRatePerPallet(inRate);
        contract.setHandlingOutRatePerPallet(outRate);
        contract.setBillingFrequency(payload.billingFrequency() != null ? payload.billingFrequency() : "MONTHLY");
        contract.setStatus("ACTIVE");
        contract.setCreatedAt(now);
        contract.setUpdatedAt(now);

        return to3plResponse(threePlRepo.save(contract));
    }

    @Transactional(readOnly = true)
    public List<ThreePlContractResponse> list3plContracts() {
        return threePlRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::to3plResponse)
                .toList();
    }

    // --- Cross-Vertical Overview Summary ---

    @Transactional(readOnly = true)
    public VerticalsSummaryResponse getVerticalsSummary() {
        List<StudentEnrollment> students = studentRepo.findAll();
        BigDecimal totalTuition = students.stream()
                .map(s -> s.getTotalTuitionFee().add(s.getTransportFee()).add(s.getBooksFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TourismBooking> bookings = tourismRepo.findAll();
        BigDecimal totalTourism = bookings.stream().map(TourismBooking::getSellingPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMargin = bookings.stream().map(TourismBooking::getGrossMargin).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgMarginPct = totalTourism.compareTo(BigDecimal.ZERO) > 0
                ? totalMargin.divide(totalTourism, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<CustomsDeclaration> customs = customsRepo.findAll();
        BigDecimal totalDuties = customs.stream().map(CustomsDeclaration::getDutyDisbursementAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ThreePlContract> contracts = threePlRepo.findAll();
        int totalPallets = contracts.stream().mapToInt(ThreePlContract::getPalletCapacity).sum();

        return new VerticalsSummaryResponse(
                students.size(),
                totalTuition,
                bookings.size(),
                totalTourism,
                avgMarginPct,
                customs.size(),
                totalDuties,
                contracts.size(),
                totalPallets
        );
    }

    // --- Mapping Helpers ---

    private StudentEnrollmentResponse toStudentResponse(StudentEnrollment s, List<TuitionInvoiceResponse> invoices) {
        BigDecimal totalAnnual = s.getTotalTuitionFee().add(s.getTransportFee()).add(s.getBooksFee());
        return new StudentEnrollmentResponse(
                s.getId(),
                s.getStudentCode(),
                s.getStudentName(),
                s.getGradeLevel(),
                s.getAcademicYear(),
                s.getGuardianName(),
                s.getGuardianPhone(),
                s.getTotalTuitionFee(),
                s.getTransportFee(),
                s.getBooksFee(),
                totalAnnual,
                s.getStatus(),
                s.getCreatedAt(),
                invoices
        );
    }

    private TuitionInvoiceResponse toInvoiceResponse(TuitionInvoice inv) {
        return new TuitionInvoiceResponse(
                inv.getId(),
                inv.getEnrollmentId(),
                inv.getInvoiceNumber(),
                inv.getInstallmentName(),
                inv.getDueDate(),
                inv.getAmountDue(),
                inv.getAmountPaid(),
                inv.getStatus(),
                inv.getCreatedAt()
        );
    }

    private TourismBookingResponse toBookingResponse(TourismBooking b) {
        BigDecimal directCost = b.getHotelCost().add(b.getFlightCost()).add(b.getExcursionCost());
        BigDecimal marginPct = b.getSellingPrice().compareTo(BigDecimal.ZERO) > 0
                ? b.getGrossMargin().divide(b.getSellingPrice(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TourismBookingResponse(
                b.getId(),
                b.getBookingCode(),
                b.getCustomerName(),
                b.getPackageName(),
                b.getDestination(),
                b.getTravelDate(),
                b.getReturnDate(),
                b.getTravelersCount(),
                b.getSellingPrice(),
                b.getHotelCost(),
                b.getFlightCost(),
                b.getExcursionCost(),
                directCost,
                b.getGrossMargin(),
                marginPct,
                b.getStatus(),
                b.getCreatedAt()
        );
    }

    private CustomsDeclarationResponse toCustomsResponse(CustomsDeclaration c) {
        return new CustomsDeclarationResponse(
                c.getId(),
                c.getFileNumber(),
                c.getImporterName(),
                c.getPortOfEntry(),
                c.getBillOfLadingNumber(),
                c.getCustomsCertificateNumber(),
                c.getDutyDisbursementAmount(),
                c.getPortHandlingAmount(),
                c.getClearanceServiceFee(),
                c.getTotalInvoiceAmount(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }

    private ThreePlContractResponse to3plResponse(ThreePlContract c) {
        BigDecimal estMonthly = c.getRatePerPalletMonthly().multiply(BigDecimal.valueOf(c.getPalletCapacity()));
        return new ThreePlContractResponse(
                c.getId(),
                c.getContractCode(),
                c.getClientName(),
                c.getWarehouseName(),
                c.getPalletCapacity(),
                c.getRatePerPalletMonthly(),
                c.getHandlingInRatePerPallet(),
                c.getHandlingOutRatePerPallet(),
                estMonthly,
                c.getBillingFrequency(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }
}
