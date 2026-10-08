<script setup lang='ts'>
import { reactive, ref, watch } from 'vue'
import { NButton, NForm, NFormItem, NInput, NInputNumber, NModal, NRadio, NRadioGroup, NSwitch, useMessage } from 'naive-ui'
import api from '@/api'
import { t } from '@/locales'

interface Props {
  show: boolean
  mcp?: Mcp.McpInfo | null
}
interface Emit {
  (ev: 'update:show', value: boolean): void
  (ev: 'saved'): void
}
const props = withDefaults(defineProps<Props>(), {
  show: false,
  mcp: null,
})
const emit = defineEmits<Emit>()
const ms = useMessage()

const innerShow = ref(false)
const saving = ref(false)

const form = reactive({
  title: '',
  transportType: 'sse',
  sseUrl: '',
  sseTimeout: 30,
  stdioCommand: '',
  stdioArg: '',
  remark: '',
  isPublic: false,
})

const isEdit = ref(false)

function resetForm() {
  form.title = ''
  form.transportType = 'sse'
  form.sseUrl = ''
  form.sseTimeout = 30
  form.stdioCommand = ''
  form.stdioArg = ''
  form.remark = ''
  form.isPublic = false
  isEdit.value = false
}

watch(() => props.show, (val) => {
  innerShow.value = val
  if (val) {
    if (props.mcp) {
      isEdit.value = true
      form.title = props.mcp.title || ''
      form.transportType = props.mcp.transportType || 'sse'
      form.sseUrl = props.mcp.sseUrl || ''
      form.sseTimeout = props.mcp.sseTimeout || 30
      form.stdioCommand = props.mcp.stdioCommand || ''
      form.stdioArg = props.mcp.stdioArg || ''
      form.remark = props.mcp.remark || ''
      form.isPublic = !!props.mcp.isPublic
    } else {
      resetForm()
    }
  }
})

watch(() => innerShow.value, (val) => {
  if (!val)
    emit('update:show', false)
})

async function onSave() {
  if (!form.title.trim()) {
    ms.warning(t('mcp.titleRequired'))
    return
  }
  if (form.transportType === 'stdio') {
    if (!form.stdioCommand.trim()) {
      ms.warning(t('mcp.stdioCommandRequired'))
      return
    }
  } else if (!form.sseUrl.trim()) {
    ms.warning(t('mcp.sseUrlRequired'))
    return
  }
  saving.value = true
  const req: Mcp.McpAddReq = {
    uuid: isEdit.value ? props.mcp?.uuid : undefined,
    title: form.title.trim(),
    transportType: form.transportType,
    sseUrl: form.sseUrl.trim(),
    sseTimeout: form.sseTimeout,
    stdioCommand: form.stdioCommand.trim(),
    stdioArg: form.stdioArg.trim(),
    installType: form.transportType === 'stdio' ? 'local' : 'remote',
    remark: form.remark.trim(),
    isPublic: form.isPublic,
  }
  try {
    if (isEdit.value)
      await api.userMcpEdit<Mcp.McpInfo>(req)
    else
      await api.userMcpAdd<Mcp.McpInfo>(req)
    ms.success(t('mcp.saveSuccess'))
    innerShow.value = false
    emit('saved')
  } catch (error) {
    console.error(error)
    ms.error(t('mcp.saveFailed'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <NModal v-model:show="innerShow" style="width: 90%; max-width: 560px;" preset="card">
    <template #header>
      <h2 class="text-xl font-bold text-ds-text">
        {{ isEdit ? t('mcp.editTool') : t('mcp.addTool') }}
      </h2>
    </template>
    <NForm label-placement="top" class="max-h-[70vh] overflow-y-auto pr-2">
      <NFormItem :label="t('mcp.titleLabel')" required>
        <NInput v-model:value="form.title" :placeholder="t('mcp.titlePlaceholder')" />
      </NFormItem>
      <NFormItem :label="t('mcp.transportType')" required>
        <NRadioGroup v-model:value="form.transportType" name="transportType">
          <NRadio value="sse">SSE</NRadio>
          <NRadio value="streamable_http">Streamable HTTP</NRadio>
          <NRadio value="stdio">STDIO</NRadio>
        </NRadioGroup>
      </NFormItem>
      <template v-if="form.transportType !== 'stdio'">
        <NFormItem :label="t('mcp.sseUrl')" required>
          <NInput v-model:value="form.sseUrl" :placeholder="t('mcp.sseUrlPlaceholder')" />
        </NFormItem>
        <NFormItem :label="t('mcp.sseTimeout')">
          <NInputNumber v-model:value="form.sseTimeout" :min="1" :max="600" class="w-full" />
        </NFormItem>
      </template>
      <template v-else>
        <NFormItem :label="t('mcp.stdioCommand')" required>
          <NInput v-model:value="form.stdioCommand" placeholder="npx" />
        </NFormItem>
        <NFormItem :label="t('mcp.stdioArg')">
          <NInput v-model:value="form.stdioArg" placeholder="-y @some/mcp-server" />
        </NFormItem>
      </template>
      <NFormItem :label="t('mcp.remark')">
        <NInput v-model:value="form.remark" type="textarea" :rows="3" :placeholder="t('mcp.remarkPlaceholder')" />
      </NFormItem>
      <NFormItem :label="t('mcp.isPublic')">
        <div class="flex items-center gap-2">
          <NSwitch v-model:value="form.isPublic" />
          <span class="text-sm text-ds-muted">{{ form.isPublic ? t('mcp.public') : t('mcp.private') }}</span>
        </div>
      </NFormItem>
    </NForm>
    <template #footer>
      <div class="flex justify-end gap-2">
        <NButton @click="innerShow = false">{{ t('common.cancel') }}</NButton>
        <NButton type="primary" :loading="saving" @click="onSave">{{ t('common.confirm') }}</NButton>
      </div>
    </template>
  </NModal>
</template>
