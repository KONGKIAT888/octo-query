package me.kongkiat.octoquery.actions;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action for generating Java projection interfaces from SQL SELECT statements.
 * This action extracts column aliases from SQL queries and creates corresponding
 * Java interface definitions with getter methods.
 *
 * Trigger: Alt+Shift+P or via the context menu
 */
public class GenerateProjectionAction extends AnAction {

    /**
     * Constructor for the projection interface generator action.
     */
    public GenerateProjectionAction() {
        super("Generate Projection Interface from SQL");
    }

    /**
     * The main entry point for the projection generation action.
     * This method orchestrates the entire process of extracting SQL,
     * finding aliases, and creating the Java interface.
     *
     * @param e The action event containing project context
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // Validate that we have all necessary elements
        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // Get selected text from the editor
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showErrorDialog(project, "Please select SQL first.", "No Selection");
            return;
        }

        // Extract the SELECT clause from the selected SQL
        String selectClause = extractSelectClause(selectedText);
        if (selectClause == null) {
            Messages.showErrorDialog(project, "No SELECT clause found in the selected SQL.", "Invalid SQL");
            return;
        }

        // Extract column aliases from the SELECT clause
        List<String> aliases = extractAliases(selectClause);
        if (aliases.isEmpty()) {
            Messages.showWarningDialog(project,
                    "No 'AS <alias>' found in the selected SQL SELECT list.",
                    "No Aliases Found");
            return;
        }

        // Prompt user for interface name
        String interfaceName = Messages.showInputDialog(project,
                "Enter interface name:",
                "Projection Interface Name",
                null,
                "",
                null);

        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            return; // User canceled or entered an empty name
        }

        // Show the package / directory chooser dialogue
        PsiDirectory selectedDirectory = chooseDirectory(project, psiFile);
        if (selectedDirectory == null) {
            return; // User canceled directory selection
        }

        // Generate interface code with the appropriate package declaration
        String packageName = getPackageName(selectedDirectory);
        String interfaceCode = buildInterfaceCode(interfaceName.trim(), aliases, packageName);

        // Create the Java file using write action
        createJavaFile(project, selectedDirectory, interfaceName.trim(), interfaceCode);
    }

    /**
     * Extracts the SELECT clause from a SQL statement.
     * This method handles nested subqueries and string literals correctly.
     *
     * @param sql The complete SQL statement
     * @return The content between SELECT and FROM keywords, or null if no SELECT found
     */
    private String extractSelectClause(String sql) {
        // Normalize SQL by removing newlines and extra spaces for easier processing
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();

        // Find the first SELECT keyword (case-insensitive)
        int selectIndex = -1;
        Pattern selectPattern = Pattern.compile("(?i)\\bSELECT\\b");
        Matcher selectMatcher = selectPattern.matcher(normalizedSql);
        if (selectMatcher.find()) {
            selectIndex = selectMatcher.start();
        }

        if (selectIndex == -1) {
            return null; // No SELECT clause found
        }

        // Find the first top-level FROM keyword after SELECT
        int fromIndex = findTopLevelFrom(normalizedSql, selectIndex + 6);
        if (fromIndex == -1) {
            // If no FROM found, return everything after SELECT
            return normalizedSql.substring(selectIndex + 6);
        }

        // Return the content between SELECT and FROM
        return normalizedSql.substring(selectIndex + 6, fromIndex).trim();
    }

    /**
     * Finds the first top-level FROM keyword in a SQL string.
     * This method properly handles nested subqueries and string literals.
     *
     * @param sql The SQL string to search
     * @param startPos Position to start searching from (after SELECT)
     * @return Position of the top-level FROM keyword, or -1 if not found
     */
    private int findTopLevelFrom(String sql, int startPos) {
        int parenLevel = 0; // Track nesting level of parentheses
        boolean inString = false; // Track if we're inside a string literal
        char stringChar = '\0'; // The type of string delimiter (single or double quote)

        for (int i = startPos; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // Handle string literals - ignore everything inside them
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar) {
                inString = false;
                continue;
            }

            // Only process when not inside strings
            if (!inString) {
                if (c == '(') {
                    parenLevel++; // Enter nested level
                } else if (c == ')') {
                    parenLevel--; // Exit nested level
                }

                // Look for FROM keyword when not in nested parentheses
                if (parenLevel == 0 && Character.toUpperCase(c) == 'F' && i + 3 < sql.length()) {
                    String potential = sql.substring(i, i + 4).toUpperCase();
                    if ("FROM".equals(potential)) {
                        // Ensure it's a standalone word, not part of another identifier
                        boolean prevIsWordChar = i > 0 && Character.isLetterOrDigit(sql.charAt(i - 1));
                        boolean nextIsWordChar = i + 4 < sql.length() && Character.isLetterOrDigit(sql.charAt(i + 4));

                        if (!prevIsWordChar && !nextIsWordChar) {
                            return i; // Found top-level FROM
                        }
                    }
                }
            }
        }

        return -1; // No top-level FROM found
    }

    /**
     * Extracts column aliases from a SELECT clause.
     * This method handles both quoted and unquoted aliases and properly ignores
     * CAST expressions and nested subqueries.
     *
     * @param selectClause The SELECT clause content (between SELECT and FROM)
     * @return List of unique column aliases found in the SELECT list
     */
    private List<String> extractAliases(String selectClause) {
        List<String> aliases = new ArrayList<>();

        // Split SELECT clause by commas at the top level (respecting nested parentheses and strings)
        List<String> columnExpressions = splitByTopLevelCommas(selectClause);

        // Regex patterns to match both unquoted and quoted aliases
        Pattern unquotedPattern = Pattern.compile("(?i)\\bAS\\s+([A-Za-z0-9_]+)(?!\\s*\\()");
        Pattern quotedPattern = Pattern.compile("(?i)\\bAS\\s+([\"'])([^\"]+|[^']+?)\\1(?!\\s*\\()");

        for (String expression : columnExpressions) {
            // Remove SQL comments first
            String cleanedExpression = removeComments(expression).trim();

            // Try to find quoted aliases first (more specific pattern)
            Matcher quotedMatcher = quotedPattern.matcher(cleanedExpression);
            String lastAlias = null;

            while (quotedMatcher.find()) {
                // Ensure this AS is not followed by a parenthesis (which would indicate CAST)
                int matchEnd = quotedMatcher.end();
                if (matchEnd < cleanedExpression.length() && cleanedExpression.charAt(matchEnd) == '(') {
                    continue; // Skip CAST expressions like "CAST(column AS type)"
                }

                // Group 2 contains the content inside quotes
                lastAlias = quotedMatcher.group(2);
            }

            // If no quoted alias found, try unquoted aliases
            if (lastAlias == null) {
                Matcher unquotedMatcher = unquotedPattern.matcher(cleanedExpression);
                while (unquotedMatcher.find()) {
                    // Ensure this AS is not followed by a parenthesis
                    int matchEnd = unquotedMatcher.end();
                    if (matchEnd < cleanedExpression.length() && cleanedExpression.charAt(matchEnd) == '(') {
                        continue; // Skip CAST expressions
                    }
                    lastAlias = unquotedMatcher.group(1);
                }
            }

            // Add unique aliases to the result list
            if (lastAlias != null && !aliases.contains(lastAlias)) {
                aliases.add(lastAlias);
            }
        }

        return aliases;
    }

    /**
     * Splits a string by commas at the top level only.
     * This method respects nested parentheses and string literals,
     * ensuring commas inside function calls or subqueries are not used as split points.
     *
     * @param str The string to split (typically a SELECT clause)
     * @return List of column expressions split by top-level commas
     */
    private List<String> splitByTopLevelCommas(String str) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenLevel = 0; // Track nested parentheses
        boolean inString = false; // Track string literals
        char stringChar = '\0'; // String delimiter type

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // Handle string literals - treat everything inside as literal text
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringChar = c;
                current.append(c);
            } else if (inString && c == stringChar) {
                inString = false;
                current.append(c);
            } else if (inString) {
                current.append(c);
            } else {
                // Handle parentheses nesting
                if (c == '(') {
                    parenLevel++;
                    current.append(c);
                } else if (c == ')') {
                    parenLevel--;
                    current.append(c);
                } else if (c == ',' && parenLevel == 0) {
                    // Found a top-level comma - split here
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }

        // Add the last part (after the final comma)
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    /**
     * Removes SQL comments from a string.
     * This method handles both single-line comments (starting with --)
     * and multi-line comments (enclosed in /* ... *​/).
     *
     * @param str the input string that may contain SQL comments
     * @return the string with all comments removed
     */
    private String removeComments(String str) {
        // Remove single-line comments (--)
        String result = str.replaceAll("--.*?(?=\\n|$)", "");

        // Remove multi-line comments (/* */)
        result = result.replaceAll("/\\*.*?\\*/", "");

        return result;
    }

    private String capitalize(String alias) {
        if (alias == null || alias.isEmpty()) {
            return alias;
        }
        return alias.substring(0, 1).toUpperCase() + alias.substring(1);
    }

    private PsiDirectory chooseDirectory(Project project, PsiFile currentFile) {
        // Try to get the current package
        PsiDirectory currentDirectory = currentFile.getParent();
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiPackage currentPackage = psiFacade.findPackage(getPackageName(Objects.requireNonNull(currentDirectory)));

        // Use PackageChooserDialog for a better tree-style interface
        PackageChooserDialog chooser = new PackageChooserDialog("Select Package for Projection Interface", project);
        if (currentPackage != null) {
            chooser.selectPackage(currentPackage.getQualifiedName());
        }

        chooser.show();
        PsiPackage selectedPackage = chooser.getSelectedPackage();

        if (selectedPackage == null) {
            return null;
        }

        // Get the directory for the selected package
        PsiDirectory[] directories = selectedPackage.getDirectories();
        return directories.length > 0 ? directories[0] : currentDirectory;
    }

    private String getPackageName(PsiDirectory directory) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(directory.getProject());
        PsiPackage psiPackage = psiFacade.findPackage(directory.getVirtualFile().getPath());

        if (psiPackage != null) {
            return psiPackage.getQualifiedName();
        }

        // Fallback: try to construct a package name from a directory path
        return constructPackageName(directory);
    }

    private String constructPackageName(PsiDirectory directory) {
        List<String> packageParts = new ArrayList<>();
        PsiDirectory current = directory;

        // Walk up the directory tree until we find a source root
        while (current != null) {
            String name = current.getName();

            // Check if this is a source root
            if (isSourceRoot(current)) {
                break;
            }

            packageParts.addFirst(name);
            current = current.getParentDirectory();
        }

        return String.join(".", packageParts);
    }

    private boolean isSourceRoot(PsiDirectory directory) {
        String name = directory.getName();
        if ("java".equals(name)) {
            PsiDirectory parent = directory.getParentDirectory();
            if (parent != null) {
                String parentName = parent.getName();
                return "main".equals(parentName) || "test".equals(parentName);
            }
        }
        return false;
    }

    private void createJavaFile(Project project, PsiDirectory directory, String interfaceName, String interfaceCode) {
//        WriteIntentReadAction.run((Runnable) () -> {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                String fileName = interfaceName + ".java";

                // Check if a file already exists
                PsiFile existingFile = directory.findFile(fileName);

                if (existingFile != null) {
                    // File already exists, ask the user if they want to overwrite
                    int result = Messages.showYesNoDialog(
                            project,
                            "File " + fileName + " already exists. Do you want to overwrite it?",
                            "File Exists",
                            Messages.getQuestionIcon()
                    );

                    if (result != Messages.YES) {
                        return;
                    }

                    existingFile.delete();
                }

                // Create a new file using PsiDirectory
                PsiFile newFile = directory.createFile(fileName);

                // Set file content
                com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(newFile);
                newFile.getVirtualFile().setWritable(true);
                com.intellij.openapi.vfs.VfsUtil.saveText(newFile.getVirtualFile(), interfaceCode);

                // Open the file in the editor
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(newFile.getVirtualFile(), true);

            } catch (Exception e) {
                Messages.showErrorDialog(
                        project,
                        "Failed to create file: " + e.getMessage(),
                        "Error"
                );
            }
        });
//        });
    }

    /**
     * Builds the complete Java interface code from extracted aliases.
     * This method creates a proper Java interface with getter methods for each column alias.
     *
     * @param interfaceName The name of the interface to create
     * @param aliases List of column aliases from the SQL query
     * @param packageName The package name for the interface (maybe empty)
     * @return Complete Java interface source code as a string
     */
    private String buildInterfaceCode(String interfaceName, List<String> aliases, String packageName) {
        StringBuilder sb = new StringBuilder();

        // Add package declaration if package exists
        if (packageName != null && !packageName.trim().isEmpty()) {
            sb.append("package ").append(packageName.trim()).append(";\n\n");
        }

        // Interface declaration
        sb.append("public interface ").append(interfaceName).append(" {\n");

        // Generate getter method for each alias
        for (String alias : aliases) {
            String methodName = "get" + capitalize(alias);
            sb.append("    Object ").append(methodName).append("();\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        boolean visible = false;
        if (editor != null && psiFile != null) {
            // Enable if there's a selected text
            String selectedText = editor.getSelectionModel().getSelectedText();
            visible = selectedText != null && !selectedText.trim().isEmpty();

            // Also check if we're in a Java or SQL file
            if (visible) {
                String languageId = psiFile.getLanguage().getID();
                String fileName = psiFile.getName().toLowerCase();
                visible = "JAVA".equalsIgnoreCase(languageId) ||
                        "SQL".equalsIgnoreCase(languageId) ||
                        fileName.endsWith(".sql") ||
                        fileName.endsWith(".java");
            }
        }

        e.getPresentation().setEnabledAndVisible(visible);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}