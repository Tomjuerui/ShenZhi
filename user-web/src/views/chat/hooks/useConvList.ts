import { useRoute } from 'vue-router'
import { useChatStore } from '@/store'
import api from '@/api'
import { t } from '@/locales'

export function useConvList() {
  const route = useRoute()
  const chatStore = useChatStore()

  /** 拉取当前用户全部会话；全新账号没有任何会话时自动建一条「无角色」空白会话 */
  async function loadConvList() {
    if (chatStore.convListLoaded)
      return
    const { data: characters } = await api.fetchCharacters<Chat.Character[]>()
    chatStore.setConvListLoaded(true)
    if (characters.length > 0) {
      chatStore.clearDefault()
      chatStore.addCharacters(characters)
      return
    }
    const { data: created } = await api.characterAdd<Chat.Character>({
      title: t('chat.defaultConversationTitle'),
      remark: '',
      aiSystemMessage: '',
    })
    chatStore.addCharacterAndActive(created)
  }

  /** 拉某会话首页消息；已加载过 / 正在加载则跳过 */
  async function ensureMessagesLoaded(uuid: string) {
    if (!uuid || uuid === 'default' || chatStore.loadingMsgs.has(uuid))
      return
    if (chatStore.getMsgsByCharacter(uuid).length > 0)
      return
    chatStore.addLoadingMsg(uuid)
    try {
      const { data } = await api.fetchMessages<Chat.CharacterMsgListResp>(uuid, '', 20)
      data.msgList.forEach(message => chatStore.addMessage(uuid, message, false))
      chatStore.updateCharacter(uuid, { minMsgUuid: data.minMsgUuid, loadedFirstPageMsg: true })
    } finally {
      chatStore.deleteLoadingMsg(uuid)
    }
  }

  /** 进入聊天路由时调用：保证会话列表已加载、active 与路由一致、消息已拉取。返回最终应处的会话 uuid */
  async function syncByRoute() {
    await loadConvList()
    const param = route.params.uuid as string
    const target = param && param !== 'default'
      ? chatStore.getCharacterByUuid(param)
      : chatStore.characters.find(item => item.uuid !== 'default')
    if (!target)
      return ''
    chatStore.setActiveOnly(target.uuid)
    await ensureMessagesLoaded(target.uuid)
    return target.uuid
  }

  return { loadConvList, ensureMessagesLoaded, syncByRoute }
}
