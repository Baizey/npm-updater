package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.dto.NpmSemanticVersion
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory

class UpdateDependencyAction(
    private val text: String,
    private val element: PsiElement,
    private val updateTo: NpmSemanticVersion
) : BaseIntentionAction() {
    override fun getFamilyName() = "$text $updateTo"
    override fun getText() = "$text $updateTo"
    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = true
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val placeholder = PsiFileFactory.getInstance(project)
            .createFileFromText("temp_${updateTo}.file", Language.findLanguageByID("JSON")!!, "\"$updateTo\"")
            .firstChild
        element.replace(placeholder)
    }
}
