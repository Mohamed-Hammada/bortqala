package com.bemo.hr.shared.shortcut.infrastructure;

import com.bemo.hr.shared.shortcut.domain.UserShortcutProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserShortcutProfileRepository extends JpaRepository<UserShortcutProfile, String> {
    Optional<UserShortcutProfile> findByUserId(String userId);
}
