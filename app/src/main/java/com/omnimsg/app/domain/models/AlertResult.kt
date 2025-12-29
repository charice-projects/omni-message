// 📁 app/src/main/java/com/omnimsg/app/domain/models/AlertResult.kt
package com.omnimsg.app.domain.models

import com.omnimsg.app.ui.viewmodels.TriggerMethod

data class AlertResult(
    val id: String,
    val timestamp: Long,
    val triggerMethod: TriggerMethod,
    val totalContacts: Int,
    val successfulSends: Int,
    val failedSends: Int,
    val includeLocation: Boolean,
    val includeAudio: Boolean,
    val includePhotos: Boolean,
    val initialStatus: String
)