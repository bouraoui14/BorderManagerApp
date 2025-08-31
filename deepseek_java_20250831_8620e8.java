import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class BorderDesignerApp extends JFrame {

    private final JTextField _rowInput = new JTextField("6", 5);
    private final JTextField _colInput = new JTextField("6", 5);
    private final JButton _createButton = new JButton("Create Table");
    private final JButton _clearButton = new JButton("Clear Borders");
    private final JButton _resetButton = new JButton("Reset");
    private JCheckBox _groupingSeparatorCheckbox;

    private JTable _table;
    private final JPanel _tablePanel = new JPanel(new BorderLayout());
    private final Map<Point, MatteBorder> _cellBorders = new HashMap<>();
    private MatteBorder _copiedBorder = null;

    public BorderDesignerApp() {
        super("Border Designer");

        // ==== Top Control Panel ====
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        controlPanel.setBackground(new Color(245, 247, 250));

        JLabel rowLabel = new JLabel("Rows:");
        JLabel colLabel = new JLabel("Columns:");

        _styleLabel(rowLabel);
        _styleLabel(colLabel);
        _styleButton(_createButton, new Color(70, 130, 180)); // Blue for create
        _styleButton(_clearButton, new Color(220, 120, 0));   // Orange for clear
        _styleButton(_resetButton, new Color(200, 60, 60));   // Red for reset
        _styleTextField(_rowInput);
        _styleTextField(_colInput);

        controlPanel.add(rowLabel);
        controlPanel.add(_rowInput);
        controlPanel.add(colLabel);
        controlPanel.add(_colInput);
        controlPanel.add(_createButton);
        controlPanel.add(_clearButton);
        controlPanel.add(_resetButton);

        // ==== Main Layout ====
        setLayout(new BorderLayout(10, 10));
        add(controlPanel, BorderLayout.NORTH);
        add(_tablePanel, BorderLayout.CENTER);

        _createButton.addActionListener(this::_createTable);
        _clearButton.addActionListener(e -> _clearBorders());
        _resetButton.addActionListener(e -> _reset());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
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

        // Create table with one extra row for headers
        _table = new JTable(rows + 1, cols); // +1 for header row
        _table.setCellSelectionEnabled(true);
        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Remove table header
        _table.setTableHeader(null);

        // Set header values in first row
        for (int i = 0; i < cols; i++) {
            _table.setValueAt("Header " + (i + 1), 0, i);
        }

        // Renderer to draw borders for all cells, including header row (row 0)
        _table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(CENTER);
                
                // Style header row differently
                if (row == 0) {
                    c.setBackground(new Color(220, 230, 240)); // Light blue for headers
                    c.setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    c.setBackground(isSelected ? new Color(220, 235, 255) : Color.WHITE);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                
                c.setForeground(Color.DARK_GRAY);
                
                // Get border - use row -1 for headers to match your requirement
                int storageRow = row == 0 ? -1 : row - 1; // Header row is -1, data rows start at 0
                MatteBorder b = _cellBorders.getOrDefault(new Point(storageRow, col),
                        new MatteBorder(0, 0, 0, 0, Color.BLACK));
                c.setBorder(b);
                return c;
            }
        });

        // Context menu for table cells
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editBorders = new JMenuItem("Edit Borders");
        JMenuItem copyBorders = new JMenuItem("Copy Borders");
        JMenuItem pasteBorders = new JMenuItem("Paste Borders");

        editBorders.addActionListener(x -> _editBorders());
        copyBorders.addActionListener(x -> _copyBorders());
        pasteBorders.addActionListener(x -> _pasteBorders());

        contextMenu.add(editBorders);
        contextMenu.add(copyBorders);
        contextMenu.add(pasteBorders);

        _table.setComponentPopupMenu(contextMenu);

        // Create scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Create panel for grouping separator checkbox
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(new Color(245, 247, 250));
        
        _groupingSeparatorCheckbox = new JCheckBox("Add Grouping Separator");
        _styleCheckbox(_groupingSeparatorCheckbox);
        bottomPanel.add(_groupingSeparatorCheckbox);

        // Add components to table panel
        _tablePanel.add(scrollPane, BorderLayout.CENTER);
        _tablePanel.add(bottomPanel, BorderLayout.SOUTH);
        
        _tablePanel.revalidate();
        _tablePanel.repaint();
    }
    
    /** Unified method to edit borders for both cells and headers */
    private void _editBorders() {
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        // Convert row 0 to -1 for headers in storage, other rows to row-1
        int storageRow = rows[0] == 0 ? -1 : rows[0] - 1;
        Point first = new Point(storageRow, cols[0]);
        MatteBorder current = _cellBorders.getOrDefault(first, new MatteBorder(0, 0, 0, 0, Color.BLACK));

        // Create a nicely styled border editing panel
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 247, 250));
        
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
        
        top.setBackground(new Color(245, 247, 250));
        bottom.setBackground(new Color(245, 247, 250));
        left.setBackground(new Color(245, 247, 250));
        right.setBackground(new Color(245, 247, 250));

        panel.add(top);
        panel.add(bottom);
        panel.add(left);
        panel.add(right);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Borders", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            // Use thinner borders (1 pixel instead of the default)
            int t = top.isSelected() ? 1 : 0;
            int b = bottom.isSelected() ? 1 : 0;
            int l = left.isSelected() ? 1 : 0;
            int r = right.isSelected() ? 1 : 0;

            MatteBorder newBorder = new MatteBorder(t, l, b, r, Color.BLACK);

            for (int row : rows) {
                for (int col : cols) {
                    // Convert row 0 to -1 for headers in storage, other rows to row-1
                    int storageRowForCell = row == 0 ? -1 : row - 1;
                    _cellBorders.put(new Point(storageRowForCell, col), newBorder);
                }
            }
            _table.repaint();
        }
    }
    
    /** Unified method to copy borders from both cells and headers */
    private void _copyBorders() {
        int row = _table.getSelectedRow();
        int col = _table.getSelectedColumn();
        if (row < 0 || col < 0) return;

        // Convert row 0 to -1 for headers in storage, other rows to row-1
        int storageRow = row == 0 ? -1 : row - 1;
        _copiedBorder = _cellBorders.getOrDefault(new Point(storageRow, col), 
                new MatteBorder(0, 0, 0, 0, Color.BLACK));
        
        JOptionPane.showMessageDialog(this, "Borders copied from " + 
                (row == 0 ? "header column " + (col + 1) : "cell (" + (row - 1) + "," + col + ")"));
    }
    
    /** Unified method to paste borders to both cells and headers */
    private void _pasteBorders() {
        if (_copiedBorder == null) {
            JOptionPane.showMessageDialog(this, "No borders copied yet!");
            return;
        }
        
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        for (int row : rows) {
            for (int col : cols) {
                // Convert row 0 to -1 for headers in storage, other rows to row-1
                int storageRow = row == 0 ? -1 : row - 1;
                _cellBorders.put(new Point(storageRow, col), _copiedBorder);
            }
        }
        _table.repaint();
    }
    
    /** Clear all borders */
    private void _clearBorders() {
        _cellBorders.clear();
        if (_table != null) {
            _table.repaint();
        }
        JOptionPane.showMessageDialog(this, "All borders cleared");
    }
    
    /** Reset the entire application */
    private void _reset() {
        _cellBorders.clear();
        _rowInput.setText(""); // Clear the input field
        _colInput.setText(""); // Clear the input field
        if (_groupingSeparatorCheckbox != null) {
            _groupingSeparatorCheckbox.setSelected(false);
        }
        _tablePanel.removeAll();
        _tablePanel.revalidate();
        _tablePanel.repaint();
        JOptionPane.showMessageDialog(this, "Application reset");
    }

    // ==== Styling helpers ====
    private void _styleLabel(JLabel lbl) {
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(new Color(60, 60, 60));
    }

    private void _styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 210)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        field.setBackground(Color.WHITE);
    }

    private void _styleButton(JButton btn, Color color) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(
                Math.max(color.getRed() - 20, 0),
                Math.max(color.getGreen() - 20, 0),
                Math.max(color.getBlue() - 20, 0)
            )),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        // Add hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(
                    Math.min(color.getRed() + 20, 255),
                    Math.min(color.getGreen() + 20, 255),
                    Math.min(color.getBlue() + 20, 255)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(color);
            }
        });
    }
    
    private void _styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        checkbox.setBackground(new Color(245, 247, 250));
        checkbox.setFocusPainted(false);
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