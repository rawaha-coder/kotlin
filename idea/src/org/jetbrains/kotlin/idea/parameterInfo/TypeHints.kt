/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes

//hack to separate type presentation from param info presentation
const val TYPE_INFO_PREFIX = "@TYPE@"

class ImportAwareClassifierNamePolicy(
    val bindingContext: BindingContext,
    val context: KtElement
) : ClassifierNamePolicy {
    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
        if (classifier.containingDeclaration is ClassDescriptor) {
            val resolutionFacade = context.getResolutionFacade()
            val scope = context.getResolutionScope(bindingContext, resolutionFacade)
            if (scope.findClassifier(classifier.name, NoLookupLocation.FROM_IDE) == classifier) {
                return classifier.name.asString()
            }
        }

        return ClassifierNamePolicy.SHORT.renderClassifier(classifier, renderer)
    }
}

fun getInlayHintsTypeRenderer(bindingContext: BindingContext, context: KtElement) =
    DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
        textFormat = RenderingFormat.PLAIN
        renderUnabbreviatedType = false
        classifierNamePolicy = ImportAwareClassifierNamePolicy(bindingContext, context)
    }

fun providePropertyTypeHint(elem: PsiElement): List<InlayInfo> {
    (elem as? KtCallableDeclaration)?.let { property ->
        property.nameIdentifier?.let { ident ->
            return provideTypeHint(property, ident.endOffset)
        }
    }
    return emptyList()
}

fun provideTypeHint(element: KtCallableDeclaration, offset: Int): List<InlayInfo> {
    var type: KotlinType = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element).unwrap()
    if (type.containsError()) return emptyList()
    val name = type.constructor.declarationDescriptor?.name
    if (name == SpecialNames.NO_NAME_PROVIDED) {
        if (element is KtProperty && element.isLocal) {
            // for local variables, an anonymous object type is not collapsed to its supertype,
            // so showing the supertype will be misleading
            return emptyList()
        }
        type = type.immediateSupertypes().singleOrNull() ?: return emptyList()
    } else if (name?.isSpecial == true) {
        return emptyList()
    }

    return if (isUnclearType(type, element)) {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
            .getCustomSettings(KotlinCodeStyleSettings::class.java)

        val declString = buildString {
            append(TYPE_INFO_PREFIX)
            if (settings.SPACE_BEFORE_TYPE_COLON)
                append(" ")
            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON)
                append(" ")
            append(getInlayHintsTypeRenderer(element.analyze(), element).renderType(type))
        }
        listOf(InlayInfo(declString, offset))
    } else {
        emptyList()
    }
}

private fun isUnclearType(type: KotlinType, element: KtCallableDeclaration): Boolean {
    if (element is KtProperty) {
        val initializer = element.initializer ?: return true
        if (initializer is KtConstantExpression || initializer is KtStringTemplateExpression) return false
        if (initializer is KtUnaryExpression && initializer.baseExpression is KtConstantExpression) return false
        if (initializer is KtCallExpression) {
            val bindingContext = element.analyze()
            val resolvedCall = initializer.getResolvedCall(bindingContext)
            val resolvedDescriptor = resolvedCall?.candidateDescriptor
            if (resolvedDescriptor is SamConstructorDescriptor) {
                return false
            }
            if (resolvedDescriptor is ConstructorDescriptor &&
                (resolvedDescriptor.constructedClass.declaredTypeParameters.isEmpty() || initializer.typeArgumentList != null)) {
                return false
            }
        }
    }
    return true
}