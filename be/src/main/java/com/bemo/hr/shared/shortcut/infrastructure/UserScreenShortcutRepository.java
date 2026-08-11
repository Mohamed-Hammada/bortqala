package com.bemo.hr.shared.shortcut.infrastructure;

import com.bemo.hr.shared.shortcut.domain.UserScreenShortcut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserScreenShortcutRepository extends JpaRepository<UserScreenShortcut, String> {
    List<UserScreenShortcut> findByProfileIdOrderBySortOrderAsc(String profileId);

    /**
     * Execute the replacement delete immediately in the database.
     *
     * A derived delete may schedule entity removals until the persistence
     * context is flushed. UserScreenShortcutService replaces the complete
     * shortcut list by deleting the old rows and then inserting new rows in
     * the same transaction. Because the table has unique constraints on
     * (app_id, profile_id, second_key_code) and (app_id, profile_id, page_code),
     * deferred deletes can make valid replacement rows collide with old rows.
     *
     * Bulk DML executes immediately, so the unique slots are released before
     * the replacement inserts are issued.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from UserScreenShortcut s where s.profileId = :profileId")
    void deleteByProfileId(@Param("profileId") String profileId);
}
