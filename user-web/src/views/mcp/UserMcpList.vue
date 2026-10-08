<script setup lang='ts'>
import { NButton } from 'naive-ui'
import { useAuthStore, useMcpStore } from '@/store'
import { useBasicLayout } from '@/hooks/useBasicLayout'
import LoginTip from '@/views/user/LoginTip.vue'
import { t } from '@/locales'
import { mcpTransportLabel, stripMarkdown } from '@/utils/functions'
import whaleUrl from '@/assets/whale.svg'

const emit = defineEmits<Emit>()
const authStore = useAuthStore()
const mcpStore = useMcpStore()
const { isMobile } = useBasicLayout()
interface Emit {
  (ev: 'showInfoModal', mcpInfo: Mcp.McpInfo): void
  (ev: 'showConfigModal', mcpInfo: Mcp.McpInfo): void
}

function onShowInfoModal(mcpInfo: Mcp.McpInfo) {
  emit('showInfoModal', mcpInfo)
}

function onShowConfigModal(mcpInfo: Mcp.McpInfo) {
  emit('showConfigModal', mcpInfo)
}
</script>

<template>
  <div class="flex flex-col w-full h-full pb-3">
    <div class="flex flex-wrap justify-start items-start overflow-y-auto">
      <div v-if="!authStore.token" class="w-full max-w-ds m-auto px-4 py-4">
        <LoginTip />
      </div>
      <div
        v-if="authStore.token && mcpStore.myUserMcpList.length === 0"
        class="flex items-center justify-center mt-4 text-center text-neutral-400" :class="[isMobile ? 'p-2' : 'p-4']"
      >
        <img :src="whaleUrl" alt="深智" class="w-8 h-8" >{{ t('common.noData') }}
      </div>
      <div
        v-for="userMcp in mcpStore.myUserMcpList" :key="userMcp.uuid"
        class="m-2 flex flex-col w-[380px] h-[200px] rounded-ds-md border border-ds-border bg-ds-bg p-4 transition-all duration-200 hover:border-ds-primary-border hover:shadow-[var(--ds-shadow-pop)]"
      >
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-2 min-w-0">
            <span class="shrink-0 w-8 h-8 rounded-ds-sm bg-ds-primary-soft text-ds-primary flex items-center justify-center text-sm font-bold uppercase">
              {{ userMcp.mcpInfo.title.charAt(0) }}
            </span>
            <div class="font-semibold text-ds-text truncate">
              {{ userMcp.mcpInfo.title }}
            </div>
          </div>
          <span class="shrink-0 text-xs px-2 py-0.5 rounded-full bg-ds-primary-soft text-ds-primary whitespace-nowrap">
            {{ mcpTransportLabel(userMcp.mcpInfo.transportType) }}
          </span>
        </div>
        <div class="mt-3 flex-1 overflow-hidden text-sm leading-relaxed text-ds-muted line-clamp-3">
          {{ stripMarkdown(userMcp.mcpInfo.remark) }}
        </div>
        <div class="mt-3 flex items-center justify-between border-t border-ds-border pt-3">
          <span class="flex items-center gap-1.5 text-xs text-ds-muted">
            <span class="w-1.5 h-1.5 rounded-full bg-[#18a058]" />
            {{ t('mcp.statusEnable') }}
          </span>
          <div class="flex space-x-1">
            <NButton size="tiny" quaternary type="primary" @click="onShowInfoModal(userMcp.mcpInfo)">
              {{ t('common.detail') }}
            </NButton>
            <NButton size="tiny" quaternary type="primary" @click="onShowConfigModal(userMcp.mcpInfo)">
              {{ t('mcp.configLabel') }}
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
