// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.JavaValue
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData.Companion.DEFAULT_COROUTINE_NAME
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData.Companion.DEFAULT_COROUTINE_STATE
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.MirrorOfCoroutineInfo

abstract class CoroutineInfoData(val descriptor: CoroutineDescriptor) {
    abstract val continuationStackFrames: List<CoroutineStackFrameItem>
    abstract val creationStackFrames: List<CreationCoroutineStackFrameItem>
    abstract val activeThread: ThreadReference?
    abstract val jobHierarchy: List<String>

    val topFrameVariables: List<JavaValue> by lazy {
        continuationStackFrames.firstOrNull()?.spilledVariables ?: emptyList()
    }

    fun isSuspended() = descriptor.state == State.SUSPENDED

    fun isCreated() = descriptor.state == State.CREATED

    fun isRunning() = descriptor.state == State.RUNNING

    companion object {
        const val DEFAULT_COROUTINE_NAME = "coroutine"
        const val DEFAULT_COROUTINE_STATE = "UNKNOWN"
    }
}

class LazyCoroutineInfoData(
    private val mirror: MirrorOfCoroutineInfo,
    private val stackTraceProvider: CoroutineStackFramesProvider,
    private val jobHierarchyProvider: CoroutineJobHierarchyProvider
) : CoroutineInfoData(CoroutineDescriptor.instance(mirror)) {

    override val creationStackFrames: List<CreationCoroutineStackFrameItem> by lazy {
        stackTraceProvider.getCreationStackTrace(mirror)
    }

    override val continuationStackFrames: List<CoroutineStackFrameItem>
        get() = stackTraceProvider.getContinuationStack(mirror)

    override val activeThread = mirror.lastObservedThread

    override val jobHierarchy by lazy {
        jobHierarchyProvider.findJobHierarchy(mirror)
    }
}

class CompleteCoroutineInfoData(
    descriptor: CoroutineDescriptor,
    override val continuationStackFrames: List<CoroutineStackFrameItem>,
    override val creationStackFrames: List<CreationCoroutineStackFrameItem>,
    override val activeThread: ThreadReference? = null, // for suspended coroutines should be null
    override val jobHierarchy: List<String> = emptyList()
) : CoroutineInfoData(descriptor)

fun CoroutineInfoData.toCompleteCoroutineInfoData() =
    when (this) {
        is CompleteCoroutineInfoData -> this
        else ->
            CompleteCoroutineInfoData(
                descriptor,
                continuationStackFrames,
                creationStackFrames,
                activeThread,
                jobHierarchy
            )
    }

data class CoroutineDescriptor(val name: String, val id: String, val state: State, val dispatcher: String?, val contextSummary: String?) {
    fun formatName() = "$name:$id"

    companion object {
        fun instance(mirror: MirrorOfCoroutineInfo): CoroutineDescriptor =
            CoroutineDescriptor(
                mirror.context?.name ?: DEFAULT_COROUTINE_NAME,
                "${mirror.sequenceNumber}",
                State.valueOf(mirror.state ?: DEFAULT_COROUTINE_STATE),
                mirror.context?.dispatcher,
                mirror.context?.summary
            )
    }
}

enum class State {
    RUNNING,
    SUSPENDED,
    CREATED,
    UNKNOWN,
    SUSPENDED_COMPLETING,
    SUSPENDED_CANCELLING,
    CANCELLED,
    COMPLETED,
    NEW
}
