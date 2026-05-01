package cc.sufuzz.stkmkdetector.detectors.tool;

import com.intellij.openapi.application.ReadAction;
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
     * 检查 element 所在方法中，是否定义了名叫 variableName 的变量
     *
     * @param variableName 变量名
     * @param element      要被搜索的元素
     * @return 搜索到的 variableName 变量定义，有可能为空
     */
    public static PsiLocalVariable searchPsiLocalVariableFromMethodBody(String variableName, PsiElement element) {
        PsiElementProcessor.FindElement<PsiLocalVariable> plvProcessor = new PsiElementProcessor.FindElement<>() {
            @Override
            public boolean execute(@NotNull PsiLocalVariable plv) {
                if (plv.getText().equals(variableName)) {
                    return setFound(plv);
                }
                return true;
            }
        };
        ReadAction.run(() -> {
            PsiMethod pm = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (Objects.isNull(pm)) return;
            PsiCodeBlock pcb = pm.getBody();
            if (Objects.isNull(pcb)) return;
            PsiTreeUtil.processElements(pcb, PsiLocalVariable.class, plvProcessor);
        });
        return plvProcessor.getFoundElement();
    }

    public static PsiMethodCallExpression searchStaticMockCloseInPcb(String variableName, PsiElement element) {
        if (StringUtils.isBlank(variableName)) return null;
        PsiElementProcessor.FindElement<PsiMethodCallExpression> closeCallProcessor = new PsiElementProcessor.FindElement<>() {
            @Override
            public boolean execute(@NotNull PsiMethodCallExpression plv) {
                PsiReferenceExpression methodExpression = plv.getMethodExpression();
                PsiElement variable = methodExpression.getQualifier();
                String methodName = methodExpression.getReferenceName();
                if (Objects.nonNull(variable) && variable.getText().equals(variableName)
                        && (METHOD_CLOSE.equals(methodName) || METHOD_CLOSE_ON_DAEMON.equals(methodName)))
                    return setFound(plv);
                return true;
            }
        };
        ReadAction.run(() -> {
            PsiCodeBlock pcb = PsiTreeUtil.getParentOfType(element, PsiCodeBlock.class);
            if (Objects.isNull(pcb)) return;
            PsiTreeUtil.processElements(pcb, PsiMethodCallExpression.class, closeCallProcessor);
        });
        return closeCallProcessor.getFoundElement();
    }
}
