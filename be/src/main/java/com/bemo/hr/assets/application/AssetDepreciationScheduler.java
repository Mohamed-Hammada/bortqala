package com.bemo.hr.assets.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WP-04: month-end depreciation cron. Runs on the first day of each month and is
 * skipped whenever a manual run holds the lock. The run itself is idempotent per
 * (asset, month), so an overlap can never double-post.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetDepreciationScheduler {

    private final AssetDepreciationService depreciationService;

    @Scheduled(cron = "${hr.assets.depreciation-cron:0 0 1 1 * *}", zone = "UTC")
    public void runMonthlyDepreciation() {
        if (!depreciationService.tryLock()) {
            log.info("Scheduled depreciation skipped — a manual run is in progress.");
            return;
        }
        try {
            String yearMonth = java.time.YearMonth.now(java.time.ZoneOffset.UTC).minusMonths(1).toString();
            var result = depreciationService.runDepreciation(yearMonth, "system");
            log.info("Monthly depreciation {} posted {} journal(s)", yearMonth, result.postedCount());
        } catch (Exception error) {
            log.error("Monthly depreciation run failed", error);
        } finally {
            depreciationService.unlock();
        }
    }
}
