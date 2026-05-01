package cc.sufuzz.stkmkdetector;

import com.intellij.psi.PsiMethodCallExpression;

public record UnClosedStaticMockIssue(String filePath, String fileName, PsiMethodCallExpression methodCallExpression) {
}
