import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlBorderManagerPanel extends JPanel {
    private JTable _table;
    private CustomTableModel _tableModel;
    private JPanel _tablePanel = new JPanel(new BorderLayout());
    private JTextField _rowInput = new JTextField(5);
    private JTextField _colInput = new JTextField(5);
    private JButton _createButton = new JButton("Create Table");
    private JButton _cleanButton = new JButton("Clean Borders");
    private JButton _resetButton = new JButton("Reset");
    private JCheckBox _groupingSeparatorCheckbox = new JCheckBox("Grouping Separator");
    
    private Map<Point, MatteBorder> _cellBorders = new HashMap<>();
    private List<String> _rowHeaders = new ArrayList<>();      // index = row number
    private List<String> _columnHeaders = new ArrayList<>();   // index = column number
    private Map<Point, MatteBorder> _copiedBorders = new HashMap<>();
    private Point _copyReferencePoint;

    public HtmlBorderManagerPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        controlPanel.setBackground(new Color(245, 247, 250));
        
        JLabel rowLabel = new JLabel("Rows:");
        JLabel colLabel = new JLabel("Columns:");
        
        styleLabel(rowLabel);
        styleLabel(colLabel);
        styleButton(_createButton);
        styleButton(_cleanButton);
        styleButton(_resetButton);
        styleTextField(_rowInput);
        styleTextField(_colInput);
        
        controlPanel.add(rowLabel);
        controlPanel.add(_rowInput);
        controlPanel.add(colLabel);
        controlPanel.add(_colInput);
        controlPanel.add(_createButton);
        controlPanel.add(_cleanButton);
        controlPanel.add(_resetButton);
        
        setLayout(new BorderLayout(10, 10));
        add(controlPanel, BorderLayout.NORTH);
        add(_tablePanel, BorderLayout.CENTER);
        
        _createButton.addActionListener(e -> createTableComponent());
        _cleanButton.addActionListener(e -> cleanBorders());
        _resetButton.addActionListener(e -> reset());
    }

    private void cleanBorders() {
        _cellBorders.clear();
        if (_table != null) {
            _table.repaint();
        }
    }

    private void reset() {
        _cellBorders.clear();
        _copiedBorders.clear();
        _rowHeaders.clear();
        _columnHeaders.clear();
        _rowInput.setText("");
        _colInput.setText("");
        _groupingSeparatorCheckbox.setSelected(false);
        _tablePanel.removeAll();
        _tablePanel.revalidate();
        _tablePanel.repaint();
        _copyReferencePoint = null;
    }

    private void createTableComponent() {
        int rows, cols;
        try {
            rows = Integer.parseInt(_rowInput.getText());
            cols = Integer.parseInt(_colInput.getText());
            if (rows <= 0 || cols <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid positive integers!");
            return;
        }
        _cellBorders.clear();
        createTable(rows, cols);
    }

    private String generateNewRowHeader() {
        int i = 1;
        while (_rowHeaders.contains("Row " + i)) {
            i++;
        }
        return "Row " + i;
    }

    private String generateNewColumnHeader() {
        int i = 1;
        while (_columnHeaders.contains("Header " + i)) {
            i++;
        }
        return "Header " + i;
    }

    private boolean isDuplicateRowHeader(String header, int currentRow) {
        for (int i = 0; i < _rowHeaders.size(); i++) {
            if (i != currentRow && header.equals(_rowHeaders.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateColumnHeader(String header, int currentColumn) {
        for (int i = 0; i < _columnHeaders.size(); i++) {
            if (i != currentColumn && header.equals(_columnHeaders.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void createTable(int rows, int cols) {
        _tablePanel.removeAll();
        _rowHeaders.clear();
        _columnHeaders.clear();
        
        _tableModel = new CustomTableModel(rows + 1, cols + 1);
        _table = new JTable(_tableModel);
        _table.setCellSelectionEnabled(true);
        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        _table.setTableHeader(null);
        
        // Initialize column headers
        for (int i = 0; i <= cols; i++) {
            String header;
            if (i == 0) {
                header = "";
            } else {
                header = generateNewColumnHeader();
            }
            // Ensure list is big enough, then set value
            while (_columnHeaders.size() <= i) {
                _columnHeaders.add("");
            }
            _columnHeaders.set(i, header);
            _tableModel.setValueAt(header, 0, i);
        }
        
        // Initialize row headers
        for (int i = 0; i <= rows; i++) {
            String header;
            if (i == 0) {
                header = "";
            } else {
                header = generateNewRowHeader();
            }
            // Ensure list is big enough, then set value
            while (_rowHeaders.size() <= i) {
                _rowHeaders.add("");
            }
            _rowHeaders.set(i, header);
            _tableModel.setValueAt(header, i, 0);
        }
        
        setupTableComponents();
    }

    private void setupTableComponents() {
        _table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                         boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(JLabel.CENTER);
                
                if (row == 0 || column == 0) {
                    if (isSelected) {
                        c.setBackground(new Color(200, 220, 240));
                    } else {
                        c.setBackground(new Color(220, 230, 240));
                    }
                    c.setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    c.setBackground(isSelected ? new Color(220, 235, 255) : Color.WHITE);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                
                c.setForeground(Color.DARK_GRAY);
                MatteBorder b = _cellBorders.getOrDefault(new Point(row, column),
                        new MatteBorder(0, 0, 0, 0, Color.GRAY));
                c.setBorder(b);
                return c;
            }
        });

        setupContextMenu();
        
        JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(new Color(245, 247, 250));
        styleCheckbox(_groupingSeparatorCheckbox);
        bottomPanel.add(_groupingSeparatorCheckbox);
        
        _tablePanel.add(scrollPane, BorderLayout.CENTER);
        _tablePanel.add(bottomPanel, BorderLayout.SOUTH);
        _tablePanel.revalidate();
        _tablePanel.repaint();
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editBorders = new JMenuItem("Edit Borders");
        JMenuItem copyBorders = new JMenuItem("Copy Borders");
        JMenuItem pasteBorders = new JMenuItem("Paste Borders");
        JMenuItem addRow = new JMenuItem("Add Row");
        JMenuItem removeRow = new JMenuItem("Remove Row");
        JMenuItem addColumn = new JMenuItem("Add Column");
        JMenuItem removeColumn = new JMenuItem("Remove Column");
        
        editBorders.addActionListener(x -> editBorders());
        copyBorders.addActionListener(x -> copyBorders());
        pasteBorders.addActionListener(x -> pasteBorders());
        addRow.addActionListener(x -> addRow());
        removeRow.addActionListener(x -> removeRow());
        addColumn.addActionListener(x -> addColumn());
        removeColumn.addActionListener(x -> removeColumn());
        
        contextMenu.add(editBorders);
        contextMenu.add(copyBorders);
        contextMenu.add(pasteBorders);
        contextMenu.addSeparator();
        contextMenu.add(addRow);
        contextMenu.add(removeRow);
        contextMenu.add(addColumn);
        contextMenu.add(removeColumn);

        _table.setComponentPopupMenu(contextMenu);
        _table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = _table.rowAtPoint(e.getPoint());
                    int col = _table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        boolean isRowHeader = col == 0 && row > 0;
                        boolean isColumnHeader = row == 0 && col > 0;
                        
                        addRow.setVisible(isRowHeader);
                        removeRow.setVisible(isRowHeader);
                        addColumn.setVisible(isColumnHeader);
                        removeColumn.setVisible(isColumnHeader);
                        pasteBorders.setEnabled(!_copiedBorders.isEmpty());
                        removeRow.setEnabled(isRowHeader && _tableModel.getRowCount() > 2);
                        removeColumn.setEnabled(isColumnHeader && _tableModel.getColumnCount() > 2);
                        contextMenu.getComponent(3).setVisible(isRowHeader || isColumnHeader);
                        contextMenu.show(_table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void addRow() {
        if (_tableModel != null) {
            int selectedRow = _table.getSelectedRow();
            if (selectedRow == -1) selectedRow = _tableModel.getRowCount() - 1;
            
            // Insert empty row
            _tableModel.insertRow(selectedRow + 1, new Object[_tableModel.getColumnCount()]);
            
            // Generate and set unique header
            String newHeader = generateNewRowHeader();
            
            // Insert new header at the correct position
            if (_rowHeaders.size() > selectedRow + 1) {
                _rowHeaders.add(selectedRow + 1, newHeader);
            } else {
                // Ensure list is big enough, then set value
                while (_rowHeaders.size() <= selectedRow + 1) {
                    _rowHeaders.add("");
                }
                _rowHeaders.set(selectedRow + 1, newHeader);
            }
            
            _tableModel.setValueAt(newHeader, selectedRow + 1, 0);
            
            // Update table model for all row headers to ensure sync
            for (int i = 1; i < _tableModel.getRowCount(); i++) {
                if (i < _rowHeaders.size()) {
                    _tableModel.setValueAt(_rowHeaders.get(i), i, 0);
                }
            }
            
            updateBordersAfterRowInsertion(selectedRow);
            _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
            _table.revalidate();
            _table.repaint();
        }
    }

    private void updateBordersAfterRowInsertion(int insertedAfterRow) {
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.x <= insertedAfterRow) {
                newBorders.put(oldPoint, entry.getValue());
            } else {
                newBorders.put(new Point(oldPoint.x + 1, oldPoint.y), entry.getValue());
            }
        }
        _cellBorders = newBorders;
    }

    private void removeRow() {
        if (_tableModel != null && _tableModel.getRowCount() > 2) {
            int selectedRow = _table.getSelectedRow();
            if (selectedRow <= 0) return;
            
            // Remove the row from list
            if (selectedRow < _rowHeaders.size()) {
                _rowHeaders.remove(selectedRow);
            }
            
            _tableModel.removeRow(selectedRow);
            
            // Update table model for all row headers to ensure sync
            for (int i = 1; i < _tableModel.getRowCount(); i++) {
                if (i < _rowHeaders.size()) {
                    _tableModel.setValueAt(_rowHeaders.get(i), i, 0);
                } else {
                    // Generate new header if needed
                    String newHeader = generateNewRowHeader();
                    _rowHeaders.add(newHeader);
                    _tableModel.setValueAt(newHeader, i, 0);
                }
            }
            
            updateBordersAfterRowRemoval(selectedRow);
            _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
            _table.revalidate();
            _table.repaint();
        }
    }

    private void updateBordersAfterRowRemoval(int removedRow) {
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.x < removedRow) {
                newBorders.put(oldPoint, entry.getValue());
            } else if (oldPoint.x > removedRow) {
                newBorders.put(new Point(oldPoint.x - 1, oldPoint.y), entry.getValue());
            }
        }
        _cellBorders = newBorders;
    }

    private void addColumn() {
        if (_tableModel != null) {
            int selectedCol = _table.getSelectedColumn();
            if (selectedCol == -1) selectedCol = _tableModel.getColumnCount() - 1;
            
            // Add column
            _tableModel.addColumn("", new Object[_tableModel.getRowCount()]);
            
            // Generate and set unique header
            String newHeader = generateNewColumnHeader();
            
            // Insert new header at the correct position
            if (_columnHeaders.size() > selectedCol + 1) {
                _columnHeaders.add(selectedCol + 1, newHeader);
            } else {
                // Ensure list is big enough, then set value
                while (_columnHeaders.size() <= selectedCol + 1) {
                    _columnHeaders.add("");
                }
                _columnHeaders.set(selectedCol + 1, newHeader);
            }
            
            _tableModel.setValueAt(newHeader, 0, selectedCol + 1);
            
            // Update table model for all column headers to ensure sync
            for (int i = 1; i < _tableModel.getColumnCount(); i++) {
                if (i < _columnHeaders.size()) {
                    _tableModel.setValueAt(_columnHeaders.get(i), 0, i);
                }
            }
            
            updateBordersAfterColumnInsertion(selectedCol);
            _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
            _table.revalidate();
            _table.repaint();
        }
    }

    private void updateBordersAfterColumnInsertion(int insertedAfterCol) {
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.y <= insertedAfterCol) {
                newBorders.put(oldPoint, entry.getValue());
            } else {
                newBorders.put(new Point(oldPoint.x, oldPoint.y + 1), entry.getValue());
            }
        }
        _cellBorders = newBorders;
    }

    private void removeColumn() {
        if (_tableModel != null && _tableModel.getColumnCount() > 2) {
            int selectedCol = _table.getSelectedColumn();
            if (selectedCol <= 0) return;
            
            // Remove the column from list
            if (selectedCol < _columnHeaders.size()) {
                _columnHeaders.remove(selectedCol);
            }
            
            // Remove column from model
            for (int row = 0; row < _tableModel.getRowCount(); row++) {
                for (int col = selectedCol; col < _tableModel.getColumnCount() - 1; col++) {
                    _tableModel.setValueAt(_tableModel.getValueAt(row, col + 1), row, col);
                }
            }
            _tableModel.setColumnCount(_tableModel.getColumnCount() - 1);
            
            // Update table model for all column headers to ensure sync
            for (int i = 1; i < _tableModel.getColumnCount(); i++) {
                if (i < _columnHeaders.size()) {
                    _tableModel.setValueAt(_columnHeaders.get(i), 0, i);
                } else {
                    // Generate new header if needed
                    String newHeader = generateNewColumnHeader();
                    _columnHeaders.add(newHeader);
                    _tableModel.setValueAt(newHeader, 0, i);
                }
            }
            
            updateBordersAfterColumnRemoval(selectedCol);
            _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
            _table.revalidate();
            _table.repaint();
        }
    }

    private void updateBordersAfterColumnRemoval(int removedCol) {
        Map<Point, MatteBorder> newBorders = new HashMap<>();
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point oldPoint = entry.getKey();
            if (oldPoint.y < removedCol) {
                newBorders.put(oldPoint, entry.getValue());
            } else if (oldPoint.y > removedCol) {
                newBorders.put(new Point(oldPoint.x, oldPoint.y - 1), entry.getValue());
            }
        }
        _cellBorders = newBorders;
    }

    private class CustomTableModel extends DefaultTableModel {
        public CustomTableModel(int rowCount, int columnCount) {
            super(rowCount, columnCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            // Cell (0,0) is not editable
            if (row == 0 && column == 0) {
                return false;
            }
            return (row == 0 && column > 0) || (column == 0 && row > 0);
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            // Cell (0,0) should not be editable
            if (row == 0 && column == 0) {
                return;
            }
            
            String newValue = value != null ? value.toString().trim() : "";
            if (row == 0 && column >= 0) {
                // Column header
                if (newValue.isEmpty()) {
                    newValue = generateNewColumnHeader();
                } else if (isDuplicateColumnHeader(newValue, column)) {
                    JOptionPane.showMessageDialog(HtmlBorderManagerPanel.this,
                        "Column header '" + newValue + "' already exists!",
                        "Duplicate Header",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Ensure list is big enough, then set value
                while (_columnHeaders.size() <= column) {
                    _columnHeaders.add("");
                }
                _columnHeaders.set(column, newValue);
                super.setValueAt(newValue, row, column);
                
            } else if (column == 0 && row > 0) {
                // Row header
                if (newValue.isEmpty()) {
                    newValue = generateNewRowHeader();
                } else if (isDuplicateRowHeader(newValue, row)) {
                    JOptionPane.showMessageDialog(HtmlBorderManagerPanel.this,
                        "Row header '" + newValue + "' already exists!",
                        "Duplicate Header",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Ensure list is big enough, then set value
                while (_rowHeaders.size() <= row) {
                    _rowHeaders.add("");
                }
                _rowHeaders.set(row, newValue);
                super.setValueAt(newValue, row, column);
                
            } else {
                // Regular cell
                super.setValueAt(value, row, column);
            }
        }
    }

    private void editBorders() {
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;
        
        Point first = new Point(rows[0], cols[0]);
        MatteBorder current = _cellBorders.getOrDefault(first, new MatteBorder(0, 0, 0, 0, Color.BLACK));
        
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        Color bgColor = new Color(246, 247, 250);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(bgColor);
        
        JCheckBox top = new JCheckBox("Top", current.getBorderInsets().top > 0);
        JCheckBox bottom = new JCheckBox("Bottom", current.getBorderInsets().bottom > 0);
        JCheckBox left = new JCheckBox("Left", current.getBorderInsets().left > 0);
        JCheckBox right = new JCheckBox("Right", current.getBorderInsets().right > 0);
        
        Font checkFont = new Font("Segoe UI", Font.PLAIN, 14);
        top.setFont(checkFont);
        bottom.setFont(checkFont);
        left.setFont(checkFont);
        right.setFont(checkFont);
        top.setBackground(bgColor);
        bottom.setBackground(bgColor);
        left.setBackground(bgColor);
        right.setBackground(bgColor);
        
        panel.add(top);
        panel.add(bottom);
        panel.add(left);
        panel.add(right);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Borders",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int t = top.isSelected() ? 1 : 0;
            int b = bottom.isSelected() ? 1 : 0;
            int l = left.isSelected() ? 1 : 0;
            int r = right.isSelected() ? 1 : 0;
            MatteBorder newBorder = new MatteBorder(t, l, b, r, Color.BLACK);
            
            for (int row : rows) {
                for (int col : cols) {
                    _cellBorders.put(new Point(row, col), newBorder);
                }
            }
            _table.repaint();
        }
    }

    private void copyBorders() {
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;
        
        _copiedBorders.clear();
        
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        for (int row : rows) {
            if (row < minRow) minRow = row;
        }
        for (int col : cols) {
            if (col < minCol) minCol = col;
        }
        _copyReferencePoint = new Point(minRow, minCol);
        
        for (int row : rows) {
            for (int col : cols) {
                Point originalPos = new Point(row, col);
                Point relativePos = new Point(row - minRow, col - minCol);
                MatteBorder border = _cellBorders.getOrDefault(originalPos,
                        new MatteBorder(0, 0, 0, 0, Color.BLACK));
                _copiedBorders.put(relativePos, border);
            }
        }
        
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
        
        int targetMinRow = Integer.MAX_VALUE;
        int targetMinCol = Integer.MAX_VALUE;
        for (int row : targetRows) {
            if (row < targetMinRow) targetMinRow = row;
        }
        for (int col : targetCols) {
            if (col < targetMinCol) targetMinCol = col;
        }
        
        int pasteCount = 0;
        for (Map.Entry<Point, MatteBorder> entry : _copiedBorders.entrySet()) {
            Point relativePos = entry.getKey();
            int targetRow = targetMinRow + relativePos.x;
            int targetCol = targetMinCol + relativePos.y;
            
            if (targetRow >= 0 && targetRow < _tableModel.getRowCount() &&
                targetCol >= 0 && targetCol < _tableModel.getColumnCount() &&
                contains(targetRows, targetRow) && contains(targetCols, targetCol)) {
                
                _cellBorders.put(new Point(targetRow, targetCol), entry.getValue());
                pasteCount++;
            }
        }
        
        _table.repaint();
        
        JOptionPane.showMessageDialog(this, 
            "Pasted " + pasteCount + " cell borders", 
            "Paste Complete", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean contains(int[] array, int value) {
        for (int item : array) {
            if (item == value) return true;
        }
        return false;
    }

    private void styleLabel(JLabel jLabel) {
        jLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        jLabel.setForeground(new Color(60, 60, 60));
    }

    private void styleTextField(JTextField field) {
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        field.setBackground(Color.WHITE);
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(70, 150, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 100, 150)),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                button.setBackground(new Color(90, 150, 200));
            }
            public void mouseExited(MouseEvent e) { 
                button.setBackground(new Color(70, 130, 180));
            }
        });
    }

    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setBackground(new Color(245, 247, 250));
        checkbox.setFocusPainted(false);
    }
}