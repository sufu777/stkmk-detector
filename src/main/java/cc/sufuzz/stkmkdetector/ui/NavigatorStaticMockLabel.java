package cc.sufuzz.stkmkdetector.ui;

import com.intellij.ui.components.labels.LinkLabel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 表格列渲染器：在原有文本后面加一个可点击的“跳转”链接
 */
public class NavigatorStaticMockLabel implements TableCellRenderer {
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel textLabel = new JLabel();
    private final LinkLabel<Integer> jumpLink = new LinkLabel<>("跳转", null);

    public NavigatorStaticMockLabel() {
        panel.add(textLabel);
        panel.add(Box.createHorizontalStrut(6)); // 文字和“跳转”之间的间距
        panel.add(jumpLink);
        panel.setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String originalText = (value == null) ? "" : value.toString();
        textLabel.setText(originalText);
        jumpLink.setListener((source, rowNo) -> {
            // 从 TableModel 中获取该行存储的跳转目标信息（例如文件、行号）
            Object target = table.getModel().getValueAt(rowNo, column); // 或者单独一列存放目标
            doJumpToSource(table, rowNo, target);
        }, row);
        Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
        panel.setBackground(bg);
        textLabel.setBackground(bg);
        jumpLink.setBackground(bg);

        // 普通文字的前景色也要跟随选中状态变化
        Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
        textLabel.setForeground(fg);

        return panel;
    }

    private void doJumpToSource(JTable table, int row, Object cellValue) {
        System.out.println(table.getModel().getValueAt(row, 0));
    }
}
