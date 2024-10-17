package com.example.myapplication

import AdapterforChatBot
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import org.threeten.bp.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random


class AppointmentPage : AppCompatActivity() {


    private lateinit var sharedPreferences: SharedPreferences
    private var datetimebtn: TextView? = null
    private var locationbtn: TextView? = null
    private var vaccinebtn: TextView? = null
    private var user_selectedTime: String? = null
    private var user_selectedDate: String? = null
    private lateinit var timePickerButton: Button
    private lateinit var chatSessionManager: ChatSessionManager
    private var isChatSessionManagerInitialized = false
    private var messages = mutableListOf<chatbot_data>()
    private lateinit var childvaccineinput: MaterialAutoCompleteTextView
    private lateinit var adultvaccineinput: MaterialAutoCompleteTextView

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        chatSessionManager = ChatSessionManager.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_page)
        FirebaseApp.initializeApp(this)
        AndroidThreeTen.init(application)
        var child_dob: String? = null
        var child_age: String? = null
        var child_gender: String? = null
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        val chatbot_btn = findViewById<ImageButton>(R.id.chatbot_btn)
        val hidechatbot = findViewById<ImageView>(R.id.hideChatbot)
        val usermessage = findViewById<EditText>(R.id.user_response)
        val sendMessageButton: ImageButton = findViewById(R.id.submit_message)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val hiddendate = findViewById<TextView>(R.id.hiddendate)
        timePickerButton = findViewById(R.id.timePickerButton)
        val imageGMaps = findViewById<WebView>(R.id.mapsDisplay)
        adultvaccineinput = findViewById<MaterialAutoCompleteTextView>(R.id.vaccineEditText)
        val prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val child_state = sharedPreferences.getString("child_state", null)!!
        val adultAge = sharedPreferences.getString("adultAge", null)!!
        var childvaccine_selection = arrayOf<String>()
        var adultvaccine_selection = arrayOf<String>()
        val primaryKey = sharedPreferences.getString("pkIdentifier", null)!!
        deleteExpiredAppointments(primaryKey,child_state)
        if (child_state.equals("TRUE")) {
            childvaccineFocusListener()
            child_age = sharedPreferences.getString("childAge", null)!!
            child_dob = sharedPreferences.getString("childdob", null)!!
            child_gender = sharedPreferences.getString("childGender", null)!!
            val dateFormatter = DateTimeFormatter.ofPattern("d-M-yyyy")
            val targetDate = LocalDate.parse(child_dob, dateFormatter)
            val currentDate = LocalDate.now()
            val weeksDifference = ((currentDate.toEpochDay() - targetDate.toEpochDay()) / 7).toInt()
            val monthsDifference = (ChronoUnit.MONTHS.between(targetDate, currentDate)).toInt()
            childvaccineinput =
                findViewById<MaterialAutoCompleteTextView>(R.id.childvaccineEditText)
            adultvaccine_selection = if (adultAge.toInt() == 60 || adultAge.toInt() == 65) {
                arrayOf(
                    "Pneumo-Polysaccharide Vaccine",
                    "Influenza Vaccine",
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "None"
                )
            } else if (adultAge.toInt() >= 60) {
                arrayOf(
                    "Influenza Vaccine",
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "None"
                )
            } else {
                arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "None"
                )
            }


            childvaccine_selection = when {
                child_age.toInt() == 0 -> {
                    when {
                        weeksDifference <= 3 -> arrayOf(
                            "BCG Vaccine",
                            "Hepatitis B",
                            "Tetanus Diptheria(1st Dose)",
                            "Others(if not listed)",
                            "None"
                        )

                        weeksDifference == 4 -> arrayOf(
                            "Tetanus Diptheria(2nd Dose)",
                            "Others(if not listed)",
                            "None"
                        )

                        weeksDifference == 6 -> arrayOf(
                            "Oral Polio Vaccine(1st Dose)",
                            "PENTA Vaccine(DPT-HepB+Hib)(1st Dose)",
                            "PCV(1st Dose)",
                            "Rotavirus Vaccine(1st Dose)",
                            "Others(if not listed)",
                            "None"
                        )

                        weeksDifference == 10 -> arrayOf(
                            "Oral Polio Vaccine(2nd Dose)",
                            "PENTA Vaccine(DPT-HepB+Hib)(2nd Dose)",
                            "PCV(2nd Dose)",
                            "Rotavirus Vaccine(2nd Dose)",
                            "Others(if not listed)",
                            "None"
                        )

                        weeksDifference == 14 -> arrayOf(
                            "Oral Polio Vaccine(3rd Dose)",
                            "PENTA Vaccine(DPT-HepB+Hib)(3rd Dose)",
                            "PCV(3rd Dose)",
                            "Inactivated Polio Vaccine",
                            "Others(if not listed)",
                            "None"
                        )

                        else -> {
                            when {
                                monthsDifference == 9 -> arrayOf(
                                    "MMR Vaccine(1st Dose)",
                                    "Japanese Encephalitis",
                                    "Others(if not listed)",
                                    "COVID Vaccine(First Dose)",
                                    "COVID Vaccine(Second Dose)",
                                    "COVID Vaccine(Booster)",
                                    "None"
                                )

                                monthsDifference == 6 -> arrayOf(
                                    "Tetanus Diptheria(3rd Dose)",
                                    "Others(if not listed)",
                                    "COVID Vaccine(First Dose)",
                                    "COVID Vaccine(Second Dose)",
                                    "COVID Vaccine(Booster)",
                                    "None"
                                )

                                monthsDifference == 12 -> arrayOf(
                                    "MMR Vaccine(2nd Dose)",
                                    "Others(if not listed)",
                                    "COVID Vaccine(First Dose)",
                                    "COVID Vaccine(Second Dose)",
                                    "COVID Vaccine(Booster)",
                                    "None"
                                )

                                monthsDifference > 6 -> arrayOf(
                                    "COVID Vaccine(First Dose)",
                                    "COVID Vaccine(Second Dose)",
                                    "COVID Vaccine(Booster)",
                                    "Others(if not listed)",
                                    "None"
                                )

                                else -> arrayOf(
                                    "COVID Vaccine(First Dose)",
                                    "COVID Vaccine(Second Dose)",
                                    "COVID Vaccine(Booster)",
                                    "Others(if not listed)",
                                    "None"
                                )
                            }
                        }
                    }
                }

                child_age.toInt() >= 5 && child_age.toInt() <= 7 -> arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "Measles-Rubella Vaccine(1st Dose)",
                    "Tetanus Diptheria(4th Dose)",
                    "Others(if not listed)",
                    "None"
                )

                child_age.toInt() >= 12 && child_age.toInt() <= 15 -> arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "Measles-Rubella Vaccine(2nd Dose)",
                    "Tetanus Diptheria(5th Dose)",
                    "Others(if not listed)",
                    "None"
                )

                child_age.toInt() >= 9 && child_age.toInt() <= 10 && child_gender == "Female" -> arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "Human Papilloma Virus(HPV) Vaccine",
                    "Others(if not listed)",
                    "None"
                )

                else -> arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "Others(if not listed)",
                    "None"
                )
            }


            val adapter_child = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                childvaccine_selection
            )
            childvaccineinput.setAdapter(adapter_child)
            val adapter_adult = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                adultvaccine_selection
            )
            adultvaccineinput.setAdapter(adapter_adult)
            removeFromArray(adultvaccine_selection,childvaccine_selection)
        } else {
            if (adultAge.toInt() in 60..65) {
                adultvaccine_selection = arrayOf(
                    "Pneumo-Polysaccharide Vaccine",
                    "Influenza Vaccine",
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)"
                )
            } else {
                adultvaccine_selection = arrayOf(
                    "COVID Vaccine(First Dose)",
                    "COVID Vaccine(Second Dose)",
                    "COVID Vaccine(Booster)",
                    "None"
                )
            }

            val adapter_adult = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                adultvaccine_selection
            )
            adultvaccineinput.setAdapter(adapter_adult)
            removeFromArray(adultvaccine_selection,arrayOf<String>())
        }





        timePickerButton.setOnClickListener { showTimePickerDialog() }
        val location_input = findViewById<MaterialAutoCompleteTextView>(R.id.locationEditText)

        location_input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                location_text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                location_text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                val query = location_text.toString()
                fetchPredictions(query)
            }

            override fun afterTextChanged(s: Editable?) {
                val textentry = s.toString()
                displaytoMap(textentry)
            }

        })
        datetimebtn = findViewById(R.id.imageView11)
        locationbtn = findViewById(R.id.imageView12)
        vaccinebtn = findViewById(R.id.imageView13)
        val nav = findViewById<BottomNavigationView>(R.id.nav_bar)
        val vaccinelabel = findViewById<TextView>(R.id.textView7)
        val locationlabel = findViewById<TextView>(R.id.locationtextview)
        val vaccineContainer = findViewById<TextInputLayout>(R.id.VaccineTextContainer)
        val child_vaccine = findViewById<TextInputLayout>(R.id.childVaccineTextContainer)
        val locationContainer = findViewById<TextInputLayout>(R.id.locationEditTextContainer)
        val submitbtn = findViewById<Button>(R.id.submitbutton)
        imageGMaps.visibility = View.INVISIBLE
        vaccinelabel.visibility = View.INVISIBLE
        locationlabel.visibility = View.INVISIBLE
        vaccineContainer.visibility = View.INVISIBLE
        locationContainer.visibility = View.INVISIBLE
        submitbtn.visibility = View.INVISIBLE
        locationFocusListener()
        vaccineFocusListener()

        calendarView.setOnDateChangeListener { calView: CalendarView, year: Int, month: Int, dayOfMonth: Int ->
            val calender: Calendar = Calendar.getInstance()
            val dat: String
            var xy: String
            calender.set(year, month, dayOfMonth)
            calView.setDate(calender.timeInMillis, true, true)
            calView.setDate(calender.timeInMillis, true, true)
            val x = month + 1
            xy = dayOfMonth.toString()
            if (dayOfMonth.toString().length == 1) {
                xy = "0" + dayOfMonth.toString()
            }
            dat = x.toString() + "/" + xy + "/" + year.toString()
            hiddendate.setText(dat)
            user_selectedDate = dat


        }
        datetimebtn?.setOnClickListener { handleTextViewClick(datetimebtn) }
        locationbtn?.setOnClickListener { handleTextViewClick(locationbtn) }
        vaccinebtn?.setOnClickListener { handleTextViewClick(vaccinebtn) }
        submitbtn.setOnClickListener {
            submitbtn.isEnabled = false
            submitbtn.isClickable = false
            insertAppointmentData()
        }


        nav.menu.findItem(R.id.book_appointment).setChecked(true)
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
            val adapter = AdapterforChatBot(this, messages, lifecycleScope)
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
                R.id.homepage -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, Homepage::class.java)
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
        locationContainer.visibility = View.INVISIBLE
        child_vaccine.visibility = View.INVISIBLE
        vaccineContainer.visibility = View.INVISIBLE

    }

    private fun fetchPredictions(query: String) {
        val location_input = findViewById<MaterialAutoCompleteTextView>(R.id.locationEditText)
        val apiKey = resources.getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        val placesClient = Places.createClient(this)
        val autocompleteSessionToken = AutocompleteSessionToken.newInstance()
        val autocompleteRequest = FindAutocompletePredictionsRequest.builder()
            .setTypeFilter(TypeFilter.ESTABLISHMENT)
            .setSessionToken(autocompleteSessionToken)
            .setCountry("PH")
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(autocompleteRequest)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                val filteredPredictions = filterHospitalsAndClinics(predictions)
                val predictionStrings = filteredPredictions.map { it.getFullText(null).toString() }
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    predictionStrings
                )
                location_input.setAdapter(adapter)
            }
    }

    private fun filterHospitalsAndClinics(predictions: List<AutocompletePrediction>): List<AutocompletePrediction> {
        val filteredPredictions = mutableListOf<AutocompletePrediction>()
        for (prediction in predictions) {
            val fullText = prediction.getFullText(null).toString()
            if (fullText.contains("hospital", ignoreCase = true) || fullText.contains(
                    "clinic",
                    ignoreCase = true
                ) || fullText.contains("medical", ignoreCase = true) || fullText.contains(
                    "ospital",
                    ignoreCase = true
                ) || fullText.contains(
                    "health",
                    ignoreCase = true
                ) || fullText.contains("health center", ignoreCase = true)
            ) {
                filteredPredictions.add(prediction)
            }
        }
        return filteredPredictions
    }

    private fun displaytoMap(location: String) {
        val imageGMaps = findViewById<WebView>(R.id.mapsDisplay)
        val apiKey = resources.getString(R.string.google_maps_key)
        val formattedAddress = location.replace(" ", "+")
        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$formattedAddress" +
                "&zoom=15" +
                "&size=400x200" +
                "&markers=color:red|$formattedAddress" +
                "&key=$apiKey"
        imageGMaps.loadUrl(staticMapUrl)

    }

    private fun handleTextViewClick(clickedTextView: TextView?) {
        val prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val child_state = sharedPreferences.getString("child_state", null)!!
        val imageGMaps = findViewById<WebView>(R.id.mapsDisplay)
        val vaccinelabel = findViewById<TextView>(R.id.textView7)
        val locationlabel = findViewById<TextView>(R.id.locationtextview)
        val vaccineContainer = findViewById<TextInputLayout>(R.id.VaccineTextContainer)
        val locationContainer = findViewById<TextInputLayout>(R.id.locationEditTextContainer)
        val submitbtn = findViewById<Button>(R.id.submitbutton)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timepicker = findViewById<Button>(R.id.timePickerButton)
        val vaccine_txt = findViewById<TextView>(R.id.textView7)
        val adult_txt = findViewById<TextView>(R.id.textView11)
        val child_txt = findViewById<TextView>(R.id.textView23)
        val child_vaccine = findViewById<TextInputLayout>(R.id.childVaccineTextContainer)
        val adult_vaccine = findViewById<TextInputLayout>(R.id.VaccineTextContainer)
        datetimebtn?.isSelected = false
        locationbtn?.isSelected = false
        vaccinebtn?.isSelected = false
        clickedTextView?.isSelected = true
        val oneIsSelected =
            datetimebtn?.isSelected == true || locationbtn?.isSelected == true || vaccinebtn?.isSelected == true
        if (oneIsSelected) {
            if (datetimebtn?.isSelected == true) {
                imageGMaps.visibility = View.INVISIBLE
                vaccinelabel.visibility = View.INVISIBLE
                locationlabel.visibility = View.INVISIBLE
                vaccineContainer.visibility = View.INVISIBLE
                locationContainer.visibility = View.INVISIBLE
                submitbtn.visibility = View.INVISIBLE
                calendarView.visibility = View.VISIBLE
                timepicker.visibility = View.VISIBLE
                vaccine_txt.visibility = View.INVISIBLE
                adult_txt.visibility = View.INVISIBLE
                child_txt.visibility = View.INVISIBLE
                child_vaccine.visibility = View.INVISIBLE
                adult_vaccine.visibility = View.INVISIBLE

            }
            if (locationbtn?.isSelected == true) {
                imageGMaps.visibility = View.VISIBLE
                vaccinelabel.visibility = View.INVISIBLE
                locationlabel.visibility = View.VISIBLE
                vaccineContainer.visibility = View.INVISIBLE
                locationContainer.visibility = View.VISIBLE
                submitbtn.visibility = View.INVISIBLE
                calendarView.visibility = View.INVISIBLE
                timepicker.visibility = View.INVISIBLE
                vaccine_txt.visibility = View.INVISIBLE
                adult_txt.visibility = View.INVISIBLE
                child_txt.visibility = View.INVISIBLE
                child_vaccine.visibility = View.INVISIBLE
                adult_vaccine.visibility = View.INVISIBLE
            }
            if (vaccinebtn?.isSelected == true) {
                imageGMaps.visibility = View.INVISIBLE
                vaccinelabel.visibility = View.VISIBLE
                locationlabel.visibility = View.INVISIBLE
                vaccineContainer.visibility = View.VISIBLE
                locationContainer.visibility = View.INVISIBLE
                submitbtn.visibility = View.VISIBLE
                calendarView.visibility = View.INVISIBLE
                timepicker.visibility = View.INVISIBLE
                vaccine_txt.visibility = View.VISIBLE
                if (child_state.equals("FALSE")) {
                    adult_txt.visibility = View.VISIBLE
                    child_txt.visibility = View.INVISIBLE
                    child_vaccine.visibility = View.INVISIBLE
                    adult_vaccine.visibility = View.VISIBLE
                } else {
                    adult_txt.visibility = View.VISIBLE
                    child_txt.visibility = View.VISIBLE
                    child_vaccine.visibility = View.VISIBLE
                    adult_vaccine.visibility = View.VISIBLE
                }


            }

        }


    }

    private fun locationFocusListener() {
        val location_input = findViewById<MaterialAutoCompleteTextView>(R.id.locationEditText)
        location_input.setOnFocusChangeListener { _, focused ->
            if (!focused) {
                val locationContainer =
                    findViewById<TextInputLayout>(R.id.locationEditTextContainer)
                locationContainer.helperText = validateLocation()

            }
        }
    }

    private fun validateLocation(): String? {
        val location_input = findViewById<MaterialAutoCompleteTextView>(R.id.locationEditText)
        val locationVal = location_input.text.toString()
        if (locationVal.isBlank()) {
            return "Please Input your Preferred Location"
        }
        return null
    }

    private fun vaccineFocusListener() {
        val adultvaccineinput = findViewById<MaterialAutoCompleteTextView>(R.id.vaccineEditText)
        adultvaccineinput.setOnFocusChangeListener { _, focused ->
            if (!focused) {
                val vaccineContainer = findViewById<TextInputLayout>(R.id.VaccineTextContainer)
                vaccineContainer.helperText = validateVaccine()
            }
        }
    }

    private fun validateVaccine(): String? {
        val adultvaccineinput = findViewById<MaterialAutoCompleteTextView>(R.id.vaccineEditText)
        val childvaccineinput =
            findViewById<MaterialAutoCompleteTextView>(R.id.childvaccineEditText)
        val adultvaccineVal = adultvaccineinput.text.toString()
        val childvaccineVal = childvaccineinput.text.toString()
        val child_state = sharedPreferences.getString("child_state", null)!!
        if (child_state == "TRUE"){
            if (adultvaccineVal.isBlank()|| childvaccineVal.isBlank() || (adultvaccineVal == "None" && childvaccineVal == "None")) {
                return "Please Select a Vaccine"
            }
        }
        else{
            if (adultvaccineVal.isBlank() || adultvaccineVal == "None" ){
                return "Please Select a Vaccine"
            }
        }

        return null
    }

    private fun childvaccineFocusListener() {
        val childvaccineinput =
            findViewById<MaterialAutoCompleteTextView>(R.id.childvaccineEditText)
        childvaccineinput.setOnFocusChangeListener { _, focused ->
            if (!focused) {
                val childvaccineContainer =
                    findViewById<TextInputLayout>(R.id.childVaccineTextContainer)
                childvaccineContainer.helperText = validateVaccine()

            }
        }
    }

    private fun updateAdapter(adapterValue: Array<String>, values: List<String>,autoCompleteTextView: MaterialAutoCompleteTextView) {
        val updatedList = adapterValue.toMutableList()
        updatedList.removeAll(values)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            updatedList
        )
        autoCompleteTextView.setAdapter(adapter)



    }

    private fun removeFromArray(adultSelection: Array<String>, childSelection: Array<String>) {
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null) ?: return
        val childState = sharedPreferences.getString("child_state", null) ?: return
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")


        when (childState) {
            "TRUE" -> {
                collectionReference.whereEqualTo("pkIdentifier", pkIdentifier)
                    .addSnapshotListener { querySnapshot, exception ->
                        if (exception != null) {
                            println("Error fetching data: ${exception.message}")
                            return@addSnapshotListener
                        }

                        querySnapshot?.forEach { document ->
                            val documentID = document.id

                            // Real-time listener for child completed appointments
                            firestore.collection("users/$documentID/childCompletedAppointments")
                                .addSnapshotListener { appointmentQuerySnapshot, _ ->
                                    val childVaccineList = appointmentQuerySnapshot?.documents?.mapNotNull {
                                        it.getString("childvaccine")
                                    } ?: emptyList()
                                    updateAdapter(childSelection, childVaccineList, childvaccineinput)
                                }

                            // Real-time listener for adult completed appointments
                            firestore.collection("users/$documentID/adultCompletedAppointments")
                                .addSnapshotListener { appointmentQuerySnapshot, _ ->
                                    val adultVaccineList = appointmentQuerySnapshot?.documents?.mapNotNull {
                                        it.getString("adultvaccine")
                                    } ?: emptyList()
                                    updateAdapter(adultSelection, adultVaccineList, adultvaccineinput)
                                }
                        }
                    }
            }
            "FALSE" -> {
                collectionReference.whereEqualTo("pkIdentifier", pkIdentifier)
                    .addSnapshotListener { querySnapshot, exception ->
                        if (exception != null) {
                            println("Error fetching data: ${exception.message}")
                            return@addSnapshotListener
                        }

                        querySnapshot?.forEach { document ->
                            val documentID = document.id

                            // Real-time listener for adult completed appointments
                            firestore.collection("users/$documentID/adultCompletedAppointments")
                                .addSnapshotListener { appointmentQuerySnapshot, _ ->
                                    val adultVaccineList = appointmentQuerySnapshot?.documents?.mapNotNull {
                                        it.getString("adultvaccine")
                                    } ?: emptyList()
                                    updateAdapter(adultSelection, adultVaccineList,adultvaccineinput)
                                }
                        }
                    }
            }
            else -> {
                println("Unknown state: $childState")
            }
        }
    }
    private fun insertAppointmentData() {
        val submitbtn = findViewById<Button>(R.id.submitbutton)
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        val prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!
        var vaccination_count = 0
        val email = sharedPreferences.getString("email", null)!!
        val hiddendate = findViewById<TextView>(R.id.hiddendate)
        val hiddentime = findViewById<TextView>(R.id.hiddentime)
        val childvaccineinput =
            findViewById<MaterialAutoCompleteTextView>(R.id.childvaccineEditText)
        val adultvaccineinput = findViewById<MaterialAutoCompleteTextView>(R.id.vaccineEditText)
        val location_input = findViewById<MaterialAutoCompleteTextView>(R.id.locationEditText)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val vaccineContainer = findViewById<TextInputLayout>(R.id.VaccineTextContainer)
        vaccineContainer.helperText = validateVaccine()
        val locationContainer = findViewById<TextInputLayout>(R.id.locationEditTextContainer)
        locationContainer.helperText = validateLocation()
        val childvaccineContainer = findViewById<TextInputLayout>(R.id.childVaccineTextContainer)
        childvaccineContainer.helperText =  validateVaccine()
        val datevalue = hiddendate.text.toString()
        val validadultvaccineinput = vaccineContainer.helperText == null
        val validlocation = locationContainer.helperText == null
        val validchildvaccineinput = childvaccineContainer.helperText == null
        val adultvaccine_input = adultvaccineinput.text.toString()
        val childvaccine_input = childvaccineinput.text.toString()
        val hiddendate_input = hiddendate.text.toString()
        val location_finalinput = location_input.text.toString()
        val hiddentime_input = hiddentime.text.toString()
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")

        collectionReference.whereEqualTo("pkIdentifier", pkIdentifier).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val userID = document.id
                    collectionReference.document(userID).get().addOnSuccessListener { ref ->
                        val vaccinationCountStr = ref.getString("vaccination_count")
                        vaccination_count = vaccinationCountStr?.toIntOrNull() ?: 0
                        val appointmentList = ref?.get("appointmentList") as? MutableMap<String, Any> ?: mutableMapOf()
                        val containsKey2 = appointmentList.containsKey("2")
                        val containsKey1 = appointmentList.containsKey("1")
                        if (containsKey2 && !containsKey1) {
                            vaccination_count = 1
                            collectionReference.document(userID)
                                .update("vaccination_count", vaccination_count)
                        }
                    }

                }
            }




        if (user_selectedDate.isNullOrBlank()) {
            AlertDialog.Builder(this@AppointmentPage)
                .setTitle("Invalid Form")
                .setMessage("Please Enter your Preferred Date, Please be advised you are only allowed to schedule 2 Appointments by default")
                .setPositiveButton("Ok") { _, _ ->
                    submitbtn.isEnabled = true
                    submitbtn.isClickable = true
                }
                .show()
        }
        if (user_selectedTime.isNullOrBlank()) {
            AlertDialog.Builder(this@AppointmentPage)
                .setTitle("Invalid Form")
                .setMessage("Please Enter your Preferred Time, Please be advised business Hours for Most Clinics and Hospitals are from 6am to 6pm, Please Contact your Preferred Hospital, for further Inquiries of Business Hours")
                .setPositiveButton("Ok") { _, _ ->
                    submitbtn.isEnabled = true
                    submitbtn.isClickable = true
                }
                .show()
        }
        if (!validlocation) {
            AlertDialog.Builder(this@AppointmentPage)
                .setTitle("Invalid Form")
                .setMessage("Please Fill up your Preferred Location to take your Vaccine")
                .setPositiveButton("Ok") { _, _ ->
                    submitbtn.isEnabled = true
                    submitbtn.isClickable = true
                }
                .show()
        }
        if (!validadultvaccineinput) {
            AlertDialog.Builder(this@AppointmentPage)
                .setTitle("Invalid Form")
                .setMessage("Please Fill up your Preferred Vaccine")
                .setPositiveButton("Ok") { _, _ ->
                    submitbtn.isEnabled = true
                    submitbtn.isClickable = true
                }
                .show()
        }


        generateUniqueRandomString { uniqueString ->
            if (vaccination_count < 2) {
                collectionReference.whereEqualTo("pkIdentifier", pkIdentifier).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (child_state.equals("TRUE")) {
                            collectionReference.whereEqualTo("pkIdentifier", pkIdentifier)
                                .get().addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot) {
                                        val documentID = document.id
                                        firestore.collection("users/$documentID/appointments")
                                            .get()
                                            .addOnSuccessListener { appointmentQuerySnapshot ->
                                                var childVaccineExistsforAppointment = false
                                                var adultVaccineExistsforAppointment = false
                                                var childVaccineExistsTaken = false
                                                var adultVaccineExistsTaken = false
                                                if (!appointmentQuerySnapshot.isEmpty) {
                                                    for (appointment in appointmentQuerySnapshot) {
                                                        val childvaccine =
                                                            appointment.getString("childvaccine")
                                                        val adultvaccine =
                                                            appointment.getString("adultvaccine")
                                                        if ((childvaccine_input == "None" || childvaccine_input == "Others(if not listed)") && (childvaccine == "None" || childvaccine == "Others(if not listed)")) {
                                                            childVaccineExistsforAppointment = false
                                                        } else if (childvaccine == childvaccine_input) {
                                                            childVaccineExistsforAppointment = true
                                                        }

                                                        if ((adultvaccine_input == "None" || adultvaccine_input == "Others(if not listed)") && (adultvaccine == "None" || adultvaccine == "Others(if not listed)")) {
                                                            adultVaccineExistsforAppointment = false
                                                        } else if (adultvaccine == adultvaccine_input) {
                                                            adultVaccineExistsforAppointment = true
                                                        }

                                                        if (childVaccineExistsforAppointment && adultVaccineExistsforAppointment) {
                                                            break
                                                        }
                                                    }

                                                    if (childVaccineExistsforAppointment && adultVaccineExistsforAppointment) {
                                                        AlertDialog.Builder(this@AppointmentPage)
                                                            .setTitle("Invalid Vaccine")
                                                            .setMessage("Please be advised, Both Vaccines are already scheduled, Kindly Choose Another Vaccine")
                                                            .setPositiveButton("Ok") { _, _ ->
                                                            }
                                                            .show()
                                                        submitbtn.isEnabled = true
                                                        submitbtn.isClickable = true


                                                    }
                                                    else if (childVaccineExistsforAppointment) {
                                                        AlertDialog.Builder(this@AppointmentPage)
                                                            .setTitle("Invalid Vaccine")
                                                            .setMessage("Please be advised, Child Vaccine is already scheduled, Kindly Choose Another Vaccine")
                                                            .setPositiveButton("Ok") { _, _ ->
                                                            }
                                                            .show()
                                                        submitbtn.isEnabled = true
                                                        submitbtn.isClickable = true

                                                    }
                                                    else if (adultVaccineExistsforAppointment) {
                                                        AlertDialog.Builder(this@AppointmentPage)
                                                            .setTitle("Invalid Vaccine")
                                                            .setMessage("Please be advised, Adult Vaccine is already scheduled, Kindly Choose Another Vaccine")
                                                            .setPositiveButton("Ok") { _, _ ->
                                                            }
                                                            .show()
                                                        submitbtn.isEnabled = true
                                                        submitbtn.isClickable = true

                                                    }
                                                    else {
                                                        firestore.collection("users/$documentID/childCompletedAppointments")
                                                            .whereEqualTo(
                                                                "childvaccine",
                                                                childvaccine_input
                                                            )
                                                            .get()
                                                            .addOnSuccessListener { childQuerySnapshot ->
                                                                if (childQuerySnapshot.size() > 0) {
                                                                    childVaccineExistsTaken = true
                                                                }

                                                                firestore.collection("users/$documentID/adultCompletedAppointments")
                                                                    .whereEqualTo(
                                                                        "adultvaccine",
                                                                        adultvaccine_input
                                                                    )
                                                                    .get()
                                                                    .addOnSuccessListener { adultQuerySnapshot ->
                                                                        if (adultQuerySnapshot.size() > 0) {
                                                                            adultVaccineExistsTaken =
                                                                                true
                                                                        }

                                                                        if (childVaccineExistsTaken && adultVaccineExistsTaken) {
                                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                                .setTitle("Invalid Vaccine")
                                                                                .setMessage("Please be advised, Both Vaccines are already taken, Kindly Choose Another Vaccine")
                                                                                .setPositiveButton("Ok") { _, _ ->
                                                                                }
                                                                                .show()
                                                                            submitbtn.isEnabled = true
                                                                            submitbtn.isClickable = true
                                                                        } else if (childVaccineExistsTaken) {
                                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                                .setTitle("Invalid Vaccine")
                                                                                .setMessage("Please be advised, Child Vaccine is already taken, Kindly Choose Another Vaccine")
                                                                                .setPositiveButton("Ok") { _, _ ->
                                                                                }
                                                                                .show()
                                                                            submitbtn.isEnabled = true
                                                                            submitbtn.isClickable = true
                                                                        } else if (adultVaccineExistsTaken) {
                                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                                .setTitle("Invalid Vaccine")
                                                                                .setMessage("Please be advised, Adult Vaccine is already taken, Kindly Choose Another Vaccine")
                                                                                .setPositiveButton("Ok") { _, _ ->
                                                                                }
                                                                                .show()
                                                                            submitbtn.isEnabled = true
                                                                            submitbtn.isClickable = true
                                                                        } else {
                                                                            if (adultvaccine_input.isEmpty()) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Vaccine")
                                                                                    .setMessage("No Vaccine is Selected for Adult, Kindly choose a vaccine")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true
                                                                            }
                                                                            if (childvaccine_input.isEmpty()) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Vaccine")
                                                                                    .setMessage("No Vaccine is Selected for Child, Kindly choose a vaccine")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true
                                                                            }
                                                                            if (datevalue.length == 8) {
                                                                                val formattedmonth =
                                                                                    datevalue.substring(
                                                                                        0,
                                                                                        1
                                                                                    ).toInt()
                                                                                val formattedday =
                                                                                    datevalue.substring(
                                                                                        2,
                                                                                        3
                                                                                    ).toInt()
                                                                                val formattedyear =
                                                                                    datevalue.substring(
                                                                                        4
                                                                                    ).toInt()
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday < currentDay && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Day Please Choose a Different Day")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth < currentMonth && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Month Please Choose a Different Month")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear < currentYear
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Year Please Choose a Different Year")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                    val selectedDateCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                time =
                                                                                                    user_selectedDate?.let {
                                                                                                        dateFormat.parse(
                                                                                                            it
                                                                                                        )
                                                                                                    }!!
                                                                                            }
                                                                                    val timeFormatter: DateTimeFormatter =
                                                                                        DateTimeFormatter.ofPattern(
                                                                                            "h:mm a"
                                                                                        )
                                                                                    val selectedTime =
                                                                                        LocalTime.parse(
                                                                                            user_selectedTime,
                                                                                            timeFormatter
                                                                                        )
                                                                                    val selectedHour =
                                                                                        selectedTime.hour
                                                                                    val selectedMinute =
                                                                                        selectedTime.minute
                                                                                    val selectedTimeCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                set(
                                                                                                    Calendar.HOUR_OF_DAY,
                                                                                                    selectedHour
                                                                                                )
                                                                                                set(
                                                                                                    Calendar.MINUTE,
                                                                                                    selectedMinute
                                                                                                )
                                                                                            }
                                                                                    if (selectedDateCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ) ||
                                                                                        (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ))
                                                                                    ) {
                                                                                        showInvalidTimeAlert1()
                                                                                        submitbtn.isEnabled =
                                                                                            true
                                                                                        submitbtn.isClickable =
                                                                                            true
                                                                                    } else {
                                                                                        val user =
                                                                                            hashMapOf(
                                                                                                "adultvaccine" to adultvaccine_input,
                                                                                                "childvaccine" to childvaccine_input,
                                                                                                "date" to hiddendate_input,
                                                                                                "location" to location_finalinput,
                                                                                                "pkIdentifier" to pkIdentifier,
                                                                                                "time" to hiddentime_input,
                                                                                                "appointmentKey" to uniqueString
                                                                                            )
                                                                                        collectionReference.whereEqualTo(
                                                                                            "pkIdentifier",
                                                                                            pkIdentifier
                                                                                        )
                                                                                            .get()
                                                                                            .addOnSuccessListener { querySnapshot ->
                                                                                                for (document in querySnapshot) {
                                                                                                    val documentID =
                                                                                                        document.id
                                                                                                    vaccination_count++
                                                                                                    val updates =
                                                                                                        hashMapOf<String, Any>(
                                                                                                            "vaccination_count" to vaccination_count.toString()
                                                                                                        )
                                                                                                    val editor =
                                                                                                        sharedPreferences.edit()
                                                                                                    editor.putString(
                                                                                                        "vaccination_count",
                                                                                                        vaccination_count.toString()
                                                                                                    )
                                                                                                    editor.apply()
                                                                                                    val existingData =
                                                                                                        document.data
                                                                                                    val docRef =
                                                                                                        collectionReference.document(
                                                                                                            documentID
                                                                                                        )
                                                                                                    val appointmentList =
                                                                                                        existingData?.get(
                                                                                                            "appointmentList"
                                                                                                        ) as? MutableMap<String, Any>
                                                                                                            ?: mutableMapOf()
                                                                                                    appointmentList[vaccination_count.toString()] =
                                                                                                        uniqueString
                                                                                                    docRef.update(
                                                                                                        "appointmentList",
                                                                                                        appointmentList
                                                                                                    )
                                                                                                    docRef.update(
                                                                                                        updates
                                                                                                    )
                                                                                                        .addOnSuccessListener {
                                                                                                            docRef.collection(
                                                                                                                "appointments"
                                                                                                            )
                                                                                                                .add(
                                                                                                                    user
                                                                                                                )
                                                                                                                .addOnCompleteListener {
                                                                                                                    AlertDialog.Builder(
                                                                                                                        this
                                                                                                                    )
                                                                                                                        .setTitle(
                                                                                                                            "Appointment has been Scheduled"
                                                                                                                        )
                                                                                                                        .setMessage(
                                                                                                                            "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                        )
                                                                                                                        .setPositiveButton(
                                                                                                                            "Ok"
                                                                                                                        ) { _, _ ->
                                                                                                                            val intent =
                                                                                                                                Intent(
                                                                                                                                    this,
                                                                                                                                    AppointmentPage::class.java
                                                                                                                                )
                                                                                                                            startActivity(
                                                                                                                                intent
                                                                                                                            )
                                                                                                                        }
                                                                                                                        .show()
                                                                                                                    val messageContent =
                                                                                                                        "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                    sendEmail(
                                                                                                                        email,
                                                                                                                        messageContent
                                                                                                                    )
                                                                                                                    submitbtn.isEnabled =
                                                                                                                        true
                                                                                                                    submitbtn.isClickable =
                                                                                                                        true


                                                                                                                }

                                                                                                        }


                                                                                                }
                                                                                            }
                                                                                    }


                                                                                }

                                                                            }
                                                                            if (datevalue.length == 9) {
                                                                                val formattedmonth =
                                                                                    datevalue.substring(
                                                                                        0,
                                                                                        1
                                                                                    ).toInt()
                                                                                val formattedday =
                                                                                    datevalue.substring(
                                                                                        2,
                                                                                        4
                                                                                    ).toInt()
                                                                                val formattedyear =
                                                                                    datevalue.substring(
                                                                                        5
                                                                                    ).toInt()
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday < currentDay && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Day Please Choose a Different Day")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth < currentMonth && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Month Please Choose a Different Month")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear < currentYear
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Year Please Choose a Different Year")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }

                                                                                if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                    val selectedDateCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                time =
                                                                                                    user_selectedDate?.let {
                                                                                                        dateFormat.parse(
                                                                                                            it
                                                                                                        )
                                                                                                    }!!
                                                                                            }
                                                                                    val timeFormatter: DateTimeFormatter =
                                                                                        DateTimeFormatter.ofPattern(
                                                                                            "h:mm a"
                                                                                        )
                                                                                    val selectedTime =
                                                                                        LocalTime.parse(
                                                                                            user_selectedTime,
                                                                                            timeFormatter
                                                                                        )
                                                                                    val selectedHour =
                                                                                        selectedTime.hour
                                                                                    val selectedMinute =
                                                                                        selectedTime.minute


                                                                                    val selectedTimeCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                set(
                                                                                                    Calendar.HOUR_OF_DAY,
                                                                                                    selectedHour
                                                                                                )
                                                                                                set(
                                                                                                    Calendar.MINUTE,
                                                                                                    selectedMinute
                                                                                                )
                                                                                            }
                                                                                    if (selectedDateCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ) ||
                                                                                        (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ))
                                                                                    ) {
                                                                                        showInvalidTimeAlert1()
                                                                                        submitbtn.isEnabled =
                                                                                            true
                                                                                        submitbtn.isClickable =
                                                                                            true
                                                                                    } else {
                                                                                        val user =
                                                                                            hashMapOf(
                                                                                                "adultvaccine" to adultvaccine_input,
                                                                                                "childvaccine" to childvaccine_input,
                                                                                                "date" to hiddendate_input,
                                                                                                "location" to location_finalinput,
                                                                                                "pkIdentifier" to pkIdentifier,
                                                                                                "time" to hiddentime_input,
                                                                                                "appointmentKey" to uniqueString
                                                                                            )
                                                                                        collectionReference.whereEqualTo(
                                                                                            "pkIdentifier",
                                                                                            pkIdentifier
                                                                                        )
                                                                                            .get()
                                                                                            .addOnSuccessListener { querySnapshot ->
                                                                                                for (document in querySnapshot) {
                                                                                                    val documentID =
                                                                                                        document.id
                                                                                                    vaccination_count++
                                                                                                    val updates =
                                                                                                        hashMapOf<String, Any>(
                                                                                                            "vaccination_count" to vaccination_count.toString()
                                                                                                        )
                                                                                                    val editor =
                                                                                                        sharedPreferences.edit()
                                                                                                    editor.putString(
                                                                                                        "vaccination_count",
                                                                                                        vaccination_count.toString()
                                                                                                    )
                                                                                                    editor.apply()
                                                                                                    val existingData =
                                                                                                        document.data
                                                                                                    val docRef =
                                                                                                        collectionReference.document(
                                                                                                            documentID
                                                                                                        )
                                                                                                    val appointmentList =
                                                                                                        existingData?.get(
                                                                                                            "appointmentList"
                                                                                                        ) as? MutableMap<String, Any>
                                                                                                            ?: mutableMapOf()
                                                                                                    appointmentList[vaccination_count.toString()] =
                                                                                                        uniqueString
                                                                                                    docRef.update(
                                                                                                        "appointmentList",
                                                                                                        appointmentList
                                                                                                    )
                                                                                                    docRef.update(
                                                                                                        updates
                                                                                                    )
                                                                                                        .addOnSuccessListener {
                                                                                                            docRef.collection(
                                                                                                                "appointments"
                                                                                                            )
                                                                                                                .add(
                                                                                                                    user
                                                                                                                )
                                                                                                                .addOnCompleteListener {
                                                                                                                    AlertDialog.Builder(
                                                                                                                        this
                                                                                                                    )
                                                                                                                        .setTitle(
                                                                                                                            "Appointment has been Scheduled"
                                                                                                                        )
                                                                                                                        .setMessage(
                                                                                                                            "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                        )
                                                                                                                        .setPositiveButton(
                                                                                                                            "Ok"
                                                                                                                        ) { _, _ ->
                                                                                                                            val intent =
                                                                                                                                Intent(
                                                                                                                                    this,
                                                                                                                                    AppointmentPage::class.java
                                                                                                                                )
                                                                                                                            startActivity(
                                                                                                                                intent
                                                                                                                            )
                                                                                                                        }
                                                                                                                        .show()
                                                                                                                    val messageContent =
                                                                                                                        "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                    sendEmail(
                                                                                                                        email,
                                                                                                                        messageContent
                                                                                                                    )
                                                                                                                    submitbtn.isEnabled =
                                                                                                                        true
                                                                                                                    submitbtn.isClickable =
                                                                                                                        true

                                                                                                                }

                                                                                                        }


                                                                                                }
                                                                                            }
                                                                                    }


                                                                                }
                                                                            }
                                                                            if (datevalue.length == 10) {
                                                                                val formattedmonth =
                                                                                    datevalue.substring(
                                                                                        0,
                                                                                        2
                                                                                    ).toInt()
                                                                                val formattedday =
                                                                                    datevalue.substring(
                                                                                        3,
                                                                                        5
                                                                                    ).toInt()
                                                                                val formattedyear =
                                                                                    datevalue.substring(
                                                                                        6
                                                                                    ).toInt()
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday < currentDay && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Day Please Choose a Different Day")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth < currentMonth && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear.equals(
                                                                                        currentYear
                                                                                    )
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Month Please Choose a Different Month")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (formattedmonth.equals(
                                                                                        currentMonth
                                                                                    ) && formattedday.equals(
                                                                                        currentDay
                                                                                    ) && formattedyear < currentYear
                                                                                ) {
                                                                                    AlertDialog.Builder(
                                                                                        this@AppointmentPage
                                                                                    )
                                                                                        .setTitle("Invalid Form")
                                                                                        .setMessage("Invalid Year Please Choose a Different Year")
                                                                                        .setPositiveButton(
                                                                                            "Ok"
                                                                                        ) { _, _ ->
                                                                                        }
                                                                                        .show()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true

                                                                                }
                                                                                if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                    val selectedDateCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                time =
                                                                                                    user_selectedDate?.let {
                                                                                                        dateFormat.parse(
                                                                                                            it
                                                                                                        )
                                                                                                    }!!
                                                                                            }
                                                                                    val timeFormatter: DateTimeFormatter =
                                                                                        DateTimeFormatter.ofPattern(
                                                                                            "h:mm a"
                                                                                        )
                                                                                    val selectedTime =
                                                                                        LocalTime.parse(
                                                                                            user_selectedTime,
                                                                                            timeFormatter
                                                                                        )
                                                                                    val selectedHour =
                                                                                        selectedTime.hour
                                                                                    val selectedMinute =
                                                                                        selectedTime.minute


                                                                                    val selectedTimeCalendar =
                                                                                        Calendar.getInstance()
                                                                                            .apply {
                                                                                                set(
                                                                                                    Calendar.HOUR_OF_DAY,
                                                                                                    selectedHour
                                                                                                )
                                                                                                set(
                                                                                                    Calendar.MINUTE,
                                                                                                    selectedMinute
                                                                                                )
                                                                                            }
                                                                                    if (selectedDateCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ) ||
                                                                                        (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                            Calendar.getInstance()
                                                                                        ))
                                                                                    ) {
                                                                                        showInvalidTimeAlert1()
                                                                                        submitbtn.isEnabled =
                                                                                            true
                                                                                        submitbtn.isClickable =
                                                                                            true
                                                                                    } else {
                                                                                        val user =
                                                                                            hashMapOf(
                                                                                                "adultvaccine" to adultvaccine_input,
                                                                                                "childvaccine" to childvaccine_input,
                                                                                                "date" to hiddendate_input,
                                                                                                "location" to location_finalinput,
                                                                                                "pkIdentifier" to pkIdentifier,
                                                                                                "time" to hiddentime_input,
                                                                                                "appointmentKey" to uniqueString
                                                                                            )
                                                                                        collectionReference.whereEqualTo(
                                                                                            "pkIdentifier",
                                                                                            pkIdentifier
                                                                                        )
                                                                                            .get()
                                                                                            .addOnSuccessListener { querySnapshot ->
                                                                                                for (document in querySnapshot) {
                                                                                                    val documentID =
                                                                                                        document.id
                                                                                                    vaccination_count++
                                                                                                    val updates =
                                                                                                        hashMapOf<String, Any>(
                                                                                                            "vaccination_count" to vaccination_count.toString()
                                                                                                        )
                                                                                                    val editor =
                                                                                                        sharedPreferences.edit()
                                                                                                    editor.putString(
                                                                                                        "vaccination_count",
                                                                                                        vaccination_count.toString()
                                                                                                    )
                                                                                                    editor.apply()
                                                                                                    val existingData =
                                                                                                        document.data
                                                                                                    val docRef =
                                                                                                        collectionReference.document(
                                                                                                            documentID
                                                                                                        )
                                                                                                    val appointmentList =
                                                                                                        existingData?.get(
                                                                                                            "appointmentList"
                                                                                                        ) as? MutableMap<String, Any>
                                                                                                            ?: mutableMapOf()
                                                                                                    appointmentList[vaccination_count.toString()] =
                                                                                                        uniqueString
                                                                                                    docRef.update(
                                                                                                        "appointmentList",
                                                                                                        appointmentList
                                                                                                    )
                                                                                                    docRef.update(
                                                                                                        updates
                                                                                                    )
                                                                                                        .addOnSuccessListener {
                                                                                                            docRef.collection(
                                                                                                                "appointments"
                                                                                                            )
                                                                                                                .add(
                                                                                                                    user
                                                                                                                )
                                                                                                                .addOnCompleteListener {
                                                                                                                    AlertDialog.Builder(
                                                                                                                        this
                                                                                                                    )
                                                                                                                        .setTitle(
                                                                                                                            "Appointment has been Scheduled"
                                                                                                                        )
                                                                                                                        .setMessage(
                                                                                                                            "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                        )
                                                                                                                        .setPositiveButton(
                                                                                                                            "Ok"
                                                                                                                        ) { _, _ ->
                                                                                                                            val intent =
                                                                                                                                Intent(
                                                                                                                                    this,
                                                                                                                                    AppointmentPage::class.java
                                                                                                                                )
                                                                                                                            startActivity(
                                                                                                                                intent
                                                                                                                            )
                                                                                                                        }
                                                                                                                        .show()
                                                                                                                    val messageContent =
                                                                                                                        "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                    sendEmail(
                                                                                                                        email,
                                                                                                                        messageContent
                                                                                                                    )
                                                                                                                    submitbtn.isEnabled =
                                                                                                                        true
                                                                                                                    submitbtn.isClickable =
                                                                                                                        true
                                                                                                                }

                                                                                                        }


                                                                                                }
                                                                                            }
                                                                                    }


                                                                                }
                                                                            }
                                                                        }


                                                                    }
                                                            }
                                                    }
                                                }
                                                else {
                                                    if (datevalue.length == 8) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                1
                                                            ).toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                2,
                                                                3
                                                            ).toInt()
                                                        val formattedyear =
                                                            datevalue.substring(
                                                                4
                                                            ).toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute
                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                                submitbtn.isEnabled =
                                                                    true
                                                                submitbtn.isClickable =
                                                                    true
                                                            } else {
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true


                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }

                                                    }
                                                    if (datevalue.length == 9) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                1
                                                            ).toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                2,
                                                                4
                                                            ).toInt()
                                                        val formattedyear =
                                                            datevalue.substring(
                                                                5
                                                            ).toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }

                                                        if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute


                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                                submitbtn.isEnabled =
                                                                    true
                                                                submitbtn.isClickable =
                                                                    true
                                                            } else {
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true

                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }
                                                    }
                                                    if (datevalue.length == 10) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                2
                                                            ).toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                3,
                                                                5
                                                            ).toInt()
                                                        val formattedyear =
                                                            datevalue.substring(
                                                                6
                                                            ).toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(
                                                                this@AppointmentPage
                                                            )
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton(
                                                                    "Ok"
                                                                ) { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (validchildvaccineinput && validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute


                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                                submitbtn.isEnabled =
                                                                    true
                                                                submitbtn.isClickable =
                                                                    true
                                                            } else {
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true
                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }
                                                    }
                                                }

                                            }
                                    }
                                }


                        }
                        if (child_state.equals("FALSE")) {
                            println("Child Check : " + child_state)
                            collectionReference.whereEqualTo("pkIdentifier", pkIdentifier)
                                .get().addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot) {
                                        val documentID = document.id
                                        println("Document ID : " + documentID)
                                        firestore.collection("users/$documentID/appointments")
                                            .get()
                                            .addOnSuccessListener { appointmentQuerySnapshot ->
                                                var adultVaccineAppointmentExists = false
                                                var adultVaccineTaken = false
                                                if (!appointmentQuerySnapshot.isEmpty) {
                                                    for (appointment in appointmentQuerySnapshot) {
                                                        val vaccine =
                                                            appointment.getString("adultvaccine")
                                                        println("Vaccine" + vaccine)
                                                        if (vaccine == adultvaccine_input) {
                                                            adultVaccineAppointmentExists = true
                                                        }
                                                        if (adultVaccineAppointmentExists) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Vaccine")
                                                                .setMessage("Please be advised, Adult Vaccine is already scheduled, Kindly Choose Another Vaccine")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled = true
                                                            submitbtn.isClickable = true
                                                        }
                                                        else {
                                                            firestore.collection("users/$documentID/adultCompletedAppointments")
                                                                .whereEqualTo(
                                                                    "adultvaccine",
                                                                    adultvaccine_input
                                                                )
                                                                .get()
                                                                .addOnSuccessListener { adultQuerySnapshot ->
                                                                    if (adultQuerySnapshot.size() > 0) {
                                                                        adultVaccineTaken = true
                                                                    } else if (adultVaccineTaken) {
                                                                        AlertDialog.Builder(this@AppointmentPage)
                                                                            .setTitle("Invalid Vaccine")
                                                                            .setMessage("Please be advised, Adult Vaccine is already taken, Kindly Choose Another Vaccine")
                                                                            .setPositiveButton("Ok") { _, _ ->
                                                                            }
                                                                            .show()
                                                                        submitbtn.isEnabled = true
                                                                        submitbtn.isClickable = true
                                                                    } else {
                                                                        if (adultvaccine_input.isEmpty()) {
                                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                                .setTitle("Invalid Vaccine")
                                                                                .setMessage("No Vaccine is Selected, Kindly choose a vaccine")
                                                                                .setPositiveButton("Ok") { _, _ ->
                                                                                }
                                                                                .show()
                                                                            submitbtn.isEnabled = true
                                                                            submitbtn.isClickable = true
                                                                        }
                                                                        if (datevalue.length == 8) {
                                                                            val formattedmonth =
                                                                                datevalue.substring(
                                                                                    0,
                                                                                    1
                                                                                )
                                                                                    .toInt()
                                                                            val formattedday =
                                                                                datevalue.substring(
                                                                                    2,
                                                                                    3
                                                                                )
                                                                                    .toInt()
                                                                            val formattedyear =
                                                                                datevalue.substring(4)
                                                                                    .toInt()
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday < currentDay && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Day Please Choose a Different Day")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth < currentMonth && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Month Please Choose a Different Month")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear < currentYear
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Year Please Choose a Different Year")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                val selectedDateCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            time =
                                                                                                user_selectedDate?.let {
                                                                                                    dateFormat.parse(
                                                                                                        it
                                                                                                    )
                                                                                                }!!
                                                                                        }
                                                                                val timeFormatter: DateTimeFormatter =
                                                                                    DateTimeFormatter.ofPattern(
                                                                                        "h:mm a"
                                                                                    )
                                                                                val selectedTime =
                                                                                    LocalTime.parse(
                                                                                        user_selectedTime,
                                                                                        timeFormatter
                                                                                    )
                                                                                val selectedHour =
                                                                                    selectedTime.hour
                                                                                val selectedMinute =
                                                                                    selectedTime.minute


                                                                                val selectedTimeCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            set(
                                                                                                Calendar.HOUR_OF_DAY,
                                                                                                selectedHour
                                                                                            )
                                                                                            set(
                                                                                                Calendar.MINUTE,
                                                                                                selectedMinute
                                                                                            )
                                                                                        }
                                                                                if (selectedDateCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ) ||
                                                                                    (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ))
                                                                                ) {
                                                                                    showInvalidTimeAlert1()
                                                                                } else {

                                                                                    println("Fetch Data : ")
                                                                                    val user =
                                                                                        hashMapOf(
                                                                                            "adultvaccine" to adultvaccine_input,
                                                                                            "childvaccine" to childvaccine_input,
                                                                                            "date" to hiddendate_input,
                                                                                            "location" to location_finalinput,
                                                                                            "pkIdentifier" to pkIdentifier,
                                                                                            "time" to hiddentime_input,
                                                                                            "appointmentKey" to uniqueString
                                                                                        )
                                                                                    collectionReference.whereEqualTo(
                                                                                        "pkIdentifier",
                                                                                        pkIdentifier
                                                                                    )
                                                                                        .get()
                                                                                        .addOnSuccessListener { querySnapshot ->
                                                                                            for (document in querySnapshot) {
                                                                                                val documentID =
                                                                                                    document.id
                                                                                                vaccination_count++
                                                                                                val updates =
                                                                                                    hashMapOf<String, Any>(
                                                                                                        "vaccination_count" to vaccination_count.toString()
                                                                                                    )
                                                                                                val editor =
                                                                                                    sharedPreferences.edit()
                                                                                                editor.putString(
                                                                                                    "vaccination_count",
                                                                                                    vaccination_count.toString()
                                                                                                )
                                                                                                editor.apply()
                                                                                                val existingData =
                                                                                                    document.data
                                                                                                val docRef =
                                                                                                    collectionReference.document(
                                                                                                        documentID
                                                                                                    )
                                                                                                val appointmentList =
                                                                                                    existingData?.get(
                                                                                                        "appointmentList"
                                                                                                    ) as? MutableMap<String, Any>
                                                                                                        ?: mutableMapOf()
                                                                                                appointmentList[vaccination_count.toString()] =
                                                                                                    uniqueString
                                                                                                docRef.update(
                                                                                                    "appointmentList",
                                                                                                    appointmentList
                                                                                                )
                                                                                                docRef.update(
                                                                                                    updates
                                                                                                )
                                                                                                    .addOnSuccessListener {
                                                                                                        docRef.collection(
                                                                                                            "appointments"
                                                                                                        )
                                                                                                            .add(
                                                                                                                user
                                                                                                            )
                                                                                                            .addOnCompleteListener {
                                                                                                                AlertDialog.Builder(
                                                                                                                    this
                                                                                                                )
                                                                                                                    .setTitle(
                                                                                                                        "Appointment has been Scheduled"
                                                                                                                    )
                                                                                                                    .setMessage(
                                                                                                                        "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                    )
                                                                                                                    .setPositiveButton(
                                                                                                                        "Ok"
                                                                                                                    ) { _, _ ->
                                                                                                                        val intent =
                                                                                                                            Intent(
                                                                                                                                this,
                                                                                                                                AppointmentPage::class.java
                                                                                                                            )
                                                                                                                        startActivity(
                                                                                                                            intent
                                                                                                                        )
                                                                                                                    }
                                                                                                                    .show()
                                                                                                                val messageContent =
                                                                                                                    "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                sendEmail(
                                                                                                                    email,
                                                                                                                    messageContent
                                                                                                                )
                                                                                                                submitbtn.isEnabled =
                                                                                                                    true
                                                                                                                submitbtn.isClickable =
                                                                                                                    true

                                                                                                            }

                                                                                                    }


                                                                                            }
                                                                                        }
                                                                                }


                                                                            }

                                                                        }
                                                                        if (datevalue.length == 9) {
                                                                            val formattedmonth =
                                                                                datevalue.substring(
                                                                                    0,
                                                                                    1
                                                                                )
                                                                                    .toInt()
                                                                            val formattedday =
                                                                                datevalue.substring(
                                                                                    2,
                                                                                    4
                                                                                )
                                                                                    .toInt()
                                                                            val formattedyear =
                                                                                datevalue.substring(5)
                                                                                    .toInt()
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday < currentDay && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Day Please Choose a Different Day")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth < currentMonth && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Month Please Choose a Different Month")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear < currentYear
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Year Please Choose a Different Year")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                val selectedDateCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            time =
                                                                                                user_selectedDate?.let {
                                                                                                    dateFormat.parse(
                                                                                                        it
                                                                                                    )
                                                                                                }!!
                                                                                        }
                                                                                val timeFormatter: DateTimeFormatter =
                                                                                    DateTimeFormatter.ofPattern(
                                                                                        "h:mm a"
                                                                                    )
                                                                                val selectedTime =
                                                                                    LocalTime.parse(
                                                                                        user_selectedTime,
                                                                                        timeFormatter
                                                                                    )
                                                                                val selectedHour =
                                                                                    selectedTime.hour
                                                                                val selectedMinute =
                                                                                    selectedTime.minute
                                                                                val selectedTimeCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            set(
                                                                                                Calendar.HOUR_OF_DAY,
                                                                                                selectedHour
                                                                                            )
                                                                                            set(
                                                                                                Calendar.MINUTE,
                                                                                                selectedMinute
                                                                                            )
                                                                                        }
                                                                                if (selectedDateCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ) ||
                                                                                    (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ))
                                                                                ) {
                                                                                    showInvalidTimeAlert1()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true
                                                                                } else {
                                                                                    val user =
                                                                                        hashMapOf(
                                                                                            "adultvaccine" to adultvaccine_input,
                                                                                            "childvaccine" to childvaccine_input,
                                                                                            "date" to hiddendate_input,
                                                                                            "location" to location_finalinput,
                                                                                            "pkIdentifier" to pkIdentifier,
                                                                                            "time" to hiddentime_input,
                                                                                            "appointmentKey" to uniqueString
                                                                                        )
                                                                                    collectionReference.whereEqualTo(
                                                                                        "pkIdentifier",
                                                                                        pkIdentifier
                                                                                    )
                                                                                        .get()
                                                                                        .addOnSuccessListener { querySnapshot ->
                                                                                            for (document in querySnapshot) {
                                                                                                val documentID =
                                                                                                    document.id
                                                                                                vaccination_count++
                                                                                                val updates =
                                                                                                    hashMapOf<String, Any>(
                                                                                                        "vaccination_count" to vaccination_count.toString()
                                                                                                    )
                                                                                                val editor =
                                                                                                    sharedPreferences.edit()
                                                                                                editor.putString(
                                                                                                    "vaccination_count",
                                                                                                    vaccination_count.toString()
                                                                                                )
                                                                                                editor.apply()
                                                                                                val existingData =
                                                                                                    document.data
                                                                                                val docRef =
                                                                                                    collectionReference.document(
                                                                                                        documentID
                                                                                                    )
                                                                                                val appointmentList =
                                                                                                    existingData?.get(
                                                                                                        "appointmentList"
                                                                                                    ) as? MutableMap<String, Any>
                                                                                                        ?: mutableMapOf()
                                                                                                appointmentList[vaccination_count.toString()] =
                                                                                                    uniqueString
                                                                                                docRef.update(
                                                                                                    "appointmentList",
                                                                                                    appointmentList
                                                                                                )
                                                                                                docRef.update(
                                                                                                    updates
                                                                                                )
                                                                                                    .addOnSuccessListener {
                                                                                                        docRef.collection(
                                                                                                            "appointments"
                                                                                                        )
                                                                                                            .add(
                                                                                                                user
                                                                                                            )
                                                                                                            .addOnCompleteListener {
                                                                                                                AlertDialog.Builder(
                                                                                                                    this
                                                                                                                )
                                                                                                                    .setTitle(
                                                                                                                        "Appointment has been Scheduled"
                                                                                                                    )
                                                                                                                    .setMessage(
                                                                                                                        "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                    )
                                                                                                                    .setPositiveButton(
                                                                                                                        "Ok"
                                                                                                                    ) { _, _ ->
                                                                                                                        val intent =
                                                                                                                            Intent(
                                                                                                                                this,
                                                                                                                                AppointmentPage::class.java
                                                                                                                            )
                                                                                                                        startActivity(
                                                                                                                            intent
                                                                                                                        )
                                                                                                                    }
                                                                                                                    .show()
                                                                                                                val messageContent =
                                                                                                                    "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                sendEmail(
                                                                                                                    email,
                                                                                                                    messageContent
                                                                                                                )
                                                                                                                submitbtn.isEnabled =
                                                                                                                    true
                                                                                                                submitbtn.isClickable =
                                                                                                                    true


                                                                                                            }

                                                                                                    }


                                                                                            }
                                                                                        }
                                                                                }


                                                                            }

                                                                        }
                                                                        if (datevalue.length == 10) {
                                                                            val formattedmonth =
                                                                                datevalue.substring(
                                                                                    0,
                                                                                    2
                                                                                )
                                                                                    .toInt()
                                                                            val formattedday =
                                                                                datevalue.substring(
                                                                                    3,
                                                                                    5
                                                                                )
                                                                                    .toInt()
                                                                            val formattedyear =
                                                                                datevalue.substring(6)
                                                                                    .toInt()
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday < currentDay && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Day Please Choose a Different Day")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth < currentMonth && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear.equals(
                                                                                    currentYear
                                                                                )
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Month Please Choose a Different Month")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (formattedmonth.equals(
                                                                                    currentMonth
                                                                                ) && formattedday.equals(
                                                                                    currentDay
                                                                                ) && formattedyear < currentYear
                                                                            ) {
                                                                                AlertDialog.Builder(this@AppointmentPage)
                                                                                    .setTitle("Invalid Form")
                                                                                    .setMessage("Invalid Year Please Choose a Different Year")
                                                                                    .setPositiveButton("Ok") { _, _ ->
                                                                                    }
                                                                                    .show()
                                                                                submitbtn.isEnabled =
                                                                                    true
                                                                                submitbtn.isClickable =
                                                                                    true

                                                                            }
                                                                            if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                                                val selectedDateCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            time =
                                                                                                user_selectedDate?.let {
                                                                                                    dateFormat.parse(
                                                                                                        it
                                                                                                    )
                                                                                                }!!
                                                                                        }
                                                                                val timeFormatter: DateTimeFormatter =
                                                                                    DateTimeFormatter.ofPattern(
                                                                                        "h:mm a"
                                                                                    )
                                                                                val selectedTime =
                                                                                    LocalTime.parse(
                                                                                        user_selectedTime,
                                                                                        timeFormatter
                                                                                    )
                                                                                val selectedHour =
                                                                                    selectedTime.hour
                                                                                val selectedMinute =
                                                                                    selectedTime.minute


                                                                                val selectedTimeCalendar =
                                                                                    Calendar.getInstance()
                                                                                        .apply {
                                                                                            set(
                                                                                                Calendar.HOUR_OF_DAY,
                                                                                                selectedHour
                                                                                            )
                                                                                            set(
                                                                                                Calendar.MINUTE,
                                                                                                selectedMinute
                                                                                            )
                                                                                        }
                                                                                if (selectedDateCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ) ||
                                                                                    (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                                        Calendar.getInstance()
                                                                                    ))
                                                                                ) {
                                                                                    showInvalidTimeAlert1()
                                                                                    submitbtn.isEnabled =
                                                                                        true
                                                                                    submitbtn.isClickable =
                                                                                        true
                                                                                } else {
                                                                                    val user =
                                                                                        hashMapOf(
                                                                                            "adultvaccine" to adultvaccine_input,
                                                                                            "childvaccine" to childvaccine_input,
                                                                                            "date" to hiddendate_input,
                                                                                            "location" to location_finalinput,
                                                                                            "pkIdentifier" to pkIdentifier,
                                                                                            "time" to hiddentime_input,
                                                                                            "appointmentKey" to uniqueString
                                                                                        )
                                                                                    collectionReference.whereEqualTo(
                                                                                        "pkIdentifier",
                                                                                        pkIdentifier
                                                                                    )
                                                                                        .get()
                                                                                        .addOnSuccessListener { querySnapshot ->
                                                                                            for (document in querySnapshot) {
                                                                                                val documentID =
                                                                                                    document.id
                                                                                                vaccination_count++
                                                                                                val updates =
                                                                                                    hashMapOf<String, Any>(
                                                                                                        "vaccination_count" to vaccination_count.toString()
                                                                                                    )
                                                                                                val editor =
                                                                                                    sharedPreferences.edit()
                                                                                                editor.putString(
                                                                                                    "vaccination_count",
                                                                                                    vaccination_count.toString()
                                                                                                )
                                                                                                editor.apply()
                                                                                                val existingData =
                                                                                                    document.data
                                                                                                val docRef =
                                                                                                    collectionReference.document(
                                                                                                        documentID
                                                                                                    )
                                                                                                val appointmentList =
                                                                                                    existingData?.get(
                                                                                                        "appointmentList"
                                                                                                    ) as? MutableMap<String, Any>
                                                                                                        ?: mutableMapOf()
                                                                                                appointmentList[vaccination_count.toString()] =
                                                                                                    uniqueString
                                                                                                docRef.update(
                                                                                                    "appointmentList",
                                                                                                    appointmentList
                                                                                                )
                                                                                                docRef.update(
                                                                                                    updates
                                                                                                )
                                                                                                    .addOnSuccessListener {
                                                                                                        docRef.collection(
                                                                                                            "appointments"
                                                                                                        )
                                                                                                            .add(
                                                                                                                user
                                                                                                            )
                                                                                                            .addOnCompleteListener {
                                                                                                                AlertDialog.Builder(
                                                                                                                    this
                                                                                                                )
                                                                                                                    .setTitle(
                                                                                                                        "Appointment has been Scheduled"
                                                                                                                    )
                                                                                                                    .setMessage(
                                                                                                                        "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                                    )
                                                                                                                    .setPositiveButton(
                                                                                                                        "Ok"
                                                                                                                    ) { _, _ ->
                                                                                                                        val intent =
                                                                                                                            Intent(
                                                                                                                                this,
                                                                                                                                AppointmentPage::class.java
                                                                                                                            )
                                                                                                                        startActivity(
                                                                                                                            intent
                                                                                                                        )
                                                                                                                    }
                                                                                                                    .show()
                                                                                                                val messageContent =
                                                                                                                    "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                                                sendEmail(
                                                                                                                    email,
                                                                                                                    messageContent
                                                                                                                )
                                                                                                                submitbtn.isEnabled =
                                                                                                                    true
                                                                                                                submitbtn.isClickable =
                                                                                                                    true


                                                                                                            }

                                                                                                    }


                                                                                            }
                                                                                        }
                                                                                }


                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }
                                                else{
                                                    if (datevalue.length == 8) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                1
                                                            )
                                                                .toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                2,
                                                                3
                                                            )
                                                                .toInt()
                                                        val formattedyear =
                                                            datevalue.substring(4)
                                                                .toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute


                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                            } else {

                                                                println("Fetch Data : ")
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true

                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }

                                                    }
                                                    if (datevalue.length == 9) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                1
                                                            )
                                                                .toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                2,
                                                                4
                                                            )
                                                                .toInt()
                                                        val formattedyear =
                                                            datevalue.substring(5)
                                                                .toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute
                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                                submitbtn.isEnabled =
                                                                    true
                                                                submitbtn.isClickable =
                                                                    true
                                                            } else {
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true


                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }

                                                    }
                                                    if (datevalue.length == 10) {
                                                        val formattedmonth =
                                                            datevalue.substring(
                                                                0,
                                                                2
                                                            )
                                                                .toInt()
                                                        val formattedday =
                                                            datevalue.substring(
                                                                3,
                                                                5
                                                            )
                                                                .toInt()
                                                        val formattedyear =
                                                            datevalue.substring(6)
                                                                .toInt()
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday < currentDay && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Day Please Choose a Different Day")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth < currentMonth && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear.equals(
                                                                currentYear
                                                            )
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Month Please Choose a Different Month")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (formattedmonth.equals(
                                                                currentMonth
                                                            ) && formattedday.equals(
                                                                currentDay
                                                            ) && formattedyear < currentYear
                                                        ) {
                                                            AlertDialog.Builder(this@AppointmentPage)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Invalid Year Please Choose a Different Year")
                                                                .setPositiveButton("Ok") { _, _ ->
                                                                }
                                                                .show()
                                                            submitbtn.isEnabled =
                                                                true
                                                            submitbtn.isClickable =
                                                                true

                                                        }
                                                        if (validadultvaccineinput && validlocation && !user_selectedDate.isNullOrBlank() && !user_selectedTime.isNullOrBlank()) {
                                                            val selectedDateCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        time =
                                                                            user_selectedDate?.let {
                                                                                dateFormat.parse(
                                                                                    it
                                                                                )
                                                                            }!!
                                                                    }
                                                            val timeFormatter: DateTimeFormatter =
                                                                DateTimeFormatter.ofPattern(
                                                                    "h:mm a"
                                                                )
                                                            val selectedTime =
                                                                LocalTime.parse(
                                                                    user_selectedTime,
                                                                    timeFormatter
                                                                )
                                                            val selectedHour =
                                                                selectedTime.hour
                                                            val selectedMinute =
                                                                selectedTime.minute


                                                            val selectedTimeCalendar =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            Calendar.HOUR_OF_DAY,
                                                                            selectedHour
                                                                        )
                                                                        set(
                                                                            Calendar.MINUTE,
                                                                            selectedMinute
                                                                        )
                                                                    }
                                                            if (selectedDateCalendar.before(
                                                                    Calendar.getInstance()
                                                                ) ||
                                                                (selectedDateCalendar == Calendar.getInstance() && selectedTimeCalendar.before(
                                                                    Calendar.getInstance()
                                                                ))
                                                            ) {
                                                                showInvalidTimeAlert1()
                                                                submitbtn.isEnabled =
                                                                    true
                                                                submitbtn.isClickable =
                                                                    true
                                                            } else {
                                                                val user =
                                                                    hashMapOf(
                                                                        "adultvaccine" to adultvaccine_input,
                                                                        "childvaccine" to childvaccine_input,
                                                                        "date" to hiddendate_input,
                                                                        "location" to location_finalinput,
                                                                        "pkIdentifier" to pkIdentifier,
                                                                        "time" to hiddentime_input,
                                                                        "appointmentKey" to uniqueString
                                                                    )
                                                                collectionReference.whereEqualTo(
                                                                    "pkIdentifier",
                                                                    pkIdentifier
                                                                )
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        for (document in querySnapshot) {
                                                                            val documentID =
                                                                                document.id
                                                                            vaccination_count++
                                                                            val updates =
                                                                                hashMapOf<String, Any>(
                                                                                    "vaccination_count" to vaccination_count.toString()
                                                                                )
                                                                            val editor =
                                                                                sharedPreferences.edit()
                                                                            editor.putString(
                                                                                "vaccination_count",
                                                                                vaccination_count.toString()
                                                                            )
                                                                            editor.apply()
                                                                            val existingData =
                                                                                document.data
                                                                            val docRef =
                                                                                collectionReference.document(
                                                                                    documentID
                                                                                )
                                                                            val appointmentList =
                                                                                existingData?.get(
                                                                                    "appointmentList"
                                                                                ) as? MutableMap<String, Any>
                                                                                    ?: mutableMapOf()
                                                                            appointmentList[vaccination_count.toString()] =
                                                                                uniqueString
                                                                            docRef.update(
                                                                                "appointmentList",
                                                                                appointmentList
                                                                            )
                                                                            docRef.update(
                                                                                updates
                                                                            )
                                                                                .addOnSuccessListener {
                                                                                    docRef.collection(
                                                                                        "appointments"
                                                                                    )
                                                                                        .add(
                                                                                            user
                                                                                        )
                                                                                        .addOnCompleteListener {
                                                                                            AlertDialog.Builder(
                                                                                                this
                                                                                            )
                                                                                                .setTitle(
                                                                                                    "Appointment has been Scheduled"
                                                                                                )
                                                                                                .setMessage(
                                                                                                    "Please Take note that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput"
                                                                                                )
                                                                                                .setPositiveButton(
                                                                                                    "Ok"
                                                                                                ) { _, _ ->
                                                                                                    val intent =
                                                                                                        Intent(
                                                                                                            this,
                                                                                                            AppointmentPage::class.java
                                                                                                        )
                                                                                                    startActivity(
                                                                                                        intent
                                                                                                    )
                                                                                                }
                                                                                                .show()
                                                                                            val messageContent =
                                                                                                "Friendly Advisory from Immunicare this is to inform you that your Vaccine is Scheduled at $hiddendate_input at $hiddentime_input in $location_finalinput\""
                                                                                            sendEmail(
                                                                                                email,
                                                                                                messageContent
                                                                                            )
                                                                                            submitbtn.isEnabled =
                                                                                                true
                                                                                            submitbtn.isClickable =
                                                                                                true


                                                                                        }

                                                                                }


                                                                        }
                                                                    }
                                                            }


                                                        }
                                                    }
                                                }



                                            }





















                                    }
                                }
                        }
                    }

            } else {
                AlertDialog.Builder(this@AppointmentPage)
                    .setTitle("Reached Maximum Allowed Appointments")
                    .setMessage("Please Wait as you have already reached the maximum allowed number of Appointments")
                    .setPositiveButton("Ok") { _, _ ->
                    }
                    .show()
                submitbtn.isEnabled = true
                submitbtn.isClickable = true
            }

        }

    }


    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val hiddentime = findViewById<TextView>(R.id.hiddentime)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = formatTime(selectedHour, selectedMinute)
                hiddentime.text = selectedTime
                user_selectedTime = selectedTime
                if (selectedHour < 6 || selectedHour >= 18) {
                    showInvalidTimeAlert()
                }
            },
            hourOfDay,
            minute,
            false
        )
        timePickerDialog.show()
    }


    private fun formatTime(hour: Int, minute: Int): String {
        val amPM: String
        val formattedHour: Int

        if (hour < 12) {
            amPM = "AM"
            formattedHour = if (hour == 0) 12 else hour
        } else {
            amPM = "PM"
            formattedHour = if (hour == 12) 12 else hour - 12
        }

        val formattedMinute = if (minute < 10) "0$minute" else minute.toString()
        return "$formattedHour:$formattedMinute $amPM"
    }

    private fun showInvalidTimeAlert() {
        AlertDialog.Builder(this)
            .setTitle("Invalid Time")
            .setMessage("Please choose a time between 6:00 AM and 6:00 PM.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showInvalidTimeAlert1() {
        AlertDialog.Builder(this)
            .setTitle("Invalid Time")
            .setMessage("Selected date and time are in the past, please select again.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun generateUniqueRandomString(callback: (String) -> Unit) {
        val prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val prefix = "IM-A"
        val random = Random(System.currentTimeMillis())
        val chars = ('A'..'Z').toList() + ('0'..'9').toList()
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        collectionReference.whereEqualTo("pkIdentifier", pkidentifier).get()
            .addOnSuccessListener { querySnapshot ->
                for (i in querySnapshot) {
                    val x = i.id
                    val data_fromfirebase =
                        collectionReference.document(x).collection("appointments")
                    generateRandomString(data_fromfirebase, random, chars, prefix, callback)
                }
            }
    }

    private fun generateRandomString(
        collectionReference: CollectionReference,
        random: Random,
        chars: List<Char>,
        prefix: String,
        callback: (String) -> Unit
    ) {
        val randomString = (1..8)
            .map { chars[random.nextInt(chars.size)] }
            .joinToString("")

        collectionReference
            .whereEqualTo("appointmentKey", randomString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback("$prefix$randomString")
                } else {
                    generateRandomString(collectionReference, random, chars, prefix, callback)
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

    private fun chatInterface() {
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        if (isChatSessionManagerInitialized == false) {
            chatInterface.visibility = View.GONE
        } else {
            chatInterface.visibility = View.VISIBLE
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
                                                                        println("keytoRemove" + keytoRemove)
                                                                        println("List" + appointmentList)
                                                                        println("DocumentID" + appointmentDocumentID)
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

    private fun scrollRecyclerViewToBottom(adapter: AdapterforChatBot) {
        val chatbotRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        chatbotRecyclerView.post {
            chatbotRecyclerView.scrollToPosition(adapter.itemCount - 1)
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
                message.setFrom(InternetAddress("noreply.immunicare@gmail.com"))
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


}

