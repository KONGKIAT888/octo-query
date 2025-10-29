package me.kongkiat.octoquery;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.sql.psi.SqlLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatQueryAction extends AnAction {
    /**
     * Main entry point for formatting SQL queries in Java files.
     * This method finds all @Query and @NativeQuery annotations and formats their SQL content.
     *
     * @param project The current IntelliJ project
     * @param psiFile The Java file to process
     * @param editor The editor instance (null for bulk formatting)
     * @param onlyCurrentBlock If true, only format the query at the cursor position
     */
    public static void formatQueriesInFile(@NotNull Project project, @NotNull PsiFile psiFile, Editor editor, boolean onlyCurrentBlock) {
        // Only process Java files
        if (!(psiFile instanceof PsiJavaFile)) return;

        // Find all query annotations in the file
        List<PsiAnnotation> allQueries = findAllQueryAnnotations(psiFile);
        if (allQueries.isEmpty()) return;

        // Determine a target annotation for single-block formatting
        PsiAnnotation target;
        if (onlyCurrentBlock && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAt = psiFile.findElementAt(offset);
            target = PsiTreeUtil.getParentOfType(elementAt, PsiAnnotation.class);
        } else {
            target = null; // Format all queries
        }

        // Execute formatting within a write command for proper undo support
        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (PsiAnnotation queryAnnotation : allQueries) {
                // Skip if formatting a single block and this is not the target
                if (onlyCurrentBlock && target != null && !queryAnnotation.equals(target)) continue;

                // Extract the SQL value from the annotation
                PsiAnnotationMemberValue value = queryAnnotation.findDeclaredAttributeValue("value");
                if (value == null) continue;

                // Remove surrounding quotes and normalize
                String rawSql = value.getText().replaceAll("^\"{1,3}|\"{1,3}$", "");
                boolean isNative = isNativeQuery(queryAnnotation);

                // Format the SQL based on type (native vs. JPQL)
                String formatted = formatQuery(project, rawSql, isNative);

                // Only update if formatting actually changed the content
                if (!formatted.trim().equals(rawSql.trim())) {
                    String newValue = "\"\"\"\n" + formatted.trim() + "\n\"\"\"";
                    value.replace(JavaPsiFacade.getElementFactory(project)
                            .createExpressionFromText(newValue, queryAnnotation));
                }
            }
        });
    }

    /**
     * Determines if a query annotation represents a native SQL query or JPQL query.
     * This affects which formatter to use.
     *
     * @param annotation The query annotation to check
     * @return true if it's a native SQL query, false if it's JPQL
     */
    public static boolean isNativeQuery(PsiAnnotation annotation) {
        String qName = annotation.getQualifiedName();
        if (qName == null) return false;

        // Check if it's explicitly a @NativeQuery annotation
        boolean isNativeAnnotation = qName.endsWith(".NativeQuery") ||
                                   qName.equals("org.springframework.data.jpa.repository.NativeQuery");
        if (isNativeAnnotation) return true;

        // Check if @Query has a nativeQuery = true attribute
        PsiAnnotationMemberValue attr = annotation.findDeclaredAttributeValue("nativeQuery");
        return attr != null && "true".equalsIgnoreCase(attr.getText());
    }

    /**
     * Recursively searches through a Java file to find all @Query and @NativeQuery annotations.
     *
     * @param psiFile The Java file to search
     * @return List of all query annotations found in the file
     */
    public static List<PsiAnnotation> findAllQueryAnnotations(PsiFile psiFile) {
        List<PsiAnnotation> list = new ArrayList<>();

        // Use recursive visitor to traverse the entire PSI tree
        psiFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitAnnotation(@NotNull PsiAnnotation annotation) {
                super.visitAnnotation(annotation);
                String qName = annotation.getQualifiedName();
                if (qName == null) return;

                // Match both @Query and @NativeQuery annotations (including fully qualified names)
                if (qName.endsWith(".Query") || qName.endsWith(".NativeQuery")) {
                    list.add(annotation);
                }
            }
        });

        return list;
    }

    /**
     * Formats a SQL query using the appropriate formatter based on a query type.
     *
     * @param project The current IntelliJ project
     * @param query The SQL/JPQL query to format
     * @param isNative true for native SQL, false for JPQL
     * @return The formatted query string
     */
    public static String formatQuery(Project project, String query, boolean isNative) {
        return isNative ? formatWithSqlFormatter(project, query) : formatWithDtoAwareFormatter(project, query);
    }

    /**
     * Formats native SQL queries using IntelliJ's SQL formatter.
     * This handles standard SQL syntax with proper indentation and keyword formatting.
     *
     * @param project The current IntelliJ project
     * @param sql The SQL query to format
     * @return The formatted SQL query
     */
    private static String formatWithSqlFormatter(Project project, String sql) {
        try {
            // Create a temporary SQL file and apply IntelliJ's SQL formatter
            PsiFile sqlFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("temp.sql", SqlLanguage.INSTANCE, sql);

            // Apply formatting using the project's code style settings
            WriteCommandAction.runWriteCommandAction(project, () -> {
                CodeStyleManager.getInstance(project).reformat(sqlFile);
            });

            return sqlFile.getText();
        } catch (Exception e) {
            // Return original SQL if formatting fails
            return sql;
        }
    }

    /**
     * Formats JPQL queries with special handling for DTO constructor expressions.
     * This method preserves DTO constructor syntax while formatting the rest of the query.
     *
     * @param project The current IntelliJ project
     * @param jpql The JPQL query to format (may contain DTO constructors)
     * @return The formatted JPQL query with properly formatted DTO constructors
     */
    private static String formatWithDtoAwareFormatter(Project project, String jpql) {
        try {
            // Step 1: Temporarily replace DTO constructors with comments to protect them from SQL formatter
            String preprocessed = jpql
                    .replaceAll("(?i)SELECT\\s+new\\s+([a-zA-Z0-9_.]+)\\s*\\(", "SELECT /* DTO_START $1 */ (")
                    .replaceAll("\\)\\s*FROM", ") /* DTO_END */ FROM");

            // Step 2: Apply standard SQL formatting to the preprocessed query
            PsiFile sqlFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("temp.sql", SqlLanguage.INSTANCE, preprocessed);
            CodeStyleManager.getInstance(project).reformat(sqlFile);
            String formatted = sqlFile.getText();

            // Step 3: Restore DTO constructors from comments
            formatted = formatted
                    .replaceAll("/\\* DTO_START ([a-zA-Z0-9_.]+) \\*/ \\(", "new $1(")
                    .replaceAll("\\) /\\* DTO_END \\*/", ")");

            // Step 4: Format the constructor parameters with proper indentation
            Pattern pattern = Pattern.compile("new\\s+([a-zA-Z0-9_.]+)\\s*\\((.*?)\\)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(formatted);
            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                String className = matcher.group(1);
                // Format constructor parameters with proper indentation and line breaks
                String inner = matcher.group(2).trim().replaceAll("\\s*,\\s*", ",\n    ");
                String replacement = "new " + className + "(\n    " + inner + "\n)";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            return sb.toString().trim();
        } catch (Exception e) {
            // Return original JPQL if formatting fails
            return jpql;
        }
    }

    /**
     * Formats the content of standalone SQL files.
     * This method is used when formatting .sql files directly in the editor.
     *
     * @param project The current IntelliJ project
     * @param psiFile The SQL file to format
     * @param editor The editor instance containing the file
     */
    public static void formatSqlFile(@NotNull Project project, @NotNull PsiFile psiFile, Editor editor) {
        if (editor == null) return;

        Document document = editor.getDocument();
        String text = document.getText();

        // Format the SQL using the native SQL formatter
        String formatted = formatWithSqlFormatter(project, text);

        // Only update if formatting actually changed the content
        if (formatted.trim().equals(text.trim())) return;

        // Apply the formatted content to the document
        WriteCommandAction.runWriteCommandAction(project, "Format SQL File", null, () ->
                document.setText(formatted.trim() + "\n"), psiFile
        );
    }

    /**
     * Action implementation for the keyboard shortcut (Ctrl+Alt+L).
     * This method is called when the user triggers the formatting action.
     *
     * @param e The action event containing project context
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || psiFile == null) return;
        formatQueriesInFile(project, psiFile, editor, false);
    }
}
