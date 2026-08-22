package cc.sufuzz.stkmkdetector.detectors.impl;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

public class EnhanceAnnotation {
    PsiClass beforeAllAnnotation;
    PsiClass beforeEachAnnotation;
    PsiClass afterAllAnnotation;
    PsiClass afterEachAnnotation;
    PsiClass beforeAnnotation;
    PsiClass afterAnnotation;
    PsiClass beforeClassAnnotation;
    PsiClass afterClassAnnotation;

    public EnhanceAnnotation(Project project) {
        beforeAllAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.BeforeAll", GlobalSearchScope.allScope(project)));
        beforeEachAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.BeforeEach", GlobalSearchScope.allScope(project)));
        afterAllAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.AfterAll", GlobalSearchScope.allScope(project)));
        afterEachAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.AfterEach", GlobalSearchScope.allScope(project)));
        beforeAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.Before", GlobalSearchScope.allScope(project)));
        afterAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.After", GlobalSearchScope.allScope(project)));
        beforeClassAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.BeforeClass", GlobalSearchScope.allScope(project)));
        afterClassAnnotation = ReadAction.compute(() -> JavaPsiFacade.getInstance(project).findClass("org.junit.AfterClass", GlobalSearchScope.allScope(project)));
    }
}
