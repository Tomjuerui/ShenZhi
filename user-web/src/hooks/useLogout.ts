import { ref } from 'vue'
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'

export function useLogout() {
  const submitting = ref(false)

  async function logout() {
    if (submitting.value)
      return
    submitting.value = true
    try {
      await api.logout()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
    useAuthStore().removeToken()
    useUserStore().resetUserInfo()
    window.location.reload()
  }

  return { submitting, logout }
}
