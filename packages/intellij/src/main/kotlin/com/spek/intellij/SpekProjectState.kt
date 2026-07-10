package com.spek.intellij

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 專案層級的 spek 狀態。
 *
 * 儲存於 workspace 檔（`.idea/workspace.xml`）而非自訂的 `.idea/spek.xml`：視窗佈局是個人偏好，
 * workspace 檔慣例上被 gitignore，而 `.idea/` 底下的自訂檔在某些 repo 會被 commit，
 * 那會讓一個人隱藏樹狀面板就強加給整個團隊。
 */
@Service(Service.Level.PROJECT)
@State(
    name = "SpekProjectState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class SpekProjectState : PersistentStateComponent<SpekProjectState.State> {

    /** 只有這個類別內的欄位會被序列化。 */
    data class State(
        var treeVisible: Boolean = true,
    )

    private var internalState = State()

    /**
     * 由 SpekStartupActivity 於每次專案開啟時重新偵測後寫入。
     *
     * 刻意留在 service 本體、不放進 [State]：一旦持久化，已移除 `openspec/` 的專案在重開後
     * 仍會以為自己有 OpenSpec 內容，"Open spek" action 也會錯誤地保持啟用。
     */
    var hasOpenSpec: Boolean = false

    /** 樹狀面板是否顯示。跨 IDE 重啟保留，預設顯示。 */
    var treeVisible: Boolean
        get() = internalState.treeVisible
        set(value) {
            internalState.treeVisible = value
        }

    override fun getState(): State = internalState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, internalState)
    }

    companion object {
        fun getInstance(project: Project): SpekProjectState {
            return project.getService(SpekProjectState::class.java)
        }
    }
}
