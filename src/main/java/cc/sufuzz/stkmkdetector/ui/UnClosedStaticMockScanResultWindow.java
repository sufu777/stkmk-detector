package cc.sufuzz.stkmkdetector.ui;

import cc.sufuzz.stkmkdetector.service.StaticMockViewManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UnClosedStaticMockScanResultWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JBTable issueTable = new JBTable();
        DefaultTableModel defaultTableModel = new DefaultTableModel(new String[]{"文件", "行号", "描述"}, 0);
        issueTable.setModel(defaultTableModel);
        issueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object source = e.getSource();
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(issueTable), BorderLayout.CENTER);

        StaticMockViewManager manager = project.getService(StaticMockViewManager.class);
        manager.setTable(issueTable, defaultTableModel);

        Content resultContent = ContentFactory.getInstance().createContent(panel, "扫描结果", false);
        toolWindow.getContentManager().addContent(resultContent);
    }
}
