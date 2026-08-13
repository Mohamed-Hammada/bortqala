package org.example.zkteco.adapter.http;
import org.example.zkteco.core.*;
import java.net.URI; import java.net.http.*; import java.nio.charset.StandardCharsets; import java.time.*; import java.util.*;
public abstract class AbstractHttpPlatformAdapter implements DeviceAdapter {
  private final DeviceProtocol protocol; protected final HttpClient client;
  protected AbstractHttpPlatformAdapter(DeviceProtocol p){this(p,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());}
  protected AbstractHttpPlatformAdapter(DeviceProtocol p,HttpClient c){protocol=p;client=c;}
  @Override public DeviceProtocol protocol(){return protocol;}
  @Override public boolean supports(DeviceEndpoint e){return e.baseUri()!=null&&(e.preferredProtocol()==protocol||e.preferredProtocol()==DeviceProtocol.AUTO);}
  @Override public DeviceProbeResult probe(DeviceEndpoint e){
    if(e.baseUri()==null)return DeviceProbeResult.offline(protocol,"baseUri is required");
    String health=e.property("healthPath","/"); URI target=e.baseUri().resolve(health); List<String>w=new ArrayList<>();
    try { HttpRequest.Builder b=HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(Long.parseLong(e.property("requestTimeoutSeconds","10")))).GET(); applyAuth(e,b,w); HttpResponse<Void> r=client.send(b.build(),HttpResponse.BodyHandlers.discarding()); boolean online=r.statusCode()>=200&&r.statusCode()<500; w.add("HTTP platform route is configurable because endpoint paths/auth vary by licensed ZKBio product/version."); return new DeviceProbeResult(online,protocol,null,null,null,null,Set.of(DeviceCapability.IDENTITY_READ),w,Instant.now()); }
    catch(InterruptedException ex){Thread.currentThread().interrupt();return DeviceProbeResult.offline(protocol,"HTTP probe interrupted");}
    catch(Exception ex){return DeviceProbeResult.offline(protocol,"HTTP probe failed for "+target+" - "+ex.getMessage());}
  }
  protected void applyAuth(DeviceEndpoint e,HttpRequest.Builder b,List<String>w){
    String mode=e.property("authMode","bearer").toLowerCase(Locale.ROOT); String secret=resolveSecret(e,w);
    if(secret==null)return;
    switch(mode){
      case "bearer" -> b.header("Authorization","Bearer "+secret);
      case "token-header" -> b.header(e.property("tokenHeader","Authorization"),secret);
      case "basic" -> { String user=e.property("username",""); b.header("Authorization","Basic "+Base64.getEncoder().encodeToString((user+":"+secret).getBytes(StandardCharsets.UTF_8))); }
      case "cookie" -> b.header("Cookie",secret);
      case "none" -> {}
      default -> w.add("Unknown authMode="+mode+"; no authorization header was added.");
    }
  }
  private String resolveSecret(DeviceEndpoint e,List<String>w){ if(e.secretRef()==null||e.secretRef().isBlank()){ if(!"none".equalsIgnoreCase(e.property("authMode","bearer")))w.add("No secretRef configured."); return null;} String s=System.getenv(e.secretRef()); if(s==null||s.isBlank()){w.add("Environment variable referenced by secretRef is missing.");return null;}return s; }
}
