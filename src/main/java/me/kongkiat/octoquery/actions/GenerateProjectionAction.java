package me.kongkiat.octoquery.actions;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteIntentReadAction;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateProjectionAction extends AnAction {

    public GenerateProjectionAction() {
        super("Generate Projection Interface from SQL");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // Get selected text
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showErrorDialog(project, "Please select SQL first.", "No Selection");
            return;
        }

        // Extract SELECT clause
        String selectClause = extractSelectClause(selectedText);
        if (selectClause == null) {
            Messages.showErrorDialog(project, "No SELECT clause found in the selected SQL.", "Invalid SQL");
            return;
        }

        // Extract aliases from SELECT clause
        List<String> aliases = extractAliases(selectClause);
        if (aliases.isEmpty()) {
            Messages.showWarningDialog(project,
                    "No 'AS <alias>' found in the selected SQL SELECT list.",
                    "No Aliases Found");
            return;
        }

        // Ask for interface name
        String interfaceName = Messages.showInputDialog(project,
                "Enter interface name:",
                "Projection Interface Name",
                null,
                "",
                null);

        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            return;
        }

        // Show package/directory chooser dialog
        PsiDirectory selectedDirectory = chooseDirectory(project, psiFile);
        if (selectedDirectory == null) {
            return; // User cancelled
        }

        // Generate interface code with package
        String packageName = getPackageName(selectedDirectory);
        String interfaceCode = buildInterfaceCode(interfaceName.trim(), aliases, packageName);

        // Create the Java file using write action
        createJavaFile(project, selectedDirectory, interfaceName.trim(), interfaceCode);
    }

    private String extractSelectClause(String sql) {
        // Normalize SQL by removing newlines and extra spaces for easier processing
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();

        // Find first SELECT (case-insensitive)
        int selectIndex = -1;
        Pattern selectPattern = Pattern.compile("(?i)\\bSELECT\\b");
        Matcher selectMatcher = selectPattern.matcher(normalizedSql);
        if (selectMatcher.find()) {
            selectIndex = selectMatcher.start();
        }

        if (selectIndex == -1) {
            return null;
        }

        // Find the first top-level FROM after SELECT
        int fromIndex = findTopLevelFrom(normalizedSql, selectIndex + 6);
        if (fromIndex == -1) {
            // If no FROM found, return everything after SELECT
            return normalizedSql.substring(selectIndex + 6);
        }

        return normalizedSql.substring(selectIndex + 6, fromIndex).trim();
    }

    private int findTopLevelFrom(String sql, int startPos) {
        int parenLevel = 0;
        boolean inString = false;
        char stringChar = '\0';

        for (int i = startPos; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // Handle string literals
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
                    parenLevel++;
                } else if (c == ')') {
                    parenLevel--;
                }

                // Look for FROM keyword when not in parentheses
                if (parenLevel == 0 && Character.toUpperCase(c) == 'F' && i + 3 < sql.length()) {
                    String potential = sql.substring(i, i + 4).toUpperCase();
                    if ("FROM".equals(potential)) {
                        // Check if it's a whole word
                        boolean prevIsWordChar = i > 0 && Character.isLetterOrDigit(sql.charAt(i - 1));
                        boolean nextIsWordChar = i + 4 < sql.length() && Character.isLetterOrDigit(sql.charAt(i + 4));

                        if (!prevIsWordChar && !nextIsWordChar) {
                            return i;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private List<String> extractAliases(String selectClause) {
        List<String> aliases = new ArrayList<>();

        // Split SELECT clause by commas at top level
        List<String> columnExpressions = splitByTopLevelCommas(selectClause);

        // For each column expression, find the last AS alias
        // Pattern to match both unquoted and quoted aliases
        Pattern unquotedPattern = Pattern.compile("(?i)\\bAS\\s+([A-Za-z0-9_]+)(?!\\s*\\()");
        Pattern quotedPattern = Pattern.compile("(?i)\\bAS\\s+([\"'])([^\"]+|[^\']+?)\\1(?!\\s*\\()");

        for (String expression : columnExpressions) {
            // Remove comments first
            String cleanedExpression = removeComments(expression).trim();

            // Try to find quoted aliases first (more specific)
            Matcher quotedMatcher = quotedPattern.matcher(cleanedExpression);
            String lastAlias = null;

            while (quotedMatcher.find()) {
                // Make sure this AS is not followed by a parenthesis (which would indicate CAST)
                int matchEnd = quotedMatcher.end();
                if (matchEnd < cleanedExpression.length() && cleanedExpression.charAt(matchEnd) == '(') {
                    continue; // Skip CAST expressions
                }

                // Group 2 contains the content inside quotes
                lastAlias = quotedMatcher.group(2);
            }

            // If no quoted alias found, try unquoted
            if (lastAlias == null) {
                Matcher unquotedMatcher = unquotedPattern.matcher(cleanedExpression);
                while (unquotedMatcher.find()) {
                    // Make sure this AS is not followed by a parenthesis
                    int matchEnd = unquotedMatcher.end();
                    if (matchEnd < cleanedExpression.length() && cleanedExpression.charAt(matchEnd) == '(') {
                        continue; // Skip CAST expressions
                    }
                    lastAlias = unquotedMatcher.group(1);
                }
            }

            if (lastAlias != null && !aliases.contains(lastAlias)) {
                aliases.add(lastAlias);
            }
        }

        return aliases;
    }

    private List<String> splitByTopLevelCommas(String str) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenLevel = 0;
        boolean inString = false;
        char stringChar = '\0';

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // Handle string literals
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
                // Handle parentheses
                if (c == '(') {
                    parenLevel++;
                    current.append(c);
                } else if (c == ')') {
                    parenLevel--;
                    current.append(c);
                } else if (c == ',' && parenLevel == 0) {
                    // Top-level comma - split here
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }

        // Add the last part
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

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

    private String cleanInterfaceName(String interfaceName) {
        if (interfaceName == null || interfaceName.isEmpty()) {
            return interfaceName;
        }

        // Remove quotes if present
        if ((interfaceName.startsWith("\"") && interfaceName.endsWith("\"")) ||
            (interfaceName.startsWith("'") && interfaceName.endsWith("'"))) {
            interfaceName = interfaceName.substring(1, interfaceName.length() - 1);
        }

        // Replace invalid characters with underscores first
        interfaceName = interfaceName.replaceAll("[^a-zA-Z0-9_$]", "_");

        // Convert to PascalCase (first letter uppercase, rest lowercase)
        interfaceName = toPascalCase(interfaceName);

        // Ensure it starts with a valid Java identifier character
        if (!interfaceName.isEmpty() && !Character.isJavaIdentifierStart(interfaceName.charAt(0))) {
            interfaceName = "_" + interfaceName;
        }

        return interfaceName;
    }

    private String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // Simply make first letter uppercase and rest lowercase
        if (str.length() == 1) {
            return str.toUpperCase();
        } else {
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }

    private PsiDirectory chooseDirectory(Project project, PsiFile currentFile) {
        // Try to get the current package
        PsiDirectory currentDirectory = currentFile.getParent();
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiPackage currentPackage = psiFacade.findPackage(getPackageName(currentDirectory));

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

        // Fallback: try to construct package name from directory path
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

            packageParts.add(0, name);
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
        WriteIntentReadAction.run((Runnable) () -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    String fileName = interfaceName + ".java";

                    // Check if file already exists
                    PsiFile existingFile = directory.findFile(fileName);

                    if (existingFile != null) {
                        // File already exists, ask user if they want to overwrite
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

                    // Create new file using PsiDirectory
                    PsiFile newFile = directory.createFile(fileName);

                    // Set file content
                    com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(newFile);
                    newFile.getVirtualFile().setWritable(true);
                    com.intellij.openapi.vfs.VfsUtil.saveText(newFile.getVirtualFile(), interfaceCode);

                    // Open the file in editor
                    com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(newFile.getVirtualFile(), true);

                } catch (Exception e) {
                    Messages.showErrorDialog(
                            project,
                            "Failed to create file: " + e.getMessage(),
                            "Error"
                    );
                }
            });
        });
    }

    private String buildInterfaceCode(String interfaceName, List<String> aliases, String packageName) {
        StringBuilder sb = new StringBuilder();

        // Add package declaration if package exists
        if (packageName != null && !packageName.trim().isEmpty()) {
            sb.append("package ").append(packageName.trim()).append(";\n\n");
        }

        sb.append("public interface ").append(interfaceName).append(" {\n");

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
            // Enable if there's selected text
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