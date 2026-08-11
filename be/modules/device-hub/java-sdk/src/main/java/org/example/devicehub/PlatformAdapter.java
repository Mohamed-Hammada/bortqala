package org.example.devicehub;
public final class PlatformAdapter extends HttpAdapter {
 private final IntegrationRoute route; private final String healthPath;
 public PlatformAdapter(IntegrationRoute route,String healthPath){this.route=route;this.healthPath=healthPath;}
 public IntegrationRoute route(){return route;}
 public ProbeResult probe(Endpoint e) throws Exception {return get(e,healthPath);}
}
