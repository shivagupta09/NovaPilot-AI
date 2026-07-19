package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_projects")
data class SavedProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val tool: String,
    val content: String,
    val date: String
)

@Entity(tableName = "developer_keys")
data class DeveloperKey(
    @PrimaryKey val id: String,
    val name: String,
    val prefix: String,
    val created: String
)

@Entity(tableName = "invoice_logs")
data class InvoiceLog(
    @PrimaryKey val invoiceId: String,
    val date: String,
    val amount: String,
    val status: String
)

@Entity(tableName = "provider_configs")
data class ProviderConfig(
    @PrimaryKey val name: String,
    val active: Boolean,
    val usageCount: Int,
    val cost: String
)

@Dao
interface NovaPilotDao {
    @Query("SELECT * FROM saved_projects ORDER BY id DESC")
    fun getAllSavedProjects(): Flow<List<SavedProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SavedProject)

    @Delete
    suspend fun deleteProject(project: SavedProject)

    @Query("SELECT * FROM developer_keys ORDER BY id DESC")
    fun getAllDeveloperKeys(): Flow<List<DeveloperKey>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeveloperKey(key: DeveloperKey)

    @Query("DELETE FROM developer_keys WHERE id = :id")
    suspend fun deleteDeveloperKey(id: String)

    @Query("SELECT * FROM invoice_logs ORDER BY invoiceId DESC")
    fun getAllInvoiceLogs(): Flow<List<InvoiceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceLog(log: InvoiceLog)

    @Query("SELECT * FROM provider_configs")
    fun getAllProviderConfigs(): Flow<List<ProviderConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderConfig(config: ProviderConfig)
}

@Database(
    entities = [SavedProject::class, DeveloperKey::class, InvoiceLog::class, ProviderConfig::class],
    version = 1,
    exportSchema = false
)
abstract class NovaPilotDatabase : RoomDatabase() {
    abstract fun dao(): NovaPilotDao
}
