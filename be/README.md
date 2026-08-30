# Backend — HR & Factory Operations API (`be/`)

Spring Boot 4.1 modular monolith for Bemo ERP. The build, CI, and container runtime use Java 21 while `options.release = 17` preserves Java 17 bytecode/API compatibility. Persistence uses Spring Data JPA, Hibernate ORM, PostgreSQL 18.4 (`jdbc:postgresql://localhost:5432/bemo_erp`), and Liquibase schema migrations.

---

## 🏢 Business Requirements & Domain Logic Services

1. **SaaS Multi-Tenancy & User Authorization**:
   - `TenantContext` extracts `appId` from JWT claim for tenant data isolation across all tables (`@TenantId`).
   - Per-user granular menu permission filtering (`allowed_menus`).
   - Tenant application session timeout configuration (`GET`/`PUT /api/v1/admin/app-settings`).

2. **Factory Worker Classification & Schedule Rules**:
   - Category-based attendance modes (`BIOMETRIC`, `MANUAL`, `HYBRID`) and pay cycles (`MONTHLY`, `HALF_MONTHLY` for 15-day daily-wage staff, `THIRTY_DAYS`).
   - Intra-month effective schedule rules (`ScheduleRule`) supporting seasonal summer/winter shifts (e.g., July 1–12 at 07:00 AM vs. July 13–31 at 09:00 AM).
   - **Power Outage & Mass Absence Detector**: Automatically detects when >50% of employees in a category lack punches on a workday and prompts HR reviewers to confirm Excused Presence vs Absence.

3. **Biometric Evidence & Attendance Calculation Engine**:
   - Sha256-verified biometric imports (`CSV`, `TXT`, `XLS`, `XLSX`) into `punch_records`.
   - Single-punch policy enforcement and attendance calculation producing daily results categorized by health tiers:
     - 🟢 **GREEN**: Clean attendance.
     - 🟡 **YELLOW**: Single punch or grace period.
     - 🔴 **RED**: Missing punches or missing schedules.

4. **Operations, Inventory, Balances & Excel Exporters**:
   - Business party management (`PARTNER`, `SUPPLIER`, `CUSTOMER`, `FARM`, `TRADER`).
   - Inventory items, immutable signed stock movements, partner financial ledgers, processing loss percentages, and category-controlled employee advances.
   - Native Excel generation (`OperationsExcelExporter.java`, `ReportsExcelExporter.java`) formatted per user theme preferences (`GOLD`, `BLUE`, `GREEN`, `GRAY`).

---

## 🇸🇦 قواعد العمل للخلفية البرمجية (Arabic Summary)

- **حسابات العمالة باليومية (15 يوماً) والعمالة الثابتة (شفتات 8، 10، 12 ساعة)**.
- **تأكيد انقطاع الكهرباء والأعطال تلقائياً بنسبة >50% غياب بصمة**.
- **فئات الصحة للحضور (خضراء، صفراء، حمراء) مع قرارات HR الجماعية**.
- **تطبيق صلاحيات القوائم لكل مستخدم بجدول `allowed_menus`**.
- **تصدير شيتات Excel منسقة ومتوافقة مع المظهر المختار**.

---

## Command Reference & Configuration

```powershell
# Run backend locally with PostgreSQL dev profile
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Clean and run all unit/integration tests
.\gradlew.bat clean test

# Build backend Docker image standalone
docker build -t bemo-hr-backend .
```

### GraalVM launcher scripts (WP-18 T-1)

Convenience launchers probe `GRAALVM_HOME` / SDKMAN / common install paths, set
`JAVA_HOME` to GraalVM when found, and otherwise fall back to the standard JDK
with an actionable message:

```powershell
# Windows
.\start-backend-graal.bat
```

```bash
# Linux / macOS / WSL
./start-backend-graal.sh
```

Building a native image requires GraalVM (JVM features like `@EnableCaching`,
Dynamic Proxies and JDK proxy generation need `--enable-preview`-free reflection
config; the Gradle Spring AOT plugin resolves these):

```bash
cd be
export GRAALVM_HOME=/path/to/graalvm
./gradlew nativeCompile
./build/native/images/bemo-erp
```

> Native-image build is a dev convenience only — it is intentionally **not**
> wired into CI.

| Configuration Variable | Purpose / Default Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bemo_erp` |
| `DB_USERNAME` | PostgreSQL username (`root`) |
| `DB_PASSWORD` | PostgreSQL password (`root`) |
| `HR_JWT_SECRET` | HS256 secret (minimum 32 bytes) |
| `HR_COMPANY_ZONE` | Time zone (`Africa/Cairo`) |

## Inventory control contract / عقد ضوابط المخزون

- `com.bemo.hr.operations` is the single inventory source of truth for warehouse/bin balances, stock statuses, reservations, transfers, cycle counts, valuation movements, and lot/serial evidence.
- Available-to-promise uses only `AVAILABLE` balances and subtracts active reservations. Delivery consumes the locked reservation and physical balance; cancellation or expiry releases ATP without creating a stock movement.
- Status movements preserve physical quantity and are audited. Transfer ship/receive and cycle-count reconciliation are idempotent state transitions.
- Lot/serial records preserve receipt, issue, and return document references; serial numbers are tenant-unique and serial quantities are always one.

- الحزمة `com.bemo.hr.operations` هي مصدر الحقيقة الوحيد للمستودعات والمواقع والأرصدة والحجوزات والتحويلات والجرد والتقييم وتتبع التشغيلات والأرقام المسلسلة.
- المتاح للوعد يحتسب الرصيد المتاح فقط بعد خصم الحجوزات النشطة، بينما الإلغاء أو الانتهاء يحرر الحجز دون حركة مخزنية وهمية.
- نقل حالة المخزون يحافظ على إجمالي الكمية ويسجل تدقيقاً، كما أن شحن/استلام التحويل وتسوية الجرد عمليتان آمنتان عند إعادة الطلب.
- يحتفظ سجل التشغيلة/الرقم المسلسل بمراجع الاستلام والصرف والمرتجع، مع منع تكرار الرقم المسلسل داخل المستأجر.
