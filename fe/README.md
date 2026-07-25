# Frontend — HR & Factory Operations Web Application (`fe/`)

Angular 22 standalone application built with signals, typed reactive forms, SCSS, Arabic RTL defaults, and a token-based design system. Zero external heavy UI component libraries.

---

## 🏢 Business Features & UI/UX Architecture

1. **Enterprise Attendance Review Workspace**:
   - Visual progress bar showing reviewed vs. total employee records (`180 / 248 Reviewed — 72%`).
   - Executive KPI cards (`Pending Review`, `Green Tier`, `Yellow Tier`, `Red Tier`).
   - Highlighted Smart AI Recommendation Box with 1-click single-punch approvals.
   - Analytics filter pills with color indicators and real-time count badges.
   - Side accent color stripes per table row (`🟢 Green`, `🟡 Yellow`, `🔴 Red`).
   - Expandable detail sub-rows displaying raw punches, expected/actual times, late/overtime minutes, and evidence logs.

2. **Factory Operations & Dynamic Item Types ("نوع الصنف")**:
   - Form controls for raw materials, packaging, production supplies, sorting outputs, finished goods, spare parts, chemicals, fuel/oils, and **custom Item Types ("نوع صنف جديد / مخصص")**.

3. **Toast Notification System**:
   - Signal-based non-disruptive floating toasts (`NotificationService` & `ToastContainerComponent`) with automatic timers and manual close.

4. **Design System & Theme Engine**:
   - Token-based design system supporting **Dark Mode** (`#0B0F14`) and **Light Mode** (`#F1F5F9`) with WCAG AA contrast and smooth micro-interactions.

5. **Per-User Menu Authorization**:
   - `AuthService.hasMenuAccess(menuId)` enforces dynamic menu navigation visibility for administrator-assigned menus (`dashboard`, `employees`, `categories`, `reports`, `imports`, `parties`, `operations`, `users`, `settings`).

---

## 🇸🇦 مميزات واجهة المستخدم باللغة العربية (Arabic Summary)

- **شاشة مراجعة الحضور والانصراف المتطورة**:
  - مؤشر نسبة المراجعة مع أشرطة التقدم الملونة.
  - كروت KPI تنشيدية، توجيه الذكاء الاصطناعي لاختصار وقت المراجعة، وتبويب الفئات الخضراء والصفراء والحمراء.
  - خطوط تمييز ملونة بجانب كل صف مع إمكانية فتح التفاصيل التوضيحية لكل موظف.
- **إشعارات Toast العائمة**:
  - إشعارات تفاعلية لحفظ البيانات والعمليات دون تغيير هيدر أو شكل الصفحة.
- **تصدير Excel وتخصيص المظهر**:
  - تصدير بضغطة زر وتغيير المظهر بين الداكن والفاتح بسلاسة.

---

## Command Reference

```powershell
# Start Angular development server
npm start

# Run i18n translation validation check
npm run check:i18n

# Build production bundle
npm run build
```
