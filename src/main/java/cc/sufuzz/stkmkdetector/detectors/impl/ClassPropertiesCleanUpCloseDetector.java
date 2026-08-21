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

import static cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool.METHOD_CLOSE;
import static cc.sufuzz.stkmkdetector.detectors.tool.PsiSearchTool.METHOD_CLOSE_ON_DAEMON;

/**
 * 扫描定义了类的属性，且在 @BeforeAll/@BeforeEach 中初始化但未在 @AfterAll/@AfterEach 中关闭的 StaticMock<br/>
 * 为了简化扫描次数，不区分两种 setup 和 cleanup 方法，即在 @BeforeAll 中初始化但在 @AfterEach 中关闭也认为是合法的，一般不会有这种用法，因为单测运行会报错
 */
public class ClassPropertiesCleanUpCloseDetector extends BaseDetector implements UnCloseDetector {
    @Override
    public String name() {
        return "classPropertiesCleanUpCloseDetector";
    }

    @Override
    public String description() {
        return "类级别的 staticMock 如果在 @BeforeAll/@BeforeEach 中初始化了staticMock，那么就应该在 @AfterAll/@AfterEach 中关闭";
    }

    @Override
    public List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator) {
        progressIndicator.setText2("Apply Scanner " + name());
        // junit5 相关注解
        EnhanceAnnotation enhanceAnnotation = new EnhanceAnnotation(project);
        Collection<VirtualFile> virtualFiles = super.testFiles(project);
        // 开始遍历
        ArrayList<UnClosedStaticMockIssue> issues = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            if (!virtualFile.isValid()) continue;
            if (progressIndicator.isCanceled()) throw new ProcessCanceledException();
            issues.addAll(doDetectVirtualFile(project, enhanceAnnotation, virtualFile));
        }
        return issues.isEmpty() ? Collections.emptyList() : issues;
    }

    private List<UnClosedStaticMockIssue> doDetectVirtualFile(Project project, EnhanceAnnotation enhanceAnnotation, VirtualFile vf) {
        PsiJavaFile pjf = ReadAction.compute(() -> (PsiJavaFile) PsiManager.getInstance(project).findFile(vf));
        if (Objects.isNull(pjf)) return Collections.emptyList();
        // 搜索 setup 方法和 cleanup 方法  此处junit 4/5 一起扫描，不考虑混用的情况
        List<PsiMethod> setupMethods = new ArrayList<>();
        Optional.ofNullable(enhanceAnnotation.beforeAllAnnotation)
                .ifPresent(b -> setupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.beforeEachAnnotation)
                .ifPresent(b -> setupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.beforeAnnotation)
                .ifPresent(b -> setupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.beforeClassAnnotation)
                .ifPresent(b -> setupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        if (setupMethods.isEmpty()) return Collections.emptyList();
        List<PsiMethod> cleanupMethods = new ArrayList<>();
        Optional.ofNullable(enhanceAnnotation.afterAllAnnotation)
                .ifPresent(b -> cleanupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.afterEachAnnotation)
                .ifPresent(b -> cleanupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.afterAnnotation)
                .ifPresent(b -> cleanupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));
        Optional.ofNullable(enhanceAnnotation.afterClassAnnotation)
                .ifPresent(b -> cleanupMethods.addAll(ReadAction.compute(() -> AnnotatedElementsSearch.searchPsiMethods(b, GlobalSearchScope.fileScope(pjf)).findAll())));

        var shouldCloseInCleanup = new ArrayList<PsiAssignmentExpression>();
        List<UnClosedStaticMockIssue> issues = new ArrayList<>();

        // 搜索 setup 中的所有 mockStatic 方法
        for (PsiMethod setupMethod : setupMethods) {
            Collection<PsiMethodCallExpression> methodCallExpressions = ReadAction.compute(() -> PsiTreeUtil.findChildrenOfType(setupMethod, PsiMethodCallExpression.class));
            List<PsiMethodCallExpression> mockStaticCalls = methodCallExpressions.stream()
                    .filter(m -> "mockStatic".equals(m.getMethodExpression().getReferenceName()))
                    .toList();
            for (PsiMethodCallExpression mockStaticCall : mockStaticCalls) {
                PsiElement parentElement = mockStaticCall.getParent();
                if (parentElement instanceof PsiAssignmentExpression ae) {
                    // 是一个赋值语句，存在两种可能：1、在当前方法前面定义了变量；2、在类属性定义了变量
                    // 其中需要优先检查是否在方法体中定义了该变量，因为同名本地变量覆盖
                    PsiExpression lExpression = ae.getLExpression();
                    String variableName;
                    if (lExpression instanceof PsiReferenceExpression pre) {
                        variableName = pre.getReferenceName();
                    } else {
                        variableName = ReadAction.compute(lExpression::getText);
                    }
                    if (PsiSearchTool.searchPsiLocalVariableFromMethodBody(variableName, ae) == null) {
                        // 如果 psiLocalVariable 为空，说明不是在此代码块中定义的，是类属性，需要在清理方法中关闭
                        // 忽略语法错误的情况
                        shouldCloseInCleanup.add(ae);
                    } else {
                        // 在当前方法中定义，需要在当前方法中关闭。
                        // 该分支为 staticMock 定义和赋值分开的写法，如果是 try-with-resource, parentElement 不会是 PsiAssignmentExpression，而是 PsiLocalVariable
                        // 忽略 if 条件等条件初始化的情况，在当前代码块赋值就要在当前代码块关闭，忽略其他情况
                        if (PsiSearchTool.searchStaticMockCloseInPcb(variableName, ae) == null) {
                            // 当前代码块中没有关闭，添加 issues
                            issues.add(new UnClosedStaticMockIssue(vf, mockStaticCall, description()));
                        }
                    }
                } else if (parentElement instanceof PsiLocalVariable lv) {
                    // setup 方法中定义的变量要在其中关闭
                    PsiResourceList resourceList = ReadAction.compute(() -> PsiTreeUtil.getParentOfType(lv, PsiResourceList.class));
                    // 在 try-with-resource 中，忽略
                    if (resourceList != null) continue;
                    // 在代码块中搜索 close 调用，如果没有close，添加到 issues
                    if (PsiSearchTool.searchStaticMockCloseInPcb(lv.getName(), lv) == null) {
                        issues.add(new UnClosedStaticMockIssue(vf, mockStaticCall, description()));
                    }
                }
            }
        }
        issues.addAll(closedInCleanupMethod(shouldCloseInCleanup, cleanupMethods, vf));
        return issues;
    }

    private List<UnClosedStaticMockIssue> closedInCleanupMethod(List<PsiAssignmentExpression> staticMockCalls, Collection<PsiMethod> cleanupMethods, VirtualFile vf) {
        for (PsiMethod cleanupMethod : cleanupMethods) {
            PsiCodeBlock codeBlock = cleanupMethod.getBody();
            if (Objects.isNull(codeBlock)) continue;
            ReadAction.compute(() -> PsiTreeUtil.processElements(codeBlock, PsiMethodCallExpression.class, mce -> {
                PsiReferenceExpression methodExpression = mce.getMethodExpression();
                PsiElement variable = methodExpression.getQualifier();
                String methodName = methodExpression.getReferenceName();
                if (Objects.nonNull(variable) && (METHOD_CLOSE.equals(methodName) || METHOD_CLOSE_ON_DAEMON.equals(methodName))) {
                    String staticMockName = variable instanceof PsiReferenceExpression pre ? pre.getReferenceName() : variable.getText();
                    if (Objects.isNull(staticMockName)) {
                        return true;
                    }
                    staticMockCalls.removeIf(ae -> {
                        PsiExpression lExpression = ae.getLExpression();
                        if (lExpression instanceof PsiReferenceExpression pre) {
                            return staticMockName.equals(pre.getReferenceName());
                        }
                        return staticMockName.equals(lExpression.getText());
                    });
                }
                return true;
            }));
        }
        return staticMockCalls.stream().map(ae -> new UnClosedStaticMockIssue(vf, (PsiMethodCallExpression) ae.getRExpression(), description())).toList();
    }
}
