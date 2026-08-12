# Production execution / تنفيذ الإنتاج

**EN:** A planned production order may inspect the current BOM for pre-start readiness. Starting the order freezes one tenant-scoped requirement row per component before any stock issue. Every active-order readiness check, completion-cost calculation, and cancellation reversal reads the frozen requirement set and never the mutable BOM. Completion totals the persisted valuation evidence of the production issue movements. Snapshot uniqueness and the order version protect retries from duplicate requirements and state transitions.

**AR:** يمكن لأمر الإنتاج المخطط استخدام قائمة المواد الحالية لفحص الجاهزية قبل البدء. عند البدء تحفظ الخدمة متطلباً ثابتاً ومعزولاً للمستأجر لكل مكوّن قبل أي صرف مخزني. بعد ذلك تعتمد الجاهزية وتكلفة الإكمال وعكس الإلغاء على المتطلبات المجمدة فقط ولا تقرأ قائمة المواد القابلة للتعديل. تجمع تكلفة الإكمال أدلة تقييم حركات صرف الإنتاج المحفوظة، وتحمي فريدة اللقطة وإصدار أمر الإنتاج من تكرار المتطلبات وانتقالات الحالة.
