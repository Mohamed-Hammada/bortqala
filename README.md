# Bemo HR & Operations Platform

An enterprise, multi-tenant HR attendance, shift scheduling, and company operations system built for businesses replacing manual paper notes and spreadsheets. The repository contains `be/` (Spring Boot 4.1 backend with bundled Angular UI), `fe/` (Angular 22 standalone frontend), `desktop/` (Tauri Windows desktop package), and `license-app/` (Ed25519 licensing service).

---

## Business Architecture & Core Capabilities

1. **Multi-Tenant SaaS Security & Granular Authorization**:
   - Isolation by `app_id` across all business entities.
   - JWT authentication with administrator-controlled session lifetime (5 minutes to 7 days).
   - Granular per-user menu authorization allowing administrators to assign allowed navigation menus (`dashboard`, `employees`, `categories`, `reports`, `imports`, `parties`, `operations`, `users`, `settings`) to individual users.

2. **Attendance Categories & Biometric Engine**:
   - Dynamic attendance categories (`BIOMETRIC`, `MANUAL`, `HYBRID`) supporting monthly, 15-day (half-monthly), or 30-day pay cycles.
   - Single-punch policy enforcement, expected daily minutes, and category-prefixed employee code generation (`<CATEGORY>-<SUFFIX>` or locked sequence).
   - Multi-format biometric imports (CSV, TXT, XLS, XLSX) with SHA-256 evidence hashing, row error logging, and unmatched device identity mapping.

3. **Intra-Month Effective Schedules & Power Outage Detection**:
   - Date-effective shift schedules supporting intra-month changes (e.g., July 1–12 at 07:00 vs. July 13–31 at 09:00).
   - **Power Outage / Mass Absence Detector**: Automatically detects when >50% of employees in a category lack punches on a workday and prompts HR reviewers to confirm Excused Presence vs Absence.

4. **Rule-Based Attendance Health Categories & Bulk HR Decisions**:
   - 🟢 **Green Tier (Clean)**: 100% clean attendance without warnings or missing punches.
   - 🟡 **Yellow Tier (Single Punch / Grace)**: Single punch days or minor grace period arrivals.
   - 🔴 **Red Tier (Critical Exceptions)**: Missing punches (`NO_PUNCH`), manual entry requirements, or unexcused absences.
   - **Bulk Decisions Toolbar**: Allows HR managers to approve all single-punch days or deduct missing days across filtered health tiers in 1 click.

5. **Operations, Inventory, Balances & Localized Excel Exports**:
   - Partner management (suppliers, customers, traders, farms), inventory stock movements, partner financial ledgers, processing loss percentages, and category-controlled employee advances.
   - Multi-sheet native Excel exports styled according to user preferences (`GOLD`, `BLUE`, `GREEN`, `GRAY`).

6. **Design System & Accessible Theme Engine**:
   - Token-based design system supporting true **Dark Mode** (`#0B0F14` canvas, `#141A22` cards, `#1B2430` elevated surfaces) and **Light Mode** (`#F1F5F9` canvas, `#FFFFFF` cards, `#F8FAFC` inputs).
   - High-contrast WCAG AA typography, gold action accents (`#D4A017`), responsive 2-column settings grid, and smooth micro-interactions.

---

## Local Development & Quick Start

### Prerequisites
- Java 26
- Node 24.18+ / npm 11+
- PostgreSQL 18.4 (`jdbc:postgresql://localhost:5432/hr_platform`, user `root`, password `root`)

### Run Backend & Frontend

```powershell
# 1. Start Backend (PostgreSQL Dev Profile)
cd be
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# 2. Start Frontend
cd ..\fe
npm install
npm start
```

Open `http://localhost:4200` in your browser. Default development credentials:
- **Company Code**: `DEMO`
- **Username**: `admin`
- **Password**: `Admin@12345`

### Run Verification & Tests

```powershell
# Backend Unit & Integration Tests
cd be
.\gradlew.bat clean test

# Frontend i18n & Build Check
cd ..\fe
npm run check:i18n
npm run build
```

---

## العربية — متطلبات العمل والهيكلية

نظام متكامل لإدارة الحضور والانصراف، وجداول العمل الشفتات، والعمليات التجارية والمالية والمخزون للشركات والمصانع:

1. **إدارة الصلاحيات وقوائم المستخدمين (Multi-Tenant SaaS)**:
   - عزل كامل لبيانات كل شركة عبر `app_id`.
   - صلاحيات مرنة لكل مستخدم تحدد القوائم المسموح بها (`dashboard`, `employees`, `categories`, `reports`, `imports`, `parties`, `operations`, `users`, `settings`).

2. **محرك الحضور والبصمة وفئات العمل**:
   - فئات عمل مرنة (بصمة، يدوي، مختلط) بدورة استحقاق شهرية أو نصف شهرية.
   - استيراد ملفات البصمة بأكثر من صيغة (CSV, TXT, XLS, XLSX) مع منع التكرار وحفظ أدلة البصمة الأصلية.

3. **جداول متغيرة وكشف انقطاع الكهرباء**:
   - مواعيد حضور متغيرة خلال نفس الشهر (مثل 1–12 يوليو الساعة 7، وبقية الشهر الساعة 9).
   - **كاشف انقطاع الكهرباء / الأعطال**: تنبيه تلقائي عند غياب البصمة لأكثر من 50% من فئة العمل في يوم واحد لاقتراح اعتمادها كـ إجازة مؤكدة أو حضور معفي.

4. **تصنيف حالات التقرير وإجراءات HR الجماعية**:
   - 🟢 **الفئة الخضراء (سليمة)**: حضور كامل بدون تأخير أو أخطاء.
   - 🟡 **الفئة الصفراء (بصمة واحدة)**: تسجيل بصمة واحدة فقط أو تأخير بسيط.
   - 🔴 **الفئة الحمراء (أخطاء/غياب)**: عدم وجود بصمة أو الحاجة لتأكيد يدوي.
   - **أزرار القرار الجماعي**: إمكانية اعتماد جميع حالات البصمة الواحدة كـ يوم طبيعي أو خصم الغياب بضغطة زر واحدة.

5. **العمليات التجارية والسُلف وتصدير Excel**:
   - إدارة الأطراف (موردين، عملاء، تجار فرزة)، المخزون، السُلف، وحسابات الأطراف المالية.
   - تصدير تقارير Excel منسقة بلغة المستخدم والشكل المختار (ذهبي، أزرق، أخضر، رمادي).

6. **نظام المظهر والتباين العالي**:
   - دعم كامل للوضع الداكن (`Dark Mode`) والوضع الفاتح (`Light Mode`) بتباين عالي متوافق مع معايير WCAG وتنسيق ألوان مريح للعين.
