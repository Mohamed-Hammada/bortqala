package org.example.zkteco.adapter.pull;

import org.example.zkteco.core.*;
import java.time.Instant;
import java.util.*;

public final class ZkPullAdapter implements DeviceAdapter {
    public static final int DEFAULT_PORT=4370;
    @Override public DeviceProtocol protocol(){ return DeviceProtocol.ZK_PULL; }
    @Override public boolean supports(DeviceEndpoint e){ return e.host()!=null&&!e.host().isBlank()&&(e.preferredProtocol()==DeviceProtocol.AUTO||e.preferredProtocol()==DeviceProtocol.ZK_PULL); }
    @Override public DeviceProbeResult probe(DeviceEndpoint e){
        int timeout=Integer.parseInt(e.property("connectTimeoutMillis","3000"));
        boolean udp=Boolean.parseBoolean(e.property("udp","false"));
        int commKey=Integer.parseInt(e.property("commKey","0"));
        try(ZkPullClient c=new ZkPullClient(e.host(),e.portOr(DEFAULT_PORT),timeout,udp)){
            c.connect(); if(commKey!=0)c.authenticate(commKey);
            String fw=safe(c::getFirmwareVersion);
            Map<String,String> options=safeMap(c::identityOptions);
            String serial=options.get("~SerialNumber"), platform=options.get("~Platform"), model=options.get("~DeviceName");
            Set<DeviceCapability> caps=EnumSet.of(DeviceCapability.IDENTITY_READ,DeviceCapability.DEVICE_TIME_READ,
                    DeviceCapability.DEVICE_TIME_WRITE,DeviceCapability.DEVICE_ENABLE,DeviceCapability.DEVICE_DISABLE,
                    DeviceCapability.DEVICE_REBOOT,DeviceCapability.DOOR_UNLOCK,DeviceCapability.RAW_COMMAND);
            List<String>warnings=new ArrayList<>();
            warnings.add("Binary handshake succeeded via "+(udp?"UDP":"TCP")+" port "+e.portOr(DEFAULT_PORT)+". User/template/log record layouts remain firmware-profile dependent.");
            return new DeviceProbeResult(true,protocol(),model,serial,fw,platform,caps,warnings,Instant.now());
        }catch(Exception ex){ return DeviceProbeResult.offline(protocol(),"PULL probe failed: "+ex.getMessage()); }
    }
    @FunctionalInterface interface IOCall<T>{T run() throws Exception;}
    private static String safe(IOCall<String> c){try{return c.run();}catch(Exception e){return null;}}
    private static Map<String,String> safeMap(IOCall<Map<String,String>> c){try{return c.run();}catch(Exception e){return Map.of();}}
}
