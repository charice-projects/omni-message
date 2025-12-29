// 📁 app/src/main/java/com/omnimsg/app/domain/usecases/emergency/UpdateEmergencyContactsUseCase.kt
package com.omnimsg.app.domain.usecases.emergency

import com.omnimsg.app.data.repository.EmergencyRepository
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateEmergencyContactsUseCase @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) {
    suspend operator fun invoke(
        currentContacts: List<EmergencyContact>,
        newContact: EmergencyContact? = null,
        contactToRemove: EmergencyContact? = null,
        updatedContact: EmergencyContact? = null,
        operation: ContactOperation
    ): Result<List<EmergencyContact>> = withContext(Dispatchers.IO) {
        try {
            val updatedList = when (operation) {
                ContactOperation.ADD -> {
                    if (newContact == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("新联系人不能为空")
                        )
                    }
                    
                    // 检查重复
                    if (currentContacts.any { it.phone == newContact.phone }) {
                        return@withContext Result.failure(
                            IllegalStateException("电话号码已存在")
                        )
                    }
                    
                    // 添加新联系人
                    currentContacts + listOf(newContact)
                }
                
                ContactOperation.REMOVE -> {
                    if (contactToRemove == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("要删除的联系人不能为空")
                        )
                    }
                    
                    // 移除联系人
                    currentContacts.filter { it.id != contactToRemove.id }
                }
                
                ContactOperation.UPDATE -> {
                    if (updatedContact == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("更新的联系人不能为空")
                        )
                    }
                    
                    // 更新联系人
                    currentContacts.map { contact ->
                        if (contact.id == updatedContact.id) updatedContact else contact
                    }
                }
            }
            
            // 更新优先级（确保不重复）
            val finalList = reassignPriorities(updatedList)
            
            // 保存到数据库
            emergencyRepository.saveEmergencyContacts(finalList)
            
            Result.success(finalList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun reassignPriorities(contacts: List<EmergencyContact>): List<EmergencyContact> {
        // 按当前优先级排序
        val sorted = contacts.sortedBy { it.priority }
        
        // 重新分配优先级（1, 2, 3...）
        return sorted.mapIndexed { index, contact ->
            contact.copy(priority = index + 1)
        }
    }
}
