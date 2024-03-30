package com.baizey.npmupdater.annotation

import com.intellij.lang.ExternalAnnotatorsFilter
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.psi.PsiFile

class PackageAnnotationFilter : ExternalAnnotatorsFilter {
    override fun isProhibited(annotator: ExternalAnnotator<*, *>?, file: PsiFile?) =
        when (annotator) {
            is PackageUpdatesAnnotator -> file?.name != "package.json"
            else -> false
        }
}