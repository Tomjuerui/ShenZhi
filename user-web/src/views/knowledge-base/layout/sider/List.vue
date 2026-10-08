<script setup lang='ts'>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { NTabPane, NTabs } from 'naive-ui'
import SubList from './SubList.vue'
import { useAuthStore, useKbStore } from '@/store'
import { t } from '@/locales'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const currentPage = ref<number>(1)
const pageSize = 20
const kbStore = useKbStore()
const { activeKbUuid, myKbInfos, publicKbInfos, selectedKbType } = storeToRefs<any>(kbStore)
const authStore = useAuthStore()
const authStoreRef = ref<AuthState>(authStore)
const { kbUuid: currKbUuid } = route.params as { kbUuid: string }

/** 路由参数或 activeKbUuid 指向不存在的知识库（含 'default'）时，落到列表里第一个真实知识库并改路由 */
function repairActiveKb() {
  const known = (uuid: string) =>
    kbStore.myKbInfos.some(item => item.uuid === uuid) || kbStore.publicKbInfos.some(item => item.uuid === uuid)

  if (currKbUuid && currKbUuid !== 'default' && known(currKbUuid)) {
    kbStore.setActive(currKbUuid)
    return
  }

  const fallback = kbStore.getSelectedKb || kbStore.myKbInfos[0] || kbStore.publicKbInfos[0]
  if (!fallback) {
    // 一个知识库都没有：保持原样，页面会显示「去知识库管理创建」引导
    kbStore.setActive(currKbUuid || 'default')
    return
  }
  kbStore.setActive(fallback.uuid)
  if (currKbUuid !== fallback.uuid)
    router.replace({ name: 'QADetail', params: { kbUuid: fallback.uuid } })
}

async function initList() {
  if (kbStore.loaddingKbList)
    return
  if (kbStore.myKbInfos.length === 0) {
    kbStore.setLoadingKbList(true)
    try {
      const { data } = await api.knowledgeBaseSearchMine<KnowledgeBase.InfoListResp>('', currentPage.value, pageSize)
      if (data.records)
        kbStore.setMyKbInfos(data.records)
    } finally {
      kbStore.setLoadingKbList(false)
    }
  }
  repairActiveKb()
}

async function initStarredList() {
  const starListResp = await api.knowledgeBaseStarListMine<KnowledgeBase.KbStarListResp>(1, 100)
  kbStore.appStarInfos(starListResp.data.records)
}

async function initPublicList() {
  const { data: publicData } = await api.knowledgeBaseSearchPublic<KnowledgeBase.InfoListResp>('', currentPage.value, pageSize)
  if (publicData.records)
    kbStore.setPublicKbInfos(publicData.records)
  repairActiveKb()
}

watch(
  () => authStoreRef.value.token,
  (newVal) => {
    if (newVal) {
      initList()
      initStarredList()
    }
  },
)

watch(
  () => kbStore.reloadKbInfosSignal,
  (newVal) => {
    if (newVal) {
      try {
        initList()
        initStarredList()
      } finally {
        kbStore.setReloadKbInfosSignal(false)
      }
    }
  },
)

onMounted(() => {
  console.log('list onMounted')
  initPublicList()
  if (authStoreRef.value.token) {
    initList()
    initStarredList()
  }
})
</script>

<template>
  <NTabs v-model:value="selectedKbType" tab-class="h-10" pane-class="h-full" type="line" justify-content="space-evenly" class="kb-sider-tabs">
    <NTabPane name="mine" :tab="t('common.mine')" size="small">
      <SubList :list="myKbInfos" :active-kb-uuid="activeKbUuid" />
    </NTabPane>
    <NTabPane name="public" :tab="t('common.public')">
      <SubList :list="publicKbInfos" :active-kb-uuid="activeKbUuid" />
    </NTabPane>
  </NTabs>
</template>

<style scoped>
.kb-sider-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>

<style>
.kb-sider-tabs .n-tabs-pane-wrapper {
  flex: 1 !important;
  min-height: 0 !important;
  overflow: hidden !important;
}
.kb-sider-tabs .n-tab-pane {
  height: 100% !important;
  overflow: hidden !important;
}
</style>
