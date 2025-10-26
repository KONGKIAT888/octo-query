package me.kongkiat.octoquery;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class FormatQueryIntention implements IntentionAction {

    @NotNull
    @Override
    public String getText() {
        return "Format SQL in @Query (OctoQuery)";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "OctoQuery";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof PsiJavaFile)) return false;
        PsiElement caretElement = file.findElementAt(editor.getCaretModel().getOffset());
        if (caretElement == null) return false;
        PsiAnnotation ann = findAnnotationAtOrAbove(caretElement);
        if (ann == null) return false;
        String qName = ann.getQualifiedName();
        if (qName == null) return false;
        return qName.endsWith(".Query") || qName.endsWith(".NativeQuery");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        WriteCommandAction.runWriteCommandAction(project, () ->
                FormatQueryAction.formatQueriesInFile(project, file, editor, true)
        );
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private PsiAnnotation findAnnotationAtOrAbove(PsiElement element) {
        PsiElement cur = element;
        while (cur != null && !(cur instanceof PsiAnnotation)) {
            cur = cur.getParent();
        }
        return (cur != null) ? (PsiAnnotation) cur : null;
    }
}
