package com.omnimsg.data.local.database.entities

import androidx.room.*
import com.omnimsg.domain.models.ContactSource
import com.omnimsg.domain.models.RelationshipType
import com.omnimsg.domain.models.SyncStatus
import java.util.*

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["phone_number"], unique = true),
        Index(value = ["email"], unique = true),
        Index(value = ["display_name"]),
        Index(value = ["company"]),
        Index(value = ["is_favorite"]),
        Index(value = ["is_blocked"]),
        Index(value = ["group_id"]),
        Index(value = ["sync_status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "contact_id")
    val contactId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String?,
    
    @ColumnInfo(name = "secondary_phone")
    val secondaryPhone: String?,
    
    @ColumnInfo(name = "email")
    val email: String?,
    
    @ColumnInfo(name = "company")
    val company: String?,
    
    @ColumnInfo(name = "position")
    val position: String?,
    
    @ColumnInfo(name = "address")
    val address: String?,
    
    @ColumnInfo(name = "birthday")
    val birthday: Long?, // Unix timestamp
    
    @ColumnInfo(name = "is_lunar_birthday")
    val isLunarBirthday: Boolean = false,
    
    @ColumnInfo(name = "notes")
    val notes: String?,
    
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String?,
    
    @ColumnInfo(name = "relationship")
    val relationship: RelationshipType = RelationshipType.OTHER,
    
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),
    
    @ColumnInfo(name = "group_id")
    val groupId: Long?,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean = false,
    
    @ColumnInfo(name = "last_contacted")
    val lastContacted: Long?, // Unix timestamp
    
    @ColumnInfo(name = "interaction_count")
    val interactionCount: Int = 0,
    
    @ColumnInfo(name = "interaction_score")
    val interactionScore: Float = 0f,
    
    @ColumnInfo(name = "custom_fields")
    val customFields: Map<String, String> = emptyMap(),
    
    @ColumnInfo(name = "source")
    val source: ContactSource = ContactSource.MANUAL,
    
    @ColumnInfo(name = "import_batch_id")
    val importBatchId: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "sync_token")
    val syncToken: String?,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
) {
    companion object {
        fun generateContactId(): String {
            return "contact_${UUID.randomUUID()}"
        }
    }
}