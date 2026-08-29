package com.bemo.hr.medical;

import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.MedicalLabService;
import com.bemo.hr.medical.domain.LabOrder;
import com.bemo.hr.medical.domain.LabTestItem;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.LabOrderRepository;
import com.bemo.hr.medical.infrastructure.LabTestItemRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalLabServiceTests {

    @Mock
    private LabTestItemRepository testRepository;
    @Mock
    private LabOrderRepository orderRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private MedicalLabService labService;

    private final String APP_ID = "tenant-medical";
    private final String PATIENT_ID = "pat-1";
    private final String DOCTOR_ID = "doc-1";
    private final String TEST_ID = "test-1";
    private final String ORDER_ID = "order-1";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        labService = new MedicalLabService(testRepository, orderRepository, patientRepository, employeeRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createLabOrder_setsOrderedStatus() {
        Patient patient = new Patient("MRN-0001", "29501011234567", "Ahmed Ali", "01012345678", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        when(patientRepository.findByAppIdAndId(APP_ID, PATIENT_ID)).thenReturn(Optional.of(patient));

        LabTestItem testItem = new LabTestItem("CBC", LabTestItem.Category.LAB, "Complete Blood Count", "BLOOD", "Normal", BigDecimal.valueOf(150));
        when(testRepository.findByAppIdAndId(APP_ID, TEST_ID)).thenReturn(Optional.of(testItem));

        when(orderRepository.save(any(LabOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLabOrderRequest request = new CreateLabOrderRequest(PATIENT_ID, "visit-1", DOCTOR_ID, TEST_ID, "lab-ext-1", "Al-Borg Lab");
        LabOrderDto result = labService.createLabOrder(request);

        assertNotNull(result);
        assertEquals("ORDERED", result.status());
        assertEquals("CBC", result.testCode());
        assertEquals("Complete Blood Count", result.testName());
    }

    @Test
    void cancelOrder_onlyAllowedWhenOrdered_throwsIfResulted() {
        LabOrder order = new LabOrder(PATIENT_ID, "vis-1", DOCTOR_ID, TEST_ID, LabTestItem.Category.LAB, "CBC", "Complete Blood Count", null, null);
        order.enterResult("14.5 g/dL", LabOrder.ResultFlag.NORMAL, "Within range", null, null);

        when(orderRepository.findByAppIdAndId(APP_ID, ORDER_ID)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> labService.cancelOrder(ORDER_ID));
        assertEquals("LAB_CANNOT_CANCEL_NON_ORDERED", ex.getCode());
    }

    @Test
    void validateOrder_requiresResultedStatus() {
        LabOrder order = new LabOrder(PATIENT_ID, "vis-1", DOCTOR_ID, TEST_ID, LabTestItem.Category.LAB, "CBC", "Complete Blood Count", null, null);
        // Order is still in ORDERED status
        when(orderRepository.findByAppIdAndId(APP_ID, ORDER_ID)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> labService.validateOrder(ORDER_ID));
        assertEquals("LAB_VALIDATION_REQUIRES_RESULT", ex.getCode());
    }

    @Test
    void enterResult_criticalFlag_and_acknowledgeCritical() {
        LabOrder order = new LabOrder(PATIENT_ID, "vis-1", DOCTOR_ID, TEST_ID, LabTestItem.Category.LAB, "K+", "Potassium", null, null);
        order.setId(ORDER_ID);

        when(orderRepository.findByAppIdAndId(APP_ID, ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        EnterLabResultRequest req = new EnterLabResultRequest("6.8 mmol/L", "CRITICAL", "Severe hyperkalemia", null, null);
        LabOrderDto resulted = labService.enterResult(ORDER_ID, req);

        assertEquals("RESULTED", resulted.status());
        assertEquals("CRITICAL", resulted.resultFlag());
        assertFalse(resulted.isCriticalAcknowledged());

        // Acknowledge critical value
        LabOrderDto acked = labService.acknowledgeCritical(ORDER_ID);
        assertTrue(acked.isCriticalAcknowledged());
        assertNotNull(acked.criticalAcknowledgedAt());
    }
}
