package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.Dependency
import com.baizey.npmupdater.PackageJsonParser
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.*
import com.baizey.npmupdater.npm.NpmService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.util.concurrentMapOf

class PackageUpdatesAnnotator : ExternalAnnotator<FileReader, List<Dependency>>() {
    data class FileReader(private val file: PsiFile) {
        val content: String get() = file.text
        val project: Project get() = file.project
    }

    private val cache = concurrentMapOf<String?, PackageJsonParser>()

    override fun collectInformation(file: PsiFile): FileReader = runReadAction { FileReader(file) }

    override fun doAnnotate(collectedInfo: FileReader?): List<Dependency>? {
        if (collectedInfo == null) return null
        val parser = cache.computeIfAbsent(collectedInfo.project.basePath) { PackageJsonParser() }
        val npm = NpmService(collectedInfo.project)

        val packageDependencies = parser.findDependencies(collectedInfo.content)
        return packageDependencies
                .map(npm::getLatestVersion)
                .filter { it.latest != it.current || it.current.isDeprecated }
    }

    override fun apply(file: PsiFile, dependencies: List<Dependency>, holder: AnnotationHolder) {
        dependencies.forEach { pack ->
            val psiElement = file.findElementAt(pack.index)!!
            if (pack.current.isDeprecated) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEPRECATED: ${pack.current.deprecatedMessage}")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, pack))
                        .registerFix()
                        .create()
            } else {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Can update to ${pack.latest}")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, pack))
                        .registerFix()
                        .create()
            }
        }
    }
}
