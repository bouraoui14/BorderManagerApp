// New field to store multiple copied borders
private Map<Point, MatteBorder> _copiedBorders = new HashMap<>();
private Point _copyReferencePoint; // Top-left corner of copied selection

private void copyBorders() {
    int[] rows = _table.getSelectedRows();
    int[] cols = _table.getSelectedColumns();
    if (rows.length == 0 || cols.length == 0) return;
    
    _copiedBorders.clear();
    
    // Find the top-left corner of the selection
    int minRow = Integer.MAX_VALUE;
    int minCol = Integer.MAX_VALUE;
    for (int row : rows) {
        if (row < minRow) minRow = row;
    }
    for (int col : cols) {
        if (col < minCol) minCol = col;
    }
    _copyReferencePoint = new Point(minRow, minCol);
    
    // Copy all selected borders with relative positions
    for (int row : rows) {
        for (int col : cols) {
            Point originalPos = new Point(row, col);
            Point relativePos = new Point(row - minRow, col - minCol);
            MatteBorder border = _cellBorders.getOrDefault(originalPos,
                    new MatteBorder(0, 0, 0, 0, Color.BLACK));
            _copiedBorders.put(relativePos, border);
        }
    }
    
    // Show feedback about how many cells were copied
    JOptionPane.showMessageDialog(this, 
        "Copied " + _copiedBorders.size() + " cell borders", 
        "Copy Successful", 
        JOptionPane.INFORMATION_MESSAGE);
}

private void pasteBorders() {
    if (_copiedBorders.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No borders copied to paste!");
        return;
    }
    
    int[] targetRows = _table.getSelectedRows();
    int[] targetCols = _table.getSelectedColumns();
    if (targetRows.length == 0 || targetCols.length == 0) {
        JOptionPane.showMessageDialog(this, "Please select target cells first!");
        return;
    }
    
    // Find the top-left corner of the target selection
    int targetMinRow = Integer.MAX_VALUE;
    int targetMinCol = Integer.MAX_VALUE;
    for (int row : targetRows) {
        if (row < targetMinRow) targetMinRow = row;
    }
    for (int col : targetCols) {
        if (col < targetMinCol) targetMinCol = col;
    }
    
    // Paste all copied borders maintaining relative positions
    int pasteCount = 0;
    for (Map.Entry<Point, MatteBorder> entry : _copiedBorders.entrySet()) {
        Point relativePos = entry.getKey();
        int targetRow = targetMinRow + relativePos.x;
        int targetCol = targetMinCol + relativePos.y;
        
        // Check if target position is within table bounds and selected
        if (targetRow >= 0 && targetRow < _tableModel.getRowCount() &&
            targetCol >= 0 && targetCol < _tableModel.getColumnCount() &&
            contains(targetRows, targetRow) && contains(targetCols, targetCol)) {
            
            _cellBorders.put(new Point(targetRow, targetCol), entry.getValue());
            pasteCount++;
        }
    }
    
    _table.repaint();
    
    // Show feedback
    JOptionPane.showMessageDialog(this, 
        "Pasted " + pasteCount + " cell borders", 
        "Paste Complete", 
        JOptionPane.INFORMATION_MESSAGE);
}

// Helper method to check if an array contains a value
private boolean contains(int[] array, int value) {
    for (int item : array) {
        if (item == value) return true;
    }
    return false;
}