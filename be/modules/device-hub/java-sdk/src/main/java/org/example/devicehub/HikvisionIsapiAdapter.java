package org.example.devicehub;
public final class HikvisionIsapiAdapter extends HttpAdapter {
 public IntegrationRoute route(){return IntegrationRoute.HIKVISION_ISAPI;}
 public ProbeResult probe(Endpoint e) throws Exception {return get(e,"/ISAPI/System/deviceInfo");}
}
