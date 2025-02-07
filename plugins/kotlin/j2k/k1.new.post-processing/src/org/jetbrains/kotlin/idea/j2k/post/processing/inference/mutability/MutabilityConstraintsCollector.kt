// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.j2k.post.processing.inference.mutability

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.BoundTypeCalculator
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.ConstraintBuilder
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.InferenceContext
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.collectors.ConstraintsCollector
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class MutabilityConstraintsCollector : ConstraintsCollector() {
    private val callResolver = CallResolver(MUTATOR_CALL_FQ_NAMES)

    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) {
        when (element) {
            is KtQualifiedExpression -> {
                val callExpression = element.selectorExpression?.safeAs<KtCallExpression>() ?: return
                if (callResolver.isNeededCall(callExpression, resolutionFacade)) {
                    element.receiverExpression.isTheSameTypeAs(
                        org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.State.LOWER,
                        org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.ConstraintPriority.USE_AS_RECEIVER
                    )
                }
            }
        }
    }

    companion object {
        private val MUTATOR_CALL_FQ_NAMES = setOf(
            FqNames.mutableCollection.child(Name.identifier("add")),
            FqNames.mutableCollection.child(Name.identifier("addAll")),
            FqNames.mutableCollection.child(Name.identifier("remove")),
            FqNames.mutableCollection.child(Name.identifier("removeAll")),
            FqNames.mutableCollection.child(Name.identifier("retainAll")),
            FqNames.mutableCollection.child(Name.identifier("clear")),
            FqNames.mutableCollection.child(Name.identifier("removeIf")),

            FqNames.mutableList.child(Name.identifier("addAll")),
            FqNames.mutableList.child(Name.identifier("set")),

            FqNames.mutableMap.child(Name.identifier("put")),
            FqNames.mutableMap.child(Name.identifier("remove")),
            FqNames.mutableMap.child(Name.identifier("putAll")),
            FqNames.mutableMap.child(Name.identifier("clear")),
            FqNames.mutableMap.child(Name.identifier("putIfAbsent")),
            FqNames.mutableMap.child(Name.identifier("replace")),
            FqNames.mutableMap.child(Name.identifier("replaceAll")),
            FqNames.mutableMap.child(Name.identifier("computeIfAbsent")),
            FqNames.mutableMap.child(Name.identifier("computeIfPresent")),
            FqNames.mutableMap.child(Name.identifier("compute")),
            FqNames.mutableMap.child(Name.identifier("merge")),

            FqNames.mutableMapEntry.child(Name.identifier("setValue")),

            FqNames.mutableIterator.child(Name.identifier("remove")),

            FqNames.mutableListIterator.child(Name.identifier("set")),
            FqNames.mutableListIterator.child(Name.identifier("add"))
        )
    }
}
