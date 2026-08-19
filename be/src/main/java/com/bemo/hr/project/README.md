# حزمة إدارة المشاريع وهيكل العمل (Project & WBS Kernel)

---

## English Summary

The `com.bemo.hr.project` package forms the core **Project / Construction Kernel** of Bemo ERP. It introduces project master data, hierarchical Work Breakdown Structure (WBS / BOQ), project cost codes, and project party roles without cloning or replacing existing ERP subledgers (Finance GL, Procurement, Inventory, Workforce, Payroll).

### Key Components:
- **Project Aggregate**: Tracks project lifecycle (`DRAFT` → `ACTIVE` → `ON_HOLD` → `COMPLETED` → `CLOSED`), contract details, site location, owner party, manager, and budget-blocking rules.
- **Hierarchical WBS / BOQ Node (`WbsNode`)**: Supports multi-level parent-child tree, deterministic code and path generation, cycle detection prevention, planned quantities/rates, and automatic BOQ planned amount computation.
- **Project Cost Code (`ProjectCostCode`)**: Master library of standardized cost categories (`LABOR`, `MATERIAL`, `EQUIPMENT`, `SUBCONTRACTOR`, `OVERHEAD`).
- **Project Party Roles (`ProjectPartyRole`)**: Stakeholder role assignments (`CLIENT_OWNER`, `MAIN_CONTRACTOR`, `SUBCONTRACTOR`, `CONSULTANT`, `SUPPLIER`).
- **REST APIs**: `/api/v1/projects`, `/api/v1/projects/{projectId}/wbs`, and `/api/v1/projects/cost-codes` secured by `@PreAuthorize` role checks.

---

## ملخص باللغة العربية

تشكل حزمة `com.bemo.hr.project` النواة المركزية لنظام **إدارة المشاريع والإنشاءات (Project & Construction Backbone)** في منصة بيمو ERP. تتيح هذه الحزمة إدارة سجل المشاريع الكامل، الهيكل التكراري لتجزئة العمل وجداول الكميات والمقايسات (WBS / BOQ)، دليل أكواد التكلفة للمشاريع، وأطراف المشروع دون استنساخ دفاتر الأستاذ المالية أو دورات المخزون والمشتريات والرواتب القائمة.

### المكونات الرئيسية:
- **مجمع المشروع (Project Aggregate)**: إدارة دورة حياة المشروع (`مسودة` ← `نشط` ← `متوقف مؤقتاً` ← `مكتمل` ← `مغلق`)، بيانات العقد، الموقع الجغرافي، المالك/العميل، مدير المشروع، وقواعد الإلزام بالميزانية.
- **عقدة هيكل العمل والمقايسة (`WbsNode`)**: دعم التدرج الشجري متعدد المستويات، التوليد الحتمي للمسار والكود، منع الحلقات الدائرية (Cycle Detection)، الكميات والأسعار المخططة، وحساب إجمالي مبالغ المقايسة تلقائياً.
- **أكواد التكلفة للمشاريع (`ProjectCostCode`)**: دليل مركزي لتصنيفات التكلفة القياسية (عمالة، مواد، معدات، مقاولي باطن، مصاريف موقع).
- **أدوار أطراف المشروع (`ProjectPartyRole`)**: تعيين الجهات والشركاء في المشروع (مالك، مقاول رئيسي، مقاول باطن، استشاري، مورد).
- **واجهات البرمجة REST**: توفير واجهات `/api/v1/projects` و`/api/v1/projects/{projectId}/wbs` و`/api/v1/projects/cost-codes` مؤمنة بصلاحيات `@PreAuthorize`.
