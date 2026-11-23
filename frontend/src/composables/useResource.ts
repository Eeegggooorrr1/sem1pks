import { ref, shallowRef } from 'vue'
export function useResource<T>(loader: () => Promise<T>) {
  const data = shallowRef<T | null>(null), error = shallowRef<unknown>(null), loading = ref(false)
  let generation = 0
  async function refresh() {
    const current = ++generation
    loading.value = true; error.value = null
    try { const result = await loader(); if (current === generation) data.value = result }
    catch (ex) { if (current === generation) error.value = ex }
    finally { if (current === generation) loading.value = false }
  }
  return { data, error, loading, refresh }
}
