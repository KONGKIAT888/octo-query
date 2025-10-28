package me.kongkiat.octoquery;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OctoQueryDocumentListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !file.getName().endsWith(".java")) return;

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (!(psiFile instanceof PsiJavaFile)) continue;

            ApplicationManager.getApplication().invokeLater(() -> {
                PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        try {
                            List<PsiAnnotation> queries = FormatQueryAction.findAllQueryAnnotations(psiFile);
                            for (PsiAnnotation ann : queries) {
                                PsiAnnotationMemberValue val = ann.findDeclaredAttributeValue("value");
                                if (val == null) continue;

                                String rawSql = val.getText().replaceAll("^\"{1,3}|\"{1,3}$", "");
                                boolean isNative = FormatQueryAction.isNativeQuery(ann);
                                String formatted = FormatQueryAction.formatQuery(project, rawSql, isNative);

                                if (!formatted.trim().equals(rawSql.trim())) {
                                    String newValue = "\"\"\"\n" + formatted.trim() + "\n\"\"\"";
                                    val.replace(JavaPsiFacade.getElementFactory(project)
                                            .createExpressionFromText(newValue, ann));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });
                });
            });
        }
    }
}
