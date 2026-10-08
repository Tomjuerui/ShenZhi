<script setup lang='ts'>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store'
import { useConvList } from '@/views/chat/hooks/useConvList'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { syncByRoute } = useConvList()

const isChatRoute = computed(() => ['Root', 'Chat', 'ChatDetail'].includes(route.name as string))

// token 就绪 / 聊天路由 uuid 变化时同步：会话列表 → active → 首页消息（收起侧栏也生效）
watch(
  () => [authStore.token, route.name, route.params.uuid] as const,
  async ([token, name]) => {
    // 只在聊天路由下动作：否则离开聊天页时会把自己又弹回来
    if (!token || !['Root', 'Chat', 'ChatDetail'].includes(name as string))
      return
    const uuid = await syncByRoute()
    if (!isChatRoute.value)
      return
    if (uuid && uuid !== route.params.uuid)
      router.replace({ name: 'ChatDetail', params: { uuid } })
  },
  { immediate: true },
)
</script>

<template>
  <div class="h-full w-full overflow-hidden bg-ds-bg">
    <RouterView v-slot="{ Component, route }">
      <KeepAlive><component :is="Component" :key="route.fullPath" /></KeepAlive>
    </RouterView>
  </div>
</template>
