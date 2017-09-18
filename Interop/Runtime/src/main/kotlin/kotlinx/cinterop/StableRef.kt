/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlinx.cinterop

@Deprecated("Use StableRef<T> instead", ReplaceWith("StableRef<T>"))
typealias StableObjPtr = StableRef<*>

/**
 * This class provides a way to create a stable handle to any Kotlin object.
 * After [converting to CPointer][asCPointer] it can be safely passed to native code e.g. to be received
 * in a Kotlin callback.
 *
 * Any [StableRef] should be manually [disposed][dispose]
 */
data class StableRef<out T : Any> @PublishedApi internal constructor(
        private val stablePtr: COpaquePointer,
        private val ref: T // Note: storing the reference itself to avoid unchecked casts.
) {

    companion object {

        /**
         * Creates a handle for given object.
         */
        fun <T : Any> create(any: T) = StableRef<T>(createStablePointer(any), any)

        /**
         * Creates [StableRef] from given raw value.
         *
         * @param value must be a [value] of some [StableRef]
         */
        @Deprecated("Use CPointer<*>.asStableRef<T>() instead", ReplaceWith("ptr.asStableRef<T>()"))
        fun fromValue(value: COpaquePointer) = value.asStableRef<Any>()
    }

    @Deprecated("Use .asCPointer() instead", ReplaceWith("this.asCPointer()"))
    val value: COpaquePointer get() = this.asCPointer()

    /**
     * Converts the handle to C pointer.
     * @see [asStableRef]
     */
    fun asCPointer(): COpaquePointer = this.stablePtr

    /**
     * Disposes the handle. It must not be used after that.
     */
    fun dispose() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the object this handle was [created][StableRef.create] for.
     */
    fun get() = this.ref

}

/**
 * Converts to [StableRef] this opaque pointer produced by [StableRef.asCPointer].
 */
inline fun <reified T : Any> CPointer<*>.asStableRef() =
        StableRef<T>(this, derefStablePointer(this) as T)
