package com.bemo.hr.shared.shortcut.infrastructure;

import com.bemo.hr.shared.shortcut.domain.UserScreenShortcut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserScreenShortcutRepository extends JpaRepository<UserScreenShortcut, String> {
    List<UserScreenShortcut> findByProfileIdOrderBySortOrderAsc(String profileId);
    void deleteByProfileId(String profileId);
}
