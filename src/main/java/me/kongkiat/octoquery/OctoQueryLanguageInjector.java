package me.kongkiat.octoquery;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.sql.psi.SqlLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class OctoQueryLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof PsiLiteralExpression host)) return;
        if (!(host instanceof PsiLanguageInjectionHost injectionHost)) return;
        if (!injectionHost.isValidHost()) return;

        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(context, PsiAnnotation.class);
        if (annotation == null) return;

        String qName = annotation.getQualifiedName();
        if (qName == null) return;

        boolean isQuery = qName.equals("org.springframework.data.jpa.repository.Query")
                || qName.endsWith(".Query");
        boolean isNativeQuery = qName.equals("org.springframework.data.jpa.repository.NativeQuery")
                || qName.endsWith(".NativeQuery");

        if (isQuery) {
            PsiAnnotationMemberValue nativeAttr = annotation.findDeclaredAttributeValue("nativeQuery");
            if (nativeAttr != null && !"true".equalsIgnoreCase(nativeAttr.getText())) {
                return;
            }
        }

        if (!isQuery && !isNativeQuery) return;

        PsiNameValuePair pair = PsiTreeUtil.getParentOfType(context, PsiNameValuePair.class);
        if (pair == null || !"value".equals(pair.getName())) return;

        registrar.startInjecting(SqlLanguage.INSTANCE)
                .addPlace(null, null, injectionHost, host.getTextRange())
                .doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(PsiLiteralExpression.class);
    }
}
