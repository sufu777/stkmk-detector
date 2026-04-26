package cc.sufuzz.stkmkdetector.ui;

import cc.sufuzz.stkmkdetector.task.JavaFileIssue;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

public class UnClosedStaticMockScanResultPanel extends JPanel {
    private final Project project;
    private final JBTable issueTable;

    public UnClosedStaticMockScanResultPanel(Project project) {
        this.project = project;
        issueTable = new JBTable();
        DefaultTableModel defaultTableModel = new DefaultTableModel(new String[]{"文件", "行号", "描述"}, 0);
        issueTable.setModel(defaultTableModel);
        issueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // 导航到对应文件
                }
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(issueTable), BorderLayout.CENTER);
        Content content = ContentFactory.getInstance().createContent(panel, "", false);

    }

    public void updateScanResult(Collection<JavaFileIssue> issues) {
//        issueTable.setListData(issues.toArray(new JavaFileIssue[0]));
    }

    public void clearResult() {

    }
}
