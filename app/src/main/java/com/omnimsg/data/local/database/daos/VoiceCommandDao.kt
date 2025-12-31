package com.omnimsg.data.local.database.daos

import androidx.room.*
import com.omnimsg.data.local.database.entities.VoiceCommandEntity
import com.omnimsg.data.local.database.entities.VoiceProfileEntity
import com.omnimsg.data.local.database.entities.WakeWordDetectionEntity
import com.omnimsg.domain.models.VoiceCommandStatus
import com.omnimsg.domain.models.VoiceCommandType
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface VoiceCommandDao {
    
    // ========== Voice Commands ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceCommand(command: VoiceCommandEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceCommands(commands: List<VoiceCommandEntity>): List<Long>
    
    @Update
    suspend fun updateVoiceCommand(command: VoiceCommandEntity): Int
    
    @Query("UPDATE voice_commands SET is_favorite = :isFavorite, updated_at = :timestamp WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("DELETE FROM voice_commands WHERE id = :id")
    suspend fun deleteVoiceCommand(id: Long): Int
    
    @Query("SELECT * FROM voice_commands WHERE id = :id")
    suspend fun getVoiceCommandById(id: Long): VoiceCommandEntity?
    
    @Query("SELECT * FROM voice_commands WHERE command_id = :commandId")
    suspend fun getVoiceCommandByCommandId(commandId: String): VoiceCommandEntity?
    
    @Query("SELECT * FROM voice_commands WHERE user_id = :userId ORDER BY created_at DESC")
    fun getVoiceCommandsByUserStream(userId: String): Flow<List<VoiceCommandEntity>>
    
    @Query("SELECT * FROM voice_commands WHERE user_id = :userId AND type = :type ORDER BY created_at DESC")
    fun getVoiceCommandsByTypeStream(userId: String, type: VoiceCommandType): Flow<List<VoiceCommandEntity>>
    
    @Query("SELECT * FROM voice_commands WHERE user_id = :userId AND status = :status ORDER BY created_at DESC")
    fun getVoiceCommandsByStatusStream(userId: String, status: VoiceCommandStatus): Flow<List<VoiceCommandEntity>>
    
    @Query("SELECT * FROM voice_commands WHERE user_id = :userId AND is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteVoiceCommandsStream(userId: String): Flow<List<VoiceCommandEntity>>
    
    @Query("""
        SELECT * FROM voice_commands 
        WHERE user_id = :userId 
          AND (voice_text LIKE '%' || :query || '%' 
               OR normalized_text LIKE '%' || :query || '%'
               OR intent LIKE '%' || :query || '%')
        ORDER BY created_at DESC
    """)
    fun searchVoiceCommands(userId: String, query: String): Flow<List<VoiceCommandEntity>>
    
    @Query("SELECT COUNT(*) FROM voice_commands WHERE user_id = :userId")
    suspend fun getVoiceCommandCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM voice_commands WHERE user_id = :userId AND status = :status")
    suspend fun getVoiceCommandCountByStatus(userId: String, status: VoiceCommandStatus): Int
    
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
            AVG(confidence) as avg_confidence
        FROM voice_commands 
        WHERE user_id = :userId
    """)
    suspend fun getVoiceCommandStats(userId: String): VoiceCommandStats
    
    data class VoiceCommandStats(
        val total: Int,
        val completed: Int,
        val failed: Int,
        val avgConfidence: Float?
    )
    
    // ========== Voice Profiles ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long
    
    @Update
    suspend fun updateVoiceProfile(profile: VoiceProfileEntity): Int
    
    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfile(id: Long): Int
    
    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceProfileById(id: Long): VoiceProfileEntity?
    
    @Query("SELECT * FROM voice_profiles WHERE profile_id = :profileId")
    suspend fun getVoiceProfileByProfileId(profileId: String): VoiceProfileEntity?
    
    @Query("SELECT * FROM voice_profiles WHERE user_id = :userId")
    suspend fun getVoiceProfilesByUser(userId: String): List<VoiceProfileEntity>
    
    @Query("SELECT * FROM voice_profiles WHERE user_id = :userId AND is_active = 1")
    suspend fun getActiveVoiceProfiles(userId: String): List<VoiceProfileEntity>
    
    @Query("SELECT * FROM voice_profiles WHERE user_id = :userId AND is_default = 1 LIMIT 1")
    suspend fun getDefaultVoiceProfile(userId: String): VoiceProfileEntity?
    
    @Query("UPDATE voice_profiles SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateAllVoiceProfiles(userId: String): Int
    
    @Query("UPDATE voice_profiles SET is_active = :isActive, updated_at = :timestamp WHERE id = :id")
    suspend fun updateProfileActiveStatus(id: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE voice_profiles SET is_default = 0 WHERE user_id = :userId")
    suspend fun clearDefaultVoiceProfile(userId: String): Int
    
    @Query("UPDATE voice_profiles SET is_default = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun setDefaultVoiceProfile(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE voice_profiles SET training_samples = :samples, accuracy_score = :accuracy, last_trained_at = :timestamp WHERE id = :id")
    suspend fun updateTrainingStats(id: Long, samples: Int, accuracy: Float, timestamp: Long = System.currentTimeMillis()): Int
    
    // ========== Wake Word Detections ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeWordDetection(detection: WakeWordDetectionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeWordDetections(detections: List<WakeWordDetectionEntity>): List<Long>
    
    @Query("SELECT * FROM wake_word_detections WHERE id = :id")
    suspend fun getWakeWordDetectionById(id: Long): WakeWordDetectionEntity?
    
    @Query("SELECT * FROM wake_word_detections WHERE detection_id = :detectionId")
    suspend fun getWakeWordDetectionByDetectionId(detectionId: String): WakeWordDetectionEntity?
    
    @Query("SELECT * FROM wake_word_detections WHERE user_id = :userId ORDER BY detected_at DESC")
    fun getWakeWordDetectionsByUserStream(userId: String): Flow<List<WakeWordDetectionEntity>>
    
    @Query("SELECT * FROM wake_word_detections WHERE user_id = :userId AND wake_word = :wakeWord ORDER BY detected_at DESC")
    fun getWakeWordDetectionsByWakeWordStream(userId: String, wakeWord: String): Flow<List<WakeWordDetectionEntity>>
    
    @Query("SELECT * FROM wake_word_detections WHERE user_id = :userId AND detected_at >= :startTime AND detected_at <= :endTime ORDER BY detected_at DESC")
    fun getWakeWordDetectionsByTimeRangeStream(userId: String, startTime: Long, endTime: Long): Flow<List<WakeWordDetectionEntity>>
    
    @Query("SELECT COUNT(*) FROM wake_word_detections WHERE user_id = :userId")
    suspend fun getWakeWordDetectionCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM wake_word_detections WHERE user_id = :userId AND wake_word = :wakeWord")
    suspend fun getWakeWordDetectionCountByWakeWord(userId: String, wakeWord: String): Int
    
    @Query("""
        SELECT 
            COUNT(*) as total_detections,
            AVG(confidence) as avg_confidence,
            MIN(detected_at) as first_detection,
            MAX(detected_at) as last_detection
        FROM wake_word_detections 
        WHERE user_id = :userId
          AND wake_word = :wakeWord
          AND detected_at >= :startTime
    """)
    suspend fun getWakeWordStats(userId: String, wakeWord: String, startTime: Long): WakeWordStats
    
    data class WakeWordStats(
        val totalDetections: Int,
        val avgConfidence: Float?,
        val firstDetection: Long?,
        val lastDetection: Long?
    )
    
    @Query("DELETE FROM wake_word_detections WHERE detected_at < :timestamp")
    suspend fun cleanupOldDetections(timestamp: Long): Int
    
    @Query("DELETE FROM wake_word_detections WHERE user_id = :userId AND confidence < :minConfidence")
    suspend fun cleanupLowConfidenceDetections(userId: String, minConfidence: Float): Int
}