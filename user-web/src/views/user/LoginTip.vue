<script setup lang='ts'>
import { NButton } from 'naive-ui'
import { useAuthStore } from '@/store'
import { t } from '@/locales'
import whaleUrl from '@/assets/whale.svg'

const authStore = useAuthStore()
</script>

<template>
  <div class="flex items-center justify-center mt-4 text-center text-neutral-400">
    <img :src="whaleUrl" alt="深智" class="w-8 h-8" >
    <span class="pl-1">~</span>
    <NButton text tag="a" type="primary" @click="authStore.setLoginView(true)">
      &nbsp;&nbsp;{{ t('common.clickToLogin') }}
    </NButton>
  </div>
</template>
