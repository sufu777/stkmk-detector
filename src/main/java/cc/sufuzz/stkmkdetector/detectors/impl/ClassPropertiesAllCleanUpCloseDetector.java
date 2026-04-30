package cc.sufuzz.stkmkdetector.detectors.impl;

import cc.sufuzz.stkmkdetector.UnClosedStaticMockIssue;
import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;

import java.util.*;

/**
 * 扫描 @BeforeAll 中初始化但未在 @AfterAll 中关闭的 StaticMock
 */
public class ClassPropertiesAllCleanUpCloseDetector extends BaseDetector implements UnCloseDetector {
    @Override
    public String name() {
        return "classPropertiesAllCleanUpCloseDetector";
    }

    @Override
    public List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator) {
        Collection<VirtualFile> virtualFiles = super.testFiles(project);
        PsiClass beforeAllAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.BeforeAll", GlobalSearchScope.allScope(project));
        // 未引入该依赖
        if (beforeAllAnnotation == null) return Collections.emptyList();
        // 开始遍历
        ArrayList<UnClosedStaticMockIssue> issues = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            if (!virtualFile.isValid()) continue;
            if (progressIndicator.isCanceled()) throw new ProcessCanceledException();
            issues.addAll(doDetectVirtualFile(project, beforeAllAnnotation, virtualFile));
        }
        return issues.isEmpty() ? Collections.emptyList() : issues;
    }

    private List<UnClosedStaticMockIssue> doDetectVirtualFile(Project project, PsiClass beforeAllAnnotation, VirtualFile vf) {
        PsiJavaFile pjf = (PsiJavaFile) PsiManager.getInstance(project).findFile(vf);
        if (Objects.isNull(pjf)) return Collections.emptyList();

        Collection<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(beforeAllAnnotation, GlobalSearchScope.fileScope(pjf)).findAll();


    }

    private List<PsiField> fields() {

    }
}
