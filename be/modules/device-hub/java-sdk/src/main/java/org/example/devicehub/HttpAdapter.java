package org.example.devicehub;
import java.net.*; import java.net.http.*; import java.time.Duration; import java.nio.charset.StandardCharsets; import java.util.Base64;
public abstract class HttpAdapter implements Adapter {
 protected HttpClient client(Endpoint e) { return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(); }
 protected HttpRequest.Builder req(Endpoint e,String path) {
   URI u=e.baseUri().resolve(path.startsWith("/")?path.substring(1):path);
   var b=HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(8));
   if(e.username()!=null && !e.username().isBlank()) {
     String v=Base64.getEncoder().encodeToString((e.username()+":"+(e.password()==null?"":e.password())).getBytes(StandardCharsets.UTF_8));
     if("basic".equalsIgnoreCase(e.options().getOrDefault("auth",""))) b.header("Authorization","Basic "+v);
   }
   return b;
 }
 protected ProbeResult get(Endpoint e,String path) throws Exception {
   var r=client(e).send(req(e,path).GET().build(),HttpResponse.BodyHandlers.ofString());
   return new ProbeResult(r.statusCode()>=200&&r.statusCode()<400,r.statusCode(),"HTTP "+r.statusCode(),r.body());
 }
}
