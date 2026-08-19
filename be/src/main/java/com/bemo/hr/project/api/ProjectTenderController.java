package com.bemo.hr.project.api;

import com.bemo.hr.project.api.TenderApi.*;
import com.bemo.hr.project.application.ProjectTenderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-tenders")
public class ProjectTenderController {

    private final ProjectTenderService tenderService;

    public ProjectTenderController(ProjectTenderService tenderService) {
        this.tenderService = tenderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public List<ProjectTenderResponse> listTenders() {
        return tenderService.listTenders();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public ProjectTenderResponse getTender(@PathVariable String id) {
        return tenderService.getTender(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectTenderResponse createTender(
            @Valid @RequestBody CreateTenderRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.createTender(req, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectTenderResponse updateTender(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenderRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.updateTender(id, req, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void deleteTender(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        tenderService.deleteTender(id, userId);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectTenderResponse publishTender(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.publishTender(id, userId);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectTenderResponse cancelTender(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.cancelTender(id, userId);
    }

    @PostMapping("/{id}/boq")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderBoqItemResponse addBoqItem(
            @PathVariable String id,
            @Valid @RequestBody CreateBoqItemRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.addBoqItem(id, req, userId);
    }

    @PutMapping("/{id}/boq/{itemId}")
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderBoqItemResponse updateBoqItem(
            @PathVariable String id,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateBoqItemRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.updateBoqItem(id, itemId, req, userId);
    }

    @DeleteMapping("/{id}/boq/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void deleteBoqItem(
            @PathVariable String id,
            @PathVariable String itemId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        tenderService.deleteBoqItem(id, itemId, userId);
    }

    @PostMapping("/{id}/bidders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderBidderResponse inviteBidder(
            @PathVariable String id,
            @Valid @RequestBody InviteBidderRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.inviteBidder(id, req, userId);
    }

    @PostMapping("/{id}/bidders/{bidderId}/submit")
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderBidderResponse submitBid(
            @PathVariable String id,
            @PathVariable String bidderId,
            @Valid @RequestBody SubmitBidRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.submitBid(id, bidderId, req, userId);
    }

    @PostMapping("/{id}/bidders/{bidderId}/bid-bond")
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderBidderResponse recordBidBond(
            @PathVariable String id,
            @PathVariable String bidderId,
            @Valid @RequestBody RecordBidBondRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.recordBidBond(id, bidderId, req, userId);
    }

    @PostMapping("/{id}/bidders/{bidderId}/technical-eval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void evaluateBidderTechnical(
            @PathVariable String id,
            @PathVariable String bidderId,
            @Valid @RequestBody TechnicalEvaluationRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        tenderService.evaluateBidderTechnical(id, bidderId, req, userId);
    }

    @PostMapping("/{id}/evaluate")
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderEvaluationSummaryResponse calculateEvaluation(
            @PathVariable String id,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.calculateTenderEvaluation(id, userId);
    }

    @PostMapping("/{id}/award")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectTenderResponse awardTender(
            @PathVariable String id,
            @Valid @RequestBody AwardTenderRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.awardTender(id, req, userId);
    }

    @PostMapping("/{id}/clarifications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderClarificationResponse addClarification(
            @PathVariable String id,
            @Valid @RequestBody CreateClarificationRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.addClarification(id, req, userId);
    }

    @PutMapping("/{id}/clarifications/{clarifId}")
    @PreAuthorize("hasAuthority('projects.manage')")
    public TenderClarificationResponse answerClarification(
            @PathVariable String id,
            @PathVariable String clarifId,
            @Valid @RequestBody AnswerClarificationRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return tenderService.answerClarification(id, clarifId, req, userId);
    }
}
