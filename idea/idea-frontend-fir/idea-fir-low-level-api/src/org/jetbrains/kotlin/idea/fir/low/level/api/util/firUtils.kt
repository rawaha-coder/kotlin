/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render

@Suppress("NOTHING_TO_INLINE")
internal inline fun FirDeclaration.checkIsResolvedTo(requiredPhase: FirResolvePhase) {
    require(resolvePhase == requiredPhase) {
        "The ${this::class.simpleName} should be resolved to $requiredPhase, but it was resolved to $resolvePhase\n${render(FirRenderer.RenderMode.WithResolvePhases)}"
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun FirDeclaration.checkIsResolvedToBodyResolve() {
    checkIsResolvedTo(FirResolvePhase.BODY_RESOLVE)
}