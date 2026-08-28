package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.RemoteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ExplorerActionHostSessionClientInstrumentationTest {

    @Test
    fun lostMoveResponseRecoversTheTerminalCommittedResult() {
        val outputId = UUID.randomUUID().toString()
        val trashId = UUID.randomUUID().toString()
        val terminal = targetTrashBundle(
            state = ExplorerActionHostSessionValues.TARGET_TRASH_STATE_COMMITTED,
            trashItemIds = listOf(trashId),
            movedCount = 1,
            recoveryCount = 0,
        )
        val host = object : UnusedTestExplorerActionHostSession() {
            private var queryResult = targetTrashBundle(
                state = ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN,
                trashItemIds = emptyList(),
                movedCount = 0,
                recoveryCount = 0,
            )

            override fun moveTargetsToTrash(
                targetIds: MutableList<String>,
                outputTransactionIds: MutableList<String>,
            ): Bundle {
                assertEquals(listOf("target-1"), targetIds)
                assertEquals(listOf(outputId), outputTransactionIds)
                queryResult = Bundle(terminal)
                throw RemoteException("simulated lost Binder response")
            }

            override fun queryTargetTrash(): Bundle = Bundle(queryResult)
        }

        val result = ExplorerActionHostSessionClient(host).moveTargetsToTrash(
            targetIds = listOf("target-1"),
            outputTransactionIds = listOf(outputId),
        )

        assertEquals(ExplorerActionHostSessionValues.TARGET_TRASH_STATE_COMMITTED, result.state)
        assertEquals(listOf(trashId), result.trashItemIds)
        assertEquals(1, result.movedCount)
        assertEquals(0, result.recoveryCount)
    }

    @Test
    fun invalidOutputProofIsRejectedBeforeCallingTheHost() {
        var moveCalled = false
        val host = object : UnusedTestExplorerActionHostSession() {
            override fun moveTargetsToTrash(
                targetIds: MutableList<String>,
                outputTransactionIds: MutableList<String>,
            ): Bundle {
                moveCalled = true
                return Bundle()
            }
        }

        expectIllegalArgument {
            ExplorerActionHostSessionClient(host).moveTargetsToTrash(
                targetIds = listOf("target-1"),
                outputTransactionIds = listOf("not-a-transaction-id"),
            )
        }

        assertFalse(moveCalled)
    }

    @Test
    fun inconsistentRecoveryResultIsRejected() {
        val outputId = UUID.randomUUID().toString()
        val host = object : UnusedTestExplorerActionHostSession() {
            override fun moveTargetsToTrash(
                targetIds: MutableList<String>,
                outputTransactionIds: MutableList<String>,
            ): Bundle = targetTrashBundle(
                state = ExplorerActionHostSessionValues.TARGET_TRASH_STATE_RECOVERY_REQUIRED,
                trashItemIds = emptyList(),
                movedCount = 1,
                recoveryCount = 0,
            )
        }

        expectIllegalArgument {
            ExplorerActionHostSessionClient(host).moveTargetsToTrash(
                targetIds = listOf("target-1"),
                outputTransactionIds = listOf(outputId),
            )
        }
    }

    @Test
    fun availableReplacementHistoryIsDecodedAndRestoredExactly() {
        val historyId = UUID.randomUUID().toString()
        val host = object : UnusedTestExplorerActionHostSession() {
            override fun queryTargetReplacement(targetId: String): Bundle {
                assertEquals("archive", targetId)
                return targetReplacementBundle(
                    historyId = historyId,
                    state = ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_AVAILABLE,
                    previousSize = 42L,
                    createdAt = 1_234L,
                )
            }

            override fun undoTargetReplacement(
                targetId: String,
                replacementHistoryId: String,
            ): Bundle {
                assertEquals("archive", targetId)
                assertEquals(historyId, replacementHistoryId)
                return targetReplacementBundle(
                    historyId = historyId,
                    state = ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED,
                    previousSize = 42L,
                    createdAt = 1_234L,
                    restoredSize = 42L,
                    restoredLastModified = 5_678L,
                )
            }
        }
        val client = ExplorerActionHostSessionClient(host)

        val available = requireNotNull(client.queryTargetReplacement("archive"))
        assertEquals(historyId, available.id)
        assertEquals(42L, available.previousSize)
        assertEquals(1_234L, available.createdAt)
        assertEquals(true, available.isAvailable)

        val restored = client.undoTargetReplacement("archive", historyId)
        assertEquals(
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED,
            restored.state,
        )
        assertEquals(42L, restored.restoredSize)
        assertEquals(5_678L, restored.restoredLastModified)
    }

    @Test
    fun invalidReplacementHistoryStateIsRejected() {
        val host = object : UnusedTestExplorerActionHostSession() {
            override fun queryTargetReplacement(targetId: String): Bundle = Bundle().apply {
                putInt(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_UNDO_STATE, Int.MAX_VALUE)
            }
        }

        expectIllegalArgument {
            ExplorerActionHostSessionClient(host).queryTargetReplacement("archive")
        }
    }

    private fun expectIllegalArgument(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }

    private companion object {
        fun targetTrashBundle(
            state: Int,
            trashItemIds: List<String>,
            movedCount: Int,
            recoveryCount: Int,
        ): Bundle = Bundle().apply {
            putInt(ExplorerActionHostSessionKeys.TARGET_TRASH_STATE, state)
            putStringArrayList(
                ExplorerActionHostSessionKeys.TARGET_TRASH_ITEM_IDS,
                ArrayList(trashItemIds),
            )
            putInt(ExplorerActionHostSessionKeys.TARGET_TRASH_MOVED_COUNT, movedCount)
            putInt(ExplorerActionHostSessionKeys.TARGET_TRASH_RECOVERY_COUNT, recoveryCount)
        }

        fun targetReplacementBundle(
            historyId: String,
            state: Int,
            previousSize: Long,
            createdAt: Long,
            restoredSize: Long? = null,
            restoredLastModified: Long? = null,
        ): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_HISTORY_ID, historyId)
            putInt(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_UNDO_STATE, state)
            putLong(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_PREVIOUS_SIZE, previousSize)
            putLong(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_CREATED_AT, createdAt)
            restoredSize?.let { putLong(ExplorerActionHostSessionKeys.SIZE, it) }
            restoredLastModified?.let {
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, it)
            }
        }
    }
}
