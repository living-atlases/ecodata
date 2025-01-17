package au.org.ala.ecodata

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference

/**
 * Converts a spreadsheet to a Map based on configuration.
 * Uses the same configuration as the grails excel-import-plugin.
 */
class ExcelImportService {

    /**
     * Takes a excel workbook and converts one sheet into a Map based on the supplied configuration.
     * @param workbook the excel workbook to import
     * @param config a Map of the form:
     * sheet:     <name of the sheet to import>
     * startRow:  <row to start the import from - used to skip header rows>
     * columnMap: <map of column letter from the sheet to key to use in the results>
     * @return a List of maps, one entry per row in the sheet, each row being a map with keys specified by the
     * columnMap config and values as found in the spreadsheet
     */
    List<Map> mapSheet(Workbook workbook, Map config) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator()
        Sheet sheet = workbook.getSheet(config.sheet)
        List<Map> results  = []
        for (int i=config.startRow; i<=sheet.getLastRowNum(); i++) {
            results << mapRow(config.columnMap, sheet.getRow(i), evaluator)
        }
        results
    }

    private Map mapRow(Map columnMap, Row row, FormulaEvaluator evaluator) {
        Map result = [:]
        columnMap.each {k, name ->
            Object value = getCellValue(row, k, evaluator)
            result[name] = value

        }
        result
    }

    /** The cell param is expected to be a Cell or CellValue */
    private Object getCellValue(Cell cell, FormulaEvaluator evaluator, Object defaultValue = null) {
        if (!cell) {
            return defaultValue
        }
        Object value = defaultValue
        switch (cell.cellType) {
            case CellType.STRING:
                value = cell.getStringCellValue()
                break
            case CellType.NUMERIC:
                value = cell.getNumericCellValue()
                break
            case CellType.FORMULA:
                CellValue evaluated = evaluator.evaluate(cell)
                value = getCellValue(evaluated, defaultValue)
                break
            case CellType.BLANK:
                value = ''
                break
            case CellType.ERROR:
            case CellType._NONE:
                value = null
                break
        }
        value
    }


    private Object getCellValue(CellValue cell, Object defaultValue = null) {
        if (!cell) {
            return defaultValue
        }
        Object value = defaultValue
        switch (cell.cellType) {
            case CellType.STRING:
                value = cell.getStringValue()
                break
            case CellType.NUMERIC:
                value = cell.getNumberValue()
                break
            case CellType.BLANK:
                value = ''
                break
            case CellType.ERROR:
            case CellType._NONE:
                value = null
                break
        }
        value
    }

    /**
     * Gets the value of the cell identified by the supplied column.
     */
    private Object getCellValue(Row row, String column, FormulaEvaluator evaluator, Object defaultValue = null) {
        Object value = defaultValue
        int columnIdx = CellReference.convertColStringToIndex(column)
        if (row && columnIdx >= 0 && row.lastCellNum > columnIdx) {
            Cell cell = row.getCell(columnIdx)
            value = getCellValue(cell, evaluator, defaultValue)
        }
        value
    }
}
