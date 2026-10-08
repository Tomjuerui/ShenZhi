<script setup lang="ts">
import type { Component } from 'vue'
import { computed, ref } from 'vue'
import { NButton, NDropdown, NIcon, NTooltip, useMessage } from 'naive-ui'
import type { DropdownOption } from 'naive-ui'
import { AppsOutline, LibraryOutline } from '@vicons/ionicons5'
import { ToolKit } from '@vicons/carbon'
import { RouterLink, useRoute } from 'vue-router'
import ConvList from '@/views/chat/layout/sider/List.vue'
import CreateConv from '@/views/chat/layout/sider/CreateConv.vue'
import UserMenu from './UserMenu.vue'
import { SvgIcon } from '@/components/common'
import { useAppStore, useAuthStore, useChatStore, useKbStore, useWfStore } from '@/store'
import { t } from '@/locales'
import api from '@/api'
import logoUrl from '@/icons/logo.svg'

const emit = defineEmits<{
  (e: 'open-prompt'): void
  (e: 'open-setting'): void
}>()

const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()
const chatStore = useChatStore()
const kbStore = useKbStore()
const wfStore = useWfStore()
const ms = useMessage()

const collapsed = computed(() => appStore.siderCollapsed)
const isChatRoute = computed(() => ['Root', 'Chat', 'ChatDetail'].includes(route.name as string))

const createConvRef = ref()
const creating = ref(false)

/** 直接建一条「无角色设定」空白会话并进入 */
async function createBlankConversation() {
  if (!authStore.checkLoginOrShow())
    return
  if (chatStore.allCharactersCount >= 50) {
    ms.warning(t('chat.characterReachLimit50'), { duration: 1000 })
    return
  }
  if (creating.value)
    return
  creating.value = true
  try {
    const { data: newCharacter } = await api.characterAdd<Chat.Character>({
      title: t('chat.defaultConversationTitle'),
      remark: '',
      aiSystemMessage: '',
    })
    chatStore.addCharacterAndActive(newCharacter)
  } catch (error: any) {
    ms.error(error?.message ?? t('common.wrong'))
  } finally {
    creating.value = false
  }
}

/** 下拉：预设角色（点选即建） + 打开完整弹窗 */
const presetOptions = computed<DropdownOption[]>(() => {
  const items: DropdownOption[] = chatStore.presetCharacters
    .slice(0, 15)
    .map(preset => ({ label: preset.title, key: preset.uuid }))
  if (items.length > 0)
    items.push({ type: 'divider', key: '__divider__' })
  items.push({ label: t('chat.newChatButton'), key: '__more__' })
  return items
})

async function handlePresetSelect(key: string) {
  if (!authStore.checkLoginOrShow())
    return
  if (key === '__more__') {
    createConvRef.value?.toggleModal()
    return
  }
  const preset = chatStore.presetCharacters.find(item => item.uuid === key)
  if (!preset)
    return
  try {
    const { data: newCharacter } = await api.characterAddByPreset<Chat.Character>({ presetCharacterUuid: preset.uuid })
    chatStore.addCharacterAndActive(newCharacter)
    chatStore.markPresetCharacterUsed(preset.uuid)
  } catch (error: any) {
    ms.error(error?.message ?? t('common.wrong'))
  }
}

const navItems = computed<{ key: string; icon: Component; label: string; to: Record<string, unknown> }[]>(() => [
  {
    key: 'kb',
    icon: LibraryOutline,
    label: t('menu.knowledgeBase'),
    to: { name: 'QADetail', params: { kbUuid: kbStore.activeKbUuid } },
  },
  {
    key: 'wf',
    icon: AppsOutline,
    label: t('menu.workflow'),
    to: { name: 'WfDetail', params: { uuid: wfStore.activeUuid } },
  },
  {
    key: 'mcp',
    icon: ToolKit,
    label: t('menu.mcp'),
    to: { name: 'Mcp' },
  },
])

function isNavActive(key: string) {
  const name = route.name as string
  if (key === 'kb')
    return ['QAIndex', 'QADetail', 'KnowledgeBaseManage', 'KnowledgeBaseManageDetail'].includes(name)
  if (key === 'wf')
    return name === 'WfDetail'
  if (key === 'mcp')
    return name === 'Mcp'
  return false
}
</script>

<template>
  <aside
    class="flex flex-col h-full shrink-0 bg-ds-sidebar border-r border-ds-border transition-[width] duration-200"
    :style="{ width: collapsed ? 'var(--ds-sidebar-w-collapsed)' : 'var(--ds-sidebar-w)' }"
  >
    <!-- 品牌 + 折叠 -->
    <div class="flex items-center h-14 px-3 shrink-0" :class="collapsed ? 'justify-center' : 'gap-2'">
      <RouterLink
        :to="{ name: 'ChatDetail', params: { uuid: chatStore.active } }"
        class="flex items-center gap-2 min-w-0 rounded-ds-md p-1 hover:bg-ds-hover"
      >
        <img :src="logoUrl" alt="深智" class="w-6 h-6 shrink-0" >
        <span v-if="!collapsed" class="text-[15px] font-semibold text-ds-text truncate">深智</span>
      </RouterLink>
      <NButton
        v-if="!collapsed" quaternary circle size="small" class="ml-auto shrink-0"
        :title="t('sidebar.collapse')" @click="appStore.setSiderCollapsed(true)"
      >
        <template #icon>
          <SvgIcon icon="ri:sidebar-fold-line" />
        </template>
      </NButton>
    </div>

    <!-- 开启新对话：主键直接建无角色会话；箭头选预设角色 -->
    <div class="px-3 pb-2 shrink-0" :class="collapsed ? '' : 'flex items-center gap-1'">
      <NTooltip :disabled="!collapsed" placement="right">
        <template #trigger>
          <NButton
            class="ds-new-chat"
            :circle="collapsed"
            :loading="creating"
            :style="collapsed ? 'width:32px;height:32px' : 'flex:1;height:40px'"
            @click="createBlankConversation"
          >
            <template #icon>
              <SvgIcon icon="ri:add-line" />
            </template>
            <template v-if="!collapsed">
              {{ t('chat.newConversation') }}
            </template>
          </NButton>
        </template>
        {{ t('chat.newConversation') }}
      </NTooltip>
      <NDropdown v-if="!collapsed" trigger="click" :options="presetOptions" @select="handlePresetSelect">
        <NButton class="ds-new-chat" style="width:32px;height:40px" :title="t('chat.presetRole')">
          <template #icon>
            <SvgIcon icon="ri:arrow-down-s-line" />
          </template>
        </NButton>
      </NDropdown>
    </div>

    <!-- 一级功能入口 -->
    <nav class="flex flex-col gap-0.5 px-2 shrink-0">
      <NTooltip v-for="item in navItems" :key="item.key" :disabled="!collapsed" placement="right">
        <template #trigger>
          <RouterLink
            :to="item.to"
            class="flex items-center gap-2 h-9 px-2 rounded-ds-md text-sm transition-colors"
            :class="[
              collapsed ? 'justify-center' : '',
              isNavActive(item.key) ? 'bg-ds-active text-ds-text' : 'text-ds-secondary hover:bg-ds-hover',
            ]"
          >
            <NIcon :component="item.icon" class="text-lg shrink-0" />
            <span v-if="!collapsed" class="truncate">{{ item.label }}</span>
          </RouterLink>
        </template>
        {{ item.label }}
      </NTooltip>
    </nav>

    <!-- 历史会话：仅聊天路由下渲染 -->
    <template v-if="isChatRoute && !collapsed">
      <div class="mx-3 my-2 h-px bg-ds-border shrink-0" />
      <div class="flex-1 min-h-0 overflow-hidden">
        <ConvList />
      </div>
    </template>
    <div v-else class="flex-1" />

    <UserMenu :collapsed="collapsed" @open-prompt="emit('open-prompt')" @open-setting="emit('open-setting')" />
  </aside>

  <CreateConv ref="createConvRef" />
</template>

<style scoped>
.ds-new-chat {
  border-radius: var(--ds-radius-pill);
}
</style>
