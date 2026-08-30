package com.bemo.hr.knowledgebase;

import com.bemo.hr.helpdesk.domain.TicketMessage;
import com.bemo.hr.helpdesk.domain.TicketMessageRepository;
import com.bemo.hr.knowledgebase.application.KbService;
import com.bemo.hr.knowledgebase.domain.KbArticle;
import com.bemo.hr.knowledgebase.domain.KbArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KbServiceTests {

    @Mock private KbArticleRepository articleRepo;
    @Mock private TicketMessageRepository messageRepo;
    @InjectMocks private KbService service;

    @Test
    void publishArticle_requiresBothLocaleBodies() {
        KbArticle article = new KbArticle("app1", "slug", "title-ar", "title-en", "", "body-en", "tag", "user1");
        when(articleRepo.findByAppIdAndId(any(), any())).thenReturn(Optional.of(article));
        assertThatThrownBy(() -> service.publishArticle("app1", "art1"))
                .hasMessageContaining("Both Arabic");
    }

    @Test
    void publishArticle_success() {
        KbArticle article = new KbArticle("app1", "slug", "title-ar", "title-en", "body-ar", "body-en", "tag", "user1");
        when(articleRepo.findByAppIdAndId(any(), any())).thenReturn(Optional.of(article));
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        KbArticle result = service.publishArticle("app1", "art1");
        assertThat(result.isPublished()).isTrue();
    }

    @Test
    void createFromTicket_filtersInternalNotes() {
        List<TicketMessage> messages = List.of(
                new TicketMessage("app1", "t1", "user1", "Public message", false),
                new TicketMessage("app1", "t1", "agent1", "Internal note", true),
                new TicketMessage("app1", "t1", "user1", "Another public", false)
        );
        when(messageRepo.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(messages);
        when(articleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        KbArticle article = service.createFromTicket("app1", "t1", "ar", "en", "user1");
        assertThat(article.getBodyAr()).doesNotContain("Internal note");
        assertThat(article.getBodyAr()).contains("Public message");
        assertThat(article.getBodyAr()).contains("Another public");
    }

    @Test
    void searchArticles_blankQuery_returnsAll() {
        when(articleRepo.findByAppIdAndPublishedTrueOrderByViewsDesc("app1")).thenReturn(List.of());
        List<KbArticle> results = service.searchArticles("app1", "  ");
        assertThat(results).isEmpty();
        verify(articleRepo).findByAppIdAndPublishedTrueOrderByViewsDesc("app1");
    }
}
