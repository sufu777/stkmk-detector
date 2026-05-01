package cc.sufuzz.stkmkdetector.detectors;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.Arrays;
import java.util.Vector;

public record UnClosedStaticMockIssue(VirtualFile vf, PsiMethodCallExpression methodCallExpression, String desc) {
    public Vector<Object> toVector() {
        String compute = ReadAction.compute(methodCallExpression::getText);
        return new Vector<>(Arrays.asList(vf.getPath(), compute, desc, vf, methodCallExpression));
    }
}
