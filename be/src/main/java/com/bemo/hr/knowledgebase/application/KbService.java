package com.bemo.hr.knowledgebase.application;

import com.bemo.hr.helpdesk.domain.Ticket;
import com.bemo.hr.helpdesk.domain.TicketMessage;
import com.bemo.hr.helpdesk.domain.TicketMessageRepository;
import com.bemo.hr.knowledgebase.domain.KbArticle;
import com.bemo.hr.knowledgebase.domain.KbArticleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KbService {

    private final KbArticleRepository articleRepo;
    private final TicketMessageRepository messageRepo;

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    @Transactional
    public KbArticle createArticle(String appId, String titleAr, String titleEn,
                                   String bodyAr, String bodyEn, String tags, String authorUserId) {
        String slug = generateSlug(titleEn);
        KbArticle article = new KbArticle(appId, slug, titleAr, titleEn, bodyAr, bodyEn, tags, authorUserId);
        return articleRepo.save(article);
    }

    @Transactional
    public KbArticle updateArticle(String appId, String articleId, String titleAr, String titleEn,
                                   String bodyAr, String bodyEn, String tags) {
        KbArticle a = articleRepo.findByAppIdAndId(appId, articleId)
                .orElseThrow(() -> new BusinessRuleException("Article not found.",
                        "KB_ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (titleAr != null) a.setTitleAr(titleAr);
        if (titleEn != null) a.setTitleEn(titleEn);
        if (bodyAr != null) a.setBodyAr(bodyAr);
        if (bodyEn != null) a.setBodyEn(bodyEn);
        if (tags != null) a.setTags(tags);
        return articleRepo.save(a);
    }

    @Transactional
    public KbArticle publishArticle(String appId, String articleId) {
        KbArticle a = articleRepo.findByAppIdAndId(appId, articleId)
                .orElseThrow(() -> new BusinessRuleException("Article not found.",
                        "KB_ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (a.getBodyAr() == null || a.getBodyAr().isBlank()
                || a.getBodyEn() == null || a.getBodyEn().isBlank()) {
            throw new BusinessRuleException("Both Arabic and English body are required to publish.",
                    "KB_BOTH_LOCALES_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        a.setPublished(true);
        return articleRepo.save(a);
    }

    @Transactional(readOnly = true)
    public List<KbArticle> searchArticles(String appId, String query) {
        if (query == null || query.isBlank()) {
            return articleRepo.findByAppIdAndPublishedTrueOrderByViewsDesc(appId);
        }
        return articleRepo.search(appId, query);
    }

    @Transactional
    public void incrementViews(String appId, String articleId) {
        articleRepo.findByAppIdAndId(appId, articleId).ifPresent(a -> {
            a.incrementViews();
            articleRepo.save(a);
        });
    }

    @Transactional
    public void voteHelpful(String appId, String articleId, boolean up) {
        KbArticle a = articleRepo.findByAppIdAndId(appId, articleId)
                .orElseThrow(() -> new BusinessRuleException("Article not found.",
                        "KB_ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (up) a.incrementHelpfulUp(); else a.incrementHelpfulDown();
        articleRepo.save(a);
    }

    @Transactional
    public KbArticle createFromTicket(String appId, String ticketId, String titleAr, String titleEn,
                                      String authorUserId) {
        List<TicketMessage> messages = messageRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
        StringBuilder body = new StringBuilder();
        for (TicketMessage m : messages) {
            if (m.isInternalNote()) continue;
            body.append(sanitize(m.getBody())).append("\n\n");
        }
        String slug = generateSlug(titleEn);
        KbArticle article = new KbArticle(appId, slug, titleAr, titleEn,
                body.toString().trim(), body.toString().trim(), "", authorUserId);
        return articleRepo.save(article);
    }

    @Transactional(readOnly = true)
    public List<KbArticle> listAll(String appId) {
        return articleRepo.findByAppIdOrderByCreatedAtDesc(appId);
    }

    @Transactional(readOnly = true)
    public KbArticle getArticle(String appId, String articleId) {
        return articleRepo.findByAppIdAndId(appId, articleId)
                .orElseThrow(() -> new BusinessRuleException("Article not found.",
                        "KB_ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private String generateSlug(String title) {
        if (title == null) title = "article";
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis();
    }

    private String sanitize(String html) {
        return HTML_TAG.matcher(html).replaceAll("");
    }
}
