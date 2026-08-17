package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.posting.PostingProfile;
import com.bemo.hr.finance.domain.posting.PostingProfileLine;
import com.bemo.hr.finance.domain.posting.PostingProfileLineRepository;
import com.bemo.hr.finance.domain.posting.PostingProfileRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialControlAccountResolver {

    private final PostingProfileRepository profileRepository;
    private final PostingProfileLineRepository lineRepository;

    public FinancialControlAccountResolver(PostingProfileRepository profileRepository,
                                           PostingProfileLineRepository lineRepository) {
        this.profileRepository = profileRepository;
        this.lineRepository = lineRepository;
    }

    public String fixedControlAccount(String businessEvent, LocalDate asOf, String side) {
        PostingProfile profile = profileRepository
                .findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc(businessEvent)
                .stream()
                .filter(candidate -> !asOf.isBefore(candidate.getEffectiveFrom())
                        && (candidate.getEffectiveTo() == null || !asOf.isAfter(candidate.getEffectiveTo())))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "No effective posting profile is configured for " + businessEvent + ".",
                        "SUBLEDGER_POSTING_PROFILE_REQUIRED",
                        HttpStatus.CONFLICT));

        List<String> accounts = lineRepository.findByProfileIdOrderByLineNoAsc(profile.getId()).stream()
                .filter(line -> side.equalsIgnoreCase(line.getSide()))
                .filter(line -> "FIXED".equalsIgnoreCase(line.getAccountSource()))
                .map(PostingProfileLine::getFixedAccountId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        if (accounts.size() != 1) {
            throw new BusinessRuleException(
                    "A single fixed " + side + " control account is required for " + businessEvent + ".",
                    "SUBLEDGER_POSTING_PROFILE_INVALID",
                    HttpStatus.CONFLICT);
        }
        return accounts.get(0);
    }
}
