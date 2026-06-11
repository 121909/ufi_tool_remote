package com.example.ufitoolsremote.easytier

import com.example.ufitoolsremote.model.EasyTierStatus

internal fun mergeEasyTierStatus(previous: EasyTierStatus, current: EasyTierStatus): EasyTierStatus {
    if (current.isEmptySnapshot && previous.runningCount > 0) {
        return previous.copy(updatedAtMillis = current.updatedAtMillis)
    }
    if (previous.runningCount > 0 && current.runningCount == 0 && current.errorMessage.isNullOrBlank() && current.instanceCount > 0) {
        return previous.copy(updatedAtMillis = current.updatedAtMillis)
    }
    return current
}
