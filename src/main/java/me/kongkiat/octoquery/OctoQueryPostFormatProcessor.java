package me.kongkiat.octoquery;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OctoQueryPostFormatProcessor implements PostFormatProcessor {

    @Override
    public @NotNull PsiElement processElement(@NotNull PsiElement element, @NotNull CodeStyleSettings settings) {
        return element;
    }

    @Override
    public @NotNull TextRange processText(@NotNull PsiFile file,
                                          @NotNull TextRange range,
                                          @NotNull CodeStyleSettings settings) {
        if (!(file instanceof PsiJavaFile)) return range;

        Project project = file.getProject();

        List<PsiAnnotation> targets = new ArrayList<>(FormatQueryAction.findAllQueryAnnotations(file));

        ApplicationManager.getApplication().invokeLater(() -> {
            PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (PsiAnnotation ann : targets) {
                        try {
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
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            });
        });

        return range;
    }

    @Override
    public boolean isWhitespaceOnly() {
        return false;
    }
}
