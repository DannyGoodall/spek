package com.spek.intellij.actions

import com.intellij.openapi.actionSystem.Presentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ToggleTreePanelActionTest {

    /**
     * 迴歸測試：action 文字經過 Presentation 的 mnemonic 解析後必須原封不動。
     * 曾用過 "Show Specs & Changes Tree"，結果 '&' 被吃掉、空白被綁成 mnemonic（=32）。
     */
    @Test
    fun actionTextSurvivesMnemonicParsing() {
        val presentation = Presentation()
        presentation.setText(ToggleTreePanelAction.TEXT)

        assertEquals(ToggleTreePanelAction.TEXT, presentation.text)
        assertEquals(0, presentation.mnemonic, "不應該有 mnemonic 被意外綁定")
    }

    @Test
    fun actionTextHasNoMnemonicMarker() {
        assertFalse(ToggleTreePanelAction.TEXT.contains('&'))
        assertFalse(ToggleTreePanelAction.TEXT.contains('_'))
    }
}
