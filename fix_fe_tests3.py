with open('fe/src/app/shared/ui/confirm-dialog/confirm-dialog.service.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("import { ConfirmDialogService } from '../../../core/confirm-dialog/confirm-dialog.service';", "import { ConfirmDialogService } from '../../../core/confirm-dialog.service';")

with open('fe/src/app/shared/ui/confirm-dialog/confirm-dialog.service.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)

with open('fe/src/app/core/download/download.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("export function exportCsv() { return true; }", "export function exportCsv(a?: any, b?: any) { return true; }")

with open('fe/src/app/core/download/download.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)
