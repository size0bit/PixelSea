package com.pixelsea.feature.gallery.ui

import com.pixelsea.core.data.model.Photo
import com.pixelsea.core.ui.architecture.ViewEffect
import com.pixelsea.core.ui.architecture.ViewEvent
import com.pixelsea.core.ui.architecture.ViewState

/**
 * 相册页面的状态数据
 * @param permissionGranted 存储权限是否已授予
 */
data class GalleryViewState(
    val permissionGranted: Boolean = false
) : ViewState

/**
 * 相册页面的用户意图（用户操作）
 * 使用 sealed interface 确保类型安全，编译器可以检查所有分支
 */
sealed interface GalleryViewEvent : ViewEvent {
    /**
     * 权限申请结果事件
     * @param granted true 表示用户授予了权限，false 表示拒绝
     */
    data class PermissionResult(val granted: Boolean) : GalleryViewEvent
}

/**
 * 相册页面的单次副作用效果
 * 用于处理只需要触发一次的 UI 事件
 */
sealed interface GalleryViewEffect : ViewEffect {
    /**
     * 显示 Toast 提示消息
     * @param message 要显示的消息内容
     */
    data class ShowToast(val message: String) : GalleryViewEffect
}