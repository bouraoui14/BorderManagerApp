import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BorderDesignerApp extends JFrame {

    private JTable _table;
    private DefaultTableModel _model;
    private Map<Point, BorderAttributes> _borderMap = new HashMap<>();
    private BorderAttributes _copiedBorder;
    private JCheckBox _groupingSeparatorCheckBox;

    public BorderDesignerApp() {
        setTitle("Border Designer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Table model ===
        _model = new DefaultTableModel(10, 5); // 10 rows, 5 cols
        _table = new JTable(_model) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new CustomCellRenderer();
            }
        };
        _table.setCellSelectionEnabled(true);

        JScrollPane scrollPane = new JScrollPane(_table);
        add(scrollPane, BorderLayout.CENTER);

        // === Add grouping checkbox under table ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        _groupingSeparatorCheckBox = new JCheckBox("Add Grouping Separator");
        bottomPanel.add(_groupingSeparatorCheckBox);
        add(bottomPanel, BorderLayout.SOUTH);

        // === Setup popup menu ===
        setupPopupMenu();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editBorders = new JMenuItem("Edit Borders...");
        JMenuItem copyBorders = new JMenuItem("Copy Borders");
        JMenuItem pasteBorders = new JMenuItem("Paste Borders");

        // === Edit Borders ===
        editBorders.addActionListener(e -> {
            Point[] points = getSelectedPoints();
            if (points.length > 0) editBorders(points);
        });

        // === Copy Borders ===
        copyBorders.addActionListener(e -> {
            Point[] points = getSelectedPoints();
            if (points.length > 0) copyBorders(points[0]);
        });

        // === Paste Borders ===
        pasteBorders.addActionListener(e -> {
            if (_copiedBorder == null) {
                JOptionPane.showMessageDialog(this, "No borders copied yet!");
                return;
            }
            Point[] points = getSelectedPoints();
            if (points.length > 0) pasteBorders(points);
        });

        popup.add(editBorders);
        popup.add(copyBorders);
        popup.add(pasteBorders);

        // Attach popup to both table and header
        _table.setComponentPopupMenu(popup);
        _table.getTableHeader().setComponentPopupMenu(popup);
    }

    private Point[] getSelectedPoints() {
        if (_table.getTableHeader().getMousePosition() != null) {
            int col = _table.getTableHeader().getColumnModel()
                    .getSelectionModel().getLeadSelectionIndex();
            if (col >= 0) return new Point[]{ new Point(-1, col) };
        } else {
            int[] rows = _table.getSelectedRows();
            int[] cols = _table.getSelectedColumns();
            java.util.List<Point> pts = new ArrayList<>();
            for (int r : rows) for (int c : cols) pts.add(new Point(r, c));
            return pts.toArray(new Point[0]);
        }
        return new Point[0];
    }

    private void editBorders(Point[] points) {
        JCheckBox top = new JCheckBox("Top");
        JCheckBox bottom = new JCheckBox("Bottom");
        JCheckBox left = new JCheckBox("Left");
        JCheckBox right = new JCheckBox("Right");

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(top);
        panel.add(bottom);
        panel.add(left);
        panel.add(right);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Select Borders", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            for (Point p : points) {
                _borderMap.put(p, new BorderAttributes(
                        top.isSelected(), bottom.isSelected(),
                        left.isSelected(), right.isSelected()
                ));
            }
            _table.repaint();
        }
    }

    private void copyBorders(Point p) {
        BorderAttributes attrs = _borderMap.get(p);
        if (attrs != null) {
            _copiedBorder = attrs.clone();
        }
    }

    private void pasteBorders(Point[] points) {
        for (Point p : points) {
            _borderMap.put(p, _copiedBorder.clone());
        }
        _table.repaint();
    }

    // === Renderer ===
    private class CustomCellRenderer extends JLabel implements TableCellRenderer {
        public CustomCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value == null ? "" : value.toString());

            BorderAttributes attrs = _borderMap.get(new Point(row, column));
            if (attrs == null) attrs = new BorderAttributes(false, false, false, false);

            int top = attrs.top ? 1 : 0;
            int bottom = attrs.bottom ? 1 : 0;
            int left = attrs.left ? 1 : 0;
            int right = attrs.right ? 1 : 0;

            setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));

            if (isSelected) {
                setBackground(new Color(200, 200, 255));
            } else {
                setBackground(Color.WHITE);
            }
            return this;
        }
    }

    // === Border Attributes ===
    private static class BorderAttributes implements Cloneable {
        boolean top, bottom, left, right;

        BorderAttributes(boolean t, boolean b, boolean l, boolean r) {
            top = t; bottom = b; left = l; right = r;
        }

        @Override
        protected BorderAttributes clone() {
            return new BorderAttributes(top, bottom, left, right);
        }
    }

    // === Main ===
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BorderDesignerApp::new);
    }
}
