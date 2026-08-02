# Bemo ERP (نظام بيمو المتكامل لإدارة الأعمال)

An enterprise, multi-tenant HR attendance, shift scheduling, inventory, and factory operations platform built for companies, factories (fruit/orange processing & packing), and industrial enterprises replacing manual paper notes and Excel spreadsheets.

The repository contains `be/` (Spring Boot 4.1 backend with bundled Angular UI), `fe/` (Angular 22 standalone frontend), `desktop/` (Tauri Windows desktop package), and `license-app/` (Ed25519 licensing service).

---

## 🏢 Comprehensive Business Specification & Factory Domain Rules

### 1. Multi-Tier Worker Classifications & Pay Cycles
- **Daily-Wage Workers (`عمال باليومية / طياري`)**: Calculated, reviewed, and paid every 15 days (`HALF_MONTHLY` pay-cycle preset).
- **Fixed Staff (`عمالة ثابتة`)**: Monthly or 30-day pay cycles categorized by department shift hours:
  - **Administrators (`إداريين`)**: 8-hour daily shifts.
  - **Security Staff (`أمن`)**: 12-hour daily shifts.
  - **Accountants (`محاسبين`)**: 10-hour daily shifts.

### 2. Seasonal Shift Configurations & Intra-Month Rules
- **Summer vs. Winter Shifts**: System allows dynamic configuration of attendance start times (e.g., Summer shifts starting at 08:00 AM, Winter shifts starting at 09:00 AM).
- **Intra-Month Effective Schedules**: Supports mid-month shift changes (e.g., July 1–12 at 07:00 AM, July 13–31 at 09:00 AM) using date-effective `ScheduleRule` records.

### 3. Biometric Device Engine & Single-Punch Rules
- Multi-format biometric file parser (`CSV`, `TXT`, `XLS`, `XLSX`) with SHA-256 evidence hashing and unmatched fingerprint identity resolution.
- **Single-Punch Logic**: Automatically groups employees who punched once in a day (e.g., 3 single-punch days in a month) into reviewable categories, allowing HR to approve single punches as normal days or deduct accordingly.

### 4. Machine Errors, Power Outage & Mass Disruption Detector
- Automatically detects when >50% of employees in a category lack punch records on a workday (e.g., machine offline, power outage, factory-wide disruption).
- System prompts HR reviewers during report generation to confirm whether the day was an **Excused Presence (حضور معفي/يوم طبيعي)** or **Unexcused Absence (غياب)**, recording confirmed holidays in the database so the question is never repeated.

### 5. Rule-Based Attendance Health Categories & Bulk HR Toolbar
- 🟢 **Green Tier (Clean / سليمة)**: 100% clean attendance without warnings or missing punches.
- 🟡 **Yellow Tier (Single Punch / Grace / بصمة واحدة)**: Single punch records or grace-period arrivals.
- 🔴 **Red Tier (Critical Exceptions / أخطاء وغياب)**: Missing punch records (`NO_PUNCH`), manual entry requirements, or unexcused absences.
- **Bulk HR Decision Toolbar**: 1-click execution to approve all single-punch days as normal or deduct missing days across filtered health tiers.

### 6. Granular RBAC & Menu Authorization
- Administrators assign custom roles (`ADMIN`, `HR_MANAGER`, `HR_REVIEWER`, `SUPERVISOR`, `DATA_ENTRY`) and toggle access per menu (`dashboard`, `employees`, `categories`, `reports`, `imports`, `parties`, `operations`, `users`, `settings`).

### 7. Executive & Feature-Specific Dashboards
- **Executive Operations Hub**: 4-quadrant breakdown of Attendance & Discipline, Warehouse & Material Valuation, Orange Processing Output & Sorting Waste, and Sales/Export Collections with clickable drill-down metrics.

### 8. Native Multi-Sheet Excel Exports
- Export complete raw data or specific analytical reports styled with user theme preferences (`GOLD`, `BLUE`, `GREEN`, `GRAY`).

---

## 🇸🇦 الشروط والوصوف الفنية باللغة العربية (Business Requirements in Arabic)

نظام متكامل لإدارة المصانع وشركات محطات الموالح والمحاصيل:

1. **فئات العمالة وتوقيتات الشفتات**:
   - عمالة يومية (طياري): دورة حساب كل 15 يوماً.
   - عمالة ثابتة: شفتات مخصصة (إداريين 8 ساعات، أمن 12 ساعة، محاسبين 10 ساعات).
   - مواعيد صيفية (8 صباحاً) وشتوية (9 صباحاً) قابلة للتعديل ديناميكياً.

2. **معالجة جهاز البصمة والبصمة الواحدة**:
   - ربط ملفات جهاز البصمة برقم البصمة والفئة الوظيفية.
   - تجميع حالات البصمة الواحدة (مثل 3 أيام بصمة واحدة) في الفئة الصفراء لاتخاذ قرار جماعي باعتمادها كـ يوم طبيعي أو خصمها.

3. **كاشف انقطاع الكهرباء والإجازات الجماعية**:
   - اكتشاف تلقائي لانقطاع الكهرباء أو تعطل جهاز البصمة عند غياب البصمة لأكثر من 50% من فئة العمل.
   - اقتراح اعتماد اليوم كـ يوم طبيعي أو إجازة رسمية وتسجيلها بالدليل في قاعدة البيانات حتى لا يُسأل المستخدم عنها مجدداً.

4. **الفئات الخضراء والصفراء والحمراء**:
   - 🟢 الخضراء: تسجيل كامل بدون أخطاء.
   - 🟡 الصفراء: بصمة واحدة أو تأخير بسيط.
   - 🔴 الحمراء: عدم وجود بصمة أو أخطاء حرجة.

5. **إدراج الصلاحيات وتصدير Excel**:
   - تحديد القوائم المسموح بها لكل مستخدم (`dashboard`, `employees`, `categories`, `reports`, `imports`, `parties`, `operations`, `users`, `settings`).
   - تصدير كافة التقارير وشاشات المعاينة إلى شيتات Excel منسقة طبقاً لتنسيق المظهر المفضل للمستخدم.

---

## 🛠️ Local Development & Quick Start

### Prerequisites
- Java 26
- Node 24.18+ / npm 11+
- PostgreSQL 18.4 (`jdbc:postgresql://localhost:5432/bemo_erp`, user `root`, password `root`)

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

## 🐳 Production Docker Deployment

The application includes full multi-container Docker support with **PostgreSQL 17**, **Spring Boot Backend**, and **Angular Nginx Web Server**.

### 1. One-Click Deployment Scripts

- **Windows**: Double-click `docker-deploy.bat` or run:
  ```cmd
  docker-deploy.bat
  ```
- **Linux / macOS**: Run:
  ```bash
  chmod +x docker-deploy.sh
  ./docker-deploy.sh
  ```

### 2. Manual Docker Compose Execution

```bash
# 1. Copy the development environment template
cp .env.development.example .env

# 2. Build and start containers in background
docker compose up --build -d

# 3. View container logs
docker compose logs -f
```

For production, copy `.env.production.example` to `.env`, replace every
`CHANGE_ME` placeholder with a real secret, and start the overlay:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 3. Service Access & Endpoints

| Service | Port | Description |
| :--- | :--- | :--- |
| **Angular Web App** | `http://localhost:80` | Nginx web server with HTML5 routing & API reverse proxy |
| **Spring Boot API** | `http://localhost:8080` | REST API, OAuth2 Security, Actuator `/actuator/health` |
| **PostgreSQL DB** | `localhost:5432` | Enterprise database with volume persistence (`postgres_data`) |

### Default Credentials
- **Company Code**: `DEMO`
- **Username**: `admin`
- **Password**: `Admin@12345`
