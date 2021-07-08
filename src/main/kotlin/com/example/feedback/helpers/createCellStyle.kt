package com.example.feedback.helpers

import org.apache.poi.ss.usermodel.*

fun Workbook.createCellStyle(
    backgroundColor: IndexedColors?,
    textColor: IndexedColors,
    alignment: HorizontalAlignment,
    verticalAlignment: VerticalAlignment = VerticalAlignment.TOP,
): CellStyle {
    val style = createCellStyle()
    if (backgroundColor != null) {
        style.fillForegroundColor = backgroundColor.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
    }
    style.alignment = alignment
    style.verticalAlignment = verticalAlignment
    style.setFont(createFont().apply { color = textColor.index })

    return style
}
