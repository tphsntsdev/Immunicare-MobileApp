package com.example.myapplication
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import java.util.Calendar
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CreateAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registeracct)
        AndroidThreeTen.init(application)
        FirebaseApp.initializeApp(this)
        setupCheckboxGroup()
        val allergyText: TextInputEditText = findViewById(R.id.allergyEditText)
        allergyText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val text = it.toString()
                    val lastChar = text.lastOrNull()
                    if (lastChar == ' ' || lastChar == '\n') { // Check for space or new line
                        val category = text.trim()
                        if (category.isNotEmpty()) {
                            addCategoryChip(category)
                            allergyText.text?.clear() // Clear input after adding
                        }
                    }
                }
            }
        })


        val address_input = findViewById<MaterialAutoCompleteTextView>(R.id.addressEditText)
        val back_btn = findViewById<TextView>(R.id.backbtn)
        address_input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(address_text: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(address_text: CharSequence?, start: Int, before: Int, count: Int) {
                val query = address_text.toString()
                fetchPredictions(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        val dropdown = findViewById<MaterialAutoCompleteTextView>(R.id.genderEditText)
        val items = arrayOf("Male","Female")
        val adapter1 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        dropdown.setAdapter(adapter1)
        back_btn.setOnClickListener {
            val intent = Intent(this,SignInActivity::class.java)
            startActivity(intent)
            finish()

        }
        val dob = findViewById<EditText>(R.id.dobEditText)
        val hiddenAge = findViewById<EditText>(R.id.hiddenAge)
        dob.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    dob.setText(dat)
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val age = (currentYear - year).toString()
                    hiddenAge.setText(age)

                },
                year,
                month,
                day

            )
            datePickerDialog.show()
        }
        val actionBar = supportActionBar
        actionBar?.hide()
        firstNameFocusListener()
        lastNameFocusListener()
        emailFocusListener()
        usernameFocusListener()
        PasswordFocusListener()
        PhoneNumberFocusListener()
        dobFocusListener()
        genderFocusListener()
        AddressFocusListener()
        val registerbutton = findViewById<Button>(R.id.button2)
        registerbutton.setOnClickListener {
            registerbutton.isEnabled = false
            registerbutton.isClickable = false
            submitForm()
        }
    }
    private fun addCategoryChip(category: String) {
        val categoryGroup: ChipGroup = findViewById(R.id.categoryGroup)
        val chip = LayoutInflater.from(this).inflate(R.layout.category_chip, categoryGroup, false) as Chip

        chip.text = category
        chip.contentDescription = category

        chip.setOnCloseIconClickListener {
            categoryGroup.removeView(chip)
        }


        categoryGroup.addView(chip)
    }
    private fun getAllChipValues(): List<String> {
        val categoryGroup: ChipGroup = findViewById(R.id.categoryGroup)
        val chipList = mutableListOf<String>()

        for (i in 0 until categoryGroup.childCount) {
            val chip = categoryGroup.getChildAt(i) as? Chip
            chip?.let {
                chipList.add(it.text.toString())
            }
        }

        return chipList
    }
    private fun fetchPredictions(query: String) {
        val address_input = findViewById<MaterialAutoCompleteTextView>(R.id.addressEditText)
        val apiKey = resources.getString(R.string.google_maps_key)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        val placesClient = Places.createClient(this)
        val autocompleteSessionToken = AutocompleteSessionToken.newInstance()
        val autocompleteRequest = FindAutocompletePredictionsRequest.builder()
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(autocompleteSessionToken)
            .setCountry("PH")
            .setQuery(query) // An empty query to start with
            .build()

        placesClient.findAutocompletePredictions(autocompleteRequest)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                val predictionStrings = predictions.map { it.getFullText(null).toString() }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, predictionStrings)
               address_input.setAdapter(adapter)
            }
            .addOnFailureListener { exception ->
            }
    }
    private fun firstNameFocusListener(){
        val firstName = findViewById<EditText>(R.id.f_nameEditText)
        firstName.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val firstNameContainer = findViewById<TextInputLayout>(R.id.f_nameEditTextContainer)
                firstNameContainer.helperText = validateFirstName()

            }
        }
    }
    private fun validateFirstName(): String? {
        val firstName = findViewById<EditText>(R.id.f_nameEditText)
        val firstNameVal = firstName.text.toString().trim()
        if (firstNameVal.isBlank()) {
            return "Please Input your First Name"
        }
        val regex = Regex("^[a-zA-Z]+( [a-zA-Z]+)?\$")
        if (!firstNameVal.matches(regex)) {
            return "Please Input Letters Only"
        }

        return null
    }
    private fun lastNameFocusListener(){
        val lastName = findViewById<EditText>(R.id.l_nameEditText)
        lastName.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val lastNameContainer = findViewById<TextInputLayout>(R.id.l_nameEditTextContainer)
                lastNameContainer.helperText = validatelastName()

            }
        }
    }
    private fun validatelastName(): String? {
        val lastName = findViewById<EditText>(R.id.l_nameEditText)
        val lastNameVal = lastName.text.toString().trim()
        if (lastNameVal.isBlank()) {
            return "Please Input your Last Name"
        }
        val regex = Regex("^[a-zA-Z]+$")
        if (!lastNameVal.matches(regex)) {
            return "Please Input Letters Only"
        }

        return null
    }

    private fun emailFocusListener(){
        val email = findViewById<EditText>(R.id.emailEditText)
        email.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val emailContainer = findViewById<TextInputLayout>(R.id.emailEditTextContainer)
                emailContainer.helperText = validateEmail()

            }
        }
    }
    private fun validateEmail() : String? {
        val email = findViewById<EditText>(R.id.emailEditText)
        val emailval = email.text.toString()

        if (emailval.isBlank()){
            return "Please Input your Email"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailval).matches()){
            return "Invalid Email Address"
        }
        return null
    }
    private fun usernameFocusListener(){
        val username = findViewById<EditText>(R.id.usernameEditText)
        username.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val usernameContainer = findViewById<TextInputLayout>(R.id.usernameEditTextContainer)
                usernameContainer.helperText = validateUsername()

            }
        }
    }
    private fun validateUsername() : String? {
        val username = findViewById<EditText>(R.id.usernameEditText)
        val usernameval = username.text.toString()
        if (usernameval.isBlank()){
            return "Please Input your Username"
        }
        return null
    }
    private fun PasswordFocusListener(){
        val password = findViewById<EditText>(R.id.passwordEditText)
        password.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val passwordContainer = findViewById<TextInputLayout>(R.id.passwordEditTextContainer)
                passwordContainer.helperText = validatePassword()

            }
        }
    }
    private fun validatePassword() : String? {
        val password = findViewById<EditText>(R.id.passwordEditText)
        val passwordval = password.text.toString()
        if (passwordval.isBlank()){
            return "Please Input your Password"
        }
        if(passwordval.length <= 8)
        {
            return "Minimum 8 Character Password"
        }
        if(!passwordval.matches(".*[A-Z].*".toRegex()))
        {
            return "Must Contain 1 Upper-case Character"
        }
        if(!passwordval.matches(".*[a-z].*".toRegex()))
        {
            return "Must Contain 1 Lower-case Character"
        }
        if(!passwordval.contains("[!@#\$%^&*()_+{}\\[\\]:;<>,.?~|\\-=/'\"]".toRegex()))
        {
            return "Must Contain 1 Special Character "
        }
        return null
    }
    private fun PhoneNumberFocusListener(){
        val phonenumber = findViewById<EditText>(R.id.p_numberEditText)
        phonenumber.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val phoneNumberContainer = findViewById<TextInputLayout>(R.id.p_numberEditTextContainer)
                phoneNumberContainer.helperText = validatePhoneNumber()

            }
        }
    }
    private fun validatePhoneNumber() : String? {
        val phonenumber = findViewById<EditText>(R.id.p_numberEditText)
        val phonenumberval = phonenumber.text.toString()
        if (phonenumberval.isBlank()){
            return "Please Input your Phone Number"
        }
        if (!phonenumberval.matches("\\b09[0-9]{9}\\b".toRegex())){
            return "Please Input a valid Phone Number starting with 09"
        }
        if(!phonenumberval.matches(".*[0-9].*".toRegex()))
        {
            return "Must be all Digits"
        }
        if(phonenumberval.length != 11)
        {
            return "Must be 11 Digits"
        }

        return null
    }
    private fun dobFocusListener(){
        val dob = findViewById<EditText>(R.id.dobEditText)
        dob.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val dobContainer = findViewById<TextInputLayout>(R.id.dobEditTextContainer)
                dobContainer.helperText = validateDOB()

            }
        }
    }
    private fun validateDOB() : String? {
        val hiddenAge = findViewById<EditText>(R.id.hiddenAge)
        val agevalue : String? = hiddenAge.text.toString().trim()
        if(agevalue.isNullOrBlank()){
            return "Please input a Valid Date"
        }
        val checkminor  = agevalue!!.toInt()
        if(checkminor < 18){
            AlertDialog.Builder(this@CreateAccountActivity)
                .setTitle("Invalid Age")
                .setMessage("Please Consult your Parents before creating an Account")
                .setPositiveButton("Ok"){ _,_ ->
                    // do nothing
                }
                .show()
        }
        return null
    }
    private fun genderFocusListener(){
        val dropdown = findViewById<MaterialAutoCompleteTextView>(R.id.genderEditText)
        dropdown.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val genderContainer = findViewById<TextInputLayout>(R.id.genderEditTextContainer)
                genderContainer.helperText = validateGender()

            }
        }
    }
    private fun validateGender() : String? {
        val dropdown = findViewById<MaterialAutoCompleteTextView>(R.id.genderEditText)
        val dropdown_val = dropdown.text.toString()
        if (dropdown_val.isBlank()){
            return "Please Input your Gender"
        }
        return null
    }
    private fun AddressFocusListener(){
        val address_val = findViewById<MaterialAutoCompleteTextView>(R.id.addressEditText)
        address_val.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val addressContainer = findViewById<TextInputLayout>(R.id.addressEditTextContainer)
                addressContainer.helperText = validateAddress()

            }
        }
    }
    private fun validateAddress() : String? {
        val user_address = findViewById<MaterialAutoCompleteTextView>(R.id.addressEditText)
        val addressval = user_address.text.toString()
        if (addressval.isBlank()){
            return "Please Input your Address"
        }
        return null
    }
    private fun validCheckValue() : Boolean {
        val selectedOption = getSelectedCheckbox()
        if(selectedOption == null){
            return false
        }
        return true
    }
    private fun setupCheckboxGroup() {
        val allergyYesChecker: MaterialCheckBox = findViewById(R.id.yes_option)
        val allergyNoChecker: MaterialCheckBox = findViewById(R.id.no_option)
        val allergyText: TextInputEditText = findViewById(R.id.allergyEditText)
        val chipvalues: ChipGroup  = findViewById(R.id.categoryGroup)
        val checkboxes = listOf(allergyYesChecker, allergyNoChecker)
        val textResult = findViewById<TextView>(R.id.checkbox_result)
        fun updateUI() {
            val selectedOption = getSelectedCheckbox()
            if (selectedOption == "Yes") {
                allergyText.isEnabled = true
            }
            else if (selectedOption == "No") {
                allergyText.isEnabled = false
                chipvalues.removeAllViews()
            }
            else{
                chipvalues.removeAllViews()
            }

        }
        checkboxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    textResult.text = ""
                    checkboxes.filter { it != checkbox }.forEach { it.isChecked = false }
                    updateUI()
                }
            }
        }
    }


    private fun getSelectedCheckbox(): String? {
        val allergyYesChecker: MaterialCheckBox = findViewById(R.id.yes_option)
        val allergyNoChecker: MaterialCheckBox = findViewById(R.id.no_option)

        return when {
            allergyYesChecker.isChecked -> "Yes"
            allergyNoChecker.isChecked -> "No"
            else -> null
        }
    }


    private fun submitForm() {
        val resultTextView: TextView = findViewById(R.id.checkbox_result)
        val registerbutton = findViewById<Button>(R.id.button2)
        val firstNameContainer = findViewById<TextInputLayout>(R.id.f_nameEditTextContainer)
        firstNameContainer.helperText = validateFirstName()
        val lastNameContainer = findViewById<TextInputLayout>(R.id.l_nameEditTextContainer)
        lastNameContainer.helperText = validatelastName()
        val emailContainer = findViewById<TextInputLayout>(R.id.emailEditTextContainer)
        emailContainer.helperText = validateEmail()
        val usernameContainer = findViewById<TextInputLayout>(R.id.usernameEditTextContainer)
        usernameContainer.helperText = validateUsername()
        val passwordContainer = findViewById<TextInputLayout>(R.id.passwordEditTextContainer)
        passwordContainer.helperText = validatePassword()
        val dobContainer = findViewById<TextInputLayout>(R.id.dobEditTextContainer)
        dobContainer.helperText = validateDOB()
        val phoneNumberContainer = findViewById<TextInputLayout>(R.id.p_numberEditTextContainer)
        phoneNumberContainer.helperText = validatePhoneNumber()
        val genderContainer = findViewById<TextInputLayout>(R.id.genderEditTextContainer)
        genderContainer.helperText = validateGender()
        val addressContainer = findViewById<TextInputLayout>(R.id.addressEditTextContainer)
        addressContainer.helperText = validateAddress()
        val validfirstName = firstNameContainer.helperText == null
        val validlastname = lastNameContainer.helperText == null
        val validemail = emailContainer.helperText == null
        val validusername = usernameContainer.helperText == null
        val validpassword = passwordContainer.helperText == null
        val validdob = dobContainer.helperText == null
        val validphonenumber = phoneNumberContainer.helperText == null
        val validgender = genderContainer.helperText == null
        val validaddress = addressContainer.helperText == null
        val hiddenAge = findViewById<EditText>(R.id.hiddenAge)
        val agevalue  = hiddenAge.text.toString().trim()
        val auth = FirebaseAuth.getInstance()
        val email = findViewById<TextInputEditText>(R.id.emailEditText)
        val allergyChecker = validCheckValue()
        val textResult = findViewById<TextView>(R.id.checkbox_result)
        val emailvalue = email.text.toString().trim()
        val selectedOption = getSelectedCheckbox()
        val chipvalues: ChipGroup  = findViewById(R.id.categoryGroup)

        if (!allergyChecker){
            AlertDialog.Builder(this)
                .setTitle("Invalid Form")
                .setMessage("Please Indicate if you have an Allergy or Not")
                .setPositiveButton("Ok") { _, _ ->
                }
                .show()
            textResult.text = "Please Ensure to Answer this Field"

        }
        if (selectedOption == "Yes" &&  chipvalues.childCount == 0){
            AlertDialog.Builder(this)
                .setTitle("Invalid Form")
                .setMessage("Kindly Specify your Allergy")
                .setPositiveButton("Ok") { _, _ ->
                }
                .show()
            textResult.text = "Kindly Specify your Allergy"
        }
        if (selectedOption != null &&  chipvalues.childCount >= 1){
            textResult.text = ""
        }
        if (agevalue.isNotEmpty()) {
            try {
                val checkminor = agevalue.toInt()
                if (validfirstName && validlastname && validemail && validusername && validpassword && validdob && validphonenumber && validgender && validaddress && checkminor > 18 && resultTextView.text.toString() == "" && allergyChecker) {
                    auth.fetchSignInMethodsForEmail(emailvalue).addOnCompleteListener { querySnapshot ->
                        if (querySnapshot.isSuccessful) {
                            val result = querySnapshot.result
                            if (result?.signInMethods?.isNotEmpty() == false) {
                                AlertDialog.Builder(this@CreateAccountActivity)
                                    .setTitle("Terms and Conditions")
                                    .setMessage("""
        1. Account Creation

        1.1 By creating an account with Immunicare, you agree to provide accurate and current information about yourself. This includes your name, contact information, and any other details required during the registration process.
        
        1.2 You are responsible for maintaining the confidentiality of your account credentials, including your username and password. You agree to notify us immediately of any unauthorized use of your account or any other breach of security.
        
        1.3 Immunicare reserves the right to suspend or terminate your account at any time, without prior notice, if we believe that you have violated these terms or engaged in any fraudulent or illegal activity.
        
        2. Privacy and Vaccine Information
        
        2.1 Immunicare is committed to protecting your privacy. We collect, store, and process your personal information in accordance with our Privacy Policy, which can be found on our website.
        
        2.2 By using Immunicare, you consent to the collection and use of your personal information, including vaccine-related data such as vaccination history, scheduled appointments, and appointment reminders. This information is used to provide you with personalized health services and to improve our platform's functionality.
        
        2.3 We do not disclose your vaccine information to third parties without your explicit consent, except where required by law or to fulfill our contractual obligations with healthcare providers.
        
        2.4 You have the right to access, update, or delete your personal information at any time. Please contact us if you wish to exercise these rights or have any questions regarding the handling of your data.
        
        3. Compliance with the Data Privacy Act of the Philippines
        
        3.1 Immunicare is committed to complying with the Data Privacy Act of 2012 (Republic Act No. 10173) and its implementing rules and regulations. We ensure that your personal information is processed in accordance with the principles of transparency, legitimate purpose, and proportionality.
        
        3.2 We will not process your personal data without your consent, unless such processing is necessary for the performance of a contract or compliance with a legal obligation.
        
        3.3 We have implemented reasonable and appropriate measures to protect your personal data from unauthorized access, disclosure, alteration, or destruction.
        
        3.4 You have the right to file a complaint with the National Privacy Commission (NPC) if you believe that your personal data has been mishandled or if your data protection rights have been violated. Contact details for the NPC can be found on their website.
        
        3.5 Our Privacy Policy details the specific types of data collected, purposes of processing, data retention periods, and your rights as a data subject under the Data Privacy Act. Please review our Privacy Policy for a comprehensive understanding of how we handle your data.

        4. Data Privacy Act of 2012 (Republic Act No. 10173)

        **Section 1. Short Title**

        This Act shall be known as the "Data Privacy Act of 2012."

        **Section 2. Declaration of Policy**

        It is the policy of the State to protect the fundamental human right of privacy, of communication while ensuring the free flow of information to promote innovation and growth.

        **Section 3. Definition of Terms**

        For the purposes of this Act, the following terms are defined:
        - (a) "Personal Information" refers to any information, whether recorded in material form or not, from which the identity of an individual is apparent or can reasonably be ascertained by an ordinary person.
        - (b) "Sensitive Personal Information" refers to personal information:
            - (1) about an individual's race, ethnic origin, marital status, age, color, and religious, philosophical or political affiliations;
            - (2) about an individual's health, education, genetic or sexual life of a person, or to any proceedings for any offense committed or alleged to have been committed by such person;
            - (3) issued by government agencies peculiar to an individual, such as the Social Security System (SSS) number, and passport number.
        - (c) "Processing" refers to any operation or any set of operations performed upon personal data, whether or not by automatic means, such as collection, recording, organization, storage, adaptation or alteration, retrieval, consultation, use, disclosure, dissemination or transmission, alignment or combination, blocking, erasure, or destruction.
        - (d) "Data Subject" refers to an individual whose personal data is processed.

        **Section 4. Scope and Application**

        This Act applies to the processing of personal information and sensitive personal information:
        - (a) by an individual or an organization engaged in commercial activities;
        - (b) that is personal information collected or processed through automated means or non-automated means.

        **Section 5. Principles of Data Privacy**

        The processing of personal information shall adhere to the following principles:
        - (a) Transparency: The data subject should be informed of the collection and processing of their personal data.
        - (b) Legitimate Purpose: Personal data should be collected and processed for legitimate purposes and not further processed in a way incompatible with those purposes.
        - (c) Proportionality: The collection of personal data should be adequate, relevant, and not excessive in relation to the purpose for which it is processed.

        **Section 6. Rights of Data Subjects**

        The data subject shall have the following rights:
        - (a) The right to be informed whether personal data pertaining to them shall be, are being, or have been processed;
        - (b) The right to access personal data that is processed;
        - (c) The right to correct or rectify inaccurate or incomplete personal data;
        - (d) The right to suspend, withdraw or order the blocking, removal, or destruction of personal data;
        - (e) The right to data portability.

        **Section 7. Obligations of Personal Information Controllers**

        Personal information controllers shall ensure compliance with the principles of data privacy and adopt appropriate measures for the protection of personal data, including:
        - (a) Implementing safeguards to ensure the security of personal data;
        - (b) Notifying the data subject and the National Privacy Commission in case of a data breach.

        **Section 8. National Privacy Commission**

        There is hereby created a National Privacy Commission (NPC) which shall be responsible for the implementation of this Act. The NPC shall have the following functions:
        - (a) To administer and implement the provisions of this Act;
        - (b) To monitor and ensure compliance with the provisions of this Act;
        - (c) To receive and investigate complaints from data subjects;
        - (d) To impose sanctions for violations of this Act.

        **Section 9. Penalties**

        Violations of this Act shall be subject to penalties which may include:
        - (a) Fines ranging from ₱500,000 to ₱5,000,000;
        - (b) Imprisonment ranging from 1 year to 6 years, or both, depending on the severity of the violation.

        **Section 10. Implementing Rules and Regulations**

        The National Privacy Commission shall issue the necessary rules and regulations for the effective implementation of this Act.

        **Section 11. Repealing Clause**

        All laws, decrees, orders, rules, and regulations, or parts thereof inconsistent with this Act are hereby repealed or modified accordingly.

        **Section 12. Effectivity**

        This Act shall take effect fifteen (15) days after its publication in the Official Gazette or in a newspaper of general circulation.

        5. Vaccine Appointments
        
        5.1 Immunicare offers the functionality to schedule vaccine appointments through our platform. The availability of appointments may vary based on location, vaccine availability, and healthcare provider schedules.
        
        5.2 By scheduling a vaccine appointment through Immunicare, you agree to provide accurate information about your availability and personal health details as required by the healthcare provider.
        
        5.3 Immunicare is not responsible for any changes or cancellations to vaccine appointments made by healthcare providers. We will make reasonable efforts to notify you of any changes in a timely manner.
        
        6. Limitation of Liability
        
        6.1 Immunicare makes every effort to ensure the accuracy and reliability of the information provided on our platform. However, we do not guarantee that the information is always up-to-date or error-free.
        
        6.2 You acknowledge and agree that your use of Immunicare is at your own risk. We shall not be liable for any direct, indirect, incidental, special, or consequential damages arising out of or in connection with your use of our services.
        
        7. Modifications to Terms
        
        7.1 Immunicare reserves the right to modify these terms and conditions at any time. Updated terms will be posted on our website, and it is your responsibility to review them periodically.
        
        7.2 Continued use of Immunicare after modifications indicates your acceptance of the updated terms. If you do not agree with the changes, you must cease using our services and close your account.
        """.trimIndent())
                                    .setPositiveButton("Agree") { _,_ ->
                                        fetchForm()
                                    }
                                    .setNegativeButton("Disagree") { dialog, _ ->
                                        dialog.cancel()

                                    }.show()
                                registerbutton.isEnabled = true
                                registerbutton.isClickable = true
                            }

                        }
                    }





                } else {
                    invalidForm()
                    registerbutton.isEnabled = true
                    registerbutton.isClickable = true
                }
            } catch (e: NumberFormatException) {
                AlertDialog.Builder(this@CreateAccountActivity)
                    .setTitle("Invalid Form")
                    .setMessage("Invalid age format, Please Try Again")
                    .setPositiveButton("Ok"){ _,_ ->
                    }
                    .show()
                registerbutton.isEnabled = true
                registerbutton.isClickable = true
            }
        } else {
            AlertDialog.Builder(this@CreateAccountActivity)
                .setTitle("Invalid Form")
                .setMessage("Age cannot be empty, Please Try Again")
                .setPositiveButton("Ok"){ _,_ ->
                }
                .show()
            registerbutton.isEnabled = true
            registerbutton.isClickable = true
        }
        }
    private fun fetchForm(){
        if (isNetworkAvailable(this)) {
            val appointmentList = mutableMapOf<String, Any>()
            val firestore = FirebaseFirestore.getInstance()
            val collectionReference = firestore.collection("users")
            val db = Firebase.firestore
            val user_instance = FirebaseAuth.getInstance()
            val dob = findViewById<TextInputEditText>(R.id.dobEditText)
            val phonenumber = findViewById<TextInputEditText>(R.id.p_numberEditText)
            val password = findViewById<TextInputEditText>(R.id.passwordEditText)
            val username = findViewById<TextInputEditText>(R.id.usernameEditText)
            val email = findViewById<TextInputEditText>(R.id.emailEditText)
            val lastName = findViewById<TextInputEditText>(R.id.l_nameEditText)
            val firstName = findViewById<TextInputEditText>(R.id.f_nameEditText)
            val hiddenAge = findViewById<EditText>(R.id.hiddenAge)
            val address_text = findViewById<MaterialAutoCompleteTextView>(R.id.addressEditText)
            val dropdown = findViewById<MaterialAutoCompleteTextView>(R.id.genderEditText)
            val chipList = getAllChipValues()

            val allergyChecker = getSelectedCheckbox()
            val dropdown_val = dropdown.text.toString().trim()
            val dobvalue = dob.text.toString().trim()
            val agevalue = hiddenAge.text.toString().trim()
            val phonenumbervalue = phonenumber.text.toString().trim()
            val passwordvalue = password.text.toString().trim()
            val usernamevalue = username.text.toString().trim()
            val address_value = address_text.text.toString().trim()
            val emailvalue = email.text.toString().trim()
            val lastNamevalue = lastName.text.toString().trim().lowercase().replaceFirstChar {if (it.isLowerCase()) it.titlecase() else it.toString() }
            val firstNamevalue = firstName.text.toString().trim().lowercase().replaceFirstChar {if (it.isLowerCase()) it.titlecase() else it.toString() }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dateNow = LocalDateTime.now()
            val formattedDateTime = dateNow.format(formatter)
            val finalacctcreated = formattedDateTime.toString()
            val salt = generateSalt()
            val encryptionHash = resources.getString(R.string.encryptionHash)
            val encryptionMethod = resources.getString(R.string.encryptionMethod)
            val spec: KeySpec = PBEKeySpec(passwordvalue.toCharArray(), salt.toByteArray(), 65536, 256)
            val secretKeyFactory = SecretKeyFactory.getInstance(encryptionHash)
            val secretKey: SecretKey = SecretKeySpec(secretKeyFactory.generateSecret(spec).encoded, encryptionMethod)
            val encryptionPadding = resources.getString(R.string.encryptionPadding)
            val encryptedPassword = encrypt(passwordvalue, secretKey, encryptionPadding)
            val anotherRandomString = generateRandomString(10)
            generateUniqueRandomString { uniqueString ->
               
                val randomString = uniqueString
                val user = hashMapOf(
                    "salt" to salt,
                    "firstName" to firstNamevalue,
                    "lastName" to lastNamevalue,
                    "email" to emailvalue,
                    "username" to usernamevalue,
                    "password" to encryptedPassword,
                    "age" to agevalue,
                    "phoneNumber" to phonenumbervalue,
                    "accountCreated" to finalacctcreated,
                    "pkIdentifier" to randomString,
                    "dateofbirth" to dobvalue,
                    "gender" to dropdown_val,
                    "address" to address_value,
                    "child_state" to "FALSE",
                    "vaccination_count" to "0",
                    "image_check" to "FALSE",
                    "childimage_check" to "FALSE",
                    "appointmentList" to appointmentList,
                    "passwordResetCode" to anotherRandomString,
                    "allergy_status" to allergyChecker,
                    "allergy_values" to chipList,
                )
                collectionReference.whereEqualTo("username", usernamevalue)
                    .get().addOnSuccessListener { querySnapshot_user ->
                                                if (querySnapshot_user.isEmpty) {
                                                    collectionReference.whereEqualTo(
                                                        "password",
                                                        passwordvalue
                                                    ).get().addOnSuccessListener { querySnapshot ->
                                                        if (querySnapshot.isEmpty) {

                                                            user_instance.createUserWithEmailAndPassword(
                                                                emailvalue,
                                                                passwordvalue
                                                            ).addOnCompleteListener(
                                                                CreateAccountActivity()
                                                            ) { task ->
                                                                if (task.isSuccessful) {
                                                                    db.collection("users").add(user)
                                                                    AlertDialog.Builder(this)
                                                                        .setTitle("Account Created Successfully")
                                                                        .setMessage("Your Account has been created, Please go back and Sign-in")
                                                                        .setPositiveButton("Ok") { _, _ ->
                                                                            val intent = Intent(
                                                                                this,
                                                                                SignInActivity::class.java
                                                                            )
                                                                            startActivity(intent)

                                                                        }
                                                                        .show()
                                                                } else {
                                                                    AlertDialog.Builder(this@CreateAccountActivity)
                                                                        .setTitle("Invalid Form")
                                                                        .setMessage("Email Address already been used, Please try using a different Email Address")
                                                                        .setPositiveButton("Ok"){ _,_ ->
                                                                        }
                                                                        .show()
                                                                }
                                                            }
                                                        } else {
                                                            AlertDialog.Builder(this@CreateAccountActivity)
                                                                .setTitle("Invalid Form")
                                                                .setMessage("Password already exists")
                                                                .setPositiveButton("Ok") { _, _ -> }
                                                                .show()
                                                        }
                                                    }
                                                } else {
                                                    AlertDialog.Builder(this@CreateAccountActivity)
                                                        .setTitle("Invalid Form")
                                                        .setMessage("Username already exists")
                                                        .setPositiveButton("Ok") { _, _ -> }
                                                        .show()
                                                }
                                            }
                        }

                    }

                }





    private fun generateUniqueRandomString(callback: (String) -> Unit) {
        val prefix = "IM"
        val random = Random(System.currentTimeMillis())
        val chars = ('A'..'Z').toList() + ('0'..'9').toList()  // Use List<Char> instead of CharRange
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")

        generateRandomString(collectionReference, random, chars, prefix, callback)
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
            .whereEqualTo("pkIdentifier", randomString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback("$prefix$randomString")
                } else {
                    generateRandomString(collectionReference, random, chars, prefix, callback)
                }
            }
    }


    private fun invalidForm(){
        AlertDialog.Builder(this@CreateAccountActivity)
            .setTitle("Invalid Form")
            .setMessage("Please Ensure to fill out necessary fields")
            .setPositiveButton("Ok"){ _,_ ->
            }
            .show()
    }

}
private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork
    val capabilities =
        connectivityManager.getNetworkCapabilities(network)
    return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
}
fun encrypt(text: String, secretKey: SecretKey, encryptionPadding : String): String {
    val cipher = Cipher.getInstance(encryptionPadding)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
    val encryptedBytes = cipher.doFinal(text.toByteArray())
    return Base64.getEncoder().encodeToString(encryptedBytes)
}
fun generateSalt(): String {
    val random = SecureRandom()
    val saltBytes = ByteArray(16) // Adjust the size as needed
    random.nextBytes(saltBytes)
    return Base64.getEncoder().encodeToString(saltBytes)
}
fun generateRandomString(length: Int): String {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val random = java.security.SecureRandom()

    return (1..length)
        .map { characters[random.nextInt(characters.length)] }
        .joinToString("")
}





