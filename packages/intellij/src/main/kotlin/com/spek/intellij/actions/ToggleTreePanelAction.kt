package com.spek.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.spek.intellij.SpekProjectState

/**
 * 切換 spek Tool Window 內樹狀面板的顯示狀態。
 *
 * 同一個實例同時掛在 Tool Window 標題列與 ⋮ gear 選單：標題列給一次點擊、有按下狀態的圖示，
 * gear 選單給有文字標籤的條目補足純圖示的可發現性。兩處各自複製 Presentation，共用實例是安全的。
 */
class ToggleTreePanelAction(
    private val project: Project,
    private val applyVisibility: (Boolean) -> Unit,
) : ToggleAction(TEXT, DESCRIPTION, AllIcons.Actions.ShowAsTree),
    DumbAware {

    // isSelected 只讀 project service、不碰 Swing，可安全地在背景執行緒更新
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean =
        SpekProjectState.getInstance(project).treeVisible

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        SpekProjectState.getInstance(project).treeVisible = state
        applyVisibility(state)
    }

    companion object {
        /**
         * 不可含 `&`：[com.intellij.openapi.actionSystem.Presentation.setText] 會把它當成 mnemonic 標記，
         * `"Specs & Changes"` 會被解析成 `"Specs  Changes"` 並把空白（ASCII 32）綁成快捷鍵。
         */
        const val TEXT = "Show Specs and Changes Tree"
        const val DESCRIPTION = "Show or hide the specs and changes tree in the spek tool window"
    }
}
