package com.bemo.hr.shared.security;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "user_preferences")
@Getter
public class UserPreference {
    public static final List<String> DEFAULT_DASHBOARD_WIDGETS = List.of(
            "summary", "report", "attendance-chart", "insights", "units", "departments", "categories", "imports");
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThemePreference theme;
    @Enumerated(EnumType.STRING)
    @Column(name = "table_density", nullable = false, length = 20)
    private TableDensity tableDensity;
    @Column(nullable = false, length = 10)
    private String locale;
    @Enumerated(EnumType.STRING)
    @Column(name = "excel_table_style", nullable = false, length = 20)
    private ExcelTableStyle excelTableStyle;
    @Column(name = "default_page_size", nullable = false)
    private int defaultPageSize = 25;
    @Column(name = "default_page", length = 100)
    private String defaultPage = "/dashboard";
    @Column(name = "show_favorites", nullable = false)
    private boolean showFavorites = true;
    @Column(name = "show_recently_used", nullable = false)
    private boolean showRecentlyUsed = true;
    @Column(name = "max_recently_used", nullable = false)
    private int maxRecentlyUsed = 4;
    @Column(name = "favorite_menu_ids", nullable = false, length = 4000)
    private String favoriteMenuIds = "";
    @Column(name = "recent_menu_ids", nullable = false, length = 4000)
    private String recentMenuIds = "";
    @Column(name = "dashboard_widget_ids", nullable = false, length = 1000)
    private String dashboardWidgetIds = String.join(",", DEFAULT_DASHBOARD_WIDGETS);
    @Column(name = "dashboard_animations_enabled", nullable = false)
    private boolean dashboardAnimationsEnabled = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreference() {
    }

    public UserPreference(String userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.theme = ThemePreference.SYSTEM;
        this.tableDensity = TableDensity.COMFORTABLE;
        this.locale = "ar-EG";
        this.excelTableStyle = ExcelTableStyle.GOLD;
        this.defaultPageSize = 25;
        this.defaultPage = "/dashboard";
        this.showFavorites = true;
        this.showRecentlyUsed = true;
        this.maxRecentlyUsed = 4;
        this.favoriteMenuIds = "";
        this.recentMenuIds = "";
        this.dashboardWidgetIds = String.join(",", DEFAULT_DASHBOARD_WIDGETS);
        this.dashboardAnimationsEnabled = true;
    }

    private static String serializeMenuIds(Set<String> values, int limit) {
        if (values == null) return "";
        return values.stream().filter(value -> value != null && value.matches("[a-z0-9-]{1,80}"))
                .distinct().limit(limit).reduce((left, right) -> left + "," + right).orElse("");
    }

    private static Set<String> parseMenuIds(String value) {
        var result = new LinkedHashSet<String>();
        if (value == null || value.isBlank()) return result;
        for (String item : value.split(",")) if (!item.isBlank()) result.add(item);
        return result;
    }

    public void update(ThemePreference theme, TableDensity tableDensity, String locale, ExcelTableStyle excelTableStyle) {
        update(theme, tableDensity, locale, excelTableStyle, null, null);
    }

    public void update(ThemePreference theme, TableDensity tableDensity, String locale, ExcelTableStyle excelTableStyle, Integer defaultPageSize) {
        update(theme, tableDensity, locale, excelTableStyle, defaultPageSize, null);
    }

    public void update(ThemePreference theme, TableDensity tableDensity, String locale, ExcelTableStyle excelTableStyle, Integer defaultPageSize, String defaultPage) {
        this.theme = theme;
        this.tableDensity = tableDensity;
        this.locale = locale.equalsIgnoreCase("en-US") ? "en-US" : "ar-EG";
        this.excelTableStyle = excelTableStyle;
        if (defaultPageSize != null && defaultPageSize > 0) {
            this.defaultPageSize = defaultPageSize;
        }
        if (defaultPage != null && !defaultPage.isBlank()) {
            this.defaultPage = defaultPage;
        }
    }

    public void updateNavigation(boolean showFavorites, boolean showRecentlyUsed, int maxRecentlyUsed,
                                 Set<String> favoriteMenuIds, Set<String> recentMenuIds) {
        this.showFavorites = showFavorites;
        this.showRecentlyUsed = showRecentlyUsed;
        this.maxRecentlyUsed = Math.max(1, Math.min(20, maxRecentlyUsed));
        this.favoriteMenuIds = serializeMenuIds(favoriteMenuIds, 100);
        this.recentMenuIds = serializeMenuIds(recentMenuIds, this.maxRecentlyUsed);
    }

    public Set<String> favoriteMenuIds() {
        return parseMenuIds(favoriteMenuIds);
    }

    public Set<String> recentMenuIds() {
        return parseMenuIds(recentMenuIds);
    }

    public void updateDashboard(List<String> widgetIds, boolean animationsEnabled, boolean layoutAllowed) {
        if (layoutAllowed) {
            var normalized = new ArrayList<String>();
            if (widgetIds != null) {
                widgetIds.stream().filter(DEFAULT_DASHBOARD_WIDGETS::contains).distinct().limit(8).forEach(normalized::add);
            }
            this.dashboardWidgetIds = String.join(",", normalized.isEmpty() ? DEFAULT_DASHBOARD_WIDGETS : normalized);
        }
        this.dashboardAnimationsEnabled = animationsEnabled;
    }

    public List<String> dashboardWidgetIds() {
        if (dashboardWidgetIds == null || dashboardWidgetIds.isBlank()) return DEFAULT_DASHBOARD_WIDGETS;
        var result = new ArrayList<String>();
        for (String id : dashboardWidgetIds.split(",")) {
            if (DEFAULT_DASHBOARD_WIDGETS.contains(id) && !result.contains(id)) result.add(id);
        }
        return result.isEmpty() ? DEFAULT_DASHBOARD_WIDGETS : List.copyOf(result);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

}
