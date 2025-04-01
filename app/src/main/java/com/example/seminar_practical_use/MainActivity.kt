package com.example.seminar_practical_use

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Use constants for request codes, and use a more descriptive name
private const val NOTE_EDIT_REQUEST_CODE = 1111

class MainActivity : AppCompatActivity() {
    private lateinit var data: MutableList<Note>
    private lateinit var adapter: NoteRecyclerViewAdapter
    private lateinit var recyclerView: RecyclerView
    private var cacheUnlocked = false;
    private val cryptoManager = CryptoManager()

    private val biometricHelper = BiometricHelper(this)
    
    private fun genSampleData(): List<Note> {
        val contents = listOf(
            "Содержание заметки 1",
            "Hi, this is the first page of the princess diary",
            "I'm Marlin and I'm seeking for my son, Nemo. I don't know where he is but I can find him in the sea. Can you help me?",
            "Who knows?",
            "Khong co gi de xem het! :D"
        )
        val tempData: List<Note> = listOf(
            Note("Заметка 1", byteArrayOf(), false),
            Note("Hello this is the first note", byteArrayOf(), true),
            Note("Where is Nemo?", byteArrayOf(), false),
            Note("Does Jack love Rose?", byteArrayOf(),true),
            Note("Biết gì chưa?", byteArrayOf(), true)
        )
        val res: MutableList<Note> = mutableListOf()
        contents.forEachIndexed { index, s ->
            res.add(Note(
                header = tempData[index].header,
                content = s.toByteArray(),
                isLocked = tempData[index].isLocked,
                initVector = tempData[index].initVector
            ))
//            Log.d("TTT check", "${s == s.encodeToByteArray().decodeToString()}")
        }

        return res
    }
//    private fun checkBiometricAvailability() {
//        val status = biometricHelper.checkBiometricCapability(this)
//        val statusMessage = when (status) {  // Use when as an expression for conciseness.
//            BiometricHelper.BiometricStatus.AVAILABLE -> ""
//            BiometricHelper.BiometricStatus.NO_HARDWARE -> "Biometric authentication is not available on this device"
//            BiometricHelper.BiometricStatus.UNAVAILABLE -> "Biometric authentication is currently unavailable"
//            BiometricHelper.BiometricStatus.NOT_ENROLLED -> "Please enroll your biometric first"
//            BiometricHelper.BiometricStatus.UNKNOWN -> "Biometric authentication status is unknown"
//        }
//
//        if (statusMessage.isNotEmpty()) { // Use isNotEmpty for clarity.
//            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setUpBiometricHelper() {
//        biometricHelper.setupBiometricAuthentication(
//            title = "Unlock your note",
//            subtitle = "Please unlock to read the note",
//            description = "Confirm your biometric to continue",
//            allowDeviceCredential = true,
//            onResult = { result ->  // Use named parameters for clarity
//                when (result) {
//                    is BiometricHelper.AuthResult.Success -> {
//
//                    }
//                    is BiometricHelper.AuthResult.Error -> {
//                        Toast.makeText(this, "Error while unlocking", Toast.LENGTH_SHORT).show()
//                    }
//                    is BiometricHelper.AuthResult.Failure -> {
//                        Toast.makeText(this, "Please use correct password to unlock", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        )
//        checkBiometricAvailability()
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.noteList)
        data = mutableListOf()
        cryptoManager.delKey()
        CoroutineScope(Dispatchers.Main).launch {
            val database = NoteDatabase.getInstance(this@MainActivity)
            val sampleData = genSampleData()
            database.NoteDao().deleteAllNotes()
            sampleData.forEach {
                if (it.isLocked) {
                    val encrData = cryptoManager.encrypt(it.content)
                    Log.d("TTT check", "${it.id} && ${encrData.data.decodeToString()}")
                    it.content = encrData.data
                    it.initVector = encrData.iv
                }
                database.NoteDao().insertNote(it)
            }

            data.addAll(database.NoteDao().getAllNotes())
            database.close()
            setupRecyclerView()
        }
//        setUpBiometricHelper()
//        biometricHelper.authenticate()
    }

    private fun setupRecyclerView() {
        val itemDivider = DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDivider)

        adapter = NoteRecyclerViewAdapter(data)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter.onItemClick = { note, pos ->
            val intent = Intent(this@MainActivity, NoteEdit::class.java)
            intent.putExtra("id", note.id)
            intent.putExtra("position", pos)
            intent.putExtra("cacheUnlocked", cacheUnlocked)
            startActivityForResult(intent, NOTE_EDIT_REQUEST_CODE) // Use constant
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (requestCode == NOTE_EDIT_REQUEST_CODE) { // Use constant
            if (resultCode == RESULT_OK) {
                val noteId = data?.getIntExtra("id", -1) ?: -1 // Provide default value directly
                val pos = data?.getIntExtra("position", -1) ?: -1 // Provide default value directly
                cacheUnlocked = data?.getBooleanExtra("cacheUnlocked", false) ?: false
                if (noteId != -1 && pos != -1) { // Simplify condition
                    CoroutineScope(Dispatchers.Main).launch {
                        val database = NoteDatabase.getInstance(this@MainActivity)
                        val note = database.NoteDao().getNoteById(noteId)
                        if (note != null) {
                            this@MainActivity.data[pos] = note
                            this@MainActivity.adapter.notifyItemChanged(pos)
                        }
                    }
                }
            }
        }
    }
}