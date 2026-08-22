# BUSINESS_README.md — Bemo ERP (نظام بيقين لإدارة الموارد والمشاريع)
*Bilingual Business & Product Reference Guide / الدليل المرجعي التجاري والمنتجي للنظام*

---

## 🌍 Executive Summary / الملخص التنفيذي

**Bemo ERP** هو نظام شامل لإدارة الموارد والتخطيط المؤسسي (Enterprise Resource Planning) مصمم خصيصاً للشركات والمؤسسات بالشرق الأوسط وشمال إفريقيا (MENA)، مع تركيز متميز على قطاعات المقاولات والإنشاءات (Contracting & Construction)، التصنيع (Manufacturing)، التجارة والتوزيع (Trade & Distribution)، والخدمات.

يوفر النظام بيئة **Multi-Tenant SaaS** متكاملة، آمنة ومبنية بـ **Micro-services / Modular Monolith Architecture** تضمن العزل الكامل لبيانات العملاء، وتعتمد نظام التحكم بالصلاحيات القائم على السياسات الديناميكية (**Policy-Based Dynamic Access Control - PBAC**).

---

## 1. Product Overview & Vision / نظرة عامة ورؤية المنتج

### English Overview
Bemo ERP is an enterprise-grade SaaS operations platform designed to bridge the gap between financial compliance, operational execution, contractor workforce logistics, and multi-vertical business processes. It unifies HR & Statutory Payroll, Project WBS & Contracting, Procurement 3-Way Matching, General Ledger & Multi-Dimensional Accounting, Manufacturing WIP & BOMs, Omnichannel CRM/POS, and ETA E-Invoicing.

### Arabic Overview / الرؤية بالعربية
نظام **Bemo ERP** يقدم حلاً استراتيجياً يربط الحوكمة المالية بالعمليات التشغيلية الميدانية. يهدف المنتج إلى تمكين الشركات من إدارة دورة العمل الكاملة من المناقصات والمشتروات وحتى المحاسبة التحليلية، إدارة العمالة المستأجرة والمقاولين، والالتزام الضريبي (الفاتورة والإيصال الإلكتروني ETA).

### Product Vision / رؤية المنتج
> *"To be the definitive, compliant, and localized Cloud ERP for MENA enterprises that seamlessly connects field operations, supply chains, workforce logistics, and multi-dimensional financial intelligence."*

---

## 2. Target Customers & Personas / العملاء المستهدفون وشخصيات المستخدمين

| Persona / الشخصية | Primary Goals / الأهداف الأساسية | Key Pain Points / المشاكل الحالية | Core Modules Used / الوظائف المستخدمة |
|---|---|---|---|
| **CFO / Finance Director**<br> المدير المالي | الحوكمة المالية، دقة التدفقات النقدية، الميزانيات، والالتزام الضريبي (ETA) | تشتت البيانات، عدم الربط بين المشتريات والمخازن والمالية، تأخر الإغلاق المالي | GL, Treasury, AR/AP, Budget Control, Financial Statements, ETA Compliance |
| **Project / Site Manager**<br> مدير المشروع والموقع | ضبط جدول WBS، تتبع DPR، مراقبة تكلفة المستخلصات IPC وحسابات المقاولين | تأخر التقارير الميدانية، عدم مطابقة نسب الإنجاز للتكاليف الفعلية | Projects, WBS, BOQ, DPR, IPC Claims, Contractor Workforce |
| **HR & Payroll Manager**<br> مدير الموارد البشرية | حساب الأجور المعقدة، البصمة، الإجازات، والامتثال لقوانين العمل والضرائب | بصمات متعددة الفروع، تباين ورديات العمالة والخصومات، التعقيد الضريبي | Attendance, Biometric Hub, Payroll, Leaves, Performance Appraisals |
| **Procurement & Inventory Manager**<br> مدير المشتريات والمخازن | المطابقة الثلاثية 3-Way Match، تقييم المخزون FIFO/AVCO، تتبع الموردين | انحرافات الأسعار والكميات، العجز المخزوني، نقص تتبع دفعات التوريد | Procurement, Goods Receipts, Inventory Analytics, Supplier 360 |
| **Operations & Plant Manager**<br> مدير الإنتاج والعمليات | تخطيط أوامر الشغل، حساب تكلفة المنتجات WIP، تتبع الهالك ومستلزمات الإنتاج | عدم وضوح تكلفة التأسيس الحقيقية، الهدر، صيانة وأعطال خطوط الإنتاج | BOM, Work Orders, Routings, Quality Control, Maintenance |

---

## 3. Existing Capabilities & Architecture / الإمكانيات والمهام الحالية

1. **Flexible Authorization & PBAC (`@auth`)**:
   - 200+ granular permissions across modules with dynamic SpEL evaluation (`@auth.hasPermission('...')`).
   - Branch & Cost-Center scoped data filtering.
   - Preset tiers: View-Only, Manage, Approver, Full Control.
2. **Dynamic Business Vertical Setup**:
   - Auto-provisioning of feature flags and policy groups for 6 verticals: `GENERAL`, `MEDICAL`, `CIVIL`, `RETAIL`, `MANUFACTURING`, `SERVICES`.
3. **Contractor Workforce & Settlement Lifecycle**:
   - Labor Requests → Worker Assignment → Attendance Lock → 15-Day Settlements → Partner Ledger & Financial Posting.
4. **Procurement 3-Way Matching & Budget Encumbrance**:
   - Matching PO / GRN / Supplier Invoice with tolerance limits.
   - Budget encumbrance & liquidation engine.
5. **Multi-Protocol Biometric Device Hub**:
   - Direct integration supporting 8+ vendor protocols with `X-Device-Hub-Key`.
6. **Multi-Currency & Online Exchange Rates**:
   - Frankfurter API scheduler for daily reference rate fetching and variance analysis.
7. **Comprehensive Audit Trail & Action Center**:
   - Append-only audit logs with break-glass justification and real-time business notification center.

---

## 4. Gap Analysis (Business, Market, Competitor, UX) / تحليل الفجوات الشامل

### 4.1 Missing Business Capabilities / الفجوات التجارية
- **Automated Customer Credit Limits Enforcement in Sales**:
  - *Current state*: Credit limits are stored on AR customer profiles, but Sales Orders do not automatically block or require manager approval when exceeding credit allowance.
- **Project Earned Value Analysis (EVM - SPI/CPI) Real-Time Engine**:
  - *Current state*: WBS cost budgets and DPR field logs exist, but real-time Planned Value (PV), Earned Value (EV), Schedule Performance Index (SPI), and Cost Performance Index (CPI) metrics are not dynamically computed in executive dashboards.
- **Advanced Procurement Requisition (PR to PO) Lifecycle**:
  - *Current state*: Direct Purchase Orders exist, but internal department Purchase Requisitions (PR) with approval workflows prior to PO creation are missing in default flows.

### 4.2 Competitor & Market Gaps / الفجوات مقارنة بالسوق والمنافسين
- **Competitors (Odoo, SAP Business One, Oracle NetSuite)**:
  - Offer integrated Customer Credit Holds in Sales, automated Purchase Requisitions (PR), and EVM (Earned Value Management) indicators for contracting.
- **Opportunity for Bemo ERP**:
  - Implement dynamic Sales Order Credit Validation with Approval Escalation, automated Purchase Requisition (PR) to PO conversion, and real-time EVM metrics (SPI/CPI/EAC) on Project Dashboards.

---

## 5. Prioritized Business Enhancement Roadmap / خريطة طريق التحسينات التجارية

| Priority | Enhancement Module / التحسين المطلوب | Business Impact / الأثر التجاري | Target Customer / العميل المستهدف | Implementation Complexity |
|---|---|---|---|---|
| **P0 (Critical)** | **Sales Credit Limit & Hold Automation**<br>(التحكم التلقائي في الائتمان وحظر أوامر البيع) | منع المخاطر الائتمانية والديون المعدومة عبر إيقاف طلبات البيع للعملاء المتجاوزين لحساب الائتمان تلقائياً. | Sales, AR, Finance Directors | Medium (BE/FE) |
| **P0 (Critical)** | **Purchase Requisition (PR) to PO Workflow Engine**<br>(دورة طلبيات الشراء الداخلية والتحويل لأوامر شراء) | تعزيز ضبط المصروفات والميزانيات من خلال إلزام الأقسام الداخلي بطلب توريد معتمد قبل إصدار أمر شراء خاري. | Procurement, Department Heads | Medium (BE/FE) |
| **P1 (High)** | **Contracting EVM Analytics Engine (SPI / CPI / EAC)**<br>(مؤشرات القيمة المكتسبة وتنبؤات تكلفة التأسيس للمشاريع) | تزويد الإدارة العليا برؤية فورية عن أداء المشاريع مالياً وزمنياً وفق معايير PMI الإدارية. | Project Managers, Executives | Medium (BE/FE) |

---

## 6. Success Metrics & KPIs / معايير النجاح ومؤشرات الأداء

1. **Zero Financial Leakage**: 100% of Sales Orders exceeding customer credit limits are held for credit manager review.
2. **Budget Control Compliance**: 100% of unbudgeted procurement passes through Purchase Requisitions approval.
3. **Project Predictability**: Real-time SPI and CPI indicators available for 100% of active WBS projects.
4. **Quality Gates**:
   - 100% test pass rate across JUnit and Angular Vitest suites.
   - 0 hardcoded strings across all HTML templates and TypeScript files.
   - Complete i18n key coverage in `ar-EG` and `en-US`.

---

## 7. Current Limitations & Future Opportunities / القيود الحالية والفرص المستقبلية

- **Current Limitation**: Native Mobile application is delivered via Responsive Web/PWA; dedicated offline iOS/Android native binaries are planned for future releases.
- **Future Opportunity**: AI-driven cash flow forecasting and automatic invoice OCR extraction for supplier bills.
