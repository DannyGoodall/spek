package com.spek.intellij.tree

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreeRefreshGateTest {

    @Test
    fun requestRefreshWhileVisibleRefreshesImmediately() {
        val gate = TreeRefreshGate(visible = true)
        assertTrue(gate.requestRefresh())
    }

    @Test
    fun requestRefreshWhileHiddenDefersAndRecordsPending() {
        val gate = TreeRefreshGate(visible = true)
        gate.setVisible(false)

        assertFalse(gate.requestRefresh(), "隱藏時不該立刻重建 model")
        assertTrue(gate.setVisible(true), "顯示時應補刷一次")
    }

    @Test
    fun repeatedRequestsWhileHiddenCollapseIntoOneRefresh() {
        val gate = TreeRefreshGate(visible = true)
        gate.setVisible(false)
        repeat(5) { assertFalse(gate.requestRefresh()) }

        assertTrue(gate.setVisible(true))
        gate.setVisible(false)
        assertFalse(gate.setVisible(true), "補刷過後 pending 應已清掉")
    }

    @Test
    fun showingWithoutPendingRefreshDoesNotRebuild() {
        val gate = TreeRefreshGate(visible = true)
        gate.setVisible(false)

        assertFalse(gate.setVisible(true), "隱藏期間沒有檔案變動就不該重建 model")
    }

    @Test
    fun hidingNeverRequestsRefresh() {
        val gate = TreeRefreshGate(visible = true)
        assertFalse(gate.setVisible(false))

        gate.requestRefresh()
        assertFalse(gate.setVisible(false), "隱藏動作本身永遠不需要重建")
    }

    @Test
    fun initiallyHiddenGateRefreshesOnFirstShow() {
        val gate = TreeRefreshGate(visible = false)
        assertFalse(gate.isVisible())

        assertTrue(gate.setVisible(true), "初始隱藏時 model 從未建立過，首次顯示必須重建")
        gate.setVisible(false)
        assertFalse(gate.setVisible(true))
    }

    @Test
    fun initiallyVisibleGateDoesNotRefreshOnRedundantShow() {
        val gate = TreeRefreshGate(visible = true)
        assertTrue(gate.isVisible())
        assertFalse(gate.setVisible(true))
    }

    /** 重建期間被隱藏時，閘門必須回報 not visible，讓 SpekTreePanel 的 onDone 不要把樹叫回來。 */
    @Test
    fun hidingDuringPendingRebuildLeavesGateHidden() {
        val gate = TreeRefreshGate(visible = false)
        assertTrue(gate.setVisible(true), "初始隱藏，首次顯示需重建")

        // 重建仍在背景進行時，使用者又按了隱藏
        gate.setVisible(false)

        assertFalse(gate.isVisible(), "重建完成後 onDone 應據此放棄顯示")
    }
}
