package com.bemo.hr.shared.security;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class EntitlementManagementService {
    private final EntitlementCatalog catalog;private final TenantFeatureRepository repository;private final TenantFeatureService featureService;private final AuditService auditService;
    @Transactional(readOnly=true) public List<EntitlementApi.ModuleResponse> catalog(String appId){Map<String,TenantFeature> rows=repository.findByAppId(appId).stream().collect(java.util.stream.Collectors.toMap(TenantFeature::getFeatureKey,java.util.function.Function.identity()));
        return catalog.modules().stream().map(m->new EntitlementApi.ModuleResponse(m.key(),m.features().stream().map(f->view(appId,f,rows.get(f.key()))).toList())).toList();}
    @Transactional public EntitlementApi.FeatureResponse update(String appId,String key,EntitlementApi.UpdateRequest request,String actor){EntitlementCatalog.Feature definition=catalog.feature(key).orElseThrow(()->error("ENTITLEMENT_UNKNOWN_FEATURE"));
        TenantFeature row=repository.findById(new TenantFeatureId(appId,key)).orElseGet(()->new TenantFeature(appId,key,featureService.isEnabledForTenant(appId,key),null,actor));
        if(row.getVersion()!=request.expectedVersion())throw new BusinessRuleException("STALE_STATE","STALE_STATE",HttpStatus.CONFLICT);
        if(request.enabled())for(String dependency:definition.dependencies())if(!featureService.isEnabledForTenant(appId,dependency))throw new BusinessRuleException("ENTITLEMENT_DEPENDENCY_MISSING","ENTITLEMENT_DEPENDENCY_MISSING",HttpStatus.CONFLICT,List.of(dependency));
        if(!request.enabled())for(String dependent:catalog.dependents(key))if(featureService.isEnabledForTenant(appId,dependent))throw new BusinessRuleException("ENTITLEMENT_DEPENDENT_ACTIVE","ENTITLEMENT_DEPENDENT_ACTIVE",HttpStatus.CONFLICT,List.of(dependent));
        boolean before=row.isEnabled();row.update(request.enabled(),request.configJson(),request.reason(),actor);repository.save(row);auditService.record("UPDATE","TENANT_ENTITLEMENT",key,actor,"{\"before\":"+before+",\"after\":"+request.enabled()+",\"reason\":\""+escape(request.reason())+"\"}",null);return view(appId,definition,row);}
    @Transactional public void applyPlan(String appId,Set<String> desired,String reason,String actor){
        Set<String> known=catalog.modules().stream().flatMap(m->m.features().stream()).map(EntitlementCatalog.Feature::key).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if(!known.containsAll(desired))throw new BusinessRuleException("ENTITLEMENT_UNKNOWN_FEATURE","ENTITLEMENT_UNKNOWN_FEATURE",HttpStatus.BAD_REQUEST);
        for(String key:desired){var feature=catalog.feature(key).orElseThrow();if(!desired.containsAll(feature.dependencies()))throw new BusinessRuleException("ENTITLEMENT_DEPENDENCY_MISSING","ENTITLEMENT_DEPENDENCY_MISSING",HttpStatus.CONFLICT,List.copyOf(feature.dependencies()));}
        Map<String,TenantFeature> rows=repository.findByAppId(appId).stream().collect(java.util.stream.Collectors.toMap(TenantFeature::getFeatureKey,java.util.function.Function.identity()));
        Set<String> enabled=new LinkedHashSet<>();for(String key:known)if(rows.containsKey(key)?rows.get(key).isEnabled():catalog.defaultEnabled(key))enabled.add(key);
        while(!enabled.containsAll(desired)){boolean progressed=false;for(String key:desired)if(!enabled.contains(key)&&enabled.containsAll(catalog.feature(key).orElseThrow().dependencies())){set(appId,key,true,rows,reason,actor);enabled.add(key);progressed=true;}if(!progressed)throw new BusinessRuleException("ENTITLEMENT_DEPENDENCY_MISSING","ENTITLEMENT_DEPENDENCY_MISSING",HttpStatus.CONFLICT);}
        Set<String> remove=new LinkedHashSet<>(enabled);remove.removeAll(desired);while(!remove.isEmpty()){boolean progressed=false;for(String key:new ArrayList<>(remove))if(catalog.dependents(key).stream().noneMatch(remove::contains)){set(appId,key,false,rows,reason,actor);remove.remove(key);progressed=true;}if(!progressed)throw new BusinessRuleException("ENTITLEMENT_DEPENDENT_ACTIVE","ENTITLEMENT_DEPENDENT_ACTIVE",HttpStatus.CONFLICT);}
        auditService.record("PLAN_SYNC","TENANT_ENTITLEMENT",appId,actor,"{\"features\":"+desired.size()+",\"reason\":\""+escape(reason)+"\"}",null);
    }
    private void set(String appId,String key,boolean enabled,Map<String,TenantFeature>rows,String reason,String actor){TenantFeature row=rows.computeIfAbsent(key,k->new TenantFeature(appId,k,!enabled,null,actor));row.update(enabled,row.getConfigJson(),reason,actor);repository.save(row);}
    private EntitlementApi.FeatureResponse view(String appId,EntitlementCatalog.Feature f,TenantFeature row){return new EntitlementApi.FeatureResponse(f.key(),row==null?featureService.isEnabledForTenant(appId,f.key()):row.isEnabled(),List.copyOf(f.dependencies()),row==null?null:row.getConfigJson(),row==null?0:row.getVersion(),row==null?null:row.getUpdatedBy(),row==null?null:row.getChangeReason(),row==null||row.getUpdatedAt()==null?0:row.getUpdatedAt().toEpochMilli());}
    private static BusinessRuleException error(String code){return new BusinessRuleException(code,code,HttpStatus.NOT_FOUND);}private static String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}
}
