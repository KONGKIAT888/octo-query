package me.kongkiat.octoquery;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
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

    public static void formatQueriesInFile(@NotNull Project project, @NotNull PsiFile psiFile, Editor editor, boolean onlyCurrentBlock) {
        if (!(psiFile instanceof PsiJavaFile)) return;
        List<PsiAnnotation> allQueries = findAllQueryAnnotations(psiFile);
        if (allQueries.isEmpty()) return;

        PsiAnnotation target;
        if (onlyCurrentBlock && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAt = psiFile.findElementAt(offset);
            target = PsiTreeUtil.getParentOfType(elementAt, PsiAnnotation.class);
        } else {
            target = null;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (PsiAnnotation queryAnnotation : allQueries) {
                if (onlyCurrentBlock && target != null && !queryAnnotation.equals(target)) continue;
                PsiAnnotationMemberValue value = queryAnnotation.findDeclaredAttributeValue("value");
                if (value == null) continue;
                String rawSql = value.getText().replaceAll("^\"{1,3}|\"{1,3}$", "");
                boolean isNative = isNativeQuery(queryAnnotation);
                String formatted = formatQuery(project, rawSql, isNative);
                if (!formatted.trim().equals(rawSql.trim())) {
                    String newValue = "\"\"\"\n" + formatted.trim() + "\n\"\"\"";
                    value.replace(JavaPsiFacade.getElementFactory(project)
                            .createExpressionFromText(newValue, queryAnnotation));
                }
            }
        });
    }

    public static boolean isNativeQuery(PsiAnnotation annotation) {
        String qName = annotation.getQualifiedName();
        if (qName == null) return false;
        boolean isNativeAnnotation = qName.endsWith(".NativeQuery") || qName.equals("org.springframework.data.jpa.repository.NativeQuery");
        if (isNativeAnnotation) return true;
        PsiAnnotationMemberValue attr = annotation.findDeclaredAttributeValue("nativeQuery");
        return attr != null && "true".equalsIgnoreCase(attr.getText());
    }

    public static List<PsiAnnotation> findAllQueryAnnotations(PsiFile psiFile) {
        List<PsiAnnotation> list = new ArrayList<>();
        psiFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitAnnotation(@NotNull PsiAnnotation annotation) {
                super.visitAnnotation(annotation);
                String qName = annotation.getQualifiedName();
                if (qName == null) return;
                if (qName.endsWith(".Query") || qName.endsWith(".NativeQuery")) list.add(annotation);
            }
        });
        return list;
    }

    public static String formatQuery(Project project, String query, boolean isNative) {
        return isNative ? formatWithSqlFormatter(project, query) : formatWithDtoAwareFormatter(project, query);
    }

    private static String formatWithSqlFormatter(Project project, String sql) {
        try {
            PsiFile sqlFile = PsiFileFactory.getInstance(project).createFileFromText("temp.sql", SqlLanguage.INSTANCE, sql);
            CodeStyleManager.getInstance(project).reformat(sqlFile);
            return sqlFile.getText();
        } catch (Exception e) {
            return sql;
        }
    }

    private static String formatWithDtoAwareFormatter(Project project, String jpql) {
        try {
            String preprocessed = jpql
                    .replaceAll("(?i)SELECT\\s+new\\s+([a-zA-Z0-9_.]+)\\s*\\(", "SELECT /* DTO_START $1 */ (")
                    .replaceAll("\\)\\s*FROM", ") /* DTO_END */ FROM");
            PsiFile sqlFile = PsiFileFactory.getInstance(project).createFileFromText("temp.sql", SqlLanguage.INSTANCE, preprocessed);
            CodeStyleManager.getInstance(project).reformat(sqlFile);
            String formatted = sqlFile.getText();
            formatted = formatted
                    .replaceAll("/\\* DTO_START ([a-zA-Z0-9_.]+) \\*/ \\(", "new $1(")
                    .replaceAll("\\) /\\* DTO_END \\*/", ")");
            Pattern pattern = Pattern.compile("new\\s+([a-zA-Z0-9_.]+)\\s*\\((.*?)\\)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(formatted);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String className = matcher.group(1);
                String inner = matcher.group(2).trim().replaceAll("\\s*,\\s*", ",\n    ");
                String replacement = "new " + className + "(\n    " + inner + "\n)";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString().trim();
        } catch (Exception e) {
            return jpql;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (project == null || psiFile == null) return;
        formatQueriesInFile(project, psiFile, editor, false);
    }
}
