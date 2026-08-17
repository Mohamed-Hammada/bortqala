export type SmartImportColumnType = 'STRING' | 'DATE' | 'DECIMAL' | 'INTEGER' | 'BOOLEAN' | 'ENUM';

export interface SmartImportColumn {
  key: string;
  headerEn: string;
  headerAr: string;
  type: SmartImportColumnType;
  required: boolean;
  allowedValues: string[];
  aliases: string[];
}

export interface SmartImportSheet {
  key: string;
  titleEn: string;
  titleAr: string;
  columns: SmartImportColumn[];
}

export interface SmartImportWorkflow {
  key: string;
  titleEn: string;
  titleAr: string;
  route: string;
  templateFileName: string;
  priority: string;
  domainCommitAvailable: boolean;
  sheets: SmartImportSheet[];
}

export interface SmartImportCellError {
  rowNumber: number;
  sheet: string;
  column: string;
  messageEn: string;
  messageAr: string;
}

export interface SmartImportPreviewRow {
  rowNumber: number;
  sheet: string;
  values: Record<string, string>;
  errors: SmartImportCellError[];
}

export interface SmartImportPreview {
  previewId: string;
  workflow: SmartImportWorkflow;
  fileName: string;
  totalRows: number;
  validRows: number;
  errorRows: number;
  rows: SmartImportPreviewRow[];
  errors: SmartImportCellError[];
}

export interface SmartImportCommitResult {
  batchId: string;
  workflow: string;
  status: 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'VALIDATED_ONLY';
  persisted: boolean;
  committedRows: number;
  rejectedRows: number;
  messageEn: string;
  messageAr: string;
  errors: SmartImportCellError[];
}
