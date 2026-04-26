package cc.sufuzz.stkmkdetector.task;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.ArrayList;
import java.util.List;

public record UnCloseIssue(PsiJavaFile psiJavaFile, List<PsiField> psiFieldList,
                           List<PsiMethodCallExpression> methodCall) {
    public UnCloseIssue(PsiJavaFile javaFile) {
        this(javaFile, new ArrayList<>(), new ArrayList<>());
    }
}
