private String generateNewRowHeader() {
    int i = 1;
    // Check existing row headers for uniqueness
    while (_rowHeaders.containsValue("Row " + i)) {
        i++;
    }
    return "Row " + i;
}

private String generateNewColumnHeader() {
    int i = 1;
    // Check existing column headers for uniqueness
    while (_columnHeaders.containsValue("Header " + i)) {
        i++;
    }
    return "Header " + i;
}

private void addRow() {
    if (_tableModel != null) {
        int selectedRow = _table.getSelectedRow();
        if (selectedRow == -1) selectedRow = _tableModel.getRowCount() - 1;
        int colCount = _tableModel.getColumnCount();
        
        _tableModel.insertRow(selectedRow + 1, new Object[colCount]);
        String newRowHeader = generateNewRowHeader();
        _tableModel.setValueAt(newRowHeader, selectedRow + 1, 0);
        
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.x >= selectedRow) {
                newBorders.put(new Point(oldPoint.x + 1, oldPoint.y), entry.getValue());
            } else {
                newBorders.put(oldPoint, entry.getValue());
            }
        }
        _cellBorders = newBorders;

        Map<Integer, String> newRowHeaders = new HashMap<>();
        
        // Rebuild row headers map ensuring uniqueness
        for (int i = 1; i < _tableModel.getRowCount(); i++) {
            if (i < selectedRow + 1) {
                String header = _rowHeaders.get(i);
                // Ensure the header we're keeping is still unique
                if (header != null && isDuplicateRowHeader(header, i)) {
                    header = generateNewRowHeader();
                }
                newRowHeaders.put(i, header);
            } else if (i == selectedRow + 1) {
                newRowHeaders.put(i, newRowHeader);
            } else {
                String header = _rowHeaders.get(i - 1);
                // Ensure the header we're moving is still unique
                if (header != null && isDuplicateRowHeader(header, i)) {
                    header = generateNewRowHeader();
                }
                newRowHeaders.put(i, header);
            }
        }
        _rowHeaders = newRowHeaders;
        _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
        _table.revalidate();
        _table.repaint();
    }
}


private void addColumn() {
    if (_tableModel != null) {
        int selectedCol = _table.getSelectedColumn();
        if (selectedCol == -1) selectedCol = _tableModel.getColumnCount() - 1;
        int rowCount = _tableModel.getRowCount();
        
        _tableModel.addColumn("New Column", new Object[rowCount]);
        String newColumnHeader = generateNewColumnHeader();
        _tableModel.setValueAt(newColumnHeader, 0, _tableModel.getColumnCount() - 1);
        
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.y >= selectedCol) {
                newBorders.put(new Point(oldPoint.x, oldPoint.y + 1), entry.getValue());
            } else {
                newBorders.put(oldPoint, entry.getValue());
            }
        }
        _cellBorders = newBorders;
        
        Map<Integer, String> newColumnHeaders = new HashMap<>();
        
        // Rebuild column headers map ensuring uniqueness
        for (int i = 1; i < _tableModel.getColumnCount(); i++) {
            if (i < selectedCol + 1) {
                String header = _columnHeaders.get(i);
                // Ensure the header we're keeping is still unique
                if (header != null && isDuplicateColumnHeader(header, i)) {
                    header = generateNewColumnHeader();
                }
                newColumnHeaders.put(i, header);
            } else if (i == selectedCol + 1) {
                newColumnHeaders.put(i, newColumnHeader);
            } else {
                String header = _columnHeaders.get(i - 1);
                // Ensure the header we're moving is still unique
                if (header != null && isDuplicateColumnHeader(header, i)) {
                    header = generateNewColumnHeader();
                }
                newColumnHeaders.put(i, header);
            }
        }
        _columnHeaders = newColumnHeaders;
        _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
        _table.revalidate();
        _table.repaint();
    }
}

private class CustomTableModel extends DefaultTableModel {
    public CustomTableModel(int rowCount, int columnCount) {
        super(rowCount, columnCount);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return row == 0 || column == 0;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        String newValue = value != null ? value.toString().trim() : "";
        
        if (row == 0 && column >= 0) {
            // Column header
            if (!newValue.isEmpty() && !isDuplicateColumnHeader(newValue, column)) {
                _columnHeaders.put(column, newValue);
                super.setValueAt(newValue, row, column);
            } else if (newValue.isEmpty()) {
                String generatedHeader = generateNewColumnHeader();
                _columnHeaders.put(column, generatedHeader);
                super.setValueAt(generatedHeader, row, column);
            } else {
                // Duplicate found, show warning and revert
                JOptionPane.showMessageDialog(HtmlBorderManagerPanel.this, 
                    "Column header '" + newValue + "' already exists!", 
                    "Duplicate Header", 
                    JOptionPane.WARNING_MESSAGE);
                // Revert to current value
                super.setValueAt(_columnHeaders.get(column), row, column);
            }
        } else if (column == 0 && row > 0) {
            // Row header
            if (!newValue.isEmpty() && !isDuplicateRowHeader(newValue, row)) {
                _rowHeaders.put(row, newValue);
                super.setValueAt(newValue, row, column);
            } else if (newValue.isEmpty()) {
                String generatedHeader = generateNewRowHeader();
                _rowHeaders.put(row, generatedHeader);
                super.setValueAt(generatedHeader, row, column);
            } else {
                // Duplicate found, show warning and revert
                JOptionPane.showMessageDialog(HtmlBorderManagerPanel.this, 
                    "Row header '" + newValue + "' already exists!", 
                    "Duplicate Header", 
                    JOptionPane.WARNING_MESSAGE);
                // Revert to current value
                super.setValueAt(_rowHeaders.get(row), row, column);
            }
        } else {
            // Regular cell
            super.setValueAt(value, row, column);
        }
    }
}



private boolean isDuplicateRowHeader(String header, int currentRow) {
    for (Map.Entry<Integer, String> entry : _rowHeaders.entrySet()) {
        if (entry.getKey() != currentRow && header.equals(entry.getValue())) {
            return true;
        }
    }
    return false;
}

// Helper method to check for duplicate column headers (excluding the current column)
private boolean isDuplicateColumnHeader(String header, int currentColumn) {
    for (Map.Entry<Integer, String> entry : _columnHeaders.entrySet()) {
        if (entry.getKey() != currentColumn && header.equals(entry.getValue())) {
            return true;
        }
    }
    return false;
}


private void removeRow() {
    if (_tableModel != null && _tableModel.getRowCount() > 2) {
        int selectedRow = _table.getSelectedRow();
        if (selectedRow <= 0) return;
        
        _rowHeaders.remove(selectedRow);
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.x < selectedRow) {
                newBorders.put(oldPoint, entry.getValue());
            } else if (oldPoint.x > selectedRow) {
                newBorders.put(new Point(oldPoint.x - 1, oldPoint.y), entry.getValue());
            }
        }
        _cellBorders = newBorders;
        _tableModel.removeRow(selectedRow);
        
        Map<Integer, String> newRowHeaders = new HashMap<>();
        // Rebuild row headers ensuring no duplicates after removal
        for (int i = 1; i < _tableModel.getRowCount(); i++) {
            if (i < selectedRow) {
                newRowHeaders.put(i, _rowHeaders.get(i));
            } else {
                String header = _rowHeaders.get(i + 1);
                // Check if we need to regenerate due to potential duplicates
                if (header != null && isDuplicateRowHeader(header, i)) {
                    header = generateNewRowHeader();
                }
                newRowHeaders.put(i, header);
            }
        }
        _rowHeaders = newRowHeaders;
        _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
        _table.revalidate();
        _table.repaint();
    }
}

private void removeColumn() {
    if (_tableModel != null && _tableModel.getColumnCount() > 2) {
        int selectedCol = _table.getSelectedColumn();
        if (selectedCol <= 0) return;
        
        _columnHeaders.remove(selectedCol);
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.y < selectedCol) {
                newBorders.put(oldPoint, entry.getValue());
            } else if (oldPoint.y > selectedCol) {
                newBorders.put(new Point(oldPoint.x, oldPoint.y - 1), entry.getValue());
            }
        }
        _cellBorders = newBorders;
        
        for (int row = 0; row < _tableModel.getRowCount(); row++) {
            for (int col = selectedCol; col < _tableModel.getColumnCount() - 1; col++) {
                _tableModel.setValueAt(_tableModel.getValueAt(row, col + 1), row, col);
            }
        }
        
        _tableModel.setColumnCount(_tableModel.getColumnCount() - 1);
        
        Map<Integer, String> newColumnHeaders = new HashMap<>();
        // Rebuild column headers ensuring no duplicates after removal
        for (int i = 1; i < _tableModel.getColumnCount(); i++) {
            if (i < selectedCol) {
                newColumnHeaders.put(i, _columnHeaders.get(i));
            } else {
                String header = _columnHeaders.get(i + 1);
                // Check if we need to regenerate due to potential duplicates
                if (header != null && isDuplicateColumnHeader(header, i)) {
                    header = generateNewColumnHeader();
                }
                newColumnHeaders.put(i, header);
            }
        }
        _columnHeaders = newColumnHeaders;
        _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
        _table.revalidate();
        _table.repaint();
    }
}