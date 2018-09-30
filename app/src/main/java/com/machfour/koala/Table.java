package com.machfour.koala;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Table class to hold data from processing result
 */
public class Table {
    private final int maxColumns;
    private static final int COLUMN_LIMIT = 10;
    private final List<List<String>> rows;

    private static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static Table parseFromString(@NonNull String tableString, @NonNull String columnSep) {
        String[] rowStrings = tableString.split("\n");
        List<List<String>> columnSplits = new ArrayList<>();
        int maxColumns = 0;
        for (String rowString : rowStrings) {
            String[] cells = rowString.split(columnSep, COLUMN_LIMIT);
            maxColumns = max(maxColumns, cells.length);
            columnSplits.add(Arrays.asList(cells));
        }
        return new Table(maxColumns, columnSplits);
    }

    public static Table parseFromString(@NonNull String tableString) {
        return parseFromString(tableString, "\f");
    }

    public Table(int maxColumns) {
        this(maxColumns, 10);
    }

    public Table(int maxColumns, int initialRows) {
        this(maxColumns, new ArrayList<>(initialRows));
        // initialise rows
        for (int i = 0; i < initialRows; ++i) {
            addRow();
        }
    }

    private Table(int maxColumns, List<List<String>> rowData) {
        for (List<String> row : rowData) {
            if (row.size() > maxColumns) {
                throw new IllegalArgumentException("Rows have at most " + maxColumns + " columns");
            }
        }
        this.maxColumns = maxColumns;
        this.rows = rowData;

    }

    public int getRows() {
        return rows.size();
    }
    public int getCols() {
        return maxColumns;
    }

    // returns empty string if indices are out of range
    public String getText(int row, int col) {
        if (row >= getRows() || col >= getCols()) {
            return "";
        } else {
            return rows.get(row).get(col);
        }
    }

    private void addRow() {
        List<String> newRow = new ArrayList<>(getCols());
        for (int i = 0; i < getCols(); ++i) {
            newRow.add("");
        }
        rows.add(newRow);
    }

    void setColumnText(int row, int col, String text) {
        while (row >= rows.size()) {
            addRow();
        }
        rows.get(row).set(col, text);
    }


}
