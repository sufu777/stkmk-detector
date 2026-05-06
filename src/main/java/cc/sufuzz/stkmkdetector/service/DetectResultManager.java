package cc.sufuzz.stkmkdetector.service;

import cc.sufuzz.stkmkdetector.detectors.DetectResult;
import cc.sufuzz.stkmkdetector.detectors.UnClosedStaticMockIssue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class DetectResultManager {
    private final Project project;
    private final List<DetectResult> detectResults;

    public DetectResultManager(Project project) {
        this.project = project;
        this.detectResults = new ArrayList<>();
    }

    public void addDetectResult(DetectResult detectResult) {
        this.detectResults.add(detectResult);
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("Unclosed StaticMock");
        if (Objects.isNull(tw)) {
            throw new IllegalStateException("tool window is null");
        }
        Content content = newContent(detectResult);
        ApplicationManager.getApplication().invokeLater(() -> {
            ContentManager contentManager = tw.getContentManager();
            contentManager.addContent(content);
            if (contentManager.getContentCount() > 1) {
                contentManager.selectNextContent();
            }
        });
        if (!tw.isActive()) {
            ApplicationManager.getApplication().invokeLater(() -> tw.activate(null));
        }
    }

    private Content newContent(DetectResult detectResult) {
        Vector<String> columns = new Vector<>(Arrays.asList("文件", "静态Mock", "描述"));
        Vector<Vector<Object>> tableData = detectResult.getTableData();
        JBTable issueTable = new JBTable();
        DefaultTableModel defaultTableModel = new DefaultTableModel(tableData, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        issueTable.setModel(defaultTableModel);
//        issueTable.getColumnModel().getColumn(1).setCellRenderer(new NavigatorStaticMockLabel());
        issueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = issueTable.rowAtPoint(e.getPoint());
                    int col = issueTable.columnAtPoint(e.getPoint());
                    if (row != -1 && col == 1) {
                        // 双击第二列
                        UnClosedStaticMockIssue issue = detectResult.getIssues().get(row);
                        navigateToIssue(issue.vf(), issue.methodCallExpression());
                    }
                }
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(issueTable), BorderLayout.CENTER);
        Content scanResult = ContentFactory.getInstance().createContent(panel, "扫描结果", false);
        scanResult.setCloseable(true);
        return scanResult;
    }


    public void navigateToIssue(VirtualFile vf, PsiMethodCallExpression element) {
        if (!vf.isValid()) {
            Messages.showErrorDialog(project, "找不到文件: " + vf.getPath(), "导航失败");
            return;
        }
        int textOffset = element.getTextOffset();
        Document document = FileDocumentManager.getInstance().getDocument(vf);
        int line = document.getLineNumber(textOffset);
        int column = textOffset - document.getLineStartOffset(line);
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, vf, line, column);
        openFileDescriptor.navigate(true);
    }
}
