import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderDesignerApp extends JFrame {

    private final JTextField _rowInput = new JTextField("6", 5);
    private final JTextField _colInput = new JTextField("6", 5);
    private final JButton _createButton = new JButton("Create Table");
    private final JButton _saveButton = new JButton("Save");
    private final JButton _loadButton = new JButton("Load");
    private final JButton _refreshButton = new JButton("Refresh");
    private final JButton _applyButton = new JButton("Apply");

    private JTable _table;
    private final JPanel _tablePanel = new JPanel(new BorderLayout());
    private final Map<Point, MatteBorder> _cellBorders = new HashMap<>();
    private MatteBorder _copiedBorder = null; // copy/paste feature
    
    private BorderItem _currentItem = new BorderItem();

    public BorderDesignerApp() {
        super("Border Designer");

        // ==== Top Control Panel ====
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(new Color(240, 240, 245));

        JLabel rowLabel = new JLabel("Rows:");
        JLabel colLabel = new JLabel("Columns:");

        _styleLabel(rowLabel);
        _styleLabel(colLabel);
        _styleButton(_createButton);
        _styleButton(_saveButton);
        _styleButton(_loadButton);
        _styleButton(_refreshButton);
        _styleButton(_applyButton);
        _styleTextField(_rowInput);
        _styleTextField(_colInput);

        controlPanel.add(rowLabel);
        controlPanel.add(_rowInput);
        controlPanel.add(colLabel);
        controlPanel.add(_colInput);
        controlPanel.add(_createButton);
        controlPanel.add(_saveButton);
        controlPanel.add(_loadButton);
        controlPanel.add(_refreshButton);
        controlPanel.add(_applyButton);

        // ==== Main Layout ====
        setLayout(new BorderLayout(10, 10));
        add(controlPanel, BorderLayout.NORTH);
        add(_tablePanel, BorderLayout.CENTER);

        _createButton.addActionListener(this::_createTable);
        _saveButton.addActionListener(e -> _saveItem());
        _loadButton.addActionListener(e -> _loadItem());
        _refreshButton.addActionListener(e -> _refresh());
        _applyButton.addActionListener(e -> _apply());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    /** Create table when button clicked */
    private void _createTable(ActionEvent e) {
        int rows, cols;
        try {
            rows = Integer.parseInt(_rowInput.getText());
            cols = Integer.parseInt(_colInput.getText());
            if (rows <= 0 || cols <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid positive integers!");
            return;
        }

        _cellBorders.clear(); // reset old borders
        _tablePanel.removeAll();

        _table = new JTable(rows, cols);
        _table.setCellSelectionEnabled(true);
        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Customize table headers
        JTableHeader header = _table.getTableHeader();
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Set custom header names
        for (int i = 0; i < _table.getColumnCount(); i++) {
            _table.getColumnModel().getColumn(i).setHeaderValue("Header" + (i + 1));
        }

        // Renderer to draw borders for both cells and headers
        _table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(CENTER);
                c.setBackground(isSelected ? new Color(220, 235, 255) : Color.WHITE);
                MatteBorder b = _cellBorders.getOrDefault(new Point(row, col),
                        new MatteBorder(0, 0, 0, 0, Color.BLACK));
                c.setBorder(b);
                return c;
            }
        });
        
        // Header renderer to apply borders to headers too
        _table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(CENTER);
                c.setBackground(new Color(70, 130, 180));
                c.setForeground(Color.WHITE);
                c.setFont(new Font("Segoe UI", Font.BOLD, 14));
                
                // Apply border to header cells too (row = -1 for headers)
                MatteBorder b = _cellBorders.getOrDefault(new Point(-1, column),
                        new MatteBorder(0, 0, 2, 0, Color.DARK_GRAY));
                c.setBorder(b);
                return c;
            }
        });

        // Right-click menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editBorders = new JMenuItem("Edit Borders...");
        JMenuItem copyBorders = new JMenuItem("Copy Borders");
        JMenuItem pasteBorders = new JMenuItem("Paste Borders");

        editBorders.addActionListener(x -> _editBordersForSelection());
        copyBorders.addActionListener(x -> _copyBordersFromSelection());
        pasteBorders.addActionListener(x -> _pasteBordersToSelection());

        popup.add(editBorders);
        popup.add(copyBorders);
        popup.add(pasteBorders);

        _table.setComponentPopupMenu(popup);

        _tablePanel.add(new JScrollPane(_table), BorderLayout.CENTER);
        _tablePanel.revalidate();
        _tablePanel.repaint();
    }

    /** BorderItem class to hold border data */
    public static class BorderItem {
        private List<String> _borderData = new ArrayList<>();
        
        public BorderItem() {}
        
        public List<String> getBorderData() {
            return _borderData;
        }
        
        public void setBorderData(List<String> borderData) {
            _borderData = borderData;
        }
    }
    
    /** Refresh the panel from the current item */
    private void _refresh() {
        if (_currentItem == null || _currentItem.getBorderData() == null || _currentItem.getBorderData().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No item data to refresh from!");
            return;
        }
        
        _loadItem(_currentItem);
        JOptionPane.showMessageDialog(this, "Panel refreshed from item");
    }
    
    /** Apply the current panel state to the item */
    private void _apply() {
        _currentItem = _saveItem();
        JOptionPane.showMessageDialog(this, "Panel state applied to item");
    }
    
    /** Save the current border configuration to a BorderItem */
    private BorderItem _saveItem() {
        BorderItem item = new BorderItem();
        List<String> borderData = new ArrayList<>();
        
        // Save table dimensions
        if (_table != null) {
            borderData.add("DIMENSIONS:" + _table.getRowCount() + ":" + _table.getColumnCount());
        } else {
            borderData.add("DIMENSIONS:" + _rowInput.getText() + ":" + _colInput.getText());
        }
        
        // Save all borders
        for (Map.Entry<Point, MatteBorder> entry : _cellBorders.entrySet()) {
            Point p = entry.getKey();
            MatteBorder border = entry.getValue();
            Insets insets = border.getBorderInsets();
            
            // Format: row:col:top:right:bottom:left
            String borderStr = p.x + ":" + p.y + ":" + 
                              insets.top + ":" + insets.right + ":" + 
                              insets.bottom + ":" + insets.left;
            borderData.add(borderStr);
        }
        
        item.setBorderData(borderData);
        return item;
    }
    
    /** Load border configuration from a BorderItem */
    private void _loadItem(BorderItem item) {
        if (item == null || item.getBorderData() == null || item.getBorderData().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid item to load!");
            return;
        }
        
        _cellBorders.clear();
        List<String> data = item.getBorderData();
        int rows = 0;
        int cols = 0;
        
        for (String line : data) {
            if (line.startsWith("DIMENSIONS:")) {
                // Parse dimensions
                String[] parts = line.split(":");
                rows = Integer.parseInt(parts[1]);
                cols = Integer.parseInt(parts[2]);
                
                // Update UI with dimensions
                _rowInput.setText(String.valueOf(rows));
                _colInput.setText(String.valueOf(cols));
                _createTable(null);
            } else {
                // Parse border data: row:col:top:right:bottom:left
                String[] parts = line.split(":");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                int top = Integer.parseInt(parts[2]);
                int right = Integer.parseInt(parts[3]);
                int bottom = Integer.parseInt(parts[4]);
                int left = Integer.parseInt(parts[5]);
                
                // Create border and store it
                MatteBorder border = new MatteBorder(top, left, bottom, right, Color.BLACK);
                _cellBorders.put(new Point(row, col), border);
            }
        }
        
        if (_table != null) {
            _table.repaint();
        }
    }
    
    /** Show a dialog to save data to a text area */
    private void _saveItem() {
        BorderItem item = _saveItem();
        JTextArea textArea = new JTextArea(String.join("\n", item.getBorderData()), 20, 30);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(this, scrollPane, "Save Data - Copy This Text", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /** Show a dialog to load data from a text area */
    private void _loadItem() {
        JTextArea textArea = new JTextArea(20, 30);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Paste your saved data here:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Load Data", 
                                                  JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String text = textArea.getText();
            if (!text.trim().isEmpty()) {
                String[] lines = text.split("\n");
                List<String> data = new ArrayList<>();
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        data.add(line.trim());
                    }
                }
                
                BorderItem item = new BorderItem();
                item.setBorderData(data);
                _currentItem = item;
                _loadItem(item);
                JOptionPane.showMessageDialog(this, "Item loaded and set as current");
            }
        }
    }

    /** Edit borders for selected cells */
    private void _editBordersForSelection() {
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        Point first = new Point(rows[0], cols[0]);
        MatteBorder current = _cellBorders.getOrDefault(first, new MatteBorder(0, 0, 0, 0, Color.BLACK));

        // Create a nicely styled border editing panel
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 245));
        
        JCheckBox top = new JCheckBox("Top", current.getBorderInsets().top > 0);
        JCheckBox bottom = new JCheckBox("Bottom", current.getBorderInsets().bottom > 0);
        JCheckBox left = new JCheckBox("Left", current.getBorderInsets().left > 0);
        JCheckBox right = new JCheckBox("Right", current.getBorderInsets().right > 0);
        
        // Style the checkboxes
        Font checkFont = new Font("Segoe UI", Font.PLAIN, 14);
        top.setFont(checkFont);
        bottom.setFont(checkFont);
        left.setFont(checkFont);
        right.setFont(checkFont);
        
        top.setBackground(new Color(240, 240, 245));
        bottom.setBackground(new Color(240, 240, 245));
        left.setBackground(new Color(240, 240, 245));
        right.setBackground(new Color(240, 240, 245));

        panel.add(top);
        panel.add(bottom);
        panel.add(left);
        panel.add(right);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Borders", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

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

    /** Copy borders from the first selected cell */
    private void _copyBordersFromSelection() {
        int row = _table.getSelectedRow();
        int col = _table.getSelectedColumn();
        if (row >= 0 && col >= 0) {
            _copiedBorder = _cellBorders.getOrDefault(new Point(row, col),
                    new MatteBorder(0, 0, 0, 0, Color.BLACK));
            JOptionPane.showMessageDialog(this, "Borders copied from (" + row + "," + col + ")");
        }
    }

    /** Paste borders to all selected cells */
    private void _pasteBordersToSelection() {
        if (_copiedBorder == null) {
            JOptionPane.showMessageDialog(this, "No borders copied yet!");
            return;
        }
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();

        for (int row : rows) {
            for (int col : cols) {
                _cellBorders.put(new Point(row, col), _copiedBorder);
            }
        }
        _table.repaint();
    }

    // ==== Styling helpers ====
    private void _styleLabel(JLabel lbl) {
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    private void _styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 210)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private void _styleButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 150)),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        // Add hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(90, 150, 200));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(70, 130, 180));
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            BorderDesignerApp app = new BorderDesignerApp();
            app.setVisible(true);
        });
    }
}