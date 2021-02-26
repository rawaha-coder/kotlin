/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

object PhaseUpdatingTransformer : FirTransformer<FirResolvePhase>() {
    override fun <E : FirElement> transformElement(element: E, data: FirResolvePhase): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, data).compose() as CompositeTransformResult<E>
    }

    override fun transformArgumentList(argumentList: FirArgumentList, data: FirResolvePhase): CompositeTransformResult<FirArgumentList> {
        argumentList.transformArguments(this, data)
        return super.transformArgumentList(argumentList, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirResolvePhase
    ): CompositeTransformResult<FirDeclaration> {
        return updatePhaseInDeclaration(simpleFunction, data)
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: FirResolvePhase
    ): CompositeTransformResult<FirStatement> {
        return updatePhaseInDeclaration(anonymousFunction, data)
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirResolvePhase
    ): CompositeTransformResult<FirDeclaration> {
        return updatePhaseInDeclaration(property, data)
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirResolvePhase
    ): CompositeTransformResult<FirStatement> {
        return updatePhaseInDeclaration(valueParameter, data)
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: FirResolvePhase
    ): CompositeTransformResult<FirDeclaration> {
        return updatePhaseInDeclaration(propertyAccessor, data)
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvePhase
    ): CompositeTransformResult<FirStatement> {
        return updatePhaseInDeclaration(regularClass, data)
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirResolvePhase
    ): CompositeTransformResult<FirStatement> {
        return updatePhaseInDeclaration(anonymousObject, data)
    }

    private fun <D : FirDeclaration> updatePhaseInDeclaration(declaration: D, phase: FirResolvePhase): CompositeTransformResult<D> {
        return if (declaration.resolvePhase >= phase) {
            declaration.compose()
        } else {
            declaration.replaceResolvePhase(phase)
            transformElement(declaration, phase)
        }
    }
}