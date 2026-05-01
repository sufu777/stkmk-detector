package cc.sufuzz.stkmkdetector.detectors.tool;

import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PsiSearchTool {
    public static final String METHOD_CLOSE = "close";
    public static final String METHOD_CLOSE_ON_DAEMON = "closeOnDaemon";

    /**
     *
     * @param variableName
     * @param cb
     * @return
     */
    public static PsiLocalVariable searchPsiLocalVariableInPcb(String variableName, PsiCodeBlock cb) {
        PsiElementProcessor.FindElement<? super PsiElement> plvProcessor = new PsiElementProcessor.FindElement<>() {
            @Override
            public boolean execute(@NotNull PsiElement element) {
                if (element instanceof PsiLocalVariable plv && plv.getText().equals(variableName)) {
                    return setFound(plv);
                }
                return true;
            }
        };
        PsiTreeUtil.processElements(cb, plvProcessor);
        return (PsiLocalVariable) plvProcessor.getFoundElement();
    }

    public static PsiMethodCallExpression searchStaticMockCloseInPcb(String variableName, PsiCodeBlock pcb) {
        if (StringUtils.isBlank(variableName)) return null;
        PsiElementProcessor.FindElement<? super PsiElement> closeCallProcessor = new PsiElementProcessor.FindElement<>() {
            @Override
            public boolean execute(@NotNull PsiElement element) {
                if (element instanceof PsiMethodCallExpression plv) {
                    PsiReferenceExpression methodExpression = plv.getMethodExpression();
                    PsiElement variable = methodExpression.getQualifier();
                    String methodName = methodExpression.getReferenceName();
                    if (Objects.nonNull(variable) && variable.getText().equals(variableName)
                            && (METHOD_CLOSE.equals(methodName) || METHOD_CLOSE_ON_DAEMON.equals(methodName)))
                        return setFound(plv);
                }
                return true;
            }
        };
        PsiTreeUtil.processElements(pcb, closeCallProcessor);
        return (PsiMethodCallExpression) closeCallProcessor.getFoundElement();
    }

//    public static List<PsiMethodCallExpression> unclosedStaticMockInPcbs(){
//
//    }
}
