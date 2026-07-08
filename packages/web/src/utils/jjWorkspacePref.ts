// 使用者的 jj workspace 聚合偏好，存於 localStorage，預設開啟。
// 與 worktree 聚合偏好（aggregatePref）獨立，可單獨開關 jj workspace。
const KEY = "spek:aggregate-jj";

/** 讀取 jj workspace 聚合偏好。localStorage 不可用或未設定時回傳 true。 */
export function getJjWorkspacePref(): boolean {
  try {
    return localStorage.getItem(KEY) !== "false";
  } catch {
    return true;
  }
}

/** 寫入 jj workspace 聚合偏好。 */
export function setJjWorkspacePref(value: boolean): void {
  try {
    localStorage.setItem(KEY, String(value));
  } catch {
    // localStorage 不可用時忽略
  }
}
