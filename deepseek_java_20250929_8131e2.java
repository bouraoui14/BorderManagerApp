import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class HtmlBorderManagerPanel extends ConfigurationItemPanel {
    
    private HtmlBorderManagerItem _item;
    private final JTextField _rowInput = new JTextField(5);
    private final JTextField _colInput = new JTextField(5);
    private final JButton _createButton = new JButton("Create Table");
    private final JButton _clearButton = new JButton("Clear Borders");
    private final JButton _resetButton = new JButton("Reset");
    private final JCheckBox _groupingSeparatorCheckbox = new JCheckBox("Add Grouping Separator");
    
    private JTable _table;
    private CustomTableModel _tableModel;
    private final JPanel _tablePanel = new JPanel(new BorderLayout());
    private Map<Point, MatteBorder> _cellBorders = new HashMap<>();
    private Map<Integer, String> _rowHeaders = new HashMap<>(); // Store row header texts
    private Map<Integer, String> _columnHeaders = new HashMap<>(); // Store column header texts
    private MatteBorder _copiedBorder = null;

    // Context menu items
    private JPopupMenu contextMenu;
    private JMenuItem editBorders;
    private JMenuItem copyBorders;
    private JMenuItem pasteBorders;
    private JMenuItem addRow;
    private JMenuItem removeRow;
    private JMenuItem addColumn;
    private JMenuItem removeColumn;

    HtmlBorderManagerPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        controlPanel.setBackground(new Color(245, 247, 250));
        
        JLabel rowLabel = new JLabel("Rows:");
        JLabel colLabel = new JLabel("Columns:");
        
        styleLabel(rowLabel);
        styleLabel(colLabel);
        styleButton(_createButton);
        styleButton(_clearButton);
        styleButton(_resetButton);
        styleTextField(_rowInput);
        styleTextField(_colInput);
        
        controlPanel.add(rowLabel);
        controlPanel.add(_rowInput);
        controlPanel.add(colLabel);
        controlPanel.add(_colInput);
        controlPanel.add(_createButton);
        controlPanel.add(_clearButton);
        controlPanel.add(_resetButton);
        
        setLayout(new BorderLayout(10, 10));
        add(controlPanel, BorderLayout.NORTH);
        add(_tablePanel, BorderLayout.CENTER);
        
        _createButton.addActionListener(e -> createTableComponent());
        _clearButton.addActionListener(e -> clearBorders());
        _resetButton.addActionListener(e -> reset());
    }

    // Custom table model to control editable cells and handle header changes
    private class CustomTableModel extends DefaultTableModel {
        public CustomTableModel(int rowCount, int columnCount) {
            super(rowCount, columnCount);
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            // Only allow editing for row headers (column 0) and column headers (row 0)
            return column == 0 || row == 0;
        }
        
        @Override
        public void setValueAt(Object value, int row, int column) {
            super.setValueAt(value, row, column);
            
            // Save header changes to the appropriate map
            if (row == 0 && column > 0) {
                // Column header edited
                _columnHeaders.put(column, value != null ? value.toString() : "Header " + column);
            } else if (column == 0 && row > 0) {
                // Row header edited
                _rowHeaders.put(row, value != null ? value.toString() : "Row " + row);
            }
        }
    }

    private void addRow() {
        if (_tableModel != null) {
            int selectedRow = _table.getSelectedRow();
            if (selectedRow == -1) selectedRow = _tableModel.getRowCount() - 1;
            
            int rowCount = _tableModel.getRowCount();
            int colCount = _tableModel.getColumnCount();
            
            // Insert row at selected position or at the end
            _tableModel.insertRow(selectedRow + 1, new Object[colCount]);
            
            // Set default value for the new row header
            String newRowHeader = "Row " + (rowCount);
            _tableModel.setValueAt(newRowHeader, selectedRow + 1, 0);
            
            // Update borders map - shift existing borders down for rows below the added row
            Map<Point, MatteBorder> newBorders = new HashMap<>();
            for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
                Point oldPoint = entry.getKey();
                if (oldPoint.x >= selectedRow - 1) {
                    newBorders.put(new Point(oldPoint.x + 1, oldPoint.y), entry.getValue());
                } else {
                    newBorders.put(oldPoint, entry.getValue());
                }
            }
            _cellBorders = newBorders;
            
            // Update row headers map
            Map<Integer, String> newRowHeaders = new HashMap<>();
            for (int i = 1; i < _tableModel.getRowCount(); i++) {
                if (i < selectedRow + 1) {
                    newRowHeaders.put(i, _rowHeaders.get(i));
                } else if (i == selectedRow + 1) {
                    newRowHeaders.put(i, newRowHeader);
                } else {
                    newRowHeaders.put(i, _rowHeaders.get(i - 1));
                }
            }
            _rowHeaders = newRowHeaders;
            
            // Update row input field
            _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
            
            updateRowHeaders();
            _table.revalidate();
            _table.repaint();
        }
    }

    private void removeRow() {
        if (_tableModel != null && _tableModel.getRowCount() > 2) { // Keep at least header row + 1 data row
            int selectedRow = _table.getSelectedRow();
            if (selectedRow <= 0) return; // Don't remove header row
            
            // Remove the row header from map
            _rowHeaders.remove(selectedRow);
            
            // Remove borders for the removed row and shift borders up
            Map<Point, MatteBorder> newBorders = new HashMap<>();
            for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
                Point oldPoint = entry.getKey();
                if (oldPoint.x < selectedRow - 1) {
                    newBorders.put(oldPoint, entry.getValue());
                } else if (oldPoint.x > selectedRow - 1) {
                    newBorders.put(new Point(oldPoint.x - 1, oldPoint.y), entry.getValue());
                }
            }
            _cellBorders = newBorders;
            
            _tableModel.removeRow(selectedRow);
            
            // Update row headers map for remaining rows
            Map<Integer, String> newRowHeaders = new HashMap<>();
            for (int i = 1; i < _tableModel.getRowCount(); i++) {
                if (i < selectedRow) {
                    newRowHeaders.put(i, _rowHeaders.get(i));
                } else {
                    newRowHeaders.put(i, _rowHeaders.get(i + 1));
                }
            }
            _rowHeaders = newRowHeaders;
            
            // Update row input field
            _rowInput.setText(String.valueOf(_tableModel.getRowCount() - 1));
            
            updateRowHeaders();
            _table.revalidate();
            _table.repaint();
        }
    }

    private void addColumn() {
        if (_tableModel != null) {
            int selectedCol = _table.getSelectedColumn();
            if (selectedCol == -1) selectedCol = _tableModel.getColumnCount() - 1;
            
            int rowCount = _tableModel.getRowCount();
            int colCount = _tableModel.getColumnCount();
            
            // Add new column
            _tableModel.addColumn("New Column", new Object[rowCount]);
            
            // Set default value for the new column header
            String newColumnHeader = "Header " + (colCount);
            _tableModel.setValueAt(newColumnHeader, 0, colCount);
            
            // Update borders map - shift existing borders right for columns after the added column
            Map<Point, MatteBorder> newBorders = new HashMap<>();
            for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
                Point oldPoint = entry.getKey();
                if (oldPoint.y >= selectedCol - 1) {
                    newBorders.put(new Point(oldPoint.x, oldPoint.y + 1), entry.getValue());
                } else {
                    newBorders.put(oldPoint, entry.getValue());
                }
            }
            _cellBorders = newBorders;
            
            // Update column headers map
            Map<Integer, String> newColumnHeaders = new HashMap<>();
            for (int i = 1; i < _tableModel.getColumnCount(); i++) {
                if (i < selectedCol + 1) {
                    newColumnHeaders.put(i, _columnHeaders.get(i));
                } else if (i == selectedCol + 1) {
                    newColumnHeaders.put(i, newColumnHeader);
                } else {
                    newColumnHeaders.put(i, _columnHeaders.get(i - 1));
                }
            }
            _columnHeaders = newColumnHeaders;
            
            // Update column input field
            _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
            
            updateColumnHeaders();
            _table.revalidate();
            _table.repaint();
        }
    }

    private void removeColumn() {
        if (_tableModel != null && _tableModel.getColumnCount() > 2) { // Keep at least header column + 1 data column
            int selectedCol = _table.getSelectedColumn();
            if (selectedCol <= 0) return; // Don't remove row header column
            
            // Remove the column header from map
            _columnHeaders.remove(selectedCol);
            
            // Remove borders for the removed column and shift borders left
            Map<Point, MatteBorder> newBorders = new HashMap<>();
            for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
                Point oldPoint = entry.getKey();
                if (oldPoint.y < selectedCol - 1) {
                    newBorders.put(oldPoint, entry.getValue());
                } else if (oldPoint.y > selectedCol - 1) {
                    newBorders.put(new Point(oldPoint.x, oldPoint.y - 1), entry.getValue());
                }
            }
            _cellBorders = newBorders;
            
            // Remove column from model
            for (int row = 0; row < _tableModel.getRowCount(); row++) {
                for (int col = selectedCol; col < _tableModel.getColumnCount() - 1; col++) {
                    _tableModel.setValueAt(_tableModel.getValueAt(row, col + 1), row, col);
                }
            }
            
            // Remove the last column
            _tableModel.setColumnCount(_tableModel.getColumnCount() - 1);
            
            // Update column headers map for remaining columns
            Map<Integer, String> newColumnHeaders = new HashMap<>();
            for (int i = 1; i < _tableModel.getColumnCount(); i++) {
                if (i < selectedCol) {
                    newColumnHeaders.put(i, _columnHeaders.get(i));
                } else {
                    newColumnHeaders.put(i, _columnHeaders.get(i + 1));
                }
            }
            _columnHeaders = newColumnHeaders;
            
            // Update column input field
            _colInput.setText(String.valueOf(_tableModel.getColumnCount() - 1));
            
            updateColumnHeaders();
            _table.revalidate();
            _table.repaint();
        }
    }

    private void updateRowHeaders() {
        if (_tableModel != null) {
            for (int i = 1; i < _tableModel.getRowCount(); i++) {
                String header = _rowHeaders.get(i);
                if (header == null) {
                    header = "Row " + i;
                    _rowHeaders.put(i, header);
                }
                _tableModel.setValueAt(header, i, 0);
            }
        }
    }

    private void updateColumnHeaders() {
        if (_tableModel != null) {
            for (int i = 1; i < _tableModel.getColumnCount(); i++) {
                String header = _columnHeaders.get(i);
                if (header == null) {
                    header = "Header " + i;
                    _columnHeaders.put(i, header);
                }
                _tableModel.setValueAt(header, 0, i);
            }
        }
    }

    private void clearBorders() {
        _cellBorders.clear();
        if (_table != null) {
            _table.repaint();
        }
    }

    private void reset() {
        _cellBorders.clear();
        _rowHeaders.clear();
        _columnHeaders.clear();
        _rowInput.setText("");
        _colInput.setText("");
        _groupingSeparatorCheckbox.setSelected(false);
        _tablePanel.removeAll();
        _tablePanel.revalidate();
        _tablePanel.repaint();
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
        _rowHeaders.clear();
        _columnHeaders.clear();
        createTable(rows, cols);
    }

    private void createTableFromItem(int rows, int cols, Map<Point, MatteBorder> cellBorders) {
        _rowInput.setText(String.valueOf(rows));
        _colInput.setText(String.valueOf(cols));
        _cellBorders = new HashMap<>(cellBorders);
        createTable(rows, cols);
    }

    private void createTable(int rows, int cols) {
        _tablePanel.removeAll();
        
        // Create table with +1 row for headers and +1 column for row headers
        _tableModel = new CustomTableModel(rows + 1, cols + 1);
        _table = new JTable(_tableModel);
        _table.setCellSelectionEnabled(true);
        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        _table.setTableHeader(null);
        
        // Set default values for headers and store in maps
        _tableModel.setValueAt("", 0, 0); // Top-left corner
        
        // Initialize column headers
        for (int i = 1; i <= cols; i++) {
            String header = _columnHeaders.get(i);
            if (header == null) {
                header = "Header " + i;
                _columnHeaders.put(i, header);
            }
            _tableModel.setValueAt(header, 0, i);
        }
        
        // Initialize row headers
        for (int i = 1; i <= rows; i++) {
            String header = _rowHeaders.get(i);
            if (header == null) {
                header = "Row " + i;
                _rowHeaders.put(i, header);
            }
            _tableModel.setValueAt(header, i, 0);
        }
        
        _table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(CENTER);
                
                // Style headers differently
                if (row == 0 || column == 0) {
                    c.setBackground(new Color(220, 230, 240)); // Light blue for headers
                    c.setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    c.setBackground(isSelected ? new Color(220, 235, 255) : Color.WHITE);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                c.setForeground(Color.DARK_GRAY);
                
                // Apply borders (storage uses data cell coordinates, not including headers)
                if (row > 0 && column > 0) {
                    MatteBorder b = _cellBorders.getOrDefault(new Point(row - 1, column - 1),
                            new MatteBorder(0, 0, 0, 0, Color.GRAY));
                    c.setBorder(b);
                } else {
                    c.setBorder(new MatteBorder(0, 0, 0, 0, Color.GRAY));
                }
                return c;
            }
        });
        
        // Create context menu
        contextMenu = new JPopupMenu();
        editBorders = new JMenuItem("Edit Borders");
        copyBorders = new JMenuItem("Copy Borders");
        pasteBorders = new JMenuItem("Paste Borders");
        addRow = new JMenuItem("Add Row");
        removeRow = new JMenuItem("Remove Row");
        addColumn = new JMenuItem("Add Column");
        removeColumn = new JMenuItem("Remove Column");
        
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
        contextMenu.addSeparator();
        contextMenu.add(addColumn);
        contextMenu.add(removeColumn);
        
        _table.setComponentPopupMenu(contextMenu);
        
        // Add mouse listener to show context menu only on appropriate cells
        _table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = _table.rowAtPoint(e.getPoint());
                    int col = _table.columnAtPoint(e.getPoint());
                    
                    if (row >= 0 && col >= 0) {
                        boolean isRowHeader = col == 0 && row > 0;
                        boolean isColumnHeader = row == 0 && col > 0;
                        boolean isDataCell = row > 0 && col > 0;
                        
                        // Show only relevant menu items based on cell type
                        editBorders.setVisible(isDataCell);
                        copyBorders.setVisible(isDataCell);
                        pasteBorders.setVisible(isDataCell);
                        addRow.setVisible(isRowHeader);
                        removeRow.setVisible(isRowHeader);
                        addColumn.setVisible(isColumnHeader);
                        removeColumn.setVisible(isColumnHeader);
                        
                        // Enable/disable based on conditions
                        boolean hasSelection = _table.getSelectedRowCount() > 0;
                        editBorders.setEnabled(isDataCell && hasSelection);
                        copyBorders.setEnabled(isDataCell);
                        pasteBorders.setEnabled(isDataCell && hasSelection && _copiedBorder != null);
                        removeRow.setEnabled(isRowHeader && _tableModel.getRowCount() > 2); // At least 1 data row + header
                        removeColumn.setEnabled(isColumnHeader && _tableModel.getColumnCount() > 2); // At least 1 data column + header
                        
                        // Show separators only if there are visible items before and after
                        contextMenu.getComponent(3).setVisible(editBorders.isVisible() || copyBorders.isVisible() || pasteBorders.isVisible());
                        contextMenu.getComponent(5).setVisible((addRow.isVisible() || removeRow.isVisible()) && (addColumn.isVisible() || removeColumn.isVisible()));
                        
                        if (isRowHeader || isColumnHeader || isDataCell) {
                            contextMenu.show(_table, e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        
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

    private void editBorders() {
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;
        
        // Filter only data cells (exclude headers)
        java.util.List<Integer> dataRows = new java.util.ArrayList<>();
        java.util.List<Integer> dataCols = new java.util.ArrayList<>();
        
        for (int row : rows) {
            if (row > 0) dataRows.add(row);
        }
        for (int col : cols) {
            if (col > 0) dataCols.add(col);
        }
        
        if (dataRows.isEmpty() || dataCols.isEmpty()) return;
        
        int firstDataRow = dataRows.get(0);
        int firstDataCol = dataCols.get(0);
        Point first = new Point(firstDataRow - 1, firstDataCol - 1);
        MatteBorder current = _cellBorders.getOrDefault(first, new MatteBorder(0, 0, 0, 0, Color.BLACK));
        
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        Color bgColor = new Color(245, 247, 250);
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
            for (int row : dataRows) {
                for (int col : dataCols) {
                    int storageRow = row - 1;
                    int storageCol = col - 1;
                    _cellBorders.put(new Point(storageRow, storageCol), newBorder);
                }
            }
            _table.repaint();
        }
    }

    private void copyBorders() {
        int row = _table.getSelectedRow();
        int col = _table.getSelectedColumn();
        if (row <= 0 || col <= 0) return; // Only copy from data cells
        
        int storageRow = row - 1;
        int storageCol = col - 1;
        _copiedBorder = _cellBorders.getOrDefault(new Point(storageRow, storageCol),
                new MatteBorder(0, 0, 0, 0, Color.BLACK));
    }

    private void pasteBorders() {
        if (_copiedBorder == null) {
            return;
        }
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;
        
        for (int row : rows) {
            for (int col : cols) {
                if (row > 0 && col > 0) { // Only paste to data cells
                    int storageRow = row - 1;
                    int storageCol = col - 1;
                    _cellBorders.put(new Point(storageRow, storageCol), _copiedBorder);
                }
            }
        }
        _table.repaint();
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
        button.setBackground(new Color(70, 130, 180));
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

    @Override
    public void refreshConfiguration(Object item) {
        if (!(item instanceof HtmlBorderManagerItem)) {
            return;
        }
        _item = (HtmlBorderManagerItem) item;
        _groupingSeparatorCheckbox.setSelected(_item.isGroupingSeparatorEnabled());
        
        // Load row and column headers if available
        if (_item.getRowHeaders() != null) {
            _rowHeaders = new HashMap<>(_item.getRowHeaders());
        }
        if (_item.getColumnHeaders() != null) {
            _columnHeaders = new HashMap<>(_item.getColumnHeaders());
        }
        
        createTableFromItem(_item.getRowNumber(), _item.getColumnNumber(), _item.getCellBorders());
    }

    @Override
    public ConfigurationItem apply(String name, boolean createNew) {
        if (_item == null || createNew) {
            _item = new HtmlBorderManagerItem(name);
        }
        // Save data rows and columns (excluding headers)
        _item.setRowNumber(_table.getRowCount() - 1);
        _item.setColumnNumber(_table.getColumnCount() - 1);
        _item.setCellBorders(_cellBorders);
        _item.setRowHeaders(_rowHeaders);
        _item.setColumnHeaders(_columnHeaders);
        _item.setGroupingSeparatorEnabled(_groupingSeparatorCheckbox.isSelected());
        return _item;
    }

    public static void registerFactory() {
        final ConfigurationItemPanelDirectory.Factory<HtmlBorderManagerItem> factory = new ConfigurationItemPanelDirectory.Factory<>() {
            @Override
            public HtmlBorderManagerPanel makeInstance() {
                return new HtmlBorderManagerPanel();
            }

            @Override
            public Class<HtmlBorderManagerItem> getItemClass() {
                return HtmlBorderManagerItem.class;
            }

            @Override
            public String getDisplayName() {
                return HtmlBorderManagerItem.NAME;
            }
        };
        ConfigurationItemPanelDirectory.getInstance().registerPanel(HtmlBorderManagerItem.ENTITY_KIND, factory);
    }
}