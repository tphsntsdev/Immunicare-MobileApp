package com.example.myapplication

import AdapterforChatBot
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class Updatespage : AppCompatActivity() {
    private lateinit var userArrayList: ArrayList<fetchdatafromFirebase>
    private lateinit var deletedAppointments: ArrayList<fetchdeletedappointments>
    private lateinit var myAdapter : AdapterforRecyclerView
    private lateinit var historyAdapter : AdapterforDeletedAppointments
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatSessionManager: ChatSessionManager
    private var isChatSessionManagerInitialized = false
    private var messages = mutableListOf<chatbot_data>()
    lateinit var prefkey: String
    override fun onCreate(savedInstanceState: Bundle?) {
        chatSessionManager = ChatSessionManager.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.updatepage)
        FirebaseApp.initializeApp(this)
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val nav = findViewById<BottomNavigationView>(R.id.nav_bar)
        val appointmentsView = findViewById<RecyclerView>(R.id.appointmentsView)
        val historyView = findViewById<RecyclerView>(R.id.historyView)
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        val chatbot_btn = findViewById<ImageButton>(R.id.chatbot_btn)
        val hidechatbot = findViewById<ImageView>(R.id.hideChatbot)
        val usermessage = findViewById<EditText>(R.id.user_response)
        val sendMessageButton: ImageButton = findViewById(R.id.submit_message)
        val primaryKey = sharedPreferences.getString("pkIdentifier", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!
        deleteExpiredAppointments(primaryKey,child_state)
        userArrayList = arrayListOf()
        deletedAppointments = arrayListOf()
        historyView.layoutManager = LinearLayoutManager(this)
        historyView.setHasFixedSize(true)
        historyAdapter = AdapterforDeletedAppointments(this, deletedAppointments)
        historyView.adapter = historyAdapter
        appointmentsView.layoutManager = LinearLayoutManager(this)
        appointmentsView.setHasFixedSize(true)
        myAdapter = AdapterforRecyclerView(this, userArrayList)
        appointmentsView.adapter = myAdapter
        EventChangeListener()
        EventChangeListener_forDeletedAppointments()
        nav.menu.findItem(R.id.updates_info).setChecked(true)
        chatInterface.visibility = View.GONE
        hidechatbot.setOnClickListener {
            isChatSessionManagerInitialized = false
            chatInterface()
        }
        chatbot_btn.setOnClickListener {
            isChatSessionManagerInitialized = true
            chatInterface()
            chatSessionManager = ChatSessionManager.getInstance(this)
            messages = chatSessionManager.getMessages().toMutableList()
            val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val adapter = AdapterforChatBot(this,messages,lifecycleScope)
            recyclerView.adapter = adapter
            scrollRecyclerViewToBottom(adapter)
            sendMessageButton.setOnClickListener {
                val userMessageText = usermessage.text.toString()
                if (userMessageText.isNotEmpty()) {
                    val userMessage = chatbot_data(userMessageText, true)
                    messages.add(userMessage)
                    chatSessionManager.saveMessages(messages)
                    val position = messages.size + 2
                    recyclerView.adapter?.notifyItemInserted(position)
                    recyclerView.smoothScrollToPosition(position)
                    usermessage.text.clear()
                }
            }
        }

        nav.setOnItemSelectedListener {item ->
            when (item.itemId){
                R.id.homepage -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this,Homepage::class.java)
                    startActivity(intent)
                    true

                }
                R.id.account_info -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this,AccountPage::class.java)
                    startActivity(intent)
                    true
                }
                R.id.book_appointment -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this,AppointmentPage::class.java)
                    startActivity(intent)
                    true

                }
                else -> false
            }

        }
    }
    private fun EventChangeListener() {
        val db = FirebaseFirestore.getInstance()
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null) ?: return
        db.collection("users")
            .whereEqualTo("pkIdentifier", pkidentifier)
            .addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    for (document in querySnapshot) {
                        val userID = document.id
                        val appointmentList = document.get("appointmentList") as? Map<String, Any> ?: emptyMap()
                        val appointmentKeys = appointmentList.values.toList() as List<String>
                        val appointmentListeners = mutableMapOf<String, ListenerRegistration>()
                        appointmentKeys.forEach { appointmentKey ->
                            val listener = db.collection("users/$userID/appointments")
                                .whereEqualTo("appointmentKey", appointmentKey)
                                .addSnapshotListener { appointmentSnapshot, e ->
                                    if (e != null) {
                                        return@addSnapshotListener
                                    }

                                    val appointments = mutableListOf<fetchdatafromFirebase>()
                                    appointmentSnapshot?.documents?.forEach { doc ->
                                        val appointment = doc.toObject(fetchdatafromFirebase::class.java)
                                        if (appointment != null) {
                                            appointments.add(appointment)
                                        } else {
                                        }
                                    }
                                    myAdapter.updateData(appointments)
                                }
                            appointmentListeners[appointmentKey] = listener
                        }
                    }
                }
            }
    }



    private fun EventChangeListener_forDeletedAppointments() {
        val db = FirebaseFirestore.getInstance()
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        db.collection("users").whereEqualTo("pkIdentifier", pkidentifier).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val userID = document.id
                    db.collection("users/$userID/deletedAppointments")
                        .whereEqualTo("pkIdentifier", pkidentifier)
                        .whereIn("status", listOf("Cancelled", "Expired"))
                        .addSnapshotListener(
                            object : EventListener<QuerySnapshot> {
                                override fun onEvent(
                                    value: QuerySnapshot?,
                                    error: FirebaseFirestoreException?
                                ) {
                                    if (error != null) {
                                        Log.e("Firestore Error", error.message.toString())
                                        return
                                    }
                                    for (dc: DocumentChange in value?.documentChanges!!) {
                                        if (dc.type == DocumentChange.Type.ADDED) {
                                            deletedAppointments.add(
                                                dc.document.toObject(
                                                    fetchdeletedappointments::class.java
                                                )
                                            )

                                        }
                                    }
                                    historyAdapter.notifyDataSetChanged()


                                }
                            }
                        )
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        messages.clear()
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        sharedPreferences.edit().clear().apply()
        chatSessionManager.clearChatSession()

    }
    private fun chatInterface(){
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        if (isChatSessionManagerInitialized == false){
            chatInterface.visibility = View.GONE
        }
        else{
            chatInterface.visibility = View.VISIBLE
        }

    }
    private fun scrollRecyclerViewToBottom(adapter: AdapterforChatBot) {
        val chatbotRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        chatbotRecyclerView.post {
            chatbotRecyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }
    private fun deleteExpiredAppointments(primaryKey : String, child_state : String){
        val firestore = FirebaseFirestore.getInstance()
        val email = sharedPreferences.getString("email", null)!!
        val collectionReference = firestore.collection("users")
        collectionReference.whereEqualTo("pkIdentifier", primaryKey)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for(data in querySnapshot){
                    val userID = data.id
                    firestore.collection("users/$userID/appointments")
                        .get()
                        .addOnSuccessListener { appointmentSnapshot ->
                            if (!appointmentSnapshot.isEmpty) {
                                for (appointmentData in appointmentSnapshot) {
                                    val appointmentDate = appointmentData.getString("date") ?: ""
                                    val appointmentTime = appointmentData.getString("time") ?: ""
                                    val adultvaccine = appointmentData.getString("adultvaccine") ?: ""
                                    val appointmentKey = appointmentData.getString("appointmentKey") ?: ""
                                    val location = appointmentData.getString("location") ?: ""
                                    val appointmentDocumentID = appointmentData.id

                                    if (appointmentDate.isNotEmpty() && appointmentTime.isNotEmpty()) {
                                        try {
                                            val dateTime = "$appointmentDate $appointmentTime"
                                            val formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a")
                                            val finaldateTime: LocalDateTime = LocalDateTime.parse(dateTime, formatter)
                                            val now: LocalDateTime = LocalDateTime.now()
                                            if (finaldateTime.isBefore(now)) {
                                                val myHashMap = hashMapOf(
                                                    "adultvaccine" to adultvaccine,
                                                    "appointmentKey" to appointmentKey,
                                                    "date" to appointmentDate,
                                                    "location" to location,
                                                    "pkIdentifier" to primaryKey,
                                                    "time" to appointmentTime,
                                                    "status" to "Expired"
                                                )

                                                if (child_state == "TRUE") {
                                                    val childvaccine = appointmentData.getString("childvaccine") ?: ""
                                                    myHashMap["childvaccine"] = childvaccine
                                                }

                                                firestore.collection("users/$userID/deletedAppointments").add(myHashMap)
                                                    .addOnSuccessListener {
                                                        firestore.collection("users/$userID/appointments")
                                                            .document(appointmentDocumentID).delete().addOnSuccessListener {
                                                                firestore.collection("users").whereEqualTo("pkIdentifier", primaryKey).get().addOnSuccessListener {querySnapshot ->
                                                                    for (x in querySnapshot){
                                                                        val userID = x.id
                                                                        var vaccination_count = x.getString("vaccination_count")!!.toInt()
                                                                        val appointmentList =
                                                                            x?.get("appointmentList") as? MutableMap<String, Any>
                                                                                ?: mutableMapOf()
                                                                        vaccination_count--
                                                                        val updates = hashMapOf(
                                                                            "vaccination_count" to vaccination_count.toString()
                                                                        )
                                                                        val keytoRemove =  findKeyByValue(appointmentList, appointmentKey)
                                                                        appointmentList.remove(keytoRemove);
                                                                        val modifiedMap =
                                                                            keytoRemove?.let { key ->
                                                                                subtractOneFromKeys(
                                                                                    appointmentList,
                                                                                    key
                                                                                )
                                                                            }
                                                                        firestore.collection("users")
                                                                            .document(userID)
                                                                            .update( "appointmentList", modifiedMap)
                                                                            .addOnSuccessListener {
                                                                                firestore.collection("users")
                                                                                    .document(userID)
                                                                                    .update(updates as Map<String, Any>)
                                                                                    .addOnSuccessListener {
                                                                                        val stringSample = "Your Appointment that was Scheduled on $appointmentDate , $appointmentTime at $location has Elapsed"
                                                                                        sendEmail(email,stringSample)
                                                                                    }
                                                                            }

                                                                    }
                                                                }
                                                            }
                                                    }


                                            }

                                        } catch (e: DateTimeParseException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }

                }
            }


    }
    private fun sendEmail(receiverEmail: String, txtContent: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val smtp_host = resources.getString(R.string.smtp_host)
                val smtp_starttls_state = resources.getString(R.string.smtp_starttls_state)
                val smtp_auth = resources.getString(R.string.smtp_auth)
                val smtp_port = resources.getString(R.string.smtp_port)
                val no_reply_email = resources.getString(R.string.no_reply_email)
                val no_reply_password = resources.getString(R.string.no_reply_password)
                val properties = Properties()
                properties["mail.smtp.host"] = smtp_host
                properties["mail.smtp.starttls.enable"] = smtp_starttls_state
                properties["mail.smtp.auth"] = smtp_auth
                properties["mail.smtp.port"] = smtp_port
                val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(no_reply_email, no_reply_password)
                    }
                })
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(no_reply_email))
                message.addRecipient(Message.RecipientType.TO, InternetAddress(receiverEmail))
                message.subject = "Appointment Confirmation"
                message.setText(txtContent)
                Transport.send(message)
                withContext(Dispatchers.Main) {

                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }
    }
    private fun findKeyByValue(map: Map<String, Any>, targetValue: String): String? {
        for ((key, value) in map) {
            if (targetValue == value.toString()) {
                return key
            }
        }
        return null
    }

    private fun subtractOneFromKeys(map: Map<String, Any>, keyToRemove: String): Map<String, Any> {
        val modifiedMap = mutableMapOf<String, Any>()
        val keyToRemoveInt = keyToRemove.toInt()

        for ((key, value) in map) {
            val originalKey = key.toInt()
            val newKey = if (originalKey > keyToRemoveInt) originalKey - 1 else originalKey
            modifiedMap[newKey.toString()] = value
        }

        return modifiedMap
    }
    }
