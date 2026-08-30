package com.bemo.hr.serviceops.application;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.domain.RentalContract;
import com.bemo.hr.serviceops.domain.RentalContractLine;
import com.bemo.hr.serviceops.domain.RentalItem;
import com.bemo.hr.serviceops.infrastructure.RentalContractLineRepository;
import com.bemo.hr.serviceops.infrastructure.RentalContractRepository;
import com.bemo.hr.serviceops.infrastructure.RentalItemRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RentalService {

    private final RentalItemRepository itemRepository;
    private final RentalContractRepository contractRepository;
    private final RentalContractLineRepository lineRepository;

    public RentalService(RentalItemRepository itemRepository,
                         RentalContractRepository contractRepository,
                         RentalContractLineRepository lineRepository) {
        this.itemRepository = itemRepository;
        this.contractRepository = contractRepository;
        this.lineRepository = lineRepository;
    }

    // --- Rental Items ---

    @Transactional
    public ServiceOpsApi.RentalItemResponse createItem(ServiceOpsApi.RentalItemCreateRequest request) {
        String appId = TenantContext.require();
        RentalItem item = new RentalItem(
                appId,
                request.code(),
                request.name(),
                request.nameEn(),
                request.category(),
                request.rateDaily(),
                request.rateWeekly(),
                request.rateMonthly(),
                request.depositAmount()
        );
        RentalItem saved = itemRepository.save(item);
        log.info("Created rental item {} for app {}", saved.getCode(), appId);
        return toItemResponse(saved);
    }

    public List<ServiceOpsApi.RentalItemResponse> listItems() {
        String appId = TenantContext.require();
        return itemRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    public ServiceOpsApi.RentalItemResponse getItem(String id) {
        String appId = TenantContext.require();
        RentalItem item = itemRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("RENTAL_ITEM_NOT_FOUND", "RENTAL_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toItemResponse(item);
    }

    // --- Rental Contracts ---

    @Transactional
    public ServiceOpsApi.RentalContractResponse createContract(ServiceOpsApi.RentalContractCreateRequest request) {
        String appId = TenantContext.require();
        RentalContract contract = new RentalContract(
                appId,
                request.contractNo(),
                request.customerPartyId(),
                request.startDate(),
                request.expectedEndDate(),
                request.rateUnit(),
                request.rateAmount(),
                request.depositAmount(),
                request.notes()
        );

        if (request.lines() != null) {
            for (ServiceOpsApi.RentalContractLineRequest lineReq : request.lines()) {
                RentalItem item = itemRepository.findByAppIdAndId(appId, lineReq.rentalItemId())
                        .orElseThrow(() -> new BusinessRuleException("RENTAL_ITEM_NOT_FOUND", "RENTAL_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND));
                if (item.getStatus() != RentalItem.Status.AVAILABLE) {
                    throw new BusinessRuleException("RENTAL_ITEM_NOT_AVAILABLE", "RENTAL_ITEM_NOT_AVAILABLE", HttpStatus.BAD_REQUEST);
                }
                RentalContractLine line = new RentalContractLine(
                        appId,
                        item.getId(),
                        lineReq.quantity(),
                        lineReq.unitRate() != null ? lineReq.unitRate() : contract.getRateAmount()
                );
                contract.addLine(line);
            }
        }

        RentalContract saved = contractRepository.save(contract);
        log.info("Created rental contract {} for customer {}", saved.getContractNo(), saved.getCustomerPartyId());
        return toContractResponse(saved);
    }

    public List<ServiceOpsApi.RentalContractResponse> listContracts() {
        String appId = TenantContext.require();
        return contractRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toContractResponse)
                .collect(Collectors.toList());
    }

    public ServiceOpsApi.RentalContractResponse getContract(String id) {
        String appId = TenantContext.require();
        RentalContract contract = contractRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("RENTAL_CONTRACT_NOT_FOUND", "RENTAL_CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toContractResponse(contract);
    }

    @Transactional
    public ServiceOpsApi.RentalContractResponse activateContract(String id) {
        String appId = TenantContext.require();
        RentalContract contract = contractRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("RENTAL_CONTRACT_NOT_FOUND", "RENTAL_CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (contract.getStatus() != RentalContract.Status.DRAFT) {
            throw new BusinessRuleException("RENTAL_CONTRACT_INVALID_STATE", "RENTAL_CONTRACT_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }

        contract.setStatus(RentalContract.Status.ACTIVE);
        for (RentalContractLine line : contract.getLines()) {
            itemRepository.findByAppIdAndId(appId, line.getRentalItemId()).ifPresent(item -> {
                item.setStatus(RentalItem.Status.RENTED);
                itemRepository.save(item);
            });
        }

        RentalContract saved = contractRepository.save(contract);
        log.info("Activated rental contract {}", saved.getContractNo());
        return toContractResponse(saved);
    }

    @Transactional
    public ServiceOpsApi.RentalContractResponse returnAndCloseContract(String id, ServiceOpsApi.ReturnRentalContractRequest request) {
        String appId = TenantContext.require();
        RentalContract contract = contractRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("RENTAL_CONTRACT_NOT_FOUND", "RENTAL_CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (contract.getStatus() != RentalContract.Status.ACTIVE) {
            throw new BusinessRuleException("RENTAL_CONTRACT_INVALID_STATE", "RENTAL_CONTRACT_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }

        String endDate = (request != null && request.actualEndDate() != null && !request.actualEndDate().isBlank())
                ? request.actualEndDate()
                : LocalDate.now().toString();

        contract.setActualEndDate(endDate);
        BigDecimal damageFee = (request != null && request.damageFee() != null) ? request.damageFee() : BigDecimal.ZERO;
        contract.setDamageFee(damageFee);

        // Calculate period charges
        BigDecimal calculatedCharges = calculateContractCharges(contract, endDate);
        BigDecimal total = calculatedCharges.add(damageFee);
        contract.setTotalAmount(total);
        contract.setStatus(RentalContract.Status.CLOSED);

        // Release rented items back to AVAILABLE
        for (RentalContractLine line : contract.getLines()) {
            itemRepository.findByAppIdAndId(appId, line.getRentalItemId()).ifPresent(item -> {
                item.setStatus(RentalItem.Status.AVAILABLE);
                itemRepository.save(item);
            });
        }

        RentalContract saved = contractRepository.save(contract);
        log.info("Closed rental contract {} with total charges {}", saved.getContractNo(), total);
        return toContractResponse(saved);
    }

    @Transactional
    public ServiceOpsApi.RentalContractResponse cancelContract(String id) {
        String appId = TenantContext.require();
        RentalContract contract = contractRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("RENTAL_CONTRACT_NOT_FOUND", "RENTAL_CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (contract.getStatus() == RentalContract.Status.CLOSED) {
            throw new BusinessRuleException("RENTAL_CONTRACT_INVALID_STATE", "RENTAL_CONTRACT_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }



        contract.setStatus(RentalContract.Status.CANCELLED);
        for (RentalContractLine line : contract.getLines()) {
            itemRepository.findByAppIdAndId(appId, line.getRentalItemId()).ifPresent(item -> {
                item.setStatus(RentalItem.Status.AVAILABLE);
                itemRepository.save(item);
            });
        }

        RentalContract saved = contractRepository.save(contract);
        return toContractResponse(saved);
    }

    public ServiceOpsApi.RentalUtilizationSummary getUtilizationSummary() {
        String appId = TenantContext.require();
        long total = itemRepository.countByAppId(appId);
        long rented = itemRepository.countByAppIdAndStatus(appId, RentalItem.Status.RENTED);
        long available = itemRepository.countByAppIdAndStatus(appId, RentalItem.Status.AVAILABLE);
        double utilization = total > 0 ? ((double) rented / total) * 100.0 : 0.0;
        return new ServiceOpsApi.RentalUtilizationSummary(total, rented, available, Math.round(utilization * 100.0) / 100.0);
    }

    // --- Calculation Helper ---

    public BigDecimal calculateContractCharges(RentalContract contract, String actualEndDate) {
        try {
            LocalDate start = LocalDate.parse(contract.getStartDate());
            LocalDate end = LocalDate.parse(actualEndDate);
            long days = Math.max(1, ChronoUnit.DAYS.between(start, end));

            BigDecimal rate = contract.getRateAmount() != null ? contract.getRateAmount() : BigDecimal.ZERO;
            RentalContract.RateUnit unit = contract.getRateUnit() != null ? contract.getRateUnit() : RentalContract.RateUnit.DAY;

            return switch (unit) {
                case DAY -> rate.multiply(BigDecimal.valueOf(days));
                case WEEK -> {
                    long fullWeeks = days / 7;
                    long remainingDays = days % 7;
                    BigDecimal weeklyCost = rate.multiply(BigDecimal.valueOf(fullWeeks));
                    BigDecimal dailyFraction = rate.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
                    yield weeklyCost.add(dailyFraction.multiply(BigDecimal.valueOf(remainingDays)));
                }
                case MONTH -> {
                    long fullMonths = days / 30;
                    long remainingDays = days % 30;
                    BigDecimal monthlyCost = rate.multiply(BigDecimal.valueOf(fullMonths));
                    BigDecimal dailyFraction = rate.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                    yield monthlyCost.add(dailyFraction.multiply(BigDecimal.valueOf(remainingDays)));
                }
            };
        } catch (Exception e) {
            log.warn("Failed to parse dates for rental charge calculation: {}", e.getMessage());
            return contract.getRateAmount() != null ? contract.getRateAmount() : BigDecimal.ZERO;
        }
    }

    // --- Mappers ---

    private ServiceOpsApi.RentalItemResponse toItemResponse(RentalItem item) {
        return new ServiceOpsApi.RentalItemResponse(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getNameEn(),
                item.getCategory(),
                item.getRateDaily(),
                item.getRateWeekly(),
                item.getRateMonthly(),
                item.getDepositAmount(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private ServiceOpsApi.RentalContractResponse toContractResponse(RentalContract contract) {
        List<ServiceOpsApi.RentalContractLineResponse> lineResponses = contract.getLines().stream()
                .map(l -> new ServiceOpsApi.RentalContractLineResponse(
                        l.getId(),
                        l.getRentalItemId(),
                        l.getQuantity(),
                        l.getUnitRate(),
                        l.getTotalAmount()
                ))
                .collect(Collectors.toList());

        return new ServiceOpsApi.RentalContractResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getCustomerPartyId(),
                contract.getStartDate(),
                contract.getExpectedEndDate(),
                contract.getActualEndDate(),
                contract.getRateUnit(),
                contract.getRateAmount(),
                contract.getDepositAmount(),
                contract.getDamageFee(),
                contract.getTotalAmount(),
                contract.getStatus(),
                contract.getInvoiceId(),
                contract.getNotes(),
                lineResponses,
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
}
