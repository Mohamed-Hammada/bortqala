package org.example.zkteco.gateway.adms;
import org.example.zkteco.adapter.adms.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/adms")
public class AdmsAdminController {
  private final AdmsPushController ingress; public AdmsAdminController(AdmsPushController ingress){this.ingress=ingress;}
  @GetMapping("/events") public List<AdmsEnvelope> recent(@RequestParam(defaultValue="100")int limit){return ingress.store().recent(limit);}
  @GetMapping("/last-seen") public Map<String,java.time.Instant> lastSeen(){return ingress.store().lastSeen();}
  @PostMapping("/devices/{serial}/commands") public AdmsCommand enqueue(@PathVariable String serial,@RequestBody Map<String,String> body){return ingress.queue().enqueue(serial,body.get("payload"));}
  @GetMapping("/devices/{serial}/queue-size") public Map<String,Integer> size(@PathVariable String serial){return Map.of("queued",ingress.queue().size(serial));}
}
