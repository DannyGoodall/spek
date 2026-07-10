package com.spek.intellij

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.ContentFactory
import com.spek.intellij.actions.ToggleTreePanelAction
import com.spek.intellij.tree.SpekTreePanel

class SpekToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val basePath = project.basePath ?: return
        val browserPanel = SpekBrowserPanel(project)

        val treeVisible = SpekProjectState.getInstance(project).treeVisible
        val treePanel = SpekTreePanel(basePath, treeVisible) { path ->
            browserPanel.navigateTo(path)
        }
        browserPanel.onFileChanged = { treePanel.refresh() }

        // JBSplitter（而非 JSplitPane）：子元件不可見時會把空間全數讓給另一側並隱藏 divider，
        // 且 proportionKey 讓分隔線比例自動持久化。
        val splitter = JBSplitter(true, SPLITTER_PROPORTION_KEY, DEFAULT_PROPORTION).apply {
            firstComponent = treePanel
            secondComponent = browserPanel.component
            dividerWidth = 4
        }

        val toggle = ToggleTreePanelAction(project) { visible -> treePanel.setTreeVisible(visible) }
        toolWindow.setTitleActions(listOf(toggle))
        toolWindow.setAdditionalGearActions(DefaultActionGroup(toggle))

        val content = ContentFactory.getInstance().createContent(splitter, "", false)
        toolWindow.contentManager.addContent(content)
        Disposer.register(content, browserPanel)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        val basePath = project.basePath ?: return false
        return com.spek.intellij.core.OpenSpecScanner.hasOpenSpec(basePath)
    }

    private companion object {
        const val SPLITTER_PROPORTION_KEY = "spek.toolwindow.splitter"
        const val DEFAULT_PROPORTION = 0.3f
    }
}
