# CRM — Omnichannel Pipeline & Chatbot (إدارة العلاقات متعددة القنوات)

**EN:** Sales pipeline CRM with omnichannel inbox (roadmap item 24, V314–V316). Deals move through pipeline stages; conversations arrive from channels including WhatsApp and Facebook Messenger (`channelType WHATSAPP|FACEBOOK_MESSENGER` rendered as badges in `fe/src/app/features/crm`) with an inbound chatbot for first-line replies. Links deals to parties/customers so quotes (trade.sales) can follow.

**AR:** إدارة مسار البيع مع صندوق محادثات متعدد القنوات (البند 24، ترحيلات V314–V316). الصفقات تنتقل بين مراحل المسار، وتصل المحادثات من قنوات تشمل واتساب وماسنجر فيسبوك (تُعرض كشارات في واجهة CRM) مع روبوت محادثة للرد الأولي، وربط الصفقات بالعملاء لتتمكن عروض الأسعار من المتابعة.

- Layers: standard api/application/domain/infrastructure under `com.bemo.hr.crm`.
- Channel note: this is conversation *intake*; outbound HR event templates (payslip ready, loan due) remain an open roadmap item (`missing-todo.md` §13.5/§17C).
- i18n: channel names and pipeline copy ship through translation CSVs — no hardcoded strings.
