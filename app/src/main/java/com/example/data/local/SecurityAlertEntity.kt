package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ThreatLevel

@Entity(tableName = "security_alerts")
data class SecurityAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packetNumber: Int,
    val timestampMs: Long,
    val threatType: String,
    val severity: ThreatLevel,
    val sourceEndpoint: String,
    val destEndpoint: String,
    val description: String,
    val technicalEvidence: String,
    val remediation: String
)
