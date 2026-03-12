package com.pixelsea.core.ui.architecture

/**
 * UI 状态：页面当前需要展示的数据集合
 * 所有页面状态都应该继承此接口，确保类型安全
 */
interface ViewState

/**
 * 用户意图：用户在 UI 上的操作（如点击、滑动、下拉刷新）
 * 使用 sealed interface 确保所有可能的事件在编译期就能被检查
 */
interface ViewEvent

/**
 * 单次副作用：只消费一次的事件（如 Toast、Snackbar、页面跳转、权限请求）
 * 这些事件在处理后应该被立即消费，不会重复触发
 */
interface ViewEffect