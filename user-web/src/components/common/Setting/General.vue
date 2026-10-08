<script lang="ts" setup>
import { computed, ref } from 'vue'
import { NAvatar, NButton, NSelect, NUpload, useMessage } from 'naive-ui'
import type { UploadCustomRequestOptions } from 'naive-ui'
import type { Language, Theme } from '@/store/modules/app/helper'
import { SvgIcon } from '@/components/common'
import { useAppStore, useAuthStore, useUserStore } from '@/store'
import { t } from '@/locales'
import { resolveAvatarUrl } from '@/utils/functions'
import { useLogout } from '@/hooks/useLogout'
import api from '@/api'
import defaultAvatar from '@/assets/avatar-default.svg'

const appStore = useAppStore()
const userStore = useUserStore()
const authStore = useAuthStore()
const ms = useMessage()
const { submitting, logout } = useLogout()

const theme = computed(() => appStore.theme)

const userInfo = computed(() => userStore.userInfo)

const avatarPreview = ref(userInfo.value.avatar ?? '')

const avatarUploading = ref(false)

const name = ref(userInfo.value.name ?? '')

const language = computed({
  get() {
    return appStore.language
  },
  set(value: Language) {
    appStore.setLanguage(value)
    if (authStore.token)
      api.userEdit({ locale: value } as User.Config)
  },
})

const themeOptions: { label: string; key: Theme; icon: string }[] = [
  {
    label: 'Auto',
    key: 'auto',
    icon: 'ri:contrast-line',
  },
  {
    label: 'Light',
    key: 'light',
    icon: 'ri:sun-foggy-line',
  },
  {
    label: 'Dark',
    key: 'dark',
    icon: 'ri:moon-foggy-line',
  },
]

const languageOptions: { label: string; key: Language; value: Language }[] = [
  { label: '简体中文', key: 'zh-CN', value: 'zh-CN' },
  { label: 'English', key: 'en-US', value: 'en-US' },
]

async function handleAvatarUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file
  if (!raw)
    return

  if (!['image/png', 'image/jpeg', 'image/webp'].includes(raw.type)) {
    ms.error(t('setting.avatarTypeError'))
    onError()
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ms.error(t('setting.avatarSizeError'))
    onError()
    return
  }

  avatarUploading.value = true
  try {
    const { data } = await api.imageUpload<{ uuid: string; url: string }>(raw)
    await api.userEdit({ avatarFileUuid: data.uuid } as User.Config)
    userStore.updateUserInfo({ avatarFileUuid: data.uuid })
    avatarPreview.value = resolveAvatarUrl({ ...userInfo.value, avatarFileUuid: data.uuid })
    ms.success(t('setting.avatarUpdated'))
    onFinish()
  } catch (error: any) {
    console.error('avatar upload error', error)
    ms.error(error?.message ?? t('setting.avatarUpdateFailed'))
    onError()
  } finally {
    avatarUploading.value = false
  }
}
</script>

<template>
  <div class="p-5 space-y-6 min-h-[200px]">
    <div class="space-y-6">
      <div class="flex flex-col items-center gap-3">
        <NAvatar round :size="72" :src="avatarPreview" :fallback-src="defaultAvatar" />
        <NUpload
          :show-file-list="false" accept="image/png,image/jpeg,image/webp"
          :custom-request="handleAvatarUpload"
        >
          <NButton size="small" :loading="avatarUploading">
            {{ t('setting.changeAvatar') }}
          </NButton>
        </NUpload>
        <p class="text-xs text-ds-muted">
          {{ t('setting.avatarHint') }}
        </p>
      </div>
      <div class="flex items-center space-x-4">
        <span class="flex-shrink-0 w-[100px]">{{ t('setting.name') }}</span>
        <div class="w-[200px]">
          {{ name }}
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <span class="flex-shrink-0 w-[100px]">{{ t('setting.theme') }}</span>
        <div class="flex flex-wrap items-center gap-4">
          <template v-for="item of themeOptions" :key="item.key">
            <NButton
              size="small" :type="item.key === theme ? 'primary' : undefined"
              @click="appStore.setTheme(item.key)"
            >
              <template #icon>
                <SvgIcon :icon="item.icon" />
              </template>
            </NButton>
          </template>
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <span class="flex-shrink-0 w-[100px]">{{ t('setting.language') }}</span>
        <div class="flex flex-wrap items-center gap-4">
          <NSelect
            style="width: 140px" :value="language" :options="languageOptions"
            @update-value="(value: Language) => language = value"
          />
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <NButton size="small" type="primary" :loading="submitting" :disabled="submitting" @click="logout">
          {{ t('common.logout') }}
        </NButton>
      </div>
    </div>
  </div>
</template>
