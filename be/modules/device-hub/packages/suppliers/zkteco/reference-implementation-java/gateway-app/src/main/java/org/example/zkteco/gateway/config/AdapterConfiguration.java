package org.example.zkteco.gateway.config;
import org.example.zkteco.adapter.adms.AdmsPushAdapter;
import org.example.zkteco.adapter.http.*;
import org.example.zkteco.adapter.pull.ZkPullAdapter;
import org.example.zkteco.core.*;
import org.springframework.context.annotation.*;
import java.util.List;
@Configuration
public class AdapterConfiguration {
  @Bean public AdmsPushAdapter admsPushAdapter(){return new AdmsPushAdapter();}
  @Bean public ZkPullAdapter zkPullAdapter(){return new ZkPullAdapter();}
  @Bean public ZkBioTimeApiAdapter zkBioTimeApiAdapter(){return new ZkBioTimeApiAdapter();}
  @Bean public ZkBioCvSecurityApiAdapter zkBioCvSecurityApiAdapter(){return new ZkBioCvSecurityApiAdapter();}
  @Bean public ZkBioCvAccessApiAdapter zkBioCvAccessApiAdapter(){return new ZkBioCvAccessApiAdapter();}
  @Bean public WdmsApiAdapter wdmsApiAdapter(){return new WdmsApiAdapter();}
  @Bean public ZkBioTimeCloudApiAdapter zkBioTimeCloudApiAdapter(){return new ZkBioTimeCloudApiAdapter();}
  @Bean public ZkBioZlinkApiAdapter zkBioZlinkApiAdapter(){return new ZkBioZlinkApiAdapter();}
  @Bean public WindowsSdkBridgeAdapter windowsSdkBridgeAdapter(){return new WindowsSdkBridgeAdapter();}
  @Bean public PlcommProBridgeAdapter plcommProBridgeAdapter(){return new PlcommProBridgeAdapter();}
  @Bean public ZkFingerBridgeAdapter zkFingerBridgeAdapter(){return new ZkFingerBridgeAdapter();}
  @Bean public AdapterRegistry adapterRegistry(List<DeviceAdapter> a){return new AdapterRegistry(a);}
}
