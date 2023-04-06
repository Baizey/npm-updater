package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.dependency.DependencyVersion
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
        current: DependencyVersion,
        updateTo: DependencyVersion) : BaseIntentionAction() {
    private val version = updateTo.versionWithType(current.type)
    override fun getFamilyName() = "$text $version"
    override fun getText() = "$text $version"
    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = true
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val placeholder = PsiFileFactory.getInstance(project)
                .createFileFromText("temp_${version}.file", Language.findLanguageByID("JSON")!!, "\"$version\"")
                .firstChild
        element.replace(placeholder)
    }
}
