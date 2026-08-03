with open('fe/src/app/features/workforce/workforce.service.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("import { WorkforceService } from './workforce.service';", "import { WorkforceService } from './data-access/workforce.service';")

with open('fe/src/app/features/workforce/workforce.service.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)

with open('fe/src/app/shared/ui/confirm-dialog/confirm-dialog.service.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("import { ConfirmDialogService } from './confirm-dialog.service';", "import { ConfirmDialogService } from '../../../core/confirm-dialog/confirm-dialog.service';")

with open('fe/src/app/shared/ui/confirm-dialog/confirm-dialog.service.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)
