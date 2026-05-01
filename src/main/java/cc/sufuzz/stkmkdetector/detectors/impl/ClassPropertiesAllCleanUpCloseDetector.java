package cc.sufuzz.stkmkdetector.detectors.impl;

import cc.sufuzz.stkmkdetector.UnClosedStaticMockIssue;
import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool.METHOD_CLOSE;
import static cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool.METHOD_CLOSE_ON_DAEMON;

/**
 * 扫描定义了类的属性，且在 @BeforeAll 中初始化但未在 @AfterAll 中关闭的 StaticMock
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
        PsiClass afterAllAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.AfterAll", GlobalSearchScope.allScope(project));
        // 开始遍历
        ArrayList<UnClosedStaticMockIssue> issues = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            if (!virtualFile.isValid()) continue;
            if (progressIndicator.isCanceled()) throw new ProcessCanceledException();
            issues.addAll(doDetectVirtualFile(project, beforeAllAnnotation, afterAllAnnotation, virtualFile));
        }
        return issues.isEmpty() ? Collections.emptyList() : issues;
    }

    private List<UnClosedStaticMockIssue> doDetectVirtualFile(Project project, PsiClass beforeAllAnnotation, PsiClass afterAllAnnotation, VirtualFile vf) {
        PsiJavaFile pjf = (PsiJavaFile) PsiManager.getInstance(project).findFile(vf);
        if (Objects.isNull(pjf)) return Collections.emptyList();
        Collection<PsiMethod> setupMethods = AnnotatedElementsSearch.searchPsiMethods(beforeAllAnnotation, GlobalSearchScope.fileScope(pjf)).findAll();
        Collection<PsiMethod> cleanupMethods = AnnotatedElementsSearch.searchPsiMethods(afterAllAnnotation, GlobalSearchScope.fileScope(pjf)).findAll();
        var shouldCloseInCleanup = new ArrayList<PsiMethodCallExpression>();
        List<UnClosedStaticMockIssue> issues = new ArrayList<>();
        for (PsiMethod setupMethod : setupMethods) {
            Collection<PsiMethodCallExpression> methodCallExpressions = PsiTreeUtil.findChildrenOfType(setupMethod, PsiMethodCallExpression.class);
            List<PsiMethodCallExpression> mockStaticCalls = methodCallExpressions.stream()
                    .filter(m -> "mockStatic".equals(m.getMethodExpression().getReferenceName()))
                    .toList();
            for (PsiMethodCallExpression mockStaticCall : mockStaticCalls) {
                PsiElement parentElement = mockStaticCall.getParent();
                if (parentElement instanceof PsiAssignmentExpression ae) {
                    // 赋值，存在两种可能：1、在当前方法前面定义了变量；2、在类属性定义了变量
                    PsiExpression lExpression = ae.getLExpression();
                    String variableName = lExpression.getText();
                    PsiCodeBlock variableCb = PsiTreeUtil.getParentOfType(ae, PsiCodeBlock.class);
                    if (Objects.isNull(variableCb)) continue;
                    if (PsiSearchTool.searchPsiLocalVariableInPcb(variableName, variableCb) == null) {
                        // 如果 psiLocalVariable 为空，说明不是在此代码块中定义的，是类属性，需要在清理方法中关闭
                        shouldCloseInCleanup.add(mockStaticCall);
                    } else {
                        // 在当前方法中定义，需要在当前方法中关闭
                        if (PsiSearchTool.searchStaticMockCloseInPcb(variableName, variableCb) == null) {
                            // 当前代码块中没有关闭，添加 issues
                            issues.add(new UnClosedStaticMockIssue(vf.getPath(), vf.getName(), mockStaticCall));
                        }
                    }
                } else if (parentElement instanceof PsiLocalVariable lv) {
                    // setup 方法中定义的变量要在其中关闭
                    PsiResourceList resourceList = PsiTreeUtil.getParentOfType(lv, PsiResourceList.class);
                    // 在 try-with-resource 中，忽略
                    if (resourceList != null) continue;
                    // 在代码块中搜索 close 调用，如果没有close，添加到 issues
                    PsiCodeBlock cb = PsiTreeUtil.getParentOfType(lv, PsiCodeBlock.class);
                    PsiSearchTool.searchStaticMockCloseInPcb(lv.getName(), cb);
                }
            }
        }
        issues.addAll(closedInCleanupMethod(shouldCloseInCleanup, project, pjf, cleanupMethods));
        return issues;
    }

    private List<UnClosedStaticMockIssue> closedInCleanupMethod(List<PsiMethodCallExpression> staticMockCalls, Project project, PsiJavaFile pjf, Collection<PsiMethod> cleanupMethods) {
        Set<String> variables = staticMockCalls.stream().map(mc -> (PsiAssignmentExpression) mc.getParent())
                .map(pae -> pae.getLExpression().getText())
                .collect(Collectors.toSet());
        PsiElementProcessor.CollectElements<PsiMethodCallExpression> closeMethodCallCollector = new PsiElementProcessor.CollectElements<>() {
            @Override
            public boolean execute(@NotNull PsiMethodCallExpression methodCallExpression) {
                PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
                PsiElement variable = methodExpression.getQualifier();
                String methodName = methodExpression.getReferenceName();
                if (Objects.nonNull(variable) && variables.contains(variable.getText()) && (METHOD_CLOSE.equals(methodName) || METHOD_CLOSE_ON_DAEMON.equals(methodName))) {
                    return super.execute(methodCallExpression);
                }
                return true;
            }
        };

        for (PsiMethod cleanupMethod : cleanupMethods) {
            PsiCodeBlock body = cleanupMethod.getBody();
            if (Objects.isNull(body)) continue;
            PsiTreeUtil.processElements(body, closeMethodCallCollector);
        }
        return Collections.emptyList();
    }
}
