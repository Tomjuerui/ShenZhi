import type { GlobalThemeOverrides } from 'naive-ui'
import { computed, watch } from 'vue'
import { darkTheme, useOsTheme } from 'naive-ui'
import { useAppStore } from '@/store'

const DS_FONT = '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Roboto, "Helvetica Neue", Arial, sans-serif'

export function useTheme() {
  const appStore = useAppStore()

  const OsTheme = useOsTheme()

  const isDark = computed(() => {
    if (appStore.theme === 'auto')
      return OsTheme.value === 'dark'
    else
      return appStore.theme === 'dark'
  })

  const theme = computed(() => {
    return isDark.value ? darkTheme : undefined
  })

  const themeOverrides = computed<GlobalThemeOverrides>(() => {
    // Naive 会对颜色做派生计算（hover/pressed/disabled），必须传字面量色值，不能传 var()
    const common: GlobalThemeOverrides['common'] = isDark.value
      ? {
          primaryColor: '#5c78ff', primaryColorHover: '#6d86ff', primaryColorPressed: '#4a66f0', primaryColorSuppl: '#6d86ff',
          bodyColor: '#212121', cardColor: '#2b2b2b', modalColor: '#2b2b2b', popoverColor: '#2b2b2b',
          borderColor: '#2f2f2f', dividerColor: '#2f2f2f',
          textColorBase: '#ececec', textColor1: '#ececec', textColor2: '#b4b4b4', textColor3: '#8f8f8f',
          placeholderColor: '#6b6b6b', borderRadius: '10px', fontFamily: DS_FONT,
        }
      : {
          primaryColor: '#4d6bfe', primaryColorHover: '#3d5af0', primaryColorPressed: '#2f4ce0', primaryColorSuppl: '#3d5af0',
          bodyColor: '#ffffff', cardColor: '#ffffff', modalColor: '#ffffff', popoverColor: '#ffffff',
          borderColor: '#e5e5e5', dividerColor: '#e5e5e5',
          textColorBase: '#1a1a1a', textColor1: '#1a1a1a', textColor2: '#5c5c5c', textColor3: '#8f8f8f',
          placeholderColor: '#b3b3b3', borderRadius: '10px', fontFamily: DS_FONT,
        }
    return { common }
  })

  watch(
    () => isDark.value,
    (dark) => {
      if (dark)
        document.documentElement.classList.add('dark')
      else
        document.documentElement.classList.remove('dark')
    },
    { immediate: true },
  )

  return { theme, themeOverrides }
}
