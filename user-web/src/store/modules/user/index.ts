import { defineStore } from 'pinia'
import type { UserState } from './helper'
import { defaultSetting, getLocalState, setLocalState } from './helper'
import { resolveAvatarUrl } from '@/utils/functions'

export const useUserStore = defineStore('user-store', {
  state: (): UserState => getLocalState(),
  actions: {
    updateUserInfo(userInfo: Partial<User.Profile>) {
      this.userInfo = { ...this.userInfo, ...userInfo }
      this.userInfo.avatar = resolveAvatarUrl(this.userInfo)
      this.recordState()
    },

    resetUserInfo() {
      this.userInfo = { ...defaultSetting().userInfo }
      this.recordState()
    },

    recordState() {
      setLocalState(this.$state)
    },
  },
})
