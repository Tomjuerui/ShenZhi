<script setup lang='ts'>
import { computed, ref } from 'vue'
import { NScrollbar } from 'naive-ui'
import { SvgIcon } from '@/components/common'
import { useAppStore, useAuthStore, useChatStore } from '@/store'
import { useBasicLayout } from '@/hooks/useBasicLayout'
import EditConv from '@/views/chat/components/Header/EditConv.vue'
import { useConvList } from '@/views/chat/hooks/useConvList'
import { t } from '@/locales'

const { isMobile } = useBasicLayout()
const appStore = useAppStore()
const chatStore = useChatStore()
const authStore = useAuthStore()
const { ensureMessagesLoaded } = useConvList()
const mouseEnterKbUuid = ref<string>('')
const showEditModal = ref<boolean>(false)
const editCharacter = ref<Chat.Character>({} as Chat.Character)

async function handleSelect({ uuid }: Chat.Character) {
  if (isActive(uuid))
    return

  if (chatStore.active)
    chatStore.updateCharacter(chatStore.active, {})
  await chatStore.setActive(uuid)

  await ensureMessagesLoaded(uuid)

  if (isMobile.value)
    appStore.setSiderCollapsed(true)
}

function handleMouseEnter({ uuid }: Chat.Character) {
  mouseEnterKbUuid.value = uuid
}
function handleMouseLeave() {
  mouseEnterKbUuid.value = ''
}
function openEditView(item: Chat.Character) {
  if (!authStore.checkLoginOrShow())
    return
  showEditModal.value = true
  editCharacter.value = item
}

function isActive(uuid: string) {
  return chatStore.active === uuid
}

const characterList = computed(() => chatStore.characters)

// 按 createTime 的 yyyy-MM 分桶，月份倒序；无时间的归入「更早」
const groupedCharacters = computed(() => {
  const buckets = new Map<string, Chat.Character[]>()
  for (const item of characterList.value) {
    const month = item.createTime ? item.createTime.slice(0, 7) : 'earlier'
    const bucket = buckets.get(month)
    if (bucket)
      bucket.push(item)
    else
      buckets.set(month, [item])
  }
  return [...buckets.entries()]
    .sort(([a], [b]) => {
      if (a === 'earlier')
        return 1
      if (b === 'earlier')
        return -1
      return b.localeCompare(a)
    })
    .map(([key, items]) => ({ key, label: key === 'earlier' ? t('sidebar.earlier') : key, items }))
})
</script>

<template>
  <EditConv v-model:showModal="showEditModal" :character="editCharacter" @show-modal="(show) => showEditModal = show" />
  <NScrollbar class="px-2">
    <template v-if="!characterList.length">
      <div class="flex flex-col items-center mt-4 text-center text-ds-muted">
        <SvgIcon icon="ri:inbox-line" class="mb-2 text-3xl" />
        <span>{{ t('common.noData') }}</span>
      </div>
    </template>
    <template v-else>
      <template v-for="group in groupedCharacters" :key="group.key">
        <div class="px-2.5 pt-3 pb-1 text-[12px] text-ds-muted">
          {{ group.label }}
        </div>
        <div class="flex flex-col gap-0.5">
          <a
            v-for="item in group.items"
            :key="item.uuid"
            class="relative flex items-center gap-2 px-2.5 py-2 rounded-ds-md cursor-pointer text-[13px] transition-colors"
            :class="isActive(item.uuid) ? 'bg-ds-active text-ds-text' : 'text-ds-secondary hover:bg-ds-hover'"
            @click="handleSelect(item)" @mouseenter="handleMouseEnter(item)" @mouseleave="handleMouseLeave"
          >
            <span class="flex-1 overflow-hidden break-all text-ellipsis whitespace-nowrap">{{ item.title }}</span>
            <button
              v-if="mouseEnterKbUuid === item.uuid || isMobile"
              class="shrink-0 p-1 text-ds-muted hover:text-ds-text"
              @click.stop="openEditView(item)"
            >
              <SvgIcon icon="carbon:edit" />
            </button>
          </a>
        </div>
      </template>
    </template>
  </NScrollbar>
</template>
