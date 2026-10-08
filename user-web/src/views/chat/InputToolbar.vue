<script setup lang='ts'>
import { computed, ref, watch } from 'vue'
import { NButton, NCheckbox, NCheckboxGroup, NFlex, NList, NListItem, NModal, NPopover, NUpload, useMessage } from 'naive-ui'
import type { UploadFileInfo } from 'naive-ui'
import ConvKnowledgeSelector from './ConvKnowledgeSelector.vue'
import { LLMSelector, SvgIcon } from '@/components/common'
import { useAppStore, useAuthStore, useChatStore, useMcpStore } from '@/store'
import { getDefaultCharacter } from '@/store/modules/chat/helper'
import { router } from '@/router'
import { t } from '@/locales'
import api from '@/api'

const emit = defineEmits<Emit>()
const allowedImageTypes = ['image/png', 'image/jpeg']
interface Emit {
  (e: 'imagesChange', imageUuids: string[]): void
  (e: 'submit'): void
  (e: 'stop'): void
  (e: 'voice'): void
}
withDefaults(defineProps<{ submitDisabled?: boolean; chatting?: boolean }>(), {
  submitDisabled: true,
  chatting: false,
})
const appStore = useAppStore()
const authStore = useAuthStore()
const chatStore = useChatStore()
const mcpStore = useMcpStore()
const token = ref<string>(authStore.token)
const ms = useMessage()
const uploadedFileInfoList = ref<UploadFileInfo[]>([])
const uploadedUuidList = ref<string[]>([])
const uploadedUrls = ref<string[]>([])
const currCharacter = computed(() => chatStore.getCurCharacter || getDefaultCharacter())
const canUploadImage = ref<boolean>(false)
const isReasoner = ref<boolean>(false)
const isThinkingClosable = ref<boolean>(false)
const mcpModalShow = ref<boolean>(false)
const knowledgeModalShow = ref<boolean>(false)
const tmpMcpIds = ref<string[]>([])
const tmpCharacterKbs = ref<Chat.CharacterKnowledge[]>([])
const tmpCharacterKbIds = ref<string[]>([])

async function beforeUpload(data: { file: UploadFileInfo; fileList: UploadFileInfo[] }) {
  const file = data.file.file
  if (!file) {
    ms.error(t('chat.fileNotExist'))
    return false
  }
  if (allowedImageTypes.findIndex(item => item === file.type) === -1) {
    ms.error(t('chat.imageFormatError'))
    return false
  }
  if (file.size > 4 * 1024 * 1024) {
    ms.error(t('chat.fileSizeExceed'))
    return false
  }
  return true
}

function handleFinish({ file, event }: { file: UploadFileInfo; event?: ProgressEvent }) {
  const res = JSON.parse((event?.target as XMLHttpRequest).response)
  if (res.success) {
    uploadedUuidList.value.push(res.data.uuid)
    uploadedUrls.value.push(res.data.url)
    uploadedFileInfoList.value.push(file)
  } else {
    console.log(`handleOriginalFinish err:${res.data}`)
  }
  emit('imagesChange', uploadedUrls.value)
}

function handlerRemove({ file }: { file: UploadFileInfo }) {
  const itemIndex = uploadedFileInfoList.value.findIndex(item => item.id === file.id)
  const removeUuid = uploadedUuidList.value.at(itemIndex)
  if (removeUuid) {
    api.fileDel(removeUuid)
    uploadedUrls.value.splice(itemIndex, 1)
    uploadedUuidList.value.splice(itemIndex, 1)
    uploadedFileInfoList.value.splice(itemIndex, 1)
  }
  emit('imagesChange', uploadedUrls.value)
}

// DeepSeek 深度思考模式与工具调用不兼容（langchain4j #3461: partialArguments cannot be null）
// TODO: 升级 langchain4j 后移除此 workaround，恢复工具调用支持
const isDeepSeekThinking = computed(() => {
  const modelName = appStore.selectedLLM?.modelName?.toLowerCase() || ''
  return currCharacter.value.isEnableThinking && isReasoner.value && modelName.includes('deepseek')
})

function handleMcpModalShow() {
  if (isDeepSeekThinking.value) {
    ms.warning(t('chat.deepThinkingIncompatibleWithTool'))
    return
  }
  mcpModalShow.value = true
  tmpMcpIds.value = [...currCharacter.value.mcpIds]
}

function handleKnowledgeModalShow() {
  knowledgeModalShow.value = true
  tmpCharacterKbs.value = [...currCharacter.value.characterKnowledgeList]
  tmpCharacterKbIds.value = currCharacter.value.characterKnowledgeList.map(kb => kb.id)
}

function handleKnowledgeSave() {
  knowledgeModalShow.value = false
}

async function handleSaveMcps() {
  try {
    currCharacter.value.mcpIds = tmpMcpIds.value
    await api.characterEdit(currCharacter.value.uuid, { mcpIds: currCharacter.value.mcpIds })
    chatStore.updateCharacter(currCharacter.value.uuid, currCharacter.value)
  } catch (error) {
    console.error('handleSaveMcps error', error)
  } finally {
    mcpModalShow.value = false
  }
}

function gotoMcp() {
  router.push({ name: 'Mcp' })
  mcpModalShow.value = false
}

function toggleUsingContext() {
  api.characterToggleUsingContext(currCharacter.value.uuid, !currCharacter.value.understandContextEnable)
  currCharacter.value.understandContextEnable = !currCharacter.value.understandContextEnable
  if (currCharacter.value.understandContextEnable)
    ms.success(t('chat.turnOnContext'))
  else
    ms.warning(t('chat.turnOffContext'))
}

async function toogleThinking() {
  if (!isReasoner.value || !isThinkingClosable.value) {
    console.log('该模型不支持对深度思考功能的开启或关闭')
    return
  }
  currCharacter.value.isEnableThinking = !currCharacter.value.isEnableThinking
  await api.characterToggleThinking(currCharacter.value.uuid, currCharacter.value.isEnableThinking)
  if (currCharacter.value.isEnableThinking) {
    // DeepSeek 深度思考模式与工具调用不兼容（langchain4j #3461, TODO: 升级后移除）
    if (isDeepSeekThinking.value && currCharacter.value.mcpIds.length > 0) {
      currCharacter.value.mcpIds = []
      await api.characterEdit(currCharacter.value.uuid, { mcpIds: [] })
      ms.warning(t('chat.deepThinkingAutoCloseTool'))
    } else {
      ms.success(t('chat.deepThinkingEnabled'))
    }
  } else {
    ms.warning(t('chat.deepThinkingDisabled'))
  }
}

async function toogleWebSearch() {
  if (!appStore.selectedLLM.isSupportWebSearch) {
    console.log('该模型不支持联网搜索功能的开启或关闭')
    return
  }
  if (isDeepSeekThinking.value) {
    ms.warning(t('chat.deepThinkingIncompatibleWithWebSearch'))
    return
  }
  currCharacter.value.isEnableWebSearch = !currCharacter.value.isEnableWebSearch
  try {
    await api.characterEdit(currCharacter.value.uuid, { isEnableWebSearch: currCharacter.value.isEnableWebSearch })
  } catch (err) {
    console.error('toogleWebSearch error', err)
    ms.error(`${t('chat.operationFailed')}${err}`, { duration: 2000 })
    return
  }
  if (currCharacter.value.isEnableWebSearch)
    ms.success(t('chat.webSearchEnabled'))
  else
    ms.warning(t('chat.webSearchDisabled'))
}

watch(
  () => appStore.selectedLLM,
  (newVal) => {
    isReasoner.value = newVal.isReasoner
    isThinkingClosable.value = newVal.isThinkingClosable
    if (newVal.inputTypes?.includes('image'))
      canUploadImage.value = true
    else
      canUploadImage.value = false
  },
  {
    immediate: true,
  },
)

watch(isDeepSeekThinking, async (newVal) => {
  if (newVal) {
    if (currCharacter.value.mcpIds.length > 0) {
      currCharacter.value.mcpIds = []
      await api.characterEdit(currCharacter.value.uuid, { mcpIds: [] })
      ms.warning(t('chat.deepThinkingAutoCloseTool'))
    }
    if (currCharacter.value.isEnableWebSearch) {
      currCharacter.value.isEnableWebSearch = false
      try {
        await api.characterEdit(currCharacter.value.uuid, { isEnableWebSearch: false })
      } catch (err) {
        console.error('auto disable webSearch error', err)
      }
      ms.warning(t('chat.deepThinkingAutoCloseWebSearch'))
    }
  }
}, { immediate: true })
</script>

<template>
  <div class="input-tool-bar flex flex-wrap items-center gap-1.5 px-3 pb-2.5 pt-1">
    <LLMSelector />

    <!-- 深度思考 -->
    <div
      class="ds-chip"
      :class="[
        currCharacter.isEnableThinking && isReasoner && isThinkingClosable ? 'ds-chip--on' : '',
        (!isReasoner || !isThinkingClosable) ? 'ds-chip--disabled' : '',
        (isReasoner && isThinkingClosable) ? 'cursor-pointer' : 'cursor-not-allowed',
      ]"
      @click="toogleThinking"
    >
      <template v-if="isReasoner && isThinkingClosable">
        <SvgIcon icon="ri:brain-line" />
        {{ t('chat.deepThinking') }}
      </template>
      <template v-if="isReasoner && !isThinkingClosable">
        <NPopover trigger="hover">
          <template #trigger>
            <span class="flex items-center gap-1">
              <SvgIcon icon="ri:brain-line" />
              {{ t('chat.deepThinking') }}
            </span>
          </template>
          <span>{{ t('chat.deepThinkingCannotDisable') }}</span>
        </NPopover>
      </template>
      <template v-if="!isReasoner">
        <NPopover trigger="hover">
          <template #trigger>
            <span class="flex items-center gap-1">
              <SvgIcon icon="ri:brain-line" />
              {{ t('chat.deepThinking') }}
            </span>
          </template>
          <span>{{ t('chat.deepThinkingNotSupported') }}</span>
        </NPopover>
      </template>
    </div>

    <!-- 联网搜索 -->
    <div
      class="ds-chip"
      :class="[
        currCharacter.isEnableWebSearch && appStore.selectedLLM.isSupportWebSearch ? 'ds-chip--on' : '',
        appStore.selectedLLM.isSupportWebSearch ? 'cursor-pointer' : 'ds-chip--disabled cursor-not-allowed',
      ]"
      @click="toogleWebSearch"
    >
      <template v-if="appStore.selectedLLM.isSupportWebSearch">
        <SvgIcon icon="ri:global-line" />
        {{ t('chat.webSearch') }}
      </template>
      <template v-else>
        <NPopover trigger="hover">
          <template #trigger>
            <span class="flex items-center gap-1">
              <SvgIcon icon="ri:global-line" />
              {{ t('chat.webSearch') }}
            </span>
          </template>
          <span>{{ t('chat.webSearchNotSupported') }}</span>
        </NPopover>
      </template>
    </div>

    <!-- 上下文 -->
    <div
      class="ds-chip cursor-pointer"
      :class="currCharacter.understandContextEnable ? 'ds-chip--on' : ''"
      @click="toggleUsingContext"
    >
      <NPopover trigger="hover">
        <template #trigger>
          <span class="flex items-center gap-1">
            <SvgIcon icon="ri:chat-history-line" />
            {{ t('chat.usingContext') }}
          </span>
        </template>
        <span>{{ currCharacter.understandContextEnable ? t('chat.understandContextEnable') : t('chat.understandContextDisable') }}</span>
      </NPopover>
    </div>

    <!-- 知识库 -->
    <div class="ds-chip cursor-pointer max-w-[180px]" @click="handleKnowledgeModalShow">
      <SvgIcon icon="ri:book-2-line" />
      <span class="truncate">
        {{ t('chat.knowledgeBaseLabel') }}
        <template v-if="currCharacter.characterKnowledgeList.length === 0">· {{ t('common.none') }}</template>
        <template v-else>
          · {{ currCharacter.characterKnowledgeList.map(k => k.title).join('、') }}
        </template>
      </span>
    </div>

    <!-- 工具 / MCP -->
    <div class="ds-chip cursor-pointer max-w-[180px]" @click="handleMcpModalShow">
      <SvgIcon icon="ri:tools-line" />
      <span class="truncate">
        {{ t('chat.toolLabel') }}
        <template v-if="currCharacter.mcpIds.length === 0">· {{ t('common.none') }}</template>
        <template v-else>
          · {{ mcpStore.myUserMcpList.filter(m => currCharacter.mcpIds.includes(m.mcpInfo.id)).map(m => m.mcpInfo.title).join('、') }}
        </template>
      </span>
    </div>

    <!-- 右侧动作区：附件 / 语音 / 发送 -->
    <div class="ml-auto flex items-center gap-1.5">
      <div class="ds-chip" :class="canUploadImage ? 'cursor-pointer' : 'ds-chip--disabled cursor-not-allowed'">
        <NUpload
          :action="`/api/image/upload?token=${token}`" response-type="text" :disabled="!canUploadImage"
          :show-file-list="false" @before-upload="beforeUpload" @finish="handleFinish"
        >
          <NPopover trigger="hover">
            <template #trigger>
              <span class="flex items-center">
                <SvgIcon icon="ri:attachment-2" class="text-base" />
              </span>
            </template>
            <span>{{ canUploadImage ? t('chat.uploadImageTip') : t('chat.uploadImageNotSupported') }}</span>
          </NPopover>
        </NUpload>
      </div>
      <NButton circle size="small" quaternary :title="t('chat.voiceChat')" @click="emit('voice')">
        <template #icon>
          <SvgIcon icon="icon-park-outline:voice" />
        </template>
      </NButton>
      <NButton
        v-if="chatting" circle size="small" type="primary" :title="t('common.stopRequest')"
        @click="emit('stop')"
      >
        <template #icon>
          <SvgIcon icon="ri:stop-fill" />
        </template>
      </NButton>
      <NButton
        v-else circle size="small" type="primary" :disabled="submitDisabled"
        :title="t('chat.sendMessageShortcut')" @click="emit('submit')"
      >
        <template #icon>
          <SvgIcon icon="ri:arrow-up-line" />
        </template>
      </NButton>
    </div>
  </div>

  <NList v-if="uploadedFileInfoList.length > 0" hoverable show-divider>
    <NListItem v-for="fileInfo in uploadedFileInfoList" :key="fileInfo.id">
      <div class="flex">
        <span class="flex-1 text-xs">{{ fileInfo.name }}</span>
        <SvgIcon
          class="flex-none cursor-pointer text-sm" icon="clarity:remove-line"
          @click="handlerRemove({ file: fileInfo })"
        />
      </div>
    </NListItem>
  </NList>

  <NModal
    v-model:show="knowledgeModalShow" display-directive="show" style="width: 90%; max-width: 800px"
    preset="card" :title="t('chat.configCharacterKnowledge')"
  >
    <ConvKnowledgeSelector :tmp-save="false" :character="currCharacter" @submitted="handleKnowledgeSave" />
  </NModal>
  <NModal v-model:show="mcpModalShow" style="width: 90%; max-width: 640px" preset="card" :title="t('chat.configMcp')">
    <NCheckboxGroup v-model:value="tmpMcpIds" class="my-2 flex flex-wrap space-x-2">
      <NCheckbox
        v-for="userMcp in mcpStore.myUserMcpList" :key="userMcp.uuid" :value="userMcp.mcpInfo.id"
        :label="userMcp.mcpInfo.title"
      />
    </NCheckboxGroup>
    <span v-if="mcpStore.myUserMcpList.length === 0" class="mr-1">{{ t('common.noData') }}</span>
    <NFlex justify="space-between" class="mt-4">
      <NButton type="primary" text tag="a" class="mt-4" @click="gotoMcp">
        {{ t('chat.goEnableMoreTools') }}
      </NButton>
      <NButton type="primary" @click="handleSaveMcps()">
        {{ t('common.save') }}
      </NButton>
    </NFlex>
  </NModal>
</template>

<style lang="less">
.input-tool-bar .n-upload-file-list {
  display: none;
}

.ds-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  border-radius: var(--ds-radius-pill);
  border: 1px solid var(--ds-border);
  background: var(--ds-bg);
  color: var(--ds-text-secondary);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  transition: background-color .15s, border-color .15s, color .15s;
}

.ds-chip:hover {
  background: var(--ds-bg-hover);
}

.ds-chip--on {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ds-chip--disabled {
  opacity: .6;
}
</style>
