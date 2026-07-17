import type { Component } from 'vue'
import {
  Package, Box, FolderTree, Tag, ImageIcon,
  MonitorPlay, Layers, PencilRuler,
  Database, BarChart3, Gauge,
  FileText, LayoutTemplate,
  Settings, Users, Building2, ShieldCheck, ScrollText,
} from 'lucide-vue-next'

export interface MenuItem {
  key: string
  label: string
  icon: Component
  route: string
  permission?: 'admin'
  status: 'active' | 'coming'
}

export interface MenuGroup {
  key: string
  label: string
  icon: Component
  permission?: 'admin'
  children: MenuItem[]
}

export const menuConfig: MenuGroup[] = [
  {
    key: 'resource',
    label: '资源管理',
    icon: Package,
    children: [
      { key: 'models', label: '模型管理', icon: Box, route: '/models', status: 'active' },
      { key: 'categories', label: '分类管理', icon: FolderTree, route: '/categories', status: 'active' },
      { key: 'tags', label: '标签管理', icon: Tag, route: '/tags', status: 'active' },
      { key: 'resources', label: '资源库', icon: ImageIcon, route: '/resources', status: 'coming' },
    ],
  },
  {
    key: 'scene',
    label: '场景应用',
    icon: MonitorPlay,
    children: [
      { key: 'scenes', label: '场景管理', icon: Layers, route: '/scenes', status: 'active' },
      { key: 'editor', label: '场景编辑', icon: PencilRuler, route: '/editor', status: 'active' },
    ],
  },
  {
    key: 'data',
    label: '数据中心',
    icon: Database,
    children: [
      { key: 'datasources', label: '数据源管理', icon: Database, route: '/datasources', status: 'active' },
      { key: 'analysis', label: '数据分析', icon: BarChart3, route: '/analysis', status: 'active' },
      { key: 'indicators', label: '指标管理', icon: Gauge, route: '/indicators', status: 'active' },
    ],
  },
  {
    key: 'report',
    label: '资源报表',
    icon: FileText,
    children: [
      { key: 'reports', label: '报表管理', icon: FileText, route: '/reports', status: 'coming' },
      { key: 'templates', label: '模板管理', icon: LayoutTemplate, route: '/templates', status: 'coming' },
    ],
  },
  {
    key: 'system',
    label: '系统管理',
    icon: Settings,
    permission: 'admin',
    children: [
      { key: 'users', label: '用户管理', icon: Users, route: '/admin', status: 'active' },
      { key: 'departments', label: '部门管理', icon: Building2, route: '/departments', status: 'active' },
      { key: 'roles', label: '角色权限', icon: ShieldCheck, route: '/admin/roles', status: 'coming' },
      { key: 'settings', label: '系统配置', icon: Settings, route: '/admin/settings', status: 'coming' },
      { key: 'logs', label: '操作日志', icon: ScrollText, route: '/admin/logs', status: 'coming' },
    ],
  },
]
