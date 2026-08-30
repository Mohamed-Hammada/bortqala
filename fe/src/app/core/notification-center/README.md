# Core — Notification Center (مركز الإشعارات)

**EN:** Business notification center powering the 🔔 navbar button and action-center panel: loads backend-ranked exception cards (`/api/v1/notifications`) with priority scores, impact/reason/recommendation copy and role targeting, exposes `unreadCount()` + mark-as-read/mark-all-read, and coordinates with Web Push. `web-push.service.ts` implements the full VAPID lifecycle: config discovery, per-device subscription registration with locale+preference scoping (pushApprovals/pushPayroll), test send, detach-on-logout, detach-all-on-session-revoke, and subscription-change re-registration.

**AR:** مركز إشعارات الأعمال الذي يشغّل زر الجرس في الشريط ولوحة مركز الإجراءات: يحمّل بطاقات الاستثناءات مرتبةً من الخادم بدرجات أولوية ونصوص الأثر والسبب والتوصية واستهداف الأدوار، ويوفر عدد غير المقروء وقراءة الفردية والجماعية، وينسق مع الإشعارات الفورية. خدمة الدفع الفوري تنفذ دورة VAPID كاملة: اكتشاف الإعداد، تسجيل اشتراك كل جهاز بلغة المستخدم وتفضيلاته، إرسال تجريبي، الفصل عند الخروج، فصل كل الأجهزة عند سحب الجلسات، وإعادة التسجيل عند تغير الاشتراك.

- First-login enable prompt = WP-09 (implementation guide); infra here is complete.
- All copy via `i18n.t`; unread badge lives in `app-shell.component.html`.
