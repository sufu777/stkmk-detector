package cc.sufuzz.stkmkdetector.service;

import cc.sufuzz.stkmkdetector.task.JavaFileIssue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.table.JBTable;

import javax.swing.table.DefaultTableModel;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class DetectedResultViewManager {
    private final Project project;
    private JBTable resultTable;
    private DefaultTableModel tableModel;

    public DetectedResultViewManager(Project project) {
        this.project = project;
    }

    public void setTable(JBTable table, DefaultTableModel model) {
        this.resultTable = table;
        this.tableModel = model;
    }

    public void updateIssues(List<JavaFileIssue> issues) {
        if (tableModel == null) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            tableModel.setRowCount(0);
            for (JavaFileIssue issue : issues) {
                tableModel.addRow(new Object[]{issue.fileName(), issue, "1231231"});
            }
        });
    }

    public void navigateToIssue(PsiJavaFile psiJavaFile, PsiElement element) {
        if (!psiJavaFile.isValid()) {
            Messages.showErrorDialog(project, "找不到文件: " + psiJavaFile.getProject().getBasePath(), "导航失败");
            return;
        }
        int textOffset = element.getTextOffset();
        Document document = FileDocumentManager.getInstance().getDocument(psiJavaFile.getVirtualFile());
        int line = document.getLineNumber(textOffset);
        int column = textOffset - document.getLineStartOffset(line);
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiJavaFile.getVirtualFile(), line, column);
        openFileDescriptor.navigate(true);
    }
}
