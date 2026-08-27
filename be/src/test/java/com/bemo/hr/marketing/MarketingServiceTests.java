package com.bemo.hr.marketing;

import com.bemo.hr.marketing.application.MarketingService;
import com.bemo.hr.marketing.domain.*;
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
class MarketingServiceTests {

    @Mock private CampaignRepository campaignRepo;
    @Mock private CampaignRecipientRepository recipientRepo;
    @Mock private SurveyRepository surveyRepo;
    @Mock private SurveyQuestionRepository questionRepo;
    @Mock private SurveyResponseRepository responseRepo;
    @InjectMocks private MarketingService service;

    @Test
    void sendCampaign_movesDraftToSending() {
        Campaign c = new Campaign("app1", "Test", Campaign.Channel.EMAIL, "sub", "ar", "en", "{}");
        when(campaignRepo.findById(any())).thenReturn(Optional.of(c));
        when(campaignRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipientRepo.findByCampaignIdAndStatus(any(), any())).thenReturn(List.of());
        Campaign result = service.sendCampaign("app1", c.getId());
        assertThat(result.getStatus()).isEqualTo("SENDING");
    }

    @Test
    void submitResponse_rejectsDuplicate() {
        Survey s = new Survey("app1", "Survey", "desc");
        when(surveyRepo.findByAppIdAndId(any(), any())).thenReturn(Optional.of(s));
        when(responseRepo.existsBySurveyIdAndRespondentToken(any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.submitResponse("app1", "s1", "token1", List.of()))
                .hasMessageContaining("Duplicate");
    }

    @Test
    void submitResponse_rejectsInactiveSurvey() {
        Survey s = new Survey("app1", "Survey", "desc");
        s.setActive(false);
        when(surveyRepo.findByAppIdAndId(any(), any())).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.submitResponse("app1", "s1", "token1", List.of()))
                .hasMessageContaining("not active");
    }

    @Test
    void createCampaign_savesCorrectStatus() {
        when(campaignRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Campaign c = service.createCampaign("app1", "Test", "EMAIL", "sub", "ar", "en", "{}");
        assertThat(c.getStatus()).isEqualTo("DRAFT");
        assertThat(c.getName()).isEqualTo("Test");
    }

    @Test
    void addRecipients_savesEachRecipient() {
        when(campaignRepo.findById(any())).thenReturn(Optional.of(new Campaign("app1", "C", Campaign.Channel.EMAIL, "s", "ar", "en", "{}")));
        List<MarketingService.RecipientDto> dtos = List.of(
                new MarketingService.RecipientDto("ref1", "a@b.com", "+123", "ar"),
                new MarketingService.RecipientDto("ref2", "c@d.com", "+456", "en"));
        service.addRecipients("app1", "c1", dtos);
        verify(recipientRepo, times(2)).save(any());
    }
}
