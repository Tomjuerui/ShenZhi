<script setup lang='ts'>
import { onMounted, ref } from 'vue'
import { NButton, NEmpty, NSwitch, useDialog, useLoadingBar, useMessage } from 'naive-ui'
import api from '@/api'
import { t } from '@/locales'
import { useBasicLayout } from '@/hooks/useBasicLayout'
import { mcpTransportLabel, stripMarkdown } from '@/utils/functions'

const emit = defineEmits<Emit>()
const ms = useMessage()
const dialog = useDialog()
const loaddingBar = useLoadingBar()
const { isMobile } = useBasicLayout()
const ownList = ref<Mcp.McpInfo[]>([])

interface Emit {
  (ev: 'add'): void
  (ev: 'edit', mcp: Mcp.McpInfo): void
  (ev: 'changed'): void
}

async function loadOwnList(showLoaddingBar = true) {
  if (showLoaddingBar)
    loaddingBar.start()
  try {
    const { data } = await api.userMcpOwnList<Mcp.McpInfo[]>()
    ownList.value = data || []
  } catch (error) {
    console.error(error)
  } finally {
    if (showLoaddingBar)
      loaddingBar.finish()
  }
}

function onAdd() {
  emit('add')
}

function onEdit(mcp: Mcp.McpInfo) {
  emit('edit', mcp)
}

async function onTogglePublic(mcp: Mcp.McpInfo, val: boolean) {
  const req: Mcp.McpAddReq = {
    uuid: mcp.uuid,
    title: mcp.title,
    transportType: mcp.transportType,
    sseUrl: mcp.sseUrl,
    sseTimeout: mcp.sseTimeout,
    stdioCommand: mcp.stdioCommand,
    stdioArg: mcp.stdioArg,
    installType: mcp.installType || (mcp.transportType === 'stdio' ? 'local' : 'remote'),
    remark: mcp.remark,
    isPublic: val,
  }
  try {
    await api.userMcpEdit<Mcp.McpInfo>(req)
    mcp.isPublic = val
    ms.success(t('mcp.publicToggleSuccess'))
    emit('changed')
  } catch (error) {
    console.error(error)
    ms.error(t('mcp.saveFailed'))
  }
}

function onDelete(mcp: Mcp.McpInfo) {
  dialog.warning({
    title: t('mcp.deleteConfirm'),
    content: mcp.title,
    positiveText: t('common.yes'),
    negativeText: t('common.no'),
    onPositiveClick: async () => {
      try {
        await api.userMcpDel<boolean>(mcp.uuid)
        ms.success(t('mcp.deleteSuccess'))
        emit('changed')
      } catch (error) {
        console.error(error)
        ms.error(t('mcp.deleteFailed'))
      }
    },
  })
}

onMounted(() => {
  loadOwnList()
})

defineExpose({ loadOwnList })
</script>

<template>
  <div class="flex flex-col w-full h-full pb-3">
    <div class="flex justify-end pr-2 pt-1">
      <NButton type="primary" size="small" @click="onAdd">
        {{ t('mcp.addTool') }}
      </NButton>
    </div>
    <div class="flex flex-wrap justify-start items-start overflow-y-auto">
      <div
        v-if="ownList.length === 0"
        class="flex items-center justify-center w-full mt-4 text-center text-neutral-400" :class="[isMobile ? 'p-2' : 'p-4']"
      >
        <NEmpty :description="t('common.noData')" />
      </div>
      <div
        v-for="mcp in ownList" :key="mcp.uuid"
        class="m-2 flex flex-col w-[380px] h-[200px] rounded-ds-md border border-ds-border bg-ds-bg p-4 transition-all duration-200 hover:border-ds-primary-border hover:shadow-[var(--ds-shadow-pop)]"
      >
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-2 min-w-0">
            <span class="shrink-0 w-8 h-8 rounded-ds-sm bg-ds-primary-soft text-ds-primary flex items-center justify-center text-sm font-bold uppercase">
              {{ mcp.title.charAt(0) }}
            </span>
            <div class="font-semibold text-ds-text truncate">
              {{ mcp.title }}
            </div>
          </div>
          <span class="shrink-0 text-xs px-2 py-0.5 rounded-full bg-ds-primary-soft text-ds-primary whitespace-nowrap">
            {{ mcpTransportLabel(mcp.transportType) }}
          </span>
        </div>
        <div class="mt-3 flex-1 overflow-hidden text-sm leading-relaxed text-ds-muted line-clamp-3">
          {{ stripMarkdown(mcp.remark) }}
        </div>
        <div class="mt-3 flex items-center justify-between border-t border-ds-border pt-3">
          <div class="flex items-center gap-1.5">
            <NSwitch size="small" :value="mcp.isPublic" @update:value="val => onTogglePublic(mcp, val)" />
            <span class="text-xs text-ds-muted">{{ mcp.isPublic ? t('mcp.public') : t('mcp.private') }}</span>
          </div>
          <div class="flex space-x-1">
            <NButton size="tiny" quaternary type="primary" @click="onEdit(mcp)">
              {{ t('common.edit') }}
            </NButton>
            <NButton size="tiny" quaternary type="error" @click="onDelete(mcp)">
              {{ t('common.delete') }}
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
