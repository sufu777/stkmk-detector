package cc.sufuzz.stkmkdetector;

import cc.sufuzz.stkmkdetector.task.JavaFileIssue;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class StaticMockCloseVisitor extends JavaElementVisitor {
    private final JavaFileIssue issue;
    private final Project project;

    public StaticMockCloseVisitor(JavaFileIssue issue, Project project) {
        Objects.requireNonNull(issue);
        Objects.requireNonNull(project);
        this.issue = issue;
        this.project = project;
    }

    @Override
    public void visitJavaFile(@NotNull PsiJavaFile file) {
        this.searchMethodLevel(file);
        this.searchClassFieldLevel(file);
    }

    /**
     * 搜索类属性形式定义的 staticMock，要在 beforeEach/beforeAll 中对其赋值且在 afterEach/afterAll
     *
     * @param file
     */
    public void searchClassFieldLevel(PsiJavaFile file) {
        // 当前文件定义的所有类，只处理第一个
        PsiClass[] classes = file.getClasses();
        if (classes.length == 0) {
            // 空文件不处理
            return;
        }
        PsiClass topClass = classes[0];
        PsiField[] fields = topClass.getFields();
        // 类级别的 MockedStatic
        List<PsiField> mockStaticFields = Arrays.stream(fields)
                .filter(f -> f.getType() instanceof PsiClassReferenceType)
                .filter(f -> ((PsiClassReferenceType) f.getType()).getClassName().equals("MockedStatic"))
                .toList();
        List<PsiField> assignedFields = this.searchAssignedStaticVariableNames(mockStaticFields, file);
        List<PsiField> unclosedFields = this.searchForUnClosed(assignedFields, file);
        issue.addAllFields(unclosedFields);
    }

    public void searchMethodLevel(PsiJavaFile psiJavaFile) {
        // Junit 5，暂不支持junit4
        PsiClass j5TestAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.Test", GlobalSearchScope.allScope(project));
        if (j5TestAnnotation == null) {
            return;
        }
        // 所有测试方法
        Collection<PsiMethod> testMethods = AnnotatedElementsSearch.searchPsiMethods(j5TestAnnotation, GlobalSearchScope.fileScope(psiJavaFile)).findAll();
        for (PsiMethod testMethod : testMethods) {
            PsiCodeBlock codeBlock = testMethod.getBody();
            if (codeBlock == null) {
                return;
            }
            codeBlock.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                    PsiReferenceExpression methodExpression = expression.getMethodExpression();
                    String referenceName = methodExpression.getReferenceName();
                    PsiCodeBlock stkCodeBlock = PsiTreeUtil.getParentOfType(expression, PsiCodeBlock.class);
                    if (stkCodeBlock == null) {
                        // ERROR 有语法错误，不管
                        return;
                    }
                    if ("mockStatic".equals(referenceName)) {
                        PsiElement parent = expression.getParent();
                        if (parent instanceof PsiAssignmentExpression assignmentExpression) {
                            // 直接赋值语句 可能是在前面定义 也有可能是在类下定义，两种情况均要在当前方法块中关闭
                            if (staticMockNotCloseInCodeBlock(assignmentExpression.getLExpression().getText(), stkCodeBlock)) {
                                issue.addMethodCall(expression);
                            }
                        } else if (parent instanceof PsiLocalVariable localVariable) {
                            // 本地变量
                            PsiResourceList resourceExpression = PsiTreeUtil.getParentOfType(localVariable, PsiResourceList.class);
                            if (resourceExpression != null) {
                                // try-with-resource 直接退出
                                return;
                            }
                            if (staticMockNotCloseInCodeBlock(localVariable.getName(), stkCodeBlock)) {
                                issue.addMethodCall(expression);
                            }
                        }
                    }
                }
            });
        }
    }

    private List<PsiField> searchForUnClosed(List<PsiField> assignedFields, @NotNull PsiJavaFile psiJavaFile) {
        if (assignedFields.isEmpty()) {
            return Collections.emptyList();
        }
        PsiClass beforeEachAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.AfterEach", GlobalSearchScope.allScope(project));
        PsiClass beforeAllAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.AfterAll", GlobalSearchScope.allScope(project));
        Collection<PsiMethod> cleanMethods = new ArrayList<>();
        if (beforeEachAnnotation != null) {
            cleanMethods.addAll(AnnotatedElementsSearch.searchPsiMethods(beforeEachAnnotation, GlobalSearchScope.fileScope(psiJavaFile)).findAll());
        }
        if (beforeAllAnnotation != null) {
            cleanMethods.addAll(AnnotatedElementsSearch.searchPsiMethods(beforeAllAnnotation, GlobalSearchScope.fileScope(psiJavaFile)).findAll());
        }
        if (cleanMethods.isEmpty()) {
            return assignedFields;
        }
        Set<String> variableNames = assignedFields.stream().map(PsiField::getName).collect(Collectors.toSet());
        Set<String> unclosedVariables = new HashSet<>();
        for (PsiMethod cleanMethod : cleanMethods) {
            PsiCodeBlock cleanMethodBody = cleanMethod.getBody();
            if (cleanMethodBody == null) {
                continue;
            }
            unclosedVariables.addAll(this.staticMockNotCloseInCodeBlock(variableNames, cleanMethodBody));
        }
        return assignedFields.stream().filter(f -> unclosedVariables.contains(f.getName())).toList();
    }

    /**
     * 对于每一个类级别的 staticMock，在beforeEach,beforeAll中寻找是否对其赋值
     *
     * @param mockStaticFields class 级别定义的 staticMock 属性列表
     * @param psiJavaFile      当前 Java 文件
     * @return 在初始化方法中对类级别 staticMock 做了赋值的变量列表
     */
    private List<PsiField> searchAssignedStaticVariableNames(List<PsiField> mockStaticFields, PsiJavaFile psiJavaFile) {
        if (mockStaticFields.isEmpty()) {
            return Collections.emptyList();
        }
        PsiClass beforeEachAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.BeforeEach", GlobalSearchScope.allScope(project));
        PsiClass beforeAllAnnotation = JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.BeforeAll", GlobalSearchScope.allScope(project));
        Collection<PsiMethod> setupMethods = new ArrayList<>();
        if (beforeEachAnnotation != null) {
            Query<PsiMethod> psiMethodsQry = AnnotatedElementsSearch.searchPsiMethods(beforeEachAnnotation, GlobalSearchScope.fileScope(psiJavaFile));
            setupMethods.addAll(psiMethodsQry.findAll());
        }
        if (beforeAllAnnotation != null) {
            Query<PsiMethod> psiMethodsQry = AnnotatedElementsSearch.searchPsiMethods(beforeAllAnnotation, GlobalSearchScope.fileScope(psiJavaFile));
            setupMethods.addAll(psiMethodsQry.findAll());
        }
        if (setupMethods.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> declaredStaticNameSet = mockStaticFields.stream().map(PsiField::getName).collect(Collectors.toSet());
        Set<String> assignedStaticNameSet = new HashSet<>();
        // 检测是否其中有对 mockStaticFields 的赋值
        for (PsiMethod setupMethod : setupMethods) {
            PsiCodeBlock methodBody = setupMethod.getBody();
            if (methodBody != null) {
                methodBody.accept(new JavaRecursiveElementVisitor() {
                    @Override
                    public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
                        String variableName = expression.getLExpression().getText();
                        if (declaredStaticNameSet.contains(variableName)) {
                            assignedStaticNameSet.add(variableName);
                        }
                    }
                });
            }
        }

        return mockStaticFields.stream().filter(f -> assignedStaticNameSet.contains(f.getName())).collect(Collectors.toList());
    }


    /**
     * 搜索代码块中的未调用关闭方法的变量
     *
     * @param variableNames 要搜索的变量
     * @param codeBlock     搜索范围
     */
    public Set<String> staticMockNotCloseInCodeBlock(Set<String> variableNames, PsiCodeBlock codeBlock) {
        codeBlock.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String methodName = methodExpression.getReferenceName();
                if (methodExpression.getQualifier() != null) {
                    String objectName = methodExpression.getQualifier().getText();
                    if ("close".equals(methodName) || "closeOnDaemon".equals(methodName)) {
                        variableNames.remove(objectName);
                    }
                }
            }
        });
        return variableNames;
    }

    public boolean staticMockNotCloseInCodeBlock(String variableName, PsiCodeBlock codeBlock) {
        final boolean[] result = {true};
        codeBlock.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String methodName = methodExpression.getReferenceName();
                if (methodExpression.getQualifier() != null) {
                    String objectName = methodExpression.getQualifier().getText();
                    if ("close".equals(methodName) || "closeOnDaemon".equals(methodName)) {
                        if (objectName.equals(variableName)) {
                            result[0] = false;
                        }
                    }
                }
            }
        });
        return result[0];
    }
}
