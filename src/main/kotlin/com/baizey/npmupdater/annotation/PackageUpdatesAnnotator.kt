package com.baizey.npmupdater.annotation

import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.AnnotationInfo
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.FileInfo
import com.baizey.npmupdater.annotation.PackageUpdatesAnnotator.SemVer.*
import com.baizey.npmupdater.dto.DependencyCollection
import com.baizey.npmupdater.packagemanager.DependencyService
import com.baizey.npmupdater.packagejson.PackageJsonParser
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


class PackageUpdatesAnnotator : ExternalAnnotator<FileInfo, AnnotationInfo>() {
    data class FileInfo(val content: String, val project: Project)

    data class AnnotationInfo(val dependencies: List<DependencyCollection>)

    override fun collectInformation(file: PsiFile): FileInfo = runReadAction { FileInfo(file.text, file.project) }

    override fun doAnnotate(info: FileInfo): AnnotationInfo {
        val packages = PackageJsonParser.findDependencies(info.content)
        val service = DependencyService.getInstance(info.project)
        val dependencies = packages
            .parallelStream()
            .map(service::getVersionInfo)
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
                builder.newFix(UpdateDependencyAction("${order++}. Bump patch", psiElement, dep.patch))
                    .registerFix()
            if (dep.minor != null)
                builder.newFix(UpdateDependencyAction("${order++}. Bump feature", psiElement, dep.minor))
                    .registerFix()
            if (dep.major != null)
                builder.newFix(UpdateDependencyAction("${order++}. Bump breaking", psiElement, dep.major))
                    .registerFix()
            if (dep.matched == null)
                builder.newFix(UpdateDependencyAction("${order}. Bump latest", psiElement, dep.latest))
                    .registerFix()
            builder.create()
        }

    private fun DependencyCollection.createMessage(): String =
        if (matched == null) "No matches for this package and version"
        else if (current.isDeprecated) "DEPRECATED: ${current.deprecatedMessage}"
        else "Can update to $latest"

    private fun DependencyCollection.semanticVersionBehind(): SemVer {
        if (current.major != latest.major) return MAJOR
        if (current.minor != latest.minor) return MINOR
        if (current.patch != latest.patch) return PATCH
        if (current.label != latest.label) return PRE_RELEASE
        return NONE
    }

    private enum class SemVer {
        MAJOR,
        MINOR,
        PATCH,
        PRE_RELEASE,
        NONE
    }

    private fun DependencyCollection.highLightSeverity() =
        if (matched == null)
            ERROR
        else if (current.isDeprecated)
            ERROR
        else when (semanticVersionBehind()) {
            PRE_RELEASE -> WARNING
            MAJOR, MINOR, PATCH -> WEAK_WARNING
            NONE -> INFORMATION
        }
}