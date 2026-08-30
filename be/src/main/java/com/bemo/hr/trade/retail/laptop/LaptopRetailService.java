package com.bemo.hr.trade.retail.laptop;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LaptopRetailService {

    private final SerializedDeviceRepository deviceRepository;
    private final DeviceRepairTicketRepository repairTicketRepository;
    private final AuditService auditService;

    public LaptopRetailService(SerializedDeviceRepository deviceRepository,
                               DeviceRepairTicketRepository repairTicketRepository,
                               AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.repairTicketRepository = repairTicketRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SerializedDevice registerDevice(String username, LaptopRetailApi.RegisterDeviceRequest request) {
        if (deviceRepository.existsBySerialNumber(request.serialNumber().strip())) {
            throw new BusinessRuleException("Device serial number already exists", "RETAIL_DEVICE_SERIAL_DUPLICATE", HttpStatus.CONFLICT);
        }

        SerializedDevice device = new SerializedDevice(
                request.serialNumber(),
                request.brand(),
                request.model(),
                request.cpu(),
                request.ramGb(),
                request.storageGb(),
                request.storageType(),
                request.purchasePrice(),
                request.sellingPrice(),
                request.conditionGrade(),
                request.supplierId()
        );

        if (request.gpu() != null) {
            device.setGpu(request.gpu());
        }

        SerializedDevice saved = deviceRepository.save(device);
        auditService.record("RETAIL_DEVICE_REGISTERED", "SerializedDevice", saved.getId(), username,
                String.format("{\"serial\":\"%s\",\"model\":\"%s\"}", saved.getSerialNumber(), saved.getModel()), "0.0.0.0");

        return saved;
    }

    @Transactional
    public SerializedDevice sellDevice(String username, String deviceId, LaptopRetailApi.SellDeviceRequest request) {
        SerializedDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessRuleException("Device not found", "RETAIL_DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!device.isInStock()) {
            throw new BusinessRuleException("Device is already sold or not in stock", "RETAIL_DEVICE_NOT_IN_STOCK", HttpStatus.CONFLICT);
        }

        device.sellToCustomer(request.customerId(), request.customerName(),
                request.warrantyMonths() > 0 ? request.warrantyMonths() : 12, request.finalSellingPrice());

        SerializedDevice saved = deviceRepository.save(device);
        auditService.record("RETAIL_DEVICE_SOLD", "SerializedDevice", saved.getId(), username,
                String.format("{\"serial\":\"%s\",\"customer\":\"%s\",\"price\":%s}",
                        saved.getSerialNumber(), saved.getCustomerName(), saved.getSellingPrice()), "0.0.0.0");

        return saved;
    }

    @Transactional
    public SerializedDevice returnDevice(String username, String deviceId, String reason) {
        SerializedDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessRuleException("Device not found", "RETAIL_DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        device.processReturn(reason);
        SerializedDevice saved = deviceRepository.save(device);

        auditService.record("RETAIL_DEVICE_RETURNED", "SerializedDevice", saved.getId(), username,
                String.format("{\"serial\":\"%s\",\"reason\":\"%s\"}", saved.getSerialNumber(), reason), "0.0.0.0");

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SerializedDevice> listDevices(String status, String brand) {
        if (status != null && !status.isBlank()) {
            return deviceRepository.findByStatusOrderByCreatedAtDesc(status.strip().toUpperCase());
        }
        if (brand != null && !brand.isBlank()) {
            return deviceRepository.findByBrandIgnoreCaseOrderByCreatedAtDesc(brand.strip());
        }
        return deviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SerializedDevice getDeviceBySerial(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber.strip())
                .orElseThrow(() -> new BusinessRuleException("Device not found", "RETAIL_DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public DeviceRepairTicket createRepairTicket(String username, LaptopRetailApi.CreateRepairTicketRequest request) {
        String serial = request.serialNumber().strip();
        SerializedDevice device = deviceRepository.findBySerialNumber(serial).orElse(null);
        boolean underWarranty = device != null && device.isWarrantyActive();

        String ticketNumber = "RPR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DeviceRepairTicket ticket = new DeviceRepairTicket(
                ticketNumber,
                device != null ? device.getId() : null,
                serial,
                request.customerName(),
                request.customerPhone(),
                request.reportedIssue(),
                underWarranty
        );

        DeviceRepairTicket saved = repairTicketRepository.save(ticket);
        auditService.record("REPAIR_TICKET_CREATED", "DeviceRepairTicket", saved.getId(), username,
                String.format("{\"ticketNumber\":\"%s\",\"serial\":\"%s\"}", ticketNumber, serial), "0.0.0.0");

        return saved;
    }

    @Transactional
    public DeviceRepairTicket updateRepairStatus(String username, String ticketId, LaptopRetailApi.UpdateRepairStatusRequest request) {
        DeviceRepairTicket ticket = repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessRuleException("Repair ticket not found", "RETAIL_REPAIR_TICKET_NOT_FOUND", HttpStatus.NOT_FOUND));

        ticket.updateDiagnosis(request.diagnosis(), request.technicianNotes(), request.costAmount(), request.chargedAmount(), request.status());
        DeviceRepairTicket saved = repairTicketRepository.save(ticket);

        auditService.record("REPAIR_TICKET_UPDATED", "DeviceRepairTicket", saved.getId(), username,
                String.format("{\"ticketNumber\":\"%s\",\"status\":\"%s\"}", saved.getTicketNumber(), saved.getStatus()), "0.0.0.0");

        return saved;
    }

    @Transactional(readOnly = true)
    public List<DeviceRepairTicket> listRepairTickets(String status, String serialNumber) {
        if (serialNumber != null && !serialNumber.isBlank()) {
            return repairTicketRepository.findBySerialNumberOrderByCreatedAtDesc(serialNumber.strip());
        }
        if (status != null && !status.isBlank()) {
            return repairTicketRepository.findByStatusOrderByCreatedAtDesc(status.strip().toUpperCase());
        }
        return repairTicketRepository.findAll();
    }
}
