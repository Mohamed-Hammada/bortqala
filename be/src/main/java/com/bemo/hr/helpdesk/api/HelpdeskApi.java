package com.bemo.hr.helpdesk.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class HelpdeskApi {

    public record CategoryResponse(String id, String nameAr, String nameEn,
                                    int slaFirstResponseHours, int slaResolutionHours,
                                    boolean active, Long version) {}

    public record CreateCategoryPayload(
            @NotBlank String nameAr, @NotBlank String nameEn,
            @Positive int slaFirstResponseHours, @Positive int slaResolutionHours) {}

    public record TicketResponse(String id, long ticketNo, String requesterUserId,
                                  String categoryId, String title, String description,
                                  String priority, String status, String assigneeUserId,
                                  Long firstResponseAtEpochMs, Long resolvedAtEpochMs,
                                  Long dueFirstResponseEpochMs, Long dueResolutionEpochMs,
                                  boolean slaBreachFirstResponse, boolean slaBreachResolution,
                                  Long createdAtEpochMs, Long version) {}

    public record CreateTicketPayload(
            @NotBlank String categoryId, @NotBlank String title, String description,
            String priority) {}

    public record AssignPayload(@NotBlank String assigneeUserId) {}

    public record TransitionPayload(@NotBlank String status) {}

    public record MessageResponse(String id, String ticketId, String authorUserId,
                                   String body, boolean internalNote,
                                   String attachmentName, Long createdAtEpochMs) {}

    public record AddMessagePayload(@NotBlank String body, boolean internalNote) {}

    public record TicketListResponse(List<TicketResponse> tickets, long openCount, long myOpenCount) {}
}
