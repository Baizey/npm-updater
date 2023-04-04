package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.Dependency
import com.baizey.npmupdater.PackageJsonParser
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.AnnotationInfo
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.FileInfo
import com.baizey.npmupdater.npm.NpmService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class PackageUpdatesAnnotator : ExternalAnnotator<FileInfo, AnnotationInfo>() {
    data class FileInfo(val content: String, val project: Project)
    data class AnnotationInfo(val dependencies: List<Dependency>)

    override fun collectInformation(file: PsiFile): FileInfo = runReadAction { FileInfo(file.text, file.project) }

    override fun doAnnotate(info: FileInfo): AnnotationInfo {
        val packages = PackageJsonParser.findDependencies(info.content)
        val npm = NpmService.getInstance(info.project)
        val dependencies = packages
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
