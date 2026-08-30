package com.bemo.hr.knowledgebase.api;

import com.bemo.hr.knowledgebase.application.KbService;
import com.bemo.hr.knowledgebase.domain.KbArticle;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KbController {

    private final KbService service;

    private String resolveAppId(Authentication auth) {
        if (auth.getDetails() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)
            return jwt.getClaimAsString("appId");
        return TenantContext.require();
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public KbApi.ArticleResponse createArticle(
            @Valid @RequestBody KbApi.CreateArticlePayload p, Authentication auth) {
        KbArticle a = service.createArticle(resolveAppId(auth), p.titleAr(), p.titleEn(),
                p.bodyAr(), p.bodyEn(), p.tags(), auth.getName());
        return toResp(a);
    }

    @GetMapping("/articles")
    public List<KbApi.ArticleResponse> listArticles(
            @RequestParam(required = false) String q, Authentication auth) {
        if (q != null) return service.searchArticles(resolveAppId(auth), q).stream().map(this::toResp).toList();
        return service.listAll(resolveAppId(auth)).stream().map(this::toResp).toList();
    }

    @GetMapping("/articles/{id}")
    public KbApi.ArticleResponse getArticle(@PathVariable String id, Authentication auth) {
        service.incrementViews(resolveAppId(auth), id);
        return toResp(service.getArticle(resolveAppId(auth), id));
    }

    @PutMapping("/articles/{id}")
    public KbApi.ArticleResponse updateArticle(
            @PathVariable String id,
            @Valid @RequestBody KbApi.UpdateArticlePayload p, Authentication auth) {
        return toResp(service.updateArticle(resolveAppId(auth), id,
                p.titleAr(), p.titleEn(), p.bodyAr(), p.bodyEn(), p.tags()));
    }

    @PostMapping("/articles/{id}/publish")
    public KbApi.ArticleResponse publishArticle(@PathVariable String id, Authentication auth) {
        return toResp(service.publishArticle(resolveAppId(auth), id));
    }

    @PostMapping("/articles/{id}/vote")
    public void voteArticle(@PathVariable String id,
                            @Valid @RequestBody KbApi.VotePayload p, Authentication auth) {
        service.voteHelpful(resolveAppId(auth), id, p.up());
    }

    @PostMapping("/articles/from-ticket/{ticketId}")
    @ResponseStatus(HttpStatus.CREATED)
    public KbApi.ArticleResponse createFromTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody KbApi.CreateFromTicketPayload p, Authentication auth) {
        return toResp(service.createFromTicket(resolveAppId(auth), ticketId,
                p.titleAr(), p.titleEn(), auth.getName()));
    }

    private KbApi.ArticleResponse toResp(KbArticle a) {
        return new KbApi.ArticleResponse(a.getId(), a.getSlug(), a.getTitleAr(), a.getTitleEn(),
                a.getBodyAr(), a.getBodyEn(), a.getTags(), a.isPublished(),
                a.getViews(), a.getHelpfulUp(), a.getHelpfulDown(),
                a.getAuthorUserId(), a.getCreatedAt(), a.getVersion());
    }
}
