package com.example.myapplication

import AdapterforChatBot
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import org.threeten.bp.temporal.ChronoUnit
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class Homepage : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatSessionManager: ChatSessionManager
    private var isChatSessionManagerInitialized = false
    private var messages = mutableListOf<chatbot_data>()
    private lateinit var prefkey : String
    override fun onCreate(savedInstanceState: Bundle?) {
        chatSessionManager = ChatSessionManager.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        AndroidThreeTen.init(application)
        FirebaseApp.initializeApp(this)
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("newsUpdate")
        val nav = findViewById<BottomNavigationView>(R.id.nav_bar)
        val chatbot_btn = findViewById<ImageButton>(R.id.chatbot_btn)
        val hidechatbot = findViewById<ImageView>(R.id.hideChatbot)
        val usermessage = findViewById<EditText>(R.id.user_response)
        val aboutUsButton = findViewById<Button>(R.id.aboutUsButton)
        val updatesButton = findViewById<Button>(R.id.updateButton)
        val sendMessageButton: ImageButton = findViewById(R.id.submit_message)
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val welcomeText = findViewById<TextView>(R.id.Welcometext)
        val firstName = sharedPreferences.getString("firstName", null)!!
        val primaryKey = sharedPreferences.getString("pkIdentifier", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!
        val isloggedIn = sharedPreferences.getString("isloggedIn", null)!!
        if(isloggedIn.equals("FALSE")){
            val editor = sharedPreferences.edit()
            AlertDialog.Builder(this@Homepage)
                .setTitle("Welcome")
                .setMessage(
                    Html.fromHtml(
                        "Greetings from Immunicare your Immunization Platform for your Child and you, " +
                                "Please be advised that we are currently following Department of Health Recommended Vaccination Process, " +
                                "and if your preferred Vaccine is not available for your Child please select Others, " +
                                "For more information please contact Immunicare",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )
                .setPositiveButton("Ok") { _, _ ->
                }
                .show()
            editor.putString("isloggedIn", "TRUE")
            editor.apply()

        }
        deleteExpiredAppointments(primaryKey,child_state)
        limitHistory(primaryKey)
        welcomeText.setText("Welcome" + " " + firstName + "!")
        nav.menu.findItem(R.id.homepage).setChecked(true)
        chatInterface.visibility = View.GONE
        hidechatbot.setOnClickListener {
            isChatSessionManagerInitialized = false
            chatInterface()
        }
        updatesButton.setOnClickListener{
            collectionReference.whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        for(data_value in querySnapshot){
                            val validityDate = data_value.getString("validityDate") as String
                            val content = data_value.getString("content") as String
                            val title = data_value.getString("title") as String
                            val formatter = DateTimeFormatter.ofPattern("M-d-yyyy")
                            val date = LocalDate.parse(validityDate, formatter)
                            val currentDate = LocalDate.now()
                            if (currentDate.isAfter(date)) {
                                AlertDialog.Builder(this@Homepage)
                                    .setTitle("Updates")
                                    .setMessage("It seems that there are no Updates Posted, Stay tuned as we are doing our best to provide you Current News and Updates from our Services")
                                    .setPositiveButton("Ok") { _, _ ->
                                    }
                                    .show()
                            }
                            else{
                                AlertDialog.Builder(this@Homepage)
                                    .setTitle(title)
                                    .setMessage(content)
                                    .setPositiveButton("Ok") { _, _ ->
                                    }
                                    .show()
                            }
                        }
                    }
                    else{
                        AlertDialog.Builder(this@Homepage)
                            .setTitle("Updates")
                            .setMessage("It seems that there are no Updates Posted, Stay tuned as we are doing our best to provide you Current News and Updates from our Services")
                            .setPositiveButton("Ok") { _, _ ->
                            }
                            .show()
                    }
                }
        }
        aboutUsButton.setOnClickListener{
            if(child_state == "TRUE"){
                showVaccineDialog()
            }

            if (child_state == "FALSE"){
                AlertDialog.Builder(this@Homepage)
                    .setTitle("Vaccine Guide")
                    .setMessage("While for the Adults it is mandatory to intake the following Vaccines: COVID Vaccine(First Dose),COVID Vaccine(Second Dose),COVID Vaccine(Booster)\n" +
                            "For 60 years old and above : Influenza Vaccine (Annually), Pneumo-Polysaccharide Vaccine")
                    .setPositiveButton("Ok") { _, _ ->
                    }
                    .show()
            }

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




        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.book_appointment -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, AppointmentPage::class.java)
                    startActivity(intent)
                    true

                }

                R.id.account_info -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, AccountPage::class.java)
                    startActivity(intent)
                    true
                }

                R.id.updates_info -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, Updatespage::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
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



    private fun showVaccineDialog() {
        val dialogView = layoutInflater.inflate(R.layout.vaccinedialog, null)
        val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
        val message = "As per Department of Health the Vaccine Guide for your Child is implemented by the following:\n" + "\n" +
                "At Birth : Bacillus Calmette–Guérin Vaccine, Hepatitis B Vaccine,Tetanus Diptheria (Td) Vaccine\n" + "\n" +
                "4 Weeks after Birth : Tetanus Diptheria(2nd Dose)\n" + "\n" +
                "6 Weeks after Birth : Oral Polio Vaccine(1st Dose), PENTA Vaccine(DPT-HepB+Hib)(1st Dose),Pneumococcal Vaccine(1st Dose), Rotavirus Vaccine(1st Dose)\n" + "\n" +
                "10 Weeks after Birth : Oral Polio Vaccine(2nd Dose), PENTA Vaccine(DPT-HepB+Hib)(2nd Dose),Pneumococcal Vaccine(2nd Dose), Rotavirus Vaccine(2nd Dose)\n" + "\n" +
                "14 Weeks after Birth : OPV(3rd Dose),PENTA Vaccine(DPT-HepB+Hib)(3rd Dose),PCV(3rd Dose),Inactivated Polio Vaccine\n" + "\n" +
                "6 Months after Birth : Tetanus Diptheria(3rd Dose)\n" + "\n" +
                "9 Months after Birth : MMR Vaccine(1st Dose),Japanese Encephalitis\n" + "\n" +
                "12 Months after Birth : MMR Vaccine(2nd Dose)\n" + "\n" +
                "Child Ages from 5-7 : Measles-Rubella Vaccine(1st Dose), Tetanus Diptheria(4th Dose)\n" + "\n" +
                "Child Ages from 9-10 and Female : Human Papilloma Virus(HPV) Vaccine\n" + "\n" +
                "Child Ages from 12-15: Measles-Rubella Vaccine(2nd Dose), Tetanus Diptheria(5th Dose)\n" + "\n" +
                "While for the Adults it is mandatory to intake the following Vaccines: COVID Vaccine(First Dose),COVID Vaccine(Second Dose),COVID Vaccine(Booster)\n" + "\n" +
                "For 60 years old and above : Influenza Vaccine (Annually), Pneumo-Polysaccharide Vaccine\n" + "\n" +
                "The prioritization framework for COVID-19 vaccination was formulated due to the limited global supply of COVID-19 vaccine products. This will depend on the vaccine. For those currently available, Sinovac can be given to clinically healthy individuals 18 to 59 years old, while AstraZeneca can be given to those 18 years old and above, including senior citizens and children 6 months old above., \n" + "\n"

        messageTextView.text = message
        AlertDialog.Builder(this@Homepage)
            .setTitle("Vaccine Guide")
            .setView(dialogView)
            .setPositiveButton("Ok") { _, _ ->
                // Handle positive button click if needed
            }
            .show()
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
    private fun limitHistory(pkIdentifier: String) {
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        collectionReference.whereEqualTo("pkIdentifier", pkIdentifier)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (data in querySnapshot) {
                    val userID = data.id
                    firestore.collection("users/$userID/deletedAppointments")
                        .get()
                        .addOnSuccessListener { appointmentQuerySnapshot ->
                            for (appointment in appointmentQuerySnapshot) {
                                val appointmentDateStr = appointment.getString("date")
                                val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                                val appointmentDate = LocalDate.parse(appointmentDateStr, formatter)
                                val now = LocalDate.now()

                                val monthsElapsed = ChronoUnit.MONTHS.between(appointmentDate, now)
                                if (monthsElapsed >= 1) {
                                    val updates: MutableMap<String, Any> = HashMap()
                                    updates["status"] = "Deleted"
                                    firestore.collection("users/$userID/deletedAppointments")
                                        .document(appointment.id)
                                        .update(updates)
                                }
                            }
                        }
                }
            }
    }
}











