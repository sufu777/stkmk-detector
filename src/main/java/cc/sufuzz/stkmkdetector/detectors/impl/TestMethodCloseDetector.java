package cc.sufuzz.stkmkdetector.detectors.impl;

import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.UnClosedStaticMockIssue;
import cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.*;

/**
 * 搜索测试方法中定义的 staticMock
 */
public class TestMethodCloseDetector extends BaseDetector implements UnCloseDetector {
    @Override
    public String name() {
        return "testMethodCloseDetector";
    }

    @Override
    public String description() {
        return "测试方法中的本地变量应该在测试方法本身中关闭";
    }

    @Override
    public List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator) {
        Collection<VirtualFile> virtualFiles = super.testFiles(project);
        ArrayList<UnClosedStaticMockIssue> issues = new ArrayList<>();
        for (VirtualFile vf : virtualFiles) {
            if (!vf.isValid()) continue;
            if (progressIndicator.isCanceled()) throw new ProcessCanceledException();
            List<UnClosedStaticMockIssue> unClosedStaticMockIssues = this.doDetectVirtualFile(project, vf);
            issues.addAll(unClosedStaticMockIssues);
        }
        return issues.isEmpty() ? Collections.emptyList() : issues;
    }

    private List<UnClosedStaticMockIssue> doDetectVirtualFile(Project project, VirtualFile vf) {
        PsiJavaFile pjf = ReadAction.compute(() -> (PsiJavaFile) PsiManager.getInstance(project).findFile(vf));
        if (Objects.isNull(pjf)) return Collections.emptyList();
        ArrayList<PsiMethod> psiMethods = new ArrayList<>();
        PsiClass j5TestAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.Test", GlobalSearchScope.allScope(project)));
        Optional.ofNullable(j5TestAnnotation).ifPresent(j5t -> psiMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(j5t, GlobalSearchScope.fileScope(pjf)).findAll())));
        PsiClass j4TestAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.Test", GlobalSearchScope.allScope(project)));
        Optional.ofNullable(j4TestAnnotation).ifPresent(j4t -> psiMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(j4t, GlobalSearchScope.fileScope(pjf)).findAll())));
        return psiMethods.stream().flatMap(m -> this.doDetectTestMethod(m, vf).stream()).toList();
    }

    private List<UnClosedStaticMockIssue> doDetectTestMethod(PsiMethod testMethod, VirtualFile vf) {
        // 搜索当前测试方法中的 staticMock
        Collection<PsiMethodCallExpression> methodCallExpressions = ReadAction.compute(() -> PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression.class));
        List<PsiMethodCallExpression> mockStaticCalls = methodCallExpressions.stream()
                .filter(m -> "mockStatic".equals(m.getMethodExpression().getReferenceName()))
                .toList();
        List<UnClosedStaticMockIssue> issues = new ArrayList<>();
        // 该搜索器不考虑类属性赋值的情况，仅在当前代码块寻找关闭方法
        for (PsiMethodCallExpression mockStaticCall : mockStaticCalls) {
            PsiElement parent = mockStaticCall.getParent();
            if (parent instanceof PsiAssignmentExpression pae) {
                PsiExpression lExpression = pae.getLExpression();
                if (lExpression instanceof PsiReferenceExpression pre) {
                    // PsiReferenceExpression 表明是一个引用变量，不是复合表达式如 a.b 等
                    String variableName = ReadAction.compute(pre::getText);
                    PsiMethodCallExpression closeMethodCall = PsiSearchTool.searchStaticMockCloseInPcb(variableName, pre);
                    if (Objects.isNull(closeMethodCall)) {
                        issues.add(new UnClosedStaticMockIssue(vf, mockStaticCall, description()));
                    }
                }
            } else if (parent instanceof PsiLocalVariable plv) {
                // 本地变量，要检查是否在try-with-resource中
                PsiResourceList resourceList = ReadAction.compute(() -> PsiTreeUtil.getParentOfType(plv, PsiResourceList.class));
                if (Objects.nonNull(resourceList)) continue;
                PsiMethodCallExpression closeMethodCall = PsiSearchTool.searchStaticMockCloseInPcb(plv.getName(), plv);
                if (Objects.isNull(closeMethodCall)) {
                    issues.add(new UnClosedStaticMockIssue(vf, mockStaticCall, description()));
                }
            }
        }
        return issues.isEmpty() ? Collections.emptyList() : issues;
    }
}
