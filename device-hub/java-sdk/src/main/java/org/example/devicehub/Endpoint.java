package org.example.devicehub;
import java.net.URI; import java.util.Map;
public record Endpoint(Vendor vendor, IntegrationRoute route, URI baseUri, String username, String password, Map<String,String> options) {
 public Endpoint { options=options==null?Map.of():Map.copyOf(options); }
}
