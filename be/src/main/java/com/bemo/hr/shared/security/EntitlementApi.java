package com.bemo.hr.shared.security;
import jakarta.validation.constraints.*;
import java.util.List;
public final class EntitlementApi {private EntitlementApi(){}
    public record FeatureResponse(String key,boolean enabled,List<String> dependencies,String configJson,long version,String updatedBy,String changeReason,long updatedAt){}
    public record ModuleResponse(String key,List<FeatureResponse> features){}
    public record UpdateRequest(boolean enabled,String configJson,@NotBlank @Size(max=500) String reason,long expectedVersion){}
}
