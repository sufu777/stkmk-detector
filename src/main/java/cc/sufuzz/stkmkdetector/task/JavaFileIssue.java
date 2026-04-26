package cc.sufuzz.stkmkdetector.task;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethodCallExpression;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaFileIssue {
    private final List<PsiMethodCallExpression> methodCall;
    private final List<PsiField> psiFieldList;
    private final PsiJavaFile javaFile;

    public JavaFileIssue(PsiJavaFile javaFile) {
        this.javaFile = javaFile;
        this.methodCall = new ArrayList<>();
        this.psiFieldList = new ArrayList<>();
    }

    public PsiJavaFile getJavaFile() {
        return javaFile;
    }

    boolean hasIssue() {
        return CollectionUtils.isNotEmpty(methodCall) || CollectionUtils.isNotEmpty(psiFieldList);
    }

    public void addAllFields(Collection<PsiField> psiField) {
        psiFieldList.addAll(psiField);
    }

    public void addMethodCall(PsiMethodCallExpression methodCall) {
        this.methodCall.add(methodCall);
    }

    public String fileName() {
        return javaFile.getName();
    }

    public String filePath() {
        return javaFile.getProject().getBasePath();
    }

}
