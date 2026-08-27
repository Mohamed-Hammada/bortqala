package com.bemo.hr.knowledgebase.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class KbApi {

    public record ArticleResponse(String id, String slug, String titleAr, String titleEn,
                                   String bodyAr, String bodyEn, String tags, boolean published,
                                   long views, long helpfulUp, long helpfulDown,
                                   String authorUserId, Long createdAtEpochMs, Long version) {}

    public record CreateArticlePayload(
            @NotBlank String titleAr, @NotBlank String titleEn,
            String bodyAr, String bodyEn, String tags) {}

    public record UpdateArticlePayload(String titleAr, String titleEn,
                                        String bodyAr, String bodyEn, String tags) {}

    public record VotePayload(boolean up) {}

    public record CreateFromTicketPayload(@NotBlank String titleAr, @NotBlank String titleEn) {}
}
