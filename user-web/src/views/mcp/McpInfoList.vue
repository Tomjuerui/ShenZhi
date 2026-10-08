<script setup lang='ts'>
import { onMounted, ref } from 'vue'
import { NButton, NPagination, useLoadingBar, useMessage } from 'naive-ui'
import { useMcpStore } from '@/store'
import api from '@/api'
import { t } from '@/locales'
import { debounce } from '@/utils/functions/debounce'
import { mcpTransportLabel, stripMarkdown } from '@/utils/functions'

const emit = defineEmits<Emit>()
const ms = useMessage()
const loaddingBar = useLoadingBar()
const mcpStore = useMcpStore()
const mcpInfoList = ref<Mcp.McpInfo[]>([])
const currentPage = ref<number>(1)
const totalPage = ref<number>(0)
const pageSize = 21

interface Emit {
  (ev: 'showInfoModal', mcpInfo: Mcp.McpInfo): void
  (ev: 'showConfigModal', mcpInfo: Mcp.McpInfo): void
}
/**
 * 加载公开列表
 */
async function loadMcpPage(page: number) {
  if (mcpStore.loading)
    return
  console.log('loadMcpPage', page)
  loaddingBar.start()
  mcpStore.setLoading(true)
  try {
    if (page > 1000) {
      ms.warning(t('mcp.maxPageLimit'), {
        duration: 3000,
      })
      return
    }
    const { data } = await api.mcpSearch<Mcp.McpInfoListResp>('', page, pageSize)
    data.records.forEach((mcp) => {
      const userMcp = mcpStore.myUserMcpList.find(userMcp => userMcp.mcpId === mcp.id)
      if (userMcp) {
        mcp.configured = true
        mcp.customizedParamDefinitions.forEach((uninitParam) => {
          const paramSetting = userMcp.mcpCustomizedParams.find(varItem => varItem.name === uninitParam.name)
          // 将已设置好的参数赋值给mcp的customizedParamDefinition
          if (paramSetting)
            uninitParam.value = paramSetting.value
          else
            uninitParam.value = ''
        })
      } else {
        mcp.configured = false
      }
    })
    mcpInfoList.value = data.records
    totalPage.value = data.pages
  } catch (error) {
    console.error(error)
  } finally {
    mcpStore.setLoading(false)
    loaddingBar.finish()
  }
}

function onShowInfoModal(mcpInfo: Mcp.McpInfo) {
  emit('showInfoModal', mcpInfo)
}

function onShowConfigModal(mcpInfo: Mcp.McpInfo) {
  emit('showConfigModal', mcpInfo)
}

const handleLoadNext = debounce(loadMcpPage, 300)
onMounted(() => {
  if (mcpInfoList.value.length === 0)
    handleLoadNext(currentPage.value)
})

defineExpose({
  reload() {
    loadMcpPage(currentPage.value)
  },
})
</script>

<template>
  <div class="flex flex-col w-full h-full pb-3">
    <div class="flex flex-wrap justify-start items-start overflow-y-auto">
      <div
        v-for="mcpInfo in mcpInfoList" :key="mcpInfo.uuid"
        class="m-2 flex flex-col w-[380px] h-[200px] rounded-ds-md border border-ds-border bg-ds-bg p-4 transition-all duration-200 hover:border-ds-primary-border hover:shadow-[var(--ds-shadow-pop)]"
      >
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center gap-2 min-w-0">
            <span class="shrink-0 w-8 h-8 rounded-ds-sm bg-ds-primary-soft text-ds-primary flex items-center justify-center text-sm font-bold uppercase">
              {{ mcpInfo.title.charAt(0) }}
            </span>
            <div class="font-semibold text-ds-text truncate">
              {{ mcpInfo.title }}
            </div>
          </div>
          <span class="shrink-0 text-xs px-2 py-0.5 rounded-full bg-ds-primary-soft text-ds-primary whitespace-nowrap">
            {{ mcpTransportLabel(mcpInfo.transportType) }}
          </span>
        </div>
        <div class="mt-3 flex-1 overflow-hidden text-sm leading-relaxed text-ds-muted line-clamp-3">
          {{ stripMarkdown(mcpInfo.remark) }}
        </div>
        <div class="mt-3 flex items-center justify-between border-t border-ds-border pt-3">
          <span class="text-xs text-ds-muted">
            {{ mcpInfo.configured ? t('mcp.configured') : t('mcp.notConfigured') }}
          </span>
          <div class="flex space-x-1">
            <NButton size="tiny" quaternary type="primary" @click="onShowInfoModal(mcpInfo)">
              {{ t('common.detail') }}
            </NButton>
            <NButton size="tiny" quaternary type="primary" @click="onShowConfigModal(mcpInfo)">
              <span v-if="mcpInfo.configured">{{ t('mcp.configLabel') }}</span>
              <span v-if="!mcpInfo.configured">{{ t('mcp.statusEnable') }}</span>
            </NButton>
          </div>
        </div>
      </div>
    </div>
    <div class="flex justify-end w-full pr-2">
      <NPagination
        v-show="totalPage > 1" v-model:page="currentPage" :page-size="pageSize" :page-count="totalPage"
        @update:page="loadMcpPage"
      />
    </div>
  </div>
</template>
