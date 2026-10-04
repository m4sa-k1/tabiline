package io.github.m4sak1.tabiline.core.domain

import io.github.m4sak1.tabiline.core.model.TransportLeg
import java.time.Instant

enum class LegMoment { CURRENT, NEXT, COMPLETE }

data class HighlightedLeg(val leg: TransportLeg?, val moment: LegMoment)

object NextLegResolver {
    fun resolve(legs: List<TransportLeg>, now: Instant): HighlightedLeg {
        legs.firstOrNull { now >= it.departure && now < it.arrival }
            ?.let { return HighlightedLeg(it, LegMoment.CURRENT) }
        legs.filter { it.departure > now }.minByOrNull { it.departure }
            ?.let { return HighlightedLeg(it, LegMoment.NEXT) }
        return HighlightedLeg(null, LegMoment.COMPLETE)
    }
}
