package cc.sufuzz.stkmkdetector;

import com.intellij.psi.PsiMethodCallExpression;

public record UnClosedStaticMockIssue(String filePath, String fileName, int methodCallLine, int methodCallRow,
                                      PsiMethodCallExpression methodCallExpression) {
}
