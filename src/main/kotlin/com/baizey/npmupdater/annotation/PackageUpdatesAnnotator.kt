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

class PackageUpdatesAnnotator : ExternalAnnotator<FileInfo, AnnotationInfo>() {
    data class FileInfo(val content: String, val project: Project)
    data class AnnotationInfo(val dependencies: List<Dependency>)

    private val cache = concurrentMapOf<String?, NpmService>()

    override fun collectInformation(file: PsiFile): FileInfo = runReadAction { FileInfo(file.text, file.project) }

    override fun doAnnotate(collectedInfo: FileInfo?): AnnotationInfo? {
        if (collectedInfo == null) return null

        val packageDependencies = PackageJsonParser.findDependencies(collectedInfo.content)

        val npm = cache.computeIfAbsent(collectedInfo.project.basePath) { NpmService(collectedInfo.project) }
        val dependencies = packageDependencies
                .parallelStream()
                .map(npm::getLatestVersion)
                .filter {
                    if (it.current.isDeprecated) true
                    else it.latest != it.current
                }
                .toList()
        return AnnotationInfo(dependencies)
    }

    override fun apply(file: PsiFile, annotationInfo: AnnotationInfo, holder: AnnotationHolder) {
        annotationInfo.dependencies.forEach {
            val psiElement = file.findElementAt(it.index)!!
            if (it.current.isDeprecated) {
                holder.newAnnotation(HighlightSeverity.ERROR, "DEPRECATED: ${it.current.deprecatedMessage}")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, it))
                        .registerFix()
                        .create()
            } else {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Can update to ${it.latest}")
                        .range(psiElement)
                        .newFix(UpdateDependencyQuickFixAction(psiElement, it))
                        .registerFix()
                        .create()
            }
        }
    }
}
