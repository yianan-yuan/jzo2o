<!-- 边导航栏 -->
<template>
  <div :class="sideNavCls">
    <t-menu
      :class="menuCls"
      :theme="theme"
      :value="active"
      :collapsed="collapsed"
      :default-expanded="defaultExpanded"
    >
      <template #logo>
        <span
          v-if="showLogo"
          :class="`${prefix}-side-nav-logo-wrapper`"
          @click="goHome"
        >
          <span class="sideBrand" :class="{ collapsed }">
            <span class="sideBrandMark">悦</span>
            <span v-if="!collapsed" class="sideBrandName">悦家服务</span>
          </span>
        </span>
      </template>
      <menu-content :nav-data="menu" />
      <template #operations>
        <log-info />
      </template>
    </t-menu>
    <div
      :class="`${prefix}-side-nav-placeholder${collapsed ? '-hidden' : ''}`"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import type { PropType } from 'vue'
import { useRouter } from 'vue-router'
import union from 'lodash/union'
import { storeToRefs } from 'pinia'
import LogInfo from '../components/LogInfo.vue'

import { useSettingStore } from '@/store'
import { prefix } from '@/config/global'
import type { MenuRoute } from '@/types/interface'
import { getActive, getRoutesExpanded } from '@/router'

import MenuContent from './MenuContent.vue'

const MIN_POINT = 992 - 1 // 992px

const router = useRouter()
const settingStore = useSettingStore()
const setting = storeToRefs(settingStore)

const props = defineProps({
  menu: {
    type: Array as PropType<MenuRoute[]>,
    default: () => []
  },
  showLogo: {
    type: Boolean as PropType<boolean>,
    default: true
  },
  isFixed: {
    type: Boolean as PropType<boolean>,
    default: true
  },
  layout: {
    type: String as PropType<string>,
    default: ''
  },
  headerHeight: {
    type: String as PropType<string>,
    default: '64px'
  },
  theme: {
    type: String as PropType<string>,
    default: 'light'
  },
  isCompact: {
    type: Boolean as PropType<boolean>,
    default: false
  }
})
// collapsed是侧边栏是否收起
const collapsed = computed(() => useSettingStore().isSidebarCompact)
// 是否选中
const active = computed(() => {
  // if (getActive() === '/reply/index') return
  return getActive()
})
// 展开的菜单
const defaultExpanded = computed(() => {
  // if (getActive() === '/reply/index') return
  const path = getActive()
  const parentPath = path.substring(0, path.lastIndexOf('/'))
  const expanded = getRoutesExpanded()
  return union(expanded, parentPath === '' ? [] : [parentPath])
})
// sideNavCls是计算侧边栏的样式
const sideNavCls = computed(() => {
  const { isCompact } = props
  return [
    `${prefix}-sidebar-layout`,
    {
      [`${prefix}-sidebar-compact`]: isCompact,
      modeStyle: setting.mode.value === 'black'
    }
  ]
})
// menuCls是计算侧边栏的样式
const menuCls = computed(() => {
  const { showLogo } = props
  return [
    `${prefix}-side-nav`,
    {
      [`${prefix}-side-nav-no-logo`]: !showLogo
    }
  ]
})
// autoCollapsed是自动收起
const autoCollapsed = () => {
  const isCompact = window.innerWidth <= MIN_POINT
  settingStore.updateConfig({
    isSidebarCompact: isCompact
  })
}

onMounted(() => {
  autoCollapsed()
  window.onresize = () => {
    autoCollapsed()
  }
})
// 返回首页
const goHome = () => {
  router.push('/dashboard/base')
}
</script>

<style lang="less" scoped>
:deep(.t-default-menu .t-menu__operations:not(:empty)) {
  padding: 0;
  border-top: 0;
}
:deep(.t-default-menu__inner .t-menu__logo:not(:empty)) {
  border-bottom: 0;
  margin-top: 32px;
  height: auto;
}
:deep(.yldj-side-nav-logo-wrapper){
  height: auto;
}
.sideBrand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
}
.sideBrand.collapsed {
  padding: 0;
}
.sideBrandMark {
  display: inline-flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: 13px 13px 13px 4px;
  background: var(--color-main);
  color: #fff;
  font-size: 21px;
  font-weight: 700;
}
.sideBrandName {
  color: var(--td-text-color-primary);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}
.t-default-menu__inner .t-menu--scroll {
  padding-top: 24px;
}
</style>
