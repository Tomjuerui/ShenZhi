<script setup lang="ts">
import { computed, h } from 'vue'
import { NAvatar, NDropdown, NTooltip } from 'naive-ui'
import type { DropdownOption } from 'naive-ui'
import { SvgIcon } from '@/components/common'
import { useAuthStore, useUserStore } from '@/store'
import { t } from '@/locales'
import { useLogout } from '@/hooks/useLogout'
import defaultAvatar from '@/assets/avatar-default.svg'

const props = withDefaults(defineProps<{ collapsed?: boolean }>(), {
  collapsed: false,
})

const emit = defineEmits<{
  (e: 'open-prompt'): void
  (e: 'open-setting'): void
}>()

const authStore = useAuthStore()
const userStore = useUserStore()
const { logout } = useLogout()

const userInfo = computed(() => userStore.userInfo)
const loggedIn = computed(() => !!authStore.token)
const displayName = computed(() => {
  if (!loggedIn.value)
    return t('sidebar.notLoggedIn')
  return userInfo.value.name || '深智'
})

const options = computed<DropdownOption[]>(() => {
  if (!loggedIn.value) {
    return [
      { key: 'login', label: t('common.login'), icon: () => h(SvgIcon, { icon: 'ri:login-box-line' }) },
    ]
  }
  return [
    { key: 'setting', label: t('setting.setting'), icon: () => h(SvgIcon, { icon: 'carbon:settings' }) },
    { key: 'prompt', label: t('store.siderButton'), icon: () => h(SvgIcon, { icon: 'tabler:prompt' }) },
    { type: 'divider', key: 'd1' },
    { key: 'logout', label: t('common.logout'), icon: () => h(SvgIcon, { icon: 'ri:logout-box-r-line' }) },
  ]
})

function handleSelect(key: string) {
  if (key === 'setting')
    emit('open-setting')
  else if (key === 'prompt')
    emit('open-prompt')
  else if (key === 'login')
    authStore.setLoginView(true)
  else if (key === 'logout')
    logout()
}
</script>

<template>
  <div class="p-2 border-t border-ds-border shrink-0">
    <NTooltip :disabled="!props.collapsed" placement="right">
      <template #trigger>
        <div>
          <NDropdown trigger="click" placement="top-start" :options="options" @select="handleSelect">
            <button
              type="button"
              class="flex items-center gap-2 h-10 px-2 rounded-ds-md text-left transition-colors hover:bg-ds-hover"
              :class="props.collapsed ? 'w-10 justify-center' : 'w-full'"
            >
              <NAvatar round :size="28" :src="userInfo.avatar" :fallback-src="defaultAvatar" class="shrink-0" />
              <template v-if="!props.collapsed">
                <span class="flex-1 min-w-0 text-[13px] text-ds-text truncate">{{ displayName }}</span>
                <SvgIcon icon="ri:more-2-fill" class="text-ds-muted shrink-0" />
              </template>
            </button>
          </NDropdown>
        </div>
      </template>
      {{ displayName }}
    </NTooltip>
  </div>
</template>
