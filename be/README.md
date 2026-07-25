# Backend — HR & Operations API (`be/`)

Spring Boot 4.1 modular monolith built with Java 26, Spring Data JPA, Hibernate ORM, and Gradle. Runs on PostgreSQL 18.4 (`jdbc:postgresql://localhost:5432/hr_platform`) managed exclusively by Liquibase schema migrations.

---

## Business Logic & System Services

1. **SaaS Multi-Tenancy & User Authorization**:
   - `TenantContext` extracts `appId` from JWT claim for tenant data isolation.
   - Per-user granular menu permission filtering (`allowed_menus`).
   - Tenant application session timeout configuration (`GET`/`PUT /api/v1/admin/app-settings`).

2. **Attendance Engine & Schedule Rules**:
   - Category-based attendance modes (`BIOMETRIC`, `MANUAL`, `HYBRID`) and pay cycles (`MONTHLY`, `HALF_MONTHLY`, `THIRTY_DAYS`).
   - Intra-month effective schedule rules (`ScheduleRule`) evaluated dynamically per workday.
   - Power outage & mass disruption detector in `ReportingService.java` (>50% missing punches in a category triggers automated holiday/excused proposal).

3. **Biometric Evidence & Report Processing**:
   - Sha256-verified biometric imports (CSV, TXT, XLS, XLSX) into `punch_records`.
   - Attendance review calculation producing daily attendance results with status (`PRESENT`, `SINGLE_PUNCH`, `NO_PUNCH`, `MANUAL_ENTRY`, `HOLIDAY`, `NON_WORKDAY`, `MISSING_SCHEDULE`).

4. **Operations & Finance**:
   - Business party management (`PARTNER`, `SUPPLIER`, `CUSTOMER`, `FARM`, `TRADER`).
   - Inventory items, immutable signed stock movements, partner financial balances, and category-controlled employee advances.

---

## Command Reference & Configuration

```powershell
# Run backend locally with PostgreSQL dev profile
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Clean and run all unit/integration tests
.\gradlew.bat clean test
```

| Configuration Variable | Purpose / Default Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/hr_platform` |
| `DB_USERNAME` | PostgreSQL username (`root`) |
| `DB_PASSWORD` | PostgreSQL password (`root`) |
| `HR_JWT_SECRET` | HS256 secret (minimum 32 bytes) |
| `HR_COMPANY_ZONE` | Time zone (`Africa/Cairo`) |
