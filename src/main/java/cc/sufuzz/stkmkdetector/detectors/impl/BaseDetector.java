package cc.sufuzz.stkmkdetector.detectors.impl;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopes;

import java.util.Collection;

public abstract class BaseDetector {
    protected Collection<VirtualFile> testFiles(Project project) {
        return ReadAction.compute(() -> FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScopes.projectTestScope(project)));
    }
}
