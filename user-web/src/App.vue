<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { NConfigProvider } from 'naive-ui'
import { NaiveProvider, PromptStore } from '@/components/common'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import { useTheme } from '@/hooks/useTheme'
import { useLanguage } from '@/hooks/useLanguage'
import { useAppStore, useAuthStore } from '@/store'
import { detectBrowserLocale } from '@/store/modules/app'
import Login from '@/views/user/Login.vue'
import api from '@/api'

const Setting = defineAsyncComponent(() => import('@/components/common/Setting/index.vue'))

const appStore = useAppStore()
const authStore = useAuthStore()
const { theme, themeOverrides } = useTheme()
const { language } = useLanguage()
const showPrompt = ref<boolean>(false)
const showSetting = ref<boolean>(false)

watch(
  () => authStore.token,
  (newToken, oldToken) => {
    if (!newToken && oldToken)
      showSetting.value = false
  },
)

onMounted(async () => {
  const llms = await api.loadLLMs<AiModelInfo[]>()
  appStore.setLLMs(llms.data)
  const imageModels = await api.loadImageModels<AiModelInfo[]>()
  appStore.setImageModels(imageModels.data)
  const engines = await api.loadSearchEngines<SearchEngineInfo[]>()
  appStore.setSearchEngines(engines.data)
  const sysConfig = await api.getSysConfig<SysConfigInfo>()
  appStore.setSysConfig(sysConfig.data)
  const locale = authStore.token
    ? sysConfig.data.defaultLocale
    : (detectBrowserLocale() || sysConfig.data.defaultLocale)
  appStore.initLocale(locale)
})
</script>

<template>
  <NConfigProvider class="h-full" :theme="theme" :theme-overrides="themeOverrides" :locale="language">
    <NaiveProvider>
      <div class="flex h-[100dvh] w-full overflow-hidden bg-ds-bg">
        <AppSidebar @open-prompt="showPrompt = true" @open-setting="showSetting = true" />
        <main class="flex-1 min-w-0 h-full overflow-hidden">
          <RouterView v-slot="{ Component: RouteComponent, route: viewRoute }">
            <KeepAlive>
              <component :is="RouteComponent" :key="viewRoute.fullPath" />
            </KeepAlive>
          </RouterView>
        </main>
      </div>

      <PromptStore v-model:visible="showPrompt" />
      <Setting v-model:visible="showSetting" />
      <Login />
    </NaiveProvider>
  </NConfigProvider>
</template>
