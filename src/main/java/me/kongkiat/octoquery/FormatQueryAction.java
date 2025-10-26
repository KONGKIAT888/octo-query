package me.kongkiat.octoquery;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.sql.psi.SqlLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FormatQueryAction extends AnAction {

    public static void formatQueriesInFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        if (!(psiFile instanceof PsiJavaFile)) return;

        List<PsiAnnotation> allQueries = findAllQueryAnnotations(psiFile);
        if (allQueries.isEmpty()) return;

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (PsiAnnotation queryAnnotation : allQueries) {
                PsiAnnotationMemberValue value = queryAnnotation.findDeclaredAttributeValue("value");
                if (value == null) return;

                String rawSql = value.getText().replaceAll("^\"{1,3}|\"{1,3}$", "");
                String formatted = formatWithIntelliJSqlFormatter(project, rawSql);

                if (!formatted.trim().equals(rawSql.trim())) {
                    String newValue = "\"\"\"\n" + formatted + "\n\"\"\"";
                    value.replace(JavaPsiFacade.getElementFactory(project)
                            .createExpressionFromText(newValue, queryAnnotation));
                }
            }
        });
    }

    private static List<PsiAnnotation> findAllQueryAnnotations(PsiFile psiFile) {
        List<PsiAnnotation> list = new ArrayList<>();
        psiFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitAnnotation(@NotNull PsiAnnotation annotation) {
                super.visitAnnotation(annotation);
                if (annotation.getQualifiedName() != null &&
                        annotation.getQualifiedName().endsWith(".Query")) {
                    list.add(annotation);
                }
            }
        });
        return list;
    }

    private static String formatWithIntelliJSqlFormatter(Project project, String sql) {
        try {
            Language sqlLang = SqlLanguage.INSTANCE;
            PsiFile sqlFile = PsiFileFactory.getInstance(project).createFileFromText("temp.sql", sqlLang, sql);
            CodeStyleManager.getInstance(project).reformat(sqlFile);
            return sqlFile.getText().trim();
        } catch (Exception e) {
            return sql;
        }
    }

    @Override
    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (project == null || psiFile == null || editor == null) return;
        formatQueriesInFile(project, psiFile);
    }
}
