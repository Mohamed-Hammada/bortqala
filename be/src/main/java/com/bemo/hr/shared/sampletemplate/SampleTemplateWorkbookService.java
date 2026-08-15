package com.bemo.hr.shared.sampletemplate;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class SampleTemplateWorkbookService {
    public byte[] create(SampleTemplateCatalog.Template template) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            Sheet data = workbook.createSheet("Template - النموذج");
            Row hr = data.createRow(0);
            for (int i = 0; i < template.columns().size(); i++) {
                Cell cell = hr.createCell(i); cell.setCellValue(template.columns().get(i).en()); cell.setCellStyle(header);
            }
            int rowIndex = 1;
            for (var sample : template.samples()) {
                Row row = data.createRow(rowIndex++);
                for (int i = 0; i < sample.size(); i++) setValue(row.createCell(i), sample.get(i));
            }
            addValidations(data, template);
            data.createFreezePane(0, 1);
            autoSize(data, template.columns().size());

            Sheet info = workbook.createSheet("Instructions - تعليمات");
            String[] labels = {"Column (EN)", "العمود (AR)", "Required / مطلوب", "Type / Format", "Accepted Values", "Notes / ملاحظات"};
            Row ih = info.createRow(0);
            for (int i = 0; i < labels.length; i++) { Cell cell = ih.createCell(i); cell.setCellValue(labels[i]); cell.setCellStyle(header); }
            for (int r = 0; r < template.columns().size(); r++) {
                var col = template.columns().get(r); Row row = info.createRow(r + 1);
                row.createCell(0).setCellValue(col.en()); row.createCell(1).setCellValue(col.ar()); row.createCell(2).setCellValue(col.required() ? "Yes / نعم" : "No / لا");
                row.createCell(3).setCellValue(col.type()); row.createCell(4).setCellValue(col.accepted()); row.createCell(5).setCellValue(col.notes());
            }
            info.createFreezePane(0, 1); autoSize(info, labels.length);
            workbook.write(out); return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Failed to generate sample workbook", e); }
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle(); Font font = wb.createFont(); font.setBold(true); style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN); return style;
    }
    private static void setValue(Cell cell, Object value) {
        if (value instanceof Number n) cell.setCellValue(n.doubleValue()); else if (value instanceof Boolean b) cell.setCellValue(b); else cell.setCellValue(String.valueOf(value));
    }
    private static void autoSize(Sheet sheet, int count) { for (int i=0;i<count;i++){ sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i)+768, 14000)); } }
    private static void addValidations(Sheet sheet, SampleTemplateCatalog.Template template) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        for (int i=0;i<template.columns().size();i++) {
            String accepted = template.columns().get(i).accepted();
            if (accepted == null || accepted.isBlank() || accepted.length() > 220 || !accepted.contains(",")) continue;
            String[] values = java.util.Arrays.stream(accepted.split(",")).map(String::trim).toArray(String[]::new);
            DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
            DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 5000, i, i));
            validation.setSuppressDropDownArrow(true); validation.setShowErrorBox(true); sheet.addValidationData(validation);
        }
    }
}
