# Core — Global Loading (التحميل العام)

**EN:** App-wide loading orchestration for the shell: a single signal-driven indicator that reflects in-flight HTTP activity (via the HTTP interceptor layer) and long async shell initializations, so screens never need per-component spinners for global work. Respects the operations-dashboard motion rules (no decorative animation; reduced-motion friendly).

**AR:** تنسيق التحميل على مستوى التطبيق: مؤشر واحد يقوده سيجنال يعكس نشاط الشبكة الجاري عبر المعترض وأي تهيئة طويلة للهيكل، فلا تحتاج الشاشات إلى مؤشرات خاصة للأعمال العامة، مع احترام قواعد الحركة الهادئة وخصوصية تقليل الحركة.

- Consumed by `core/shell` layout; state is read-only outside this package.
- Rule: feature-local loading states stay in their own stores — only app-global work shows here.
