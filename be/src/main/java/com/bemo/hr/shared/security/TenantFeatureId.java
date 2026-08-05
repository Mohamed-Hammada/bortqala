package com.bemo.hr.shared.security;

import java.io.Serializable;
import java.util.Objects;

public class TenantFeatureId implements Serializable {
    private String appId;
    private String featureKey;

    public TenantFeatureId() {
    }

    public TenantFeatureId(String appId, String featureKey) {
        this.appId = appId;
        this.featureKey = featureKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantFeatureId that = (TenantFeatureId) o;
        return Objects.equals(appId, that.appId) && Objects.equals(featureKey, that.featureKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, featureKey);
    }
}
