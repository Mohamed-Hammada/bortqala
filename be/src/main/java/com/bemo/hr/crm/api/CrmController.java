package com.bemo.hr.crm.api;

import com.bemo.hr.crm.application.CrmService;
import com.bemo.hr.crm.domain.CrmLeadStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crm")
public class CrmController {

    private final CrmService crmService;

    public CrmController(CrmService crmService) {
        this.crmService = crmService;
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAuthority('crm.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<CrmApi.LeadResponse> listLeads(@RequestParam(required = false) CrmLeadStatus status) {
        return crmService.listLeads(status);
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.LeadResponse saveLead(@RequestBody CrmApi.SaveLeadRequest request) {
        return crmService.saveLead(request);
    }

    @PostMapping("/leads/{id}/convert")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.LeadResponse convertLeadToCustomer(@PathVariable String id) {
        return crmService.convertLeadToCustomer(id);
    }

    @GetMapping("/leads/{leadId}/activities")
    @PreAuthorize("hasAuthority('crm.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<CrmApi.ActivityResponse> listActivities(@PathVariable String leadId) {
        return crmService.listActivities(leadId);
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.ActivityResponse createActivity(@RequestBody CrmApi.CreateActivityRequest request) {
        return crmService.createActivity(request);
    }

    @PostMapping("/activities/{id}/complete")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.ActivityResponse completeActivity(@PathVariable String id) {
        return crmService.completeActivity(id);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('crm.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.CrmSummaryResponse getSummary() {
        return crmService.getSummary();
    }

    @GetMapping("/channels")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.manage') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<CrmApi.ChannelConfigResponse> listChannelConfigs() {
        return crmService.listChannelConfigs();
    }

    @PostMapping("/channels")
    @PreAuthorize("hasAuthority('crm.manage') or hasAuthority('sales.manage') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.ChannelConfigResponse saveChannelConfig(@RequestBody CrmApi.SaveChannelConfigRequest request) {
        return crmService.saveChannelConfig(request);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAuthority('crm.omnichannel') or hasAuthority('crm.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<CrmApi.ConversationResponse> listConversations() {
        return crmService.listConversations();
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAuthority('crm.omnichannel') or hasAuthority('crm.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<CrmApi.MessageResponse> getConversationMessages(@PathVariable String id) {
        return crmService.getConversationMessages(id);
    }

    @PostMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAuthority('crm.omnichannel') or hasAuthority('crm.manage') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public CrmApi.MessageResponse sendMessage(
            @PathVariable String id,
            @RequestBody CrmApi.SendMessageRequest request
    ) {
        return crmService.sendMessage(id, request);
    }

    @PostMapping("/webhook")
    public CrmApi.MessageResponse handleWebhook(@RequestBody CrmApi.InboundWebhookRequest request) {
        return crmService.handleInboundWebhook(request);
    }
}
