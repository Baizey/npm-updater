package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.DependencyResult
import com.baizey.npmupdater.PackageJsonParser
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.*
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.concurrentMapOf

class PackageUpdatesAnnotator : ExternalAnnotator<FileReader, DependencyResult>() {
    data class FileReader(private val file: PsiFile) {
        val content: String get() = file.text
        val project: Project get() = file.project
    }

    private val cache = concurrentMapOf<String?, PackageJsonParser>()

    override fun collectInformation(file: PsiFile): FileReader = runReadAction { FileReader(file) }

    override fun doAnnotate(collectedInfo: FileReader?): DependencyResult? {
        if (collectedInfo == null) return null
        val parser = cache.computeIfAbsent(collectedInfo.project.basePath) { PackageJsonParser(collectedInfo.project) }
        val annotations = parser.findDependenciesWithUpdatesPossible(collectedInfo.content)
        return if (annotations.isNotEmpty()) DependencyResult(annotations) else null
    }

    override fun apply(file: PsiFile, annotationResult: DependencyResult, holder: AnnotationHolder) {
        annotationResult.annotations.forEach { pack ->
            val psiElement = file.findElementAt(pack.json.index)!!

            val deprecatedMessage = pack.registry.versions.firstOrNull { it == pack.json.current }?.deprecatedMessage
            if (deprecatedMessage != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEPRECATED: $deprecatedMessage")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, pack))
                        .registerFix()
                        .create()
            } else {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Can update to ${pack.registry.latest}")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, pack))
                        .registerFix()
                        .create()
            }
        }
    }
}
