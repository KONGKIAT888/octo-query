package me.kongkiat.octoquery;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static me.kongkiat.octoquery.FormatQueryAction.formatQueriesInFile;
import static me.kongkiat.octoquery.FormatQueryAction.formatSqlFile;

public class ContextMenuFormatAction extends AnAction {

    public ContextMenuFormatAction() {
        super("Format SQL Query");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || psiFile == null) return;

        String fileName = psiFile.getName().toLowerCase();
        String languageId = psiFile.getLanguage().getID();

        if (languageId.equalsIgnoreCase("SQL") || fileName.endsWith(".sql")) {
            formatSqlFile(project, psiFile, editor);
        } else if (psiFile instanceof PsiJavaFile) {
            formatQueriesInFile(project, psiFile, editor, false);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        boolean visible = false;

        if (psiFile != null) {
            if (psiFile instanceof PsiJavaFile javaFile) {
                PsiImportList imports = javaFile.getImportList();
                if (imports != null) {
                    visible = Arrays.stream(imports.getImportStatements())
                            .map(PsiImportStatement::getQualifiedName)
                            .anyMatch(q ->
                                    "org.springframework.data.jpa.repository.Query".equals(q) ||
                                            "org.springframework.data.jpa.repository.NativeQuery".equals(q));
                }
            } else if ("SQL".equalsIgnoreCase(psiFile.getLanguage().getID())
                    || psiFile.getName().toLowerCase().endsWith(".sql")) {
                visible = true;
            }
        }

        e.getPresentation().setEnabledAndVisible(visible);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}