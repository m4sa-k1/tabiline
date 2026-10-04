package io.github.m4sak1.tabiline.core.domain

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.UserSettings
import java.time.Duration

sealed interface TransferStatus {
    val minutes: Long
    data class Comfortable(override val minutes: Long) : TransferStatus
    data class Tight(override val minutes: Long, val recommendedMinutes: Int) : TransferStatus
    data class Overlap(override val minutes: Long) : TransferStatus
}

object TransferPolicy {
    fun evaluate(previous: TransportLeg, next: TransportLeg, settings: UserSettings): TransferStatus {
        val minutes = Duration.between(previous.arrival, next.departure).toMinutes()
        if (minutes < 0) return TransferStatus.Overlap(minutes)
        val threshold = settings.thresholdFor(next.mode)
        return if (minutes < threshold) TransferStatus.Tight(minutes, threshold)
        else TransferStatus.Comfortable(minutes)
    }
}
