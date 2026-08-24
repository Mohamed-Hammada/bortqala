# Purchase Requests (PRQ)

WP-03: employee/department purchase requests with an approval lifecycle and one-shot
conversion into a single purchase order.

الغرض: طلبات شراء للجهات/الموظفين مع دورة اعتماد وتحويل لمرة واحدة إلى أمر شراء واحد.

## Endpoints — `/api/v1/purchase-requests`

| Method | Path | Permission | Purpose / الغرض |
|--------|------|------------|-----------------|
| GET | `?status=&departmentId=` | `isAuthenticated()` | Filter by status and/or department |
| GET | `/{id}` | `isAuthenticated()` | Single request with lines |
| POST | `` | `procurement.manage` or ADMIN/SUPER_ADMIN | Create draft (≥ 1 line) |
| PUT | `/{id}` | `procurement.manage` or ADMIN/SUPER_ADMIN | Replace lines while DRAFT only |
| POST | `/{id}/submit` | `procurement.manage` or ADMIN/SUPER_ADMIN | DRAFT → SUBMITTED |
| POST | `/{id}/approve` | `PROCUREMENT_MANAGER` or ADMIN/SUPER_ADMIN | SUBMITTED → APPROVED |
| POST | `/{id}/reject` | `PROCUREMENT_MANAGER` or ADMIN/SUPER_ADMIN | SUBMITTED → REJECTED |
| POST | `/{id}/cancel` | `procurement.manage` or ADMIN/SUPER_ADMIN | DRAFT/SUBMITTED → CANCELLED |
| POST | `/{id}/convert` `{supplierId}` | `PROCUREMENT_MANAGER` or ADMIN/SUPER_ADMIN | APPROVED → one PO |

## Status machine — دورة الحالة

```
DRAFT → SUBMITTED → APPROVED → CONVERTED
           ↘ REJECTED      ↘ (cancel blocked)
DRAFT/SUBMITTED → CANCELLED
```

Invalid transitions fail with a stable error code (`PR_INVALID_STATE`) and HTTP 409.
الانتقالات غير الصحيحة ترمز بخطأ ثابت `PR_INVALID_STATE` وكود 409.

## Conversion rules — قواعد التحويل

- Only from `APPROVED`; builds exactly ONE purchase order via
  `ProcurementService.create`, mirroring every line's remaining quantity
  (`quantity − converted_quantity`) at the estimated unit price, and passes
  `purchaseRequestId` so the previously dangling `purchase_orders.purchase_request_id`
  column is finally populated.
  التحويل من حالة APPROVED فقط؛ يبني أمر شراء واحداً يطابق الكميات المتبقية ويمرر
  `purchaseRequestId` لتعبئة عمود `purchase_orders.purchase_request_id`.
- On success all remaining quantities are marked converted, the request becomes
  `CONVERTED`, and `converted_po_id` points at the new PO. A second conversion attempt
  fails with `PR_ALREADY_CONVERTED`. Over-conversion is rejected with `PR_QUANTITY_EXCEEDED`.
  عند النجاح تُعلَّم الكميات كمحولة ويصبح الطلب CONVERTED؛ أي محاولة تحويل ثانية تفشل
  بـ `PR_ALREADY_CONVERTED`، والتحويل الزائد يُرفض بـ `PR_QUANTITY_EXCEEDED`.

## Approval-engine note — ملاحظة محرك الاعتماد

Approve/reject use direct endpoints with full audit records instead of the generic
approval engine: wiring a `PURCHASE_REQUEST` workflow type into `ApprovalWorkflowService`
would require decision callbacks that the engine does not expose yet. This is the WP-03
AC-5 documented fallback; revisit when callback hooks exist.
اعتماد/رفض الطلب يتم عبر نقاط مباشرة مع تسجيل تدقيق كامل بدلاً من محرك الاعتماد العام،
لأن ربط نوع `PURCHASE_REQUEST` يتطلب استدعاءات عكسية غير متوفرة حالياً في المحرك.

## Error codes

`PR_NOT_FOUND`, `PR_INVALID_STATE`, `PR_EMPTY_LINES`, `PR_ALREADY_CONVERTED`,
`PR_QUANTITY_EXCEEDED`, `PR_LINE_QUANTITY_INVALID` — seeded in V345 translations (en-US/ar-EG).

## Tests

`PurchaseRequestServiceTests`: creation numbering, empty-line rejection, quantity guard,
full transition matrix (submit/approve/reject/cancel/update), unknown-id 404, and the four
conversion rules including double-conversion and over-conversion blocking.
