package com.example.seminar_practical_use

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteEdit : AppCompatActivity() {
    private lateinit var header: String
    private lateinit var content: ByteArray
    private var isLocked: Boolean = false
    private lateinit var initialVector: ByteArray
    private var note: Note? = null
    private lateinit var biometricHelper: BiometricHelper
    private var edtContent: EditText? = null
    private var edtHeader: EditText? = null
    private var btnLock: ImageButton? = null
    private var itemPosition: Int = -1
    private var cacheUnlocked: Boolean = false
    private var isFirst = true
//    private var tmpContent: String? = null
//    private val myNoteConverter = MyNoteConverter()

    private fun setUpBiometricHelper() {
        biometricHelper = BiometricHelper(this@NoteEdit)
        biometricHelper.setupBiometricAuthentication(
            title = "Unlock your note",
            subtitle = "Please unlock to read the note",
            description = "Confirm your biometric to continue",
            allowDeviceCredential = true,
            onResult = { result ->  // Use named parameters for clarity
                when (result) {
                    is BiometricHelper.AuthResult.Success -> {
                        showNote()
                        cacheUnlocked = true
                    }
                    is BiometricHelper.AuthResult.Error -> {
                        Toast.makeText(this, "Error while unlocking", Toast.LENGTH_SHORT).show()
                    }
                    is BiometricHelper.AuthResult.Failure -> {
                        Toast.makeText(this, "Please use correct password to unlock", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        checkBiometricAvailability()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpBiometricHelper()
        val intent = intent
        val noteid = intent.getIntExtra("id", -1)
        itemPosition = intent.getIntExtra("position", -1)
        cacheUnlocked = intent.getBooleanExtra("cacheUnlocked", false)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val replyIntent = Intent()
                replyIntent.putExtra("position", itemPosition)
                replyIntent.putExtra("id", noteid)
                replyIntent.putExtra("cacheUnlocked", cacheUnlocked)
                setResult(RESULT_OK, replyIntent)
                finish()
            }
        })

        findViewById<ImageButton>(R.id.btnAddLock).setOnClickListener {
            if (note == null) return@setOnClickListener
            isLocked = !isLocked
            val database = NoteDatabase.getInstance(this)
            if (isLocked) {
                val encryptedData = CryptoManager().encrypt(edtContent?.text.toString().toByteArray())
                val newNote = Note(edtHeader?.text.toString(), encryptedData.data, true, encryptedData.iv)
                newNote.id = note?.id as Int

                CoroutineScope(Dispatchers.Main).launch {
                    database.NoteDao().updateNote(newNote)
                }.invokeOnCompletion {
                    displayNoteOnLoad(newNote.id)
                }
            } else {
                val newNote = Note( edtHeader?.text.toString(), edtContent?.text.toString().toByteArray(), false, byteArrayOf() )
                newNote.id = note?.id as Int

                CoroutineScope(Dispatchers.Main).launch {
                    database.NoteDao().updateNote(newNote)
                }.invokeOnCompletion {
                    displayNoteOnLoad(newNote.id)
                }
            }
        }

        edtContent = findViewById(R.id.content)
        edtHeader = findViewById(R.id.header)
        btnLock = findViewById(R.id.btnLock)
        btnLock?.setOnClickListener {
            if (cacheUnlocked) {
                lockNote()
            } else {
                unlockAndShowNote()
            }
            cacheUnlocked = !cacheUnlocked;
        }

        displayNoteOnLoad(noteid)
    }

    private fun displayNoteOnLoad(noteid: Int) {
        if (noteid == -1) {
            // no note was given --> create new note
        } else {
            // load note from database
            CoroutineScope(Dispatchers.Main).launch {
                val database = NoteDatabase.getInstance(this@NoteEdit)
                note = database.NoteDao().getNoteById(noteid)
                database.close()
            }.invokeOnCompletion { cause: Throwable? ->
                if (cause != null || note == null) {
                    finish()
                }

                header = note!!.header
                content = note!!.content
                isLocked = note!!.isLocked
                initialVector = note!!.initVector

                edtHeader?.setText(header)

                if (!isLocked) {
                    btnLock?.visibility = View.GONE
                    btnLock?.isClickable = false
                    showNote()
                } else if (!cacheUnlocked) {
                    lockNote()
                    unlockAndShowNote()
                } else {
                    showNote()
                }
            }
        }
    }

    private fun lockNote() {
        edtHeader?.isEnabled = false
        if (isFirst) isFirst = false
        else {
            val encryptData = CryptoManager().encrypt(edtContent?.text.toString().toByteArray())
            content = encryptData.data
            initialVector = encryptData.iv
        }
        edtContent?.setText("<please unlock to see the content>")
        edtContent?.isEnabled = false
        btnLock?.setBackgroundResource(R.drawable.lock_button)
    }

    private fun showNote() {
        edtHeader?.isEnabled = true
        edtContent?.isEnabled = true
        if (isLocked) {
            val tmp = CryptoManager().decrypt(content, initialVector).decodeToString()
            edtContent?.setText(tmp)
        } else {
            edtContent?.setText(content.decodeToString())
        }

        btnLock?.setBackgroundResource(R.drawable.unlock_button)
    }

    private fun unlockAndShowNote() {
        biometricHelper.authenticate()
    }

    private fun checkBiometricAvailability() {
        val status = biometricHelper.checkBiometricCapability(this)
        val statusMessage = when (status) {  // Use when as an expression for conciseness.
            BiometricHelper.BiometricStatus.AVAILABLE -> ""
            BiometricHelper.BiometricStatus.NO_HARDWARE -> "Biometric authentication is not available on this device"
            BiometricHelper.BiometricStatus.UNAVAILABLE -> "Biometric authentication is currently unavailable"
            BiometricHelper.BiometricStatus.NOT_ENROLLED -> "Please enroll your biometric first"
            BiometricHelper.BiometricStatus.UNKNOWN -> "Biometric authentication status is unknown"
        }

        if (statusMessage.isNotEmpty()) { // Use isNotEmpty for clarity.
            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }
}