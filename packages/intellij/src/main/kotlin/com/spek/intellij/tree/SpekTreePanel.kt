package com.spek.intellij.tree

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode

class SpekTreePanel(
    private val projectPath: String,
    initiallyVisible: Boolean,
    private val onNavigate: (path: String) -> Unit,
) : JPanel(BorderLayout()) {

    private val tree: Tree
    private val gate = TreeRefreshGate(initiallyVisible)

    init {
        // 隱藏時不掃磁碟：以空 model 建立 Tree，等首次顯示再由閘門補刷
        val model = if (initiallyVisible) SpekTreeModel.build(projectPath) else SpekTreeModel.empty()
        tree = Tree(model)
        tree.isRootVisible = false
        tree.cellRenderer = SpekTreeCellRenderer()

        expandRoots()

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    handleDoubleClick()
                }
            }
        })

        isVisible = initiallyVisible
        add(JScrollPane(tree), BorderLayout.CENTER)
    }

    private fun handleDoubleClick() {
        val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val nodeData = selectedNode.userObject as? SpekTreeNode ?: return

        val path = when (nodeData) {
            is SpekTreeNode.SpecItem -> "/specs/${nodeData.spec.topic}"
            is SpekTreeNode.ChangeItem -> "/changes/${nodeData.change.slug}"
            else -> return
        }

        onNavigate(path)
    }

    /** 展開 Specs 和 Changes 根節點。rowCount 隨展開而增長，故邊走邊展。 */
    private fun expandRoots() {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    /** 由 file watcher 的 Timer 執行緒呼叫，故可見性判斷走閘門，不讀 Swing 狀態。 */
    fun refresh() {
        if (!gate.requestRefresh()) return
        rebuildModel()
    }

    /**
     * 切換樹狀面板的顯示狀態，須於 EDT 呼叫。
     *
     * 有待處理的刷新時，先在背景重建 model、完成後才顯示，避免使用者看見隱藏期間累積的過時內容。
     */
    fun setTreeVisible(visible: Boolean) {
        val needsRefresh = gate.setVisible(visible)
        if (visible && needsRefresh) {
            // 重建期間使用者可能又把樹關掉，故以閘門的當下狀態為準，別讓已隱藏的樹被 onDone 叫回來
            rebuildModel(onDone = { if (gate.isVisible()) applyVisibility(true) })
        } else {
            applyVisibility(visible)
        }
    }

    private fun applyVisibility(visible: Boolean) {
        isVisible = visible
        // Splitter 依子元件的可見性決定佈局，須讓父容器重新配置
        (parent as? JComponent)?.let {
            it.revalidate()
            it.repaint()
        }
    }

    private fun rebuildModel(onDone: (() -> Unit)? = null) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val newModel = SpekTreeModel.build(projectPath)
            ApplicationManager.getApplication().invokeLater {
                tree.model = newModel
                expandRoots()
                onDone?.invoke()
            }
        }
    }
}
