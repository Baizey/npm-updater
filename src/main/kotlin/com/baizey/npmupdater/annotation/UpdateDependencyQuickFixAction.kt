package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.Dependency
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory

class UpdateDependencyQuickFixAction(
        private val element: PsiElement,
        dependency: Dependency) : BaseIntentionAction() {
    private val version = dependency.latest.versionWithType(dependency.current.type)
    override fun getFamilyName() = "Replace with $version"
    override fun getText() = "Replace with $version"
    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = true
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val placeholder = PsiFileFactory.getInstance(project)
                .createFileFromText("temp.file", Language.findLanguageByID("JSON")!!, "\"$version\"")
                .firstChild
        element.replace(placeholder)
    }
}
