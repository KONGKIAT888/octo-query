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

/**
 * Context menu action for formatting SQL queries.
 * This action appears in the right-click context menu and provides
 * easy access to SQL formatting functionality.
 *
 * Visibility: Only shows in Java files with Spring Data JPA imports or SQL files
 */
public class ContextMenuFormatAction extends AnAction {

    /**
     * Constructor for the context menu format action.
     */
    public ContextMenuFormatAction() {
        super("Format SQL Query");
    }

    /**
     * Performs SQL formatting based on the file type.
     * For Java files: formats @Query and @NativeQuery annotations
     * For SQL files: formats the entire file content
     *
     * @param e The action event containing project context
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || psiFile == null) return;

        // Determine the file type and route to the appropriate formatter
        String fileName = psiFile.getName().toLowerCase();
        String languageId = psiFile.getLanguage().getID();

        if (languageId.equalsIgnoreCase("SQL") || fileName.endsWith(".sql")) {
            // Handle standalone SQL files
            formatSqlFile(project, psiFile, editor);
        } else if (psiFile instanceof PsiJavaFile) {
            // Handle Java files with JPA annotations
            formatQueriesInFile(project, psiFile, editor, false);
        }
    }

    /**
     * Updates the visibility and enabled state of the context menu action.
     * The action is only visible in relevant file types.
     *
     * @param e The action event to update
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        boolean visible = false;

        if (psiFile != null) {
            if (psiFile instanceof PsiJavaFile javaFile) {
                // Check for Spring Data JPA imports
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
                // Always show for SQL files
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