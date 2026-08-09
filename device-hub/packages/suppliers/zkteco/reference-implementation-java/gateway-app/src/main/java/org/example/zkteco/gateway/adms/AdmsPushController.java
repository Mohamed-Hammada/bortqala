package org.example.zkteco.gateway.adms;

import org.example.zkteco.adapter.adms.AdmsCommand;
import org.example.zkteco.adapter.adms.AdmsCommandQueue;
import org.example.zkteco.adapter.adms.AdmsIngressStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AdmsPushController {
    private final AdmsIngressStore store;
    private final AdmsCommandQueue queue;

    public AdmsPushController(AdmsIngressStore store, AdmsCommandQueue queue) {
        this.store = store;
        this.queue = queue;
    }

    @RequestMapping(value = "/iclock/cdata", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.TEXT_PLAIN_VALUE)
    public String cdata(@RequestParam(name = "SN", required = false) String sn,
                        @RequestParam Map<String, String> query,
                        @RequestHeader(name = "Content-Type", required = false) String contentType,
                        @RequestBody(required = false) String body,
                        jakarta.servlet.http.HttpServletRequest request) {
        store.append(sn, "/iclock/cdata", request.getMethod(), query, contentType, body);
        return "OK";
    }

    @GetMapping(value = "/iclock/getrequest", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getrequest(@RequestParam(name = "SN", required = false) String sn,
                             @RequestParam Map<String, String> query) {
        store.append(sn, "/iclock/getrequest", "GET", query, null, "");
        AdmsCommand command = queue.poll(sn);
        return command == null ? "OK" : command.wireValue();
    }

    @PostMapping(value = "/iclock/devicecmd", produces = MediaType.TEXT_PLAIN_VALUE)
    public String devicecmd(@RequestParam(name = "SN", required = false) String sn,
                            @RequestParam Map<String, String> query,
                            @RequestBody(required = false) String body) {
        store.append(sn, "/iclock/devicecmd", "POST", query, "text/plain", body);
        return "OK";
    }

    public AdmsIngressStore store() { return store; }
    public AdmsCommandQueue queue() { return queue; }
}
