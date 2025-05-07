package com.codewithfk.chatooz_android

import androidx.core.view.WindowCompat
import android.content.Intent // Import Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu // Import Menu (if you still have the menu)
import android.view.MenuItem // Import MenuItem (if you still have the menu)
import android.widget.Button // Import Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Import Toolbar (if you still use Toolbar)
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton // Import FloatingActionButton
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.auth.ktx.auth // Import Firebase Auth KTX
import com.google.firebase.database.FirebaseDatabase // Import FirebaseDatabase for push()
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.services.ZIMKit
import com.zegocloud.zimkit.services.callback.CreateGroupCallback
import com.zegocloud.zimkit.services.model.ZIMKitGroupInfo
import im.zego.zim.entity.ZIMError
import im.zego.zim.entity.ZIMErrorUserInfo
import im.zego.zim.enums.ZIMErrorCode
import java.util.ArrayList
import java.util.UUID // Still keep UUID import just in case, though we won't use it for generated ID
// Import ZIMInviteUsersToGroupCallback (if you still have the invite logic)
// import im.zego.zim.callback.ZIMInviteUsersToGroupCallback


class ConversationActivity : AppCompatActivity() {
    // REMOVED: lateinit var newChat: Button // New Chat is now a FAB
    lateinit var groupChat: Button
    lateinit var profileButton: Button // Button to navigate to ProfileActivity

    // ADDED: FloatingActionButton for New Chat
    lateinit var newChatFab: FloatingActionButton


    private val database: FirebaseDatabase by lazy { Firebase.database }
    private val usersRef: DatabaseReference by lazy { database.reference.child("users") }
    private lateinit var auth: FirebaseAuth // Firebase Auth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_conversation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // REMOVED: newChat initialization
        // newChat = findViewById(R.id.new_chat)
        groupChat = findViewById(R.id.group_chat)
        profileButton = findViewById(R.id.profileButton) // Initialize the Profile Button

        // ADDED: Initialize the New Chat FAB
        newChatFab = findViewById(R.id.newChatFab)


        // Initialize Firebase Auth
        auth = Firebase.auth // Initialize Firebase Auth

        setupClickListeners()

    }

    // Removed Toolbar menu methods as we reverted from using a Toolbar
    // override fun onCreateOptionsMenu(menu: Menu?): Boolean { ... }
    // override fun onOptionsItemSelected(item: MenuItem): Boolean { ... }


    private fun setupClickListeners() {
        // REMOVED: The click listener for the old newChat button
        // newChat.setOnClickListener { ... }

        // ADDED: Click listener for the New Chat FAB
        newChatFab.setOnClickListener {
            Log.d("Conversation", "New Chat FAB clicked.")
            val recipientEmailEditText = EditText(this)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Recipient Email Address")
            recipientEmailEditText.hint = "Email"
            recipientEmailEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            builder.setView(recipientEmailEditText)
            builder.setPositiveButton("OK") { dialog, which ->
                val email = recipientEmailEditText.text.toString().trim()
                if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Log.d("Conversation", "Looking up user with email: $email")
                    findUserByEmailAndConnect(email)
                } else {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    Log.w("Conversation", "Invalid email entered for new chat.")
                }
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
                Log.d("Conversation", "New chat dialog cancelled.")
            }
            builder.create().show()
        }


        groupChat.setOnClickListener {
            Log.d("Conversation", "Create Group button clicked.")

            // Modified AlertDialog to ask for Group Name and User Emails
            val groupNameEditText = EditText(this)
            val userEmailsEditText = EditText(this) // EditText for user emails

            val builder = AlertDialog.Builder(this)
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.VERTICAL

            groupNameEditText.hint = "Group Name"
            userEmailsEditText.hint = "User Emails (comma-separated)" // Hint for emails

            linearLayout.addView(groupNameEditText)
            linearLayout.addView(userEmailsEditText) // Add emails EditText

            builder.setTitle("Create Group Chat")
            builder.setView(linearLayout)

            builder.setPositiveButton("OK") { dialog, which ->
                val groupName = groupNameEditText.text.toString().trim()
                val userEmailsString = userEmailsEditText.text.toString().trim()

                if (groupName.isNotEmpty() && userEmailsString.isNotEmpty()) {
                    val userEmails = userEmailsString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() } // Validate emails

                    if (userEmails.isNotEmpty()) {
                        Log.d("Conversation", "Attempting to create group chat. Name: $groupName, Emails: $userEmails")
                        createGroupByEmails(groupName, userEmails) // Call the new function
                    } else {
                        Toast.makeText(this, "Please enter valid email addresses", Toast.LENGTH_SHORT).show()
                        Log.w("Conversation", "No valid emails entered for group chat.")
                    }
                } else {
                    Toast.makeText(this, "Please fill in Group Name and User Emails", Toast.LENGTH_SHORT).show()
                    Log.w("Conversation", "Missing fields for group chat creation.")
                }
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
                Log.d("Conversation", "Create group dialog cancelled.")
            }
            builder.create().show()
        }

        // Click listener for the Profile button
        profileButton.setOnClickListener {
            Log.d("Conversation", "Profile button clicked. Navigating to ProfileActivity.")
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }


    private fun findUserByEmailAndConnect(email: String) {
        Log.d("Conversation", "Starting database query for email: $email")

        usersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Conversation", "Database query onDataChange triggered. Snapshot exists: ${snapshot.exists()}. Children count: ${snapshot.childrenCount}")

                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java) // Assuming User data class is accessible
                            if (user != null) {
                                Log.d("Conversation", "Found user with email $email, UID: ${user.uid}")
                                connectToUser(user.uid, ZIMKitConversationType.ZIMKitConversationTypePeer)
                                return
                            } else {
                                Log.e("Conversation", "User data null for snapshot: ${userSnapshot.key}")
                            }
                        }
                        Log.e("Conversation", "Snapshot exists for email $email but no valid user data found among children.")
                        Toast.makeText(this@ConversationActivity, "Error finding user data", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.w("Conversation", "No user found with email: $email (snapshot does not exist)")
                        Toast.makeText(this@ConversationActivity, "No user found with that email address", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Conversation", "Database query onCancelled triggered. Error: ${error.message}", error.toException())
                    Toast.makeText(this@ConversationActivity, "Error searching for user: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        Log.d("Conversation", "Database query listener attached.")
    }


    fun connectToUser(userId: String, type: ZIMKitConversationType) {
        Log.d("Conversation", "Connecting to peer message activity with ZIM User ID: $userId")
        // Use ZIMKitRouter to go to the message activity with the found user ID
        ZIMKitRouter.toMessageActivity(this, userId, type)
    }

    // Function to create group chat by looking up users via emails
    private fun createGroupByEmails(groupName: String, emails: List<String>) {
        Log.d("GroupChat", "Starting user lookups for group chat: $groupName with emails: $emails")

        val foundUids = mutableListOf<String>()
        val emailsToProcess = emails.toMutableList() // Create a mutable copy
        val totalEmails = emails.size
        var completedLookups = 0

        if (emails.isEmpty()) {
            Toast.makeText(this, "Please provide user emails for the group", Toast.LENGTH_SHORT).show()
            Log.w("GroupChat", "Attempted to create group with empty email list.")
            return
        }

        // Show a loading indicator (optional but recommended for async operations)
        // You would need a ProgressBar or similar and functions to show/hide it

        emails.forEach { email ->
            usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                val user = userSnapshot.getValue(User::class.java)
                                user?.uid?.let { foundUids.add(it) } // Add UID if found
                            }
                            Log.d("GroupChat", "Lookup successful for email: $email, found UID(s): ${snapshot.children.mapNotNull { it.getValue(User::class.java)?.uid }}")
                        } else {
                            Log.w("GroupChat", "No user found in DB for email: $email")
                            // Optionally inform the user that some emails were not found
                            runOnUiThread {
                                Toast.makeText(this@ConversationActivity, "User with email $email not found", Toast.LENGTH_SHORT).show()
                            }
                        }

                        completedLookups++
                        // Check if all lookups are completed
                        if (completedLookups == totalEmails) {
                            Log.d("GroupChat", "All email lookups completed. Found UIDs: $foundUids")
                            // Hide loading indicator here

                            if (foundUids.isNotEmpty()) {
                                // Generate a unique group ID using Firebase Push ID (alphanumeric and safe)
                                val generatedGroupId = FirebaseDatabase.getInstance().reference.push().key ?: UUID.randomUUID().toString() // Fallback to UUID just in case
                                Log.d("GroupChat", "Generated Group ID: $generatedGroupId")

                                // Create the group using the found UIDs and generated ID
                                createGroup(groupName, generatedGroupId, foundUids)

                            } else {
                                Log.w("GroupChat", "No valid user IDs found after email lookups. Group creation cancelled.")
                                runOnUiThread {
                                    Toast.makeText(this@ConversationActivity, "No users found with provided emails. Group not created.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("GroupChat", "Database query failed for email: $email. Error: ${error.message}", error.toException())
                        runOnUiThread {
                            Toast.makeText(this@ConversationActivity, "Error looking up user for email $email", Toast.LENGTH_SHORT).show()
                        }

                        completedLookups++
                        // Even if a lookup fails, we still need to know all attempts are done
                        if (completedLookups == totalEmails) {
                            Log.d("GroupChat", "All email lookups completed (with errors). Found UIDs: $foundUids")
                            // Hide loading indicator here

                            if (foundUids.isNotEmpty()) {
                                // Generate a unique group ID using Firebase Push ID (alphanumeric and safe)
                                val generatedGroupId = FirebaseDatabase.getInstance().reference.push().key ?: UUID.randomUUID().toString() // Fallback to UUID just in case
                                Log.d("GroupChat", "Generated Group ID (with errors): $generatedGroupId")
                                // Create the group using the found UIDs and generated ID
                                createGroup(groupName, generatedGroupId, foundUids)
                            } else {
                                Log.w("GroupChat", "No valid user IDs found after email lookups (with errors). Group creation cancelled.")
                                runOnUiThread {
                                    Toast.makeText(this@ConversationActivity, "No users found with provided emails. Group not created.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                })
        }
    }


    private fun createGroup(
        name: String,
        id: String,
        users: List<String>,
        // Removed type parameter as it's always Group
    ) {
        Log.d("Conversation", "Creating group chat with ID: $id, Name: $name, Users: $users")
        // Ensure the current user's ID is also in the list of users if they are creating the group
        val currentUserId = auth.currentUser?.uid
        val participantIds = if (currentUserId != null && !users.contains(currentUserId)) {
            users.toMutableList().apply { add(currentUserId) }
                .toList() // Add current user if not included
        } else {
            users
        }
        if (participantIds.isEmpty()) {
            Log.w(
                "Conversation",
                "Group creation cancelled: No participants including current user."
            )
            runOnUiThread {
                Toast.makeText(
                    this@ConversationActivity,
                    "Group needs at least one participant.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        ZIMKit.createGroup(name, id, participantIds, object : CreateGroupCallback {
            override fun onCreateGroup(
                groupInfo: ZIMKitGroupInfo?,
                inviteUserErrors: ArrayList<ZIMErrorUserInfo>?,
                error: ZIMError?
            ) {
                if (error?.code == ZIMErrorCode.SUCCESS) {
                    // Using the corrected 'id' property
                    Log.d(
                        "Conversation",
                        "Group chat created successfully. Group ID: ${groupInfo?.id}"
                    )
                    runOnUiThread {
                        Toast.makeText(
                            this@ConversationActivity,
                            "Group created!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Navigate to the newly created group chat
                    groupInfo?.let {
                        // Using the corrected 'id' property
                        ZIMKitRouter.toMessageActivity(
                            this@ConversationActivity,
                            it.id,
                            ZIMKitConversationType.ZIMKitConversationTypeGroup
                        )
                    }

                } else {
                    val msg = error?.message ?: "Unknown error"
                    Log.e(
                        "Conversation",
                        "Failed to create group chat: Code ${error?.code}, Message: $msg"
                    )
                    runOnUiThread {
                        AlertDialog.Builder(this@ConversationActivity)
                            .setTitle("Error Creating Group")
                            .setMessage(msg)
                            .setPositiveButton("OK") { dialog, which ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }
        })
    }
}