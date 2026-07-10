package com.spek.intellij.tree

/**
 * 樹狀面板的刷新閘門。
 *
 * 樹隱藏時不該為了沒人在看的面板重建 model，但重新顯示時又不能顯示過時內容，
 * 因此隱藏期間的刷新請求只記為 pending，於下次顯示時補做一次。
 *
 * 兩端在不同執行緒：[requestRefresh] 由 file watcher 的 Timer 執行緒呼叫、
 * [setVisible] 由 EDT 呼叫，故以同一把鎖保護。[setVisible] 直接回傳「是否需要補刷」
 * 而非讓呼叫端自行讀取旗標再決定，避免 check-then-act 競態。
 *
 * @param visible 初始可見性。初始為隱藏時 pending 起始為 true，因為 model 尚未建立過。
 */
class TreeRefreshGate(visible: Boolean) {

    private val lock = Any()
    private var visible: Boolean = visible
    private var pendingRefresh: Boolean = !visible

    /** @return true 表示呼叫端應立刻重建 model；false 表示已記為 pending。 */
    fun requestRefresh(): Boolean {
        synchronized(lock) {
            if (!visible) {
                pendingRefresh = true
                return false
            }
            return true
        }
    }

    /** @return true 表示呼叫端在顯示前應補做一次 model 重建。 */
    fun setVisible(visible: Boolean): Boolean {
        synchronized(lock) {
            this.visible = visible
            if (!visible) return false
            val needsRefresh = pendingRefresh
            pendingRefresh = false
            return needsRefresh
        }
    }

    fun isVisible(): Boolean = synchronized(lock) { visible }
}
