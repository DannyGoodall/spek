package com.spek.intellij

import com.intellij.util.xmlb.XmlSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 只測 [SpekProjectState.State] 這個純資料類別，不需要 IntelliJ platform test fixture。 */
class SpekProjectStateTest {

    @Test
    fun stateDefaultsToVisibleTree() {
        assertTrue(SpekProjectState.State().treeVisible, "沒有儲存過的偏好時，樹狀面板應預設顯示")
    }

    /**
     * 迴歸測試：`hasOpenSpec` 必須留在 service 本體上。
     * 一旦被搬進 State 而持久化，已移除 openspec/ 的專案重開後會誤判仍有 OpenSpec 內容。
     */
    @Test
    fun stateSerializesOnlyTreeVisible() {
        val fields = SpekProjectState.State::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .sorted()
        assertEquals(listOf("treeVisible"), fields)
    }

    /** 直接檢查會寫進 workspace.xml 的 XML：只有非預設值的 treeVisible 會被寫出，且不含 hasOpenSpec。 */
    @Test
    fun serializedXmlCarriesTreeVisibleAndNothingElse() {
        val hidden = XmlSerializer.serialize(SpekProjectState.State(treeVisible = false))
        val options = hidden.getChildren("option")
        assertEquals(1, options.size, "只應序列化一個欄位")
        assertEquals("treeVisible", options[0].getAttributeValue("name"))
        assertEquals("false", options[0].getAttributeValue("value"))
        assertFalse(hidden.toString().contains("hasOpenSpec"))
    }

    /** 反序列化回來要拿得到原值，代表偏好真的能跨 IDE 重啟保留。 */
    @Test
    fun serializedXmlRoundTrips() {
        val element = XmlSerializer.serialize(SpekProjectState.State(treeVisible = false))
        val restored = XmlSerializer.deserialize(element, SpekProjectState.State::class.java)
        assertFalse(restored.treeVisible)
    }
}
