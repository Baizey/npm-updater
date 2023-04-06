package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.AnnotationInfo
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.FileInfo
import com.baizey.npmupdater.dependency.Dependency
import com.baizey.npmupdater.dependency.SemVer.*
import com.baizey.npmupdater.packagemanager.DependencyService
import com.baizey.npmupdater.utils.PackageJsonParser
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


class PackageUpdatesAnnotator : ExternalAnnotator<FileInfo, AnnotationInfo>() {
    data class FileInfo(val content: String, val project: Project)
    data class AnnotationInfo(val dependencies: List<Dependency>)

    override fun collectInformation(file: PsiFile): FileInfo = runReadAction { FileInfo(file.text, file.project) }

    override fun doAnnotate(info: FileInfo): AnnotationInfo {
        val packages = PackageJsonParser.findDependencies(info.content)
        val service = DependencyService.getInstance(info.project)
        val dependencies = packages
                .parallelStream()
                .map(service::getLatestVersion)
                .filter {
                    if (it.current.isDeprecated) true
                    else it.latest != it.current
                }
                .toList()

        return AnnotationInfo(dependencies)
    }

    override fun apply(file: PsiFile, annotationInfo: AnnotationInfo, holder: AnnotationHolder) =
            annotationInfo.dependencies.forEach { dep ->
                val severity = dep.highLightSeverity()
                val message = dep.createMessage()
                val psiElement = file.findElementAt(dep.index)!!
                val builder = holder.newAnnotation(severity, message).range(psiElement)

                var order = 1
                if (dep.patch != null)
                    builder.newFix(UpdateDependencyAction("${order++}. Bump patch", psiElement, dep.current, dep.patch)).registerFix()
                if (dep.minor != null)
                    builder.newFix(UpdateDependencyAction("${order++}. Bump minor", psiElement, dep.current, dep.minor)).registerFix()
                if (dep.major != null)
                    builder.newFix(UpdateDependencyAction("$order. Bump major", psiElement, dep.current, dep.major)).registerFix()

                builder.create()
            }

    private fun Dependency.createMessage(): String =
            if (current.isDeprecated) "DEPRECATED: ${current.deprecatedMessage}"
            else "Can update to $latest"

    private fun Dependency.highLightSeverity() =
            if (current.isDeprecated)
                ERROR
            else when (semanticVersionBehind()) {
                PRE_RELEASE -> WARNING
                MAJOR, MINOR, PATCH -> WEAK_WARNING
                NONE -> INFORMATION
            }
}
