# Bemo ERP — Developer Guide for Audit Roadmap Implementation

**Purpose:** This document provides the authoritative setup instructions, known quality issues, and implementation patterns for developers working on the 30-feature audit roadmap.

---

## 1. Repository Structure

```
bemo-erp/
├── be/                          # Spring Boot backend
│   ├── src/main/java/com/bemo/hr/
│   │   ├── project/             # P0: Construction backbone
│   │   │   ├── api/             # REST controllers + DTOs
│   │   │   ├── application/     # Service layer
│   │   │   ├── domain/          # JPA entities + enums
│   │   │   ├── executive/       # Executive dashboard service
│   │   │   └── infrastructure/  # Repository interfaces
│   │   ├── finance/             # GL, journals, fiscal periods, treasury
│   │   ├── trade/               # Procurement + Sales
│   │   ├── workforce/           # Contractors, workers, settlements
│   │   ├── attendance/          # Biometric, punches, imports
│   │   ├── payroll/             # Runs, components, GL posting
│   │   ├── approval/            # Workflow engine (REUSE THIS)
│   │   ├── access/              # Access catalog + permissions
│   │   ├── audit/               # Audit logging
│   │   └── shared/              # Error handling, constants
│   ├── src/main/resources/db/changelog/
│   │   ├── schema/create/       # Table creation migrations
│   │   ├── schema/alter/        # Alter migrations
│   │   ├── data/insert/         # Translation CSVs
│   │   └── data/update/         # Data update migrations
│   └── src/test/                # Unit + integration tests
├── fe/                          # Angular frontend
│   ├── src/app/
│   │   ├── core/                # Shell, interceptors, i18n, auth
│   │   ├── shared/ui/           # Reusable UI components
│   │   └── features/
│   │       ├── projects/        # Project/WBS workspace
│   │       ├── finance/         # Finance reports, journals, banks
│   │       ├── trade/           # Procurement + Sales
│   │       ├── workforce/       # Contractor management
│   │       └── ...
│   └── src/app/app.routes.ts    # All route definitions
├── docs/
│   ├── ROADMAP_STATUS.md        # 30-feature status tracker
│   ├── TEST_EVIDENCE.md         # Quality gate evidence
│   └── ROADMAP_DEVELOPER_GUIDE.md  # This file
└── AGENTS.md                    # Session history
```

---

## 2. Local Setup Instructions

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21 (toolchain) | Bytecode targets Java 17 (`options.release = 17`) |
| Node.js | 24 LTS | Per `.nvmrc` — do NOT use Node 26+ (localStorage bug) |
| PostgreSQL | 16+ | Production + integration tests |
| H2 | In-memory | Unit tests only |
| Docker | Optional | PostgresIntegrationTest suites only |

### Backend Setup

```bash
cd be

# Build (skip Docker-dependent tests in sandbox)
./gradlew.bat test -PskipDockerTests     # Windows
./gradlew test -PskipDockerTests         # Linux/Mac

# Quality gates
python tools/check-error-codes.py        # All exception codes have DB translations
python tools/check-translation-catalog.py
python tools/check-authorization-contract.py
python tools/check-test-count.py         # Enforces minimum test count
```

### Frontend Setup

```bash
cd fe

# MUST use Node 24 (per .nvmrc)
nvm use 24

# Install
npm install

# Quality gates
npm run check:i18n                        # All i18n keys exist in both locales
npm run check:hardcoded                   # Zero hardcoded UI strings
npm run test -- --watch=false             # All specs pass
npm run build                             # Production build succeeds
```

### Docker (Full Stack)

```bash
cd be
docker compose -f compose.yaml up -d postgres device-hub
```

---

## 3. Architecture Rules (Critical)

### 3.1 One Source of Truth Per Business Concept

| Concept | Source Module | Never Clone Into |
|---------|--------------|------------------|
| Accounting | Finance (GL) | Project, Procurement, etc. |
| Purchase-to-Pay | Procurement | Project |
| Stock/Valuation | Inventory/Operations | Project |
| Labor | Workforce/Payroll | Project |
| Identity | Party | Project, CRM |
| Approvals | Approval Engine | Any module |

**Rule:** New modules add dimensions (project_id, wbs_node_id) to existing transactions. They never create parallel ledgers.

### 3.2 Project Dimension Contract

Every financial document that should be project-aware carries:
- `project_id` (required FK → projects.id)
- `wbs_node_id` (optional FK → wbs_nodes.id)
- `cost_code_id` (optional FK → project_cost_codes.id)

**Server validates:** project belongs to same tenant, project is ACTIVE, WBS node belongs to project.

### 3.3 Posting/Idempotency Contract

Every financial posting must retain:
- Source module/type/ID/version
- Posting correlation key (operationId)
- Journal entry ID
- Reversal reference (if applicable)

The same source version must never post twice.

### 3.4 State Machine Contract

Use explicit guarded states, never booleans:

```
DRAFT → SUBMITTED → REVIEWED → APPROVED/CERTIFIED → POSTED → PAID/CLOSED
         ↓              ↓              ↓
      REJECTED       REOPENED       REVERSED
```

### 3.5 Approval/SOD Contract

**REUSE** the existing `ApprovalWorkflowService` for:
- Project open/close
- Budget revisions
- Tender awards
- Variations and claims
- High-risk journals

**NEVER** create custom approval booleans or parallel approval subsystems.

---

## 4. Known Quality Issues (Audit Findings)

### 4.1 Critical Issues in Project/WBS Kernel

#### ISSUE-001: N+1 Query in ProjectService
**File:** `be/.../project/application/ProjectService.java:189`  
**Problem:** `toProjectResponse()` calls `wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId())` for EVERY project in the list. With 100 projects × 50 WBS nodes each = 5,100 queries.  
**Fix:** Use a single query joining project + WBS data, or aggregate in memory from a pre-fetched map.

#### ISSUE-002: Hardcoded English Exception Messages
**File:** `be/.../project/application/ProjectService.java` (multiple lines)  
**Problem:** Exception messages are plain English strings like `"Project code is already in use."` — the backend skill requires `BusinessRuleException("message", "I18N_KEY", httpStatus)` where the key maps to DB translation rows.  
**Fix:** Replace all hardcoded messages with i18n keys and add corresponding Liquibase translation CSV rows.

#### ISSUE-003: Audit Logging via String Concatenation
**File:** `be/.../project/application/ProjectService.java:119`  
**Problem:** `"{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}"` — fragile, XSS-vulnerable, breaks with special characters.  
**Fix:** Use Jackson `ObjectMapper` to serialize a structured audit detail record.

#### ISSUE-004: Unbounded findAll()
**File:** `be/.../project/application/ProjectService.java:67,77`  
**Problem:** `getProjectSummary()` loads ALL projects and ALL WBS nodes into memory. With 10,000 projects this causes OOM.  
**Fix:** Use repository aggregate queries (`COUNT`, `SUM`) instead of loading all rows.

#### ISSUE-005: WBS Cycle Detection N+1
**File:** `be/.../project/application/WbsService.java:106-116`  
**Problem:** Cycle detection walks up the parent chain with individual `findById` calls. With deep hierarchies this is O(depth) individual queries.  
**Fix:** Fetch all nodes for the project once and walk the in-memory map.

#### ISSUE-006: updateDescendantPaths Level Calculation
**File:** `be/.../project/application/WbsService.java:121-130`  
**Problem:** After repositioning a node, descendant levels are calculated as `item.getLevel() + levelDelta`. But `levelDelta` is computed as `newLevel - node.getLevel()` at reposition time. If the node's level has already been updated by `reposition()`, the delta is wrong.  
**Fix:** Compute levelDelta BEFORE calling `node.reposition()`.

### 4.2 Medium Issues

| ID | Issue | File | Fix |
|----|-------|------|-----|
| MED-001 | No WBS depth limit | WbsService.createWbsNode | Add max depth check (e.g., 10 levels) |
| MED-002 | No project closure validation | ProjectService.closeProject | Block if WBS has IN_PROGRESS nodes |
| MED-003 | No companyId FK validation | ProjectService.createProject | Validate against Organization.Company |
| MED-004 | No ownerPartyId FK validation | ProjectService.createProject | Validate against Party |
| MED-005 | System user fallback | getCurrentUser() methods | Should never return "system" in production |
| MED-006 | `System.currentTimeMillis()` | Entity @PrePersist | Should use `Instant.now().toEpochMilli()` for consistency |

### 4.3 Frontend Issues

| ID | Issue | File | Fix |
|----|-------|------|-----|
| FE-001 | Emoji in templates | projects.page.html | Replace with proper CSS icons/SVG |
| FE-002 | Missing project-detail.spec.ts | pages/ | Add unit tests |
| FE-003 | No timezone handling | formatDate() | Use `hr.company-zone` config |
| FE-004 | Using CommonModule | projects.page.ts | Remove — use `@if`/`@for` only |

---

## 5. Implementation Patterns

### 5.1 Creating a New Backend Module

```java
// 1. Domain entity (JPA)
@Entity
@Table(name = "new_entities")
public class NewEntity {
    @Id private String id;
    @TenantId @Column(name = "app_id") private String appId;
    @Version private long version;
    @Column(name = "created_at") private long createdAt;
    @Column(name = "updated_at") private long updatedAt;
    // business fields...
}

// 2. Repository (Spring Data)
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, String> {
    List<NewEntity> findByAppIdOrderByCreatedAtDesc(String appId);
}

// 3. Service
@Service @Transactional(readOnly = true)
public class NewEntityService {
    private final NewEntityRepository repo;
    private final AuditService audit;
    
    @Transactional
    public NewEntityResponse create(CreateRequest req) {
        // validate, save, audit
    }
}

// 4. Controller
@RestController @RequestMapping("/api/v1/new-entities")
public class NewEntityController {
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', ...)")
    @PostMapping
    public NewEntityResponse create(@Valid @RequestBody CreateRequest req) {
        return service.create(req);
    }
}
```

### 5.2 Adding Liquibase Migration

```yaml
# 1. Schema: db/changelog/schema/create/YYYYMMDD_vNNN_feature_schema.yaml
databaseChangeLog:
  - changeSet:
      id: YYYYMMDD-vNNN-feature-schema
      author: bemo-erp
      changes:
        - createTable: { ... }
        - addUniqueConstraint: { ... }
        - createIndex: { ... }

# 2. Translations: db/changelog/data/insert/YYYYMMDD_vNNN_feature_translations.yaml
databaseChangeLog:
  - changeSet:
      id: YYYYMMDD-vNNN-feature-translations
      author: bemo-erp
      changes:
        - loadData:
            file: db/changelog/data/insert/files/YYYYMMDD_vNNN_feature_translations.csv
```

**CSV format** (semicolon-separated):
```csv
id;translation_key;locale;text_value
vNNN-key-001;projects.myKey;ar-EG;النص بالعربي
vNNN-key-002;projects.myKey;en-US;English text
```

### 5.3 Adding i18n Keys (Frontend)

1. Add `i18n.t('feature.key')` in template/TS
2. Add DB rows via Liquibase CSV (both `ar-EG` and `en-US`)
3. Add fallback in `i18n.service.ts` DEFAULT_FALLBACKS
4. Run `npm run check:i18n` — must pass

### 5.4 Writing Tests

```java
// Backend: Unit test (no Spring context)
class MyServiceTests {
    private MyRepository repo = mock(MyRepository.class);
    private AuditService audit = mock(AuditService.class);
    private MyService service = new MyService(repo, audit);
    
    @Test
    void create_succeeds_whenValid() { ... }
    @Test
    void create_throws_whenDuplicate() { ... }
}

// Backend: Integration test (needs Docker for Postgres)
@PostgresIntegrationTest
class MyServiceIntegrationTests {
    @Autowired private MyService service;
    @Test void tenantIsolation_blocksCrossTenantAccess() { ... }
}
```

```typescript
// Frontend: Component test
describe('MyPage', () => {
  let httpMock: HttpTestingController;
  
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyPage],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (k: string) => k } },
      ]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  it('should load data', () => {
    const fixture = TestBed.createComponent(MyPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/my-endpoint').flush([]);
  });
});
```

---

## 6. Menu Registration Protocol

When adding a new sidebar menu item, enforce this 4-part sync:

1. **Frontend (`app-shell.component.ts`):** Register in `items` array + `visible()` + `hasMenuAccess()`
2. **DB Translations (`i18n.service.ts` + CSV):** Add workspace section + nav label keys
3. **DB User Schema (Liquibase):** UPDATE `app_users.allowed_menus` to append new menu IDs
4. **User Management UI (`users.page.ts`):** Add new menu IDs to `menuOptions`

---

## 7. Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Using `findAll()` for summaries | Use repository aggregate queries |
| Hardcoded English in exceptions | Use i18n keys with DB translation rows |
| String concatenation for JSON audit | Use Jackson ObjectMapper |
| `System.currentTimeMillis()` | Use `Instant.now().toEpochMilli()` |
| `CommonModule` in Angular | Use `@if`/`@for` built-in control flow |
| Emoji in templates | Use CSS class-based icons |
| `localStorage` for auth tokens | Keep tokens in memory only (existing pattern) |
| Creating parallel approval subsystems | Reuse `ApprovalWorkflowService` |
| Creating parallel accounting ledgers | Post through existing Finance GL |
| Exposing JPA entities from controllers | Use API records/DTOs |

---

## 8. Session History

| Session | Date | Features Delivered |
|---------|------|--------------------|
| 1-16 | Jul 29 – Aug 9 | Core ERP: Attendance, Payroll, Procurement, Workforce, Finance, Approval, etc. |
| 17 | Aug 19-20 | All 30 audit roadmap features scaffolded (V269-V330) |

**Current session:** Quality remediation of P0.1 Project/WBS kernel + enhanced documentation.

---

## 9. References

- `docs/ROADMAP_STATUS.md` — Current status of all 30 features
- `docs/TEST_EVIDENCE.md` — Quality gate evidence
- `docs/BEMO_ROADMAP_IMPLEMENTATION_STATUS.md` — Central tracker
- `AGENTS.md` — Session history
- `be/skills/hr-backend/SKILL.md` — Backend coding rules
- `fe/skills/hr-frontend/SKILL.md` — Frontend coding rules
