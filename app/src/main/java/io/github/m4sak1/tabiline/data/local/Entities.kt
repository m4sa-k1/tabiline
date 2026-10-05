package io.github.m4sak1.tabiline.data.local

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isAutomatic: Boolean = false,
)

@Entity(
    tableName = "transport_legs",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class TransportLegEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val departureMillis: Long,
    val arrivalMillis: Long,
    val departureZoneId: String,
    val arrivalZoneId: String,
    val departurePlace: String,
    val arrivalPlace: String,
    val mode: String,
    val trainType: String?,
    val trainLine: String = "",
    val departureTerminal: String = "",
    val arrivalTerminal: String = "",
    val boardingGroup: String = "",
    val flightNumber: String = "",
    val departurePlatform: String,
    val arrivalPlatform: String,
    val memo: String,
    val sortOrder: Int,
    val precedingGapType: String = "WAIT",
    @ColumnInfo(defaultValue = "''") val busLine: String = "",
    val busType: String? = null,
)

data class TripWithLegsEntity(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val legs: List<TransportLegEntity>,
)
