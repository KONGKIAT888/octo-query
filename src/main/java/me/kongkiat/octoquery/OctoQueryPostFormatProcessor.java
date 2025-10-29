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

/**
 * Post-format processor that automatically formats SQL queries after IntelliJ's auto-format.
 * This processor runs after the standard code formatting and reformats any @Query or @NativeQuery
 * annotations to ensure SQL is properly formatted.
 *
 * Integration: Runs automatically when the user triggers code formatting (Ctrl+Alt+L)
 */
public class OctoQueryPostFormatProcessor implements PostFormatProcessor {

    @Override
    public @NotNull PsiElement processElement(@NotNull PsiElement element, @NotNull CodeStyleSettings settings) {
        // Element-level processing is not used
        return element;
    }

    /**
     * Processes text after code formatting to format SQL queries.
     * This method is called after IntelliJ's built-in formatting is completed.
     *
     * @param file The file being formatted
     * @param range The text range that was formatted
     * @param settings The code style settings (not used)
     * @return The processed text range
     */
    @Override
    public @NotNull TextRange processText(@NotNull PsiFile file,
                                          @NotNull TextRange range,
                                          @NotNull CodeStyleSettings settings) {
        // Only process Java files
        if (!(file instanceof PsiJavaFile)) return range;

        Project project = file.getProject();

        // Find all query annotations in the file before processing
        List<PsiAnnotation> targets = new ArrayList<>(FormatQueryAction.findAllQueryAnnotations(file));

        // Schedule SQL formatting to run after document commits are complete
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (PsiAnnotation ann : targets) {
                        try {
                            // Extract and format the SQL value
                            PsiAnnotationMemberValue val = ann.findDeclaredAttributeValue("value");
                            if (val == null) continue;

                            String rawSql = val.getText().replaceAll("^\"{1,3}|\"{1,3}$", "");
                            boolean isNative = FormatQueryAction.isNativeQuery(ann);
                            String formatted = FormatQueryAction.formatQuery(project, rawSql, isNative);

                            // Update if formatting changed the content
                            if (!formatted.trim().equals(rawSql.trim())) {
                                String newValue = "\"\"\"\n" + formatted.trim() + "\n\"\"\"";
                                val.replace(JavaPsiFacade.getElementFactory(project)
                                        .createExpressionFromText(newValue, ann));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace(); // Log errors but continue processing
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
