package com.example.seminar_practical_use

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.util.Base64

class MyNoteConverter {
    @TypeConverter
    fun fromByteArray(value: ByteArray): String {
        return Base64.encodeToString(value, Base64.DEFAULT)
    }

    @TypeConverter
    fun toByteArray(value: String): ByteArray {
        return Base64.decode(value, Base64.DEFAULT)
    }
}

@Entity(tableName = "notes")
data class Note(
    @ColumnInfo(name = "header") var header: String = "",
    @ColumnInfo(name = "content", typeAffinity = ColumnInfo.BLOB) var content: ByteArray,
    @ColumnInfo(name = "isLocked") var isLocked: Boolean = false,
    @ColumnInfo(name = "initVector", typeAffinity = ColumnInfo.BLOB) var initVector: ByteArray = byteArrayOf(),
    @PrimaryKey(autoGenerate = true) var id: Int = 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (isLocked != other.isLocked) return false
        if (id != other.id) return false
        if (header != other.header) return false
        if (!content.contentEquals(other.content)) return false
        if (!initVector.contentEquals(other.initVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLocked.hashCode()
        result = 31 * result + id
        result = 31 * result + header.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + initVector.contentHashCode()
        return result
    }
};

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long // returns the id of the inserted note

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Database(entities = arrayOf(Note::class), version = 1)
//@TypeConverters(MyNoteConverter::class)
abstract class NoteDatabase: RoomDatabase() {
    abstract fun NoteDao(): NoteDao

    companion object {
        val DB_NAME = "notes_v4"
        private var instance: NoteDatabase? = null
        fun getInstance(context: Context): NoteDatabase {
            return instance ?: buildDatabase(context).also { instance = it}
        }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, NoteDatabase::class.java,
                DB_NAME).allowMainThreadQueries().build()
    }
}