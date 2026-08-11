package com.bemo.hr.shared.security;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EntitlementManagementServiceTests {
    @Mock TenantFeatureRepository repository;@Mock TenantFeatureService featureService;@Mock AuditService auditService;
    EntitlementCatalog catalog=new EntitlementCatalog();EntitlementManagementService service;
    @BeforeEach void setup(){service=new EntitlementManagementService(catalog,repository,featureService,auditService);}
    @Test void catalogGroupsCanonicalFeaturesAndEffectiveValues(){when(repository.findByAppId("app")).thenReturn(List.of());when(featureService.isEnabledForTenant(eq("app"),anyString())).thenReturn(true);
        var modules=service.catalog("app");assertThat(modules).extracting(EntitlementApi.ModuleResponse::key).contains("FINANCE","SALES","INVENTORY");assertThat(modules).flatExtracting(EntitlementApi.ModuleResponse::features).allMatch(EntitlementApi.FeatureResponse::enabled);}
    @Test void enablingFeatureRequiresDependencies(){TenantFeature row=new TenantFeature("app","manufacturing.enabled",false,null,"admin");when(repository.findById(any())).thenReturn(Optional.of(row));when(featureService.isEnabledForTenant("app","inventory.advanced.enabled")).thenReturn(false);
        assertThatThrownBy(()->service.update("app","manufacturing.enabled",new EntitlementApi.UpdateRequest(true,null,"Purchased manufacturing",0),"admin")).isInstanceOfSatisfying(BusinessRuleException.class,e->assertThat(e.getCode()).isEqualTo("ENTITLEMENT_DEPENDENCY_MISSING"));}
    @Test void disablingFeatureRejectsActiveDependents(){TenantFeature row=new TenantFeature("app","inventory.advanced.enabled",true,null,"admin");when(repository.findById(any())).thenReturn(Optional.of(row));when(featureService.isEnabledForTenant("app","manufacturing.enabled")).thenReturn(true);
        assertThatThrownBy(()->service.update("app","inventory.advanced.enabled",new EntitlementApi.UpdateRequest(false,null,"Downgrade",0),"admin")).isInstanceOfSatisfying(BusinessRuleException.class,e->assertThat(e.getCode()).isEqualTo("ENTITLEMENT_DEPENDENT_ACTIVE"));}
    @Test void changePersistsReasonAndImmutableAudit(){TenantFeature row=new TenantFeature("app","sales.enabled",true,null,"admin");when(repository.findById(any())).thenReturn(Optional.of(row));when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        var result=service.update("app","sales.enabled",new EntitlementApi.UpdateRequest(false,"{}","Customer downgrade",0),"admin");assertThat(result.enabled()).isFalse();assertThat(row.getChangeReason()).isEqualTo("Customer downgrade");verify(auditService).record(eq("UPDATE"),eq("TENANT_ENTITLEMENT"),eq("sales.enabled"),eq("admin"),contains("Customer downgrade"),isNull());}
    @Test void planSyncDisablesDependentsAndPreservesFeatureConfiguration(){TenantFeature procurement=new TenantFeature("app","procurement.enabled",true,"{\"keep\":true}","admin");TenantFeature inventory=new TenantFeature("app","inventory.advanced.enabled",true,null,"admin");TenantFeature manufacturing=new TenantFeature("app","manufacturing.enabled",true,null,"admin");when(repository.findByAppId("app")).thenReturn(List.of(procurement,inventory,manufacturing));when(repository.save(any())).thenAnswer(i->i.getArgument(0));service.applyPlan("app",Set.of("finance.enabled"),"Downgrade","admin");assertThat(procurement.isEnabled()).isFalse();assertThat(inventory.isEnabled()).isFalse();assertThat(manufacturing.isEnabled()).isFalse();assertThat(procurement.getConfigJson()).isEqualTo("{\"keep\":true}");verify(auditService).record(eq("PLAN_SYNC"),eq("TENANT_ENTITLEMENT"),eq("app"),eq("admin"),contains("Downgrade"),isNull());}
}
