# Executive Analytics Center (مركز التحليلات التنفيذية)

**EN:** Executive multi-project/portfolio analytics (roadmap item 26, V319–V321): the enterprise KPI registry plus the executive dashboard aggregating cross-module KPIs (projects portfolio, finance, workforce, sales) into one screen for owners. `executive-analytics.*` consumes registry-defined KPIs — definitions live backend-side so thresholds/targets are data, not code. Complements the per-feature dashboard widgets (dashboard feature owns saved widget preferences).

**AR:** مركز التحليلات التنفيذية (البند 26، ترحيلات V319–V321): سجل مؤشرات الأداء للمؤسسة ولوحة تنفيذية تجمّع مؤشرات الوحدات (محفظة المشاريع، المالية، القوى العاملة، المبيعات) في شاشة واحدة للمالك. تعريف المؤشرات وحدوده أرقام وإعدادات في الخادم لا شيفرة. تكمل أدوات لوحة القيادة لكل ميزة التي تملك تفضيلات الأدوات المحفوظة.

- Files: models/service/page/spec under `executive/`.
- Export: follows dashboard Excel-export conventions (Arabic filenames).
