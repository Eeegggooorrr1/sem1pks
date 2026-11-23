import { ref, shallowRef } from 'vue'
export function useTask() {
  const busy = ref(false)
  const error = shallowRef<unknown>(null)
  async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
    if (busy.value) return
    busy.value = true; error.value = null
    try { return await action() } catch (ex) { error.value = ex } finally { busy.value = false }
  }
  return { busy, error, run }
}
