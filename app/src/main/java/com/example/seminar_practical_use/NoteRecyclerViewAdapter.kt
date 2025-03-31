package com.example.seminar_practical_use

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow



//class Note(private val id: Int, private val header: String, private val content: String, private val isLocked: Boolean) {
//    fun get(): Map<String, Any> {
//        if (isLocked)
//            return mapOf("id" to id,"header" to "Запись закрыта", "content" to "<please unlock to see the content>", "isLocked" to true)
//        return mapOf("id" to id, "header" to header, "content" to content, "isLocked" to false)
//    }
//
//    companion object {
//        val sampleData: List<Note> = listOf(
//            Note(1, "Заметка 1", "Содержание заметки 1", false),
//            Note(2, "Hello this is the first note", "Hi, this is the first page of the princess diary", true),
//            Note(3, "Where is Nemo?", "I'm Marlin and I'm seeking for my son, Nemo. I don't know where he is but I can find him in the sea. Can you help me?", false),
//            Note(4, "Does Dory love Marlin?", "Who knows?", true),
//            Note(5, "Biết gì chưa?", "50 năm 30 tháng 4!", true)
//        )
//
//        fun getSampleData(): MutableList<Note> {
//            return sampleData.toMutableList()
//        }
//    }
//}

class NoteRecyclerViewAdapter(private val noteList: MutableList<Note>): RecyclerView.Adapter<NoteRecyclerViewAdapter.ViewHolder>() {
    var onItemClick: ((Note, Int) -> Unit)? = null;
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header = itemView.findViewById<TextView>(R.id.header)
        val previewContent = itemView.findViewById<TextView>(R.id.previewContent)
        val statusIcon = itemView.findViewById<ImageView>(R.id.statusIcon)

        init {
            itemView.setOnClickListener { onItemClick?.invoke(noteList[bindingAdapterPosition], bindingAdapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val noteView = inflater.inflate(R.layout.fragment_note_list_item, parent, false)
        return ViewHolder(noteView)
    }

    override fun getItemCount(): Int {
        return noteList.size
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val noteData = noteList[position]

        holder.header.text = noteData.header
        if (noteData.isLocked) {
            holder.previewContent.text = "<please unlock to see the content>"
            holder.statusIcon.setImageResource(R.drawable.lock_button)
            holder.previewContent.setTextColor(R.color.zinc)
            holder.previewContent.setTypeface(holder.previewContent.typeface, Typeface.ITALIC)
        } else {
            holder.previewContent.text = noteData.content.decodeToString()
            holder.statusIcon.setImageResource(R.drawable.unlock_button)
        }
    }
}