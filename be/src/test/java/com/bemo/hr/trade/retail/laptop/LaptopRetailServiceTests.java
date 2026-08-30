package com.bemo.hr.trade.retail.laptop;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaptopRetailServiceTests {

    @Mock
    private SerializedDeviceRepository deviceRepository;

    @Mock
    private DeviceRepairTicketRepository repairTicketRepository;

    @Mock
    private AuditService auditService;

    private LaptopRetailService service;

    @BeforeEach
    void setUp() {
        service = new LaptopRetailService(deviceRepository, repairTicketRepository, auditService);
    }

    @Test
    void registerDevice_success_and_audited() {
        when(deviceRepository.existsBySerialNumber("SN-12345")).thenReturn(false);
        when(deviceRepository.save(any(SerializedDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        LaptopRetailApi.RegisterDeviceRequest request = new LaptopRetailApi.RegisterDeviceRequest(
                "SN-12345", "Lenovo", "ThinkPad T14", "i7-1355U", 16, 512, "NVMe SSD",
                new BigDecimal("28000"), new BigDecimal("34000"), "NEW", "supp-1", "Iris Xe", new BigDecimal("14.0")
        );

        SerializedDevice device = service.registerDevice("sales_rep", request);

        assertNotNull(device);
        assertEquals("SN-12345", device.getSerialNumber());
        assertEquals("Lenovo", device.getBrand());
        assertEquals("IN_STOCK", device.getStatus());
        assertEquals(new BigDecimal("6000"), device.getMargin());
        verify(auditService).record(eq("RETAIL_DEVICE_REGISTERED"), eq("SerializedDevice"), anyString(), eq("sales_rep"), anyString(), anyString());
    }

    @Test
    void registerDevice_duplicateSerial_throwsException() {
        when(deviceRepository.existsBySerialNumber("SN-12345")).thenReturn(true);

        LaptopRetailApi.RegisterDeviceRequest request = new LaptopRetailApi.RegisterDeviceRequest(
                "SN-12345", "Lenovo", "ThinkPad T14", "i7-1355U", 16, 512, "NVMe SSD",
                new BigDecimal("28000"), new BigDecimal("34000"), "NEW", "supp-1", null, null
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.registerDevice("sales_rep", request));

        assertEquals("RETAIL_DEVICE_SERIAL_DUPLICATE", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void sellDevice_inStock_success_withWarrantyCalculation() {
        SerializedDevice device = new SerializedDevice(
                "SN-12345", "Lenovo", "ThinkPad T14", "i7-1355U", 16, 512, "NVMe SSD",
                new BigDecimal("28000"), new BigDecimal("34000"), "NEW", "supp-1"
        );

        when(deviceRepository.findById("dev-1")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(SerializedDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        LaptopRetailApi.SellDeviceRequest request = new LaptopRetailApi.SellDeviceRequest(
                "cust-1", "Mohamed Ali", 24, new BigDecimal("33500")
        );

        SerializedDevice sold = service.sellDevice("pos_user", "dev-1", request);

        assertNotNull(sold);
        assertEquals("SOLD", sold.getStatus());
        assertEquals("Mohamed Ali", sold.getCustomerName());
        assertEquals(new BigDecimal("33500"), sold.getSellingPrice());
        assertNotNull(sold.getWarrantyEndDate());
        assertTrue(sold.isWarrantyActive());
        verify(auditService).record(eq("RETAIL_DEVICE_SOLD"), eq("SerializedDevice"), anyString(), eq("pos_user"), anyString(), anyString());
    }

    @Test
    void sellDevice_alreadySold_throwsException() {
        SerializedDevice device = new SerializedDevice(
                "SN-12345", "Lenovo", "ThinkPad T14", "i7-1355U", 16, 512, "NVMe SSD",
                new BigDecimal("28000"), new BigDecimal("34000"), "NEW", "supp-1"
        );
        device.sellToCustomer("c1", "Client A", 12, null);

        when(deviceRepository.findById("dev-1")).thenReturn(Optional.of(device));

        LaptopRetailApi.SellDeviceRequest request = new LaptopRetailApi.SellDeviceRequest(
                "cust-2", "Client B", 12, null
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.sellDevice("pos_user", "dev-1", request));

        assertEquals("RETAIL_DEVICE_NOT_IN_STOCK", ex.getCode());
    }

    @Test
    void createRepairTicket_underWarranty_detectedAutomatically() {
        SerializedDevice device = new SerializedDevice(
                "SN-12345", "Lenovo", "ThinkPad T14", "i7-1355U", 16, 512, "NVMe SSD",
                new BigDecimal("28000"), new BigDecimal("34000"), "NEW", "supp-1"
        );
        device.sellToCustomer("c1", "Client A", 24, null); // 24 months warranty

        when(deviceRepository.findBySerialNumber("SN-12345")).thenReturn(Optional.of(device));
        when(repairTicketRepository.save(any(DeviceRepairTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        LaptopRetailApi.CreateRepairTicketRequest request = new LaptopRetailApi.CreateRepairTicketRequest(
                "SN-12345", "Client A", "01000000000", "Screen flickering"
        );

        DeviceRepairTicket ticket = service.createRepairTicket("tech_lead", request);

        assertNotNull(ticket);
        assertTrue(ticket.isUnderWarranty());
        assertEquals("RECEIVED", ticket.getStatus());
        assertEquals("Screen flickering", ticket.getReportedIssue());
    }
}
