package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Calendar
import kotlin.random.Random

class AddChildActivity : AppCompatActivity() {
    lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addchild)
        FirebaseApp.initializeApp(this)
        setupCheckboxGroup()
        val allergyText: TextInputEditText = findViewById(R.id.childallergyEditText)
        allergyText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val text = it.toString()
                    val lastChar = text.lastOrNull()
                    if (lastChar == ' ' || lastChar == '\n') {
                        val category = text.trim()
                        if (category.isNotEmpty()) {
                            addCategoryChip(category)
                            allergyText.text?.clear()
                        }
                    }
                }
            }
        })
        val prefkey = resources.getString(R.string.prefKey)
        val childdropdown = findViewById<MaterialAutoCompleteTextView>(R.id.childgenderEditText)
        val addchildbtn = findViewById<Button>(R.id.addchildbtn)
        val backbtn = findViewById<Button>(R.id.backchildbtn)
        val items = arrayOf("Male","Female")
        val childadapter1 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        childdropdown.setAdapter(childadapter1)
        val childdob = findViewById<EditText>(R.id.childdobEditText)
        val hiddenchildAge = findViewById<EditText>(R.id.hiddenChildAge)
        childdob.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    childdob.setText(dat)
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val age = (currentYear - year).toString()
                    hiddenchildAge.setText(age)

                },
                year,
                month,
                day

            )
            datePickerDialog.show()
        }
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val lastName = sharedPreferences.getString("lastName", null)!!
        childfirstNameFocusListener()
        childdobFocusListener()
        childgenderFocusListener()
        addchildbtn.setOnClickListener {
            addchildbtn.isEnabled = false
            addchildbtn.isClickable = false
            addchilddata(pkidentifier,lastName)
        }
        backbtn.setOnClickListener {
            val intent = Intent(this,AccountPage::class.java)
            startActivity(intent) }



    }
    private fun childfirstNameFocusListener(){
        val childfirstName = findViewById<TextInputEditText>(R.id.childf_nameEditText)
        childfirstName.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val childfirstNameContainer = findViewById<TextInputLayout>(R.id.childf_nameEditTextContainer)
                childfirstNameContainer.helperText = childvalidateFirstName()

            }
        }
    }
    private fun childvalidateFirstName() : String? {
        val childfirstName = findViewById<TextInputEditText>(R.id.childf_nameEditText)
        val childfirstNameVal = childfirstName.text.toString()
        if (childfirstNameVal.isBlank()){
            return "Please Input your First Name"
        }
        if (childfirstNameVal.contains("[0-9\\\\W_]".toRegex())){
            return "Please Input Letters Only"
        }
        return null
    }
    private fun childdobFocusListener(){
        val childdob = findViewById<TextInputEditText>(R.id.childdobEditText)
        childdob.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val childdobContainer = findViewById<TextInputLayout>(R.id.childdobEditTextContainer)
                childdobContainer.helperText = validatechildDOB()

            }
        }
    }
    private fun validatechildDOB() : String? {
        val childdob = findViewById<TextInputEditText>(R.id.childdobEditText)
        val childdobval = childdob.text.toString().trim()
        val hiddenAge1 = findViewById<EditText>(R.id.hiddenChildAge)
        val agevalue = hiddenAge1.text.toString().trim()
        if(agevalue.isNullOrBlank()){
            return "Please input a Valid Date"
        }
        else if (agevalue.isNotEmpty()){
            val checkminor  = agevalue!!.toInt()
            if(checkminor > 18){
                AlertDialog.Builder(this@AddChildActivity)
                    .setTitle("Invalid Age")
                    .setMessage("Please Create a new Account Instead")
                    .setPositiveButton("Ok"){ _,_ ->
                        // do nothing
                    }
                    .show()
            }

            if(childdobval.length == 9) {
                val curyear = childdobval.substring(5).toInt()
                val curmonth = childdobval.substring(2,4).toInt()
                val curday = childdobval.substring(0,1).toInt()
                val chosendate = "$curyear-$curmonth-$curday"
                val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                try {
                    val parsedDate: LocalDate = LocalDate.parse(chosendate, dateFormat)
                    val currentDate = LocalDate.now()
                    return if (parsedDate.isAfter(currentDate)) {
                        "Please input a Valid Date"
                    } else {
                        null
                    }
                }
                catch (e: Exception) {
                    println("Error parsing the date: ${e.message}")
                }

            }
            if(childdobval.length == 10) {
                val curyear = childdobval.substring(6).toInt()
                val curmonth = childdobval.substring(3, 5).toInt()
                val curday = childdobval.substring(0, 2).toInt()
                val chosendate = "$curyear-$curmonth-$curday"
                val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                try {
                    val parsedDate: LocalDate = LocalDate.parse(chosendate, dateFormat)
                    val currentDate = LocalDate.now()
                    return if (parsedDate.isAfter(currentDate)) {
                        "Please input a Valid Date"
                    } else {
                        null
                    }
                }
                catch (e: Exception) {
                    println("Error parsing the date: ${e.message}")
                }
            }

        }
        return null
    }
    private fun childgenderFocusListener(){
        val childgenderdropdown = findViewById<MaterialAutoCompleteTextView>(R.id.childgenderEditText)
        childgenderdropdown.setOnFocusChangeListener{_, focused ->
            if (!focused){
                val childgenderContainer = findViewById<TextInputLayout>(R.id.childgenderEditTextContainer)
                childgenderContainer.helperText = childvalidateGender()

            }
        }
    }
    private fun childvalidateGender() : String? {
        val childgenderdropdown = findViewById<MaterialAutoCompleteTextView>(R.id.childgenderEditText)
        val childgenderdropdown_val = childgenderdropdown.text.toString()
        if (childgenderdropdown_val.isBlank()){
            return "Please Input your Gender"
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
        val allergyYesChecker: MaterialCheckBox = findViewById(R.id.childyes_option)
        val allergyNoChecker: MaterialCheckBox = findViewById(R.id.childno_option)
        val allergyText: TextInputEditText = findViewById(R.id.childallergyEditText)
        val chipvalues: ChipGroup = findViewById(R.id.childcategoryGroup)
        val checkboxes = listOf(allergyYesChecker, allergyNoChecker)
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
                    checkboxes.filter { it != checkbox }.forEach { it.isChecked = false }
                    updateUI()
                }
            }
        }
    }


    private fun getSelectedCheckbox(): String? {
        val allergyYesChecker: MaterialCheckBox = findViewById(R.id.childyes_option)
        val allergyNoChecker: MaterialCheckBox = findViewById(R.id.childno_option)

        return when {
            allergyYesChecker.isChecked -> "Yes"
            allergyNoChecker.isChecked -> "No"
            else -> null
        }
    }
    private fun addCategoryChip(category: String) {
        val categoryGroup: ChipGroup = findViewById(R.id.childcategoryGroup)
        val chip = LayoutInflater.from(this).inflate(R.layout.category_chip, categoryGroup, false) as Chip

        chip.text = category
        chip.contentDescription = category

        chip.setOnCloseIconClickListener {
            categoryGroup.removeView(chip)
        }


        categoryGroup.addView(chip)
    }
    private fun getAllChipValues(): List<String> {
        val categoryGroup: ChipGroup = findViewById(R.id.childcategoryGroup)
        val chipList = mutableListOf<String>()

        for (i in 0 until categoryGroup.childCount) {
            val chip = categoryGroup.getChildAt(i) as? Chip
            chip?.let {
                chipList.add(it.text.toString())
            }
        }

        return chipList
    }
    private fun addchilddata(pkIdentifier : String, lastName : String){
        val addchildbtn = findViewById<Button>(R.id.addchildbtn)
        val childdob = findViewById<EditText>(R.id.childdobEditText)
        val childfirstName = findViewById<TextInputEditText>(R.id.childf_nameEditText)
        val hiddenAge1 = findViewById<EditText>(R.id.hiddenChildAge)
        val childgenderdropdown = findViewById<MaterialAutoCompleteTextView>(R.id.childgenderEditText)
        val childfirstNameContainer = findViewById<TextInputLayout>(R.id.childf_nameEditTextContainer)
        childfirstNameContainer.helperText = childvalidateFirstName()
        val childgenderContainer = findViewById<TextInputLayout>(R.id.childgenderEditTextContainer)
        childgenderContainer.helperText = childvalidateGender()
        val childdobContainer = findViewById<TextInputLayout>(R.id.childdobEditTextContainer)
        childdobContainer.helperText = validatechildDOB()
        val validchildfirstName = childfirstNameContainer.helperText == null
        val validgender = childgenderContainer.helperText == null
        val validdob = childdobContainer.helperText == null
        val agevalue = hiddenAge1.text.toString().trim()
        val allergyChecker = validCheckValue()
        val selectedOption = getSelectedCheckbox()
        val chipvalues: ChipGroup  = findViewById(R.id.childcategoryGroup)
        var allergyflag = true
        if (!allergyChecker){
            AlertDialog.Builder(this)
                .setTitle("Invalid Form")
                .setMessage("Please Indicate if you have an Allergy or Not")
                .setPositiveButton("Ok") { _, _ ->
                }
                .show()
            allergyflag = false

        }
        if (selectedOption == "Yes" &&  chipvalues.childCount == 0){
            AlertDialog.Builder(this)
                .setTitle("Invalid Form")
                .setMessage("Kindly Specify your Allergy")
                .setPositiveButton("Ok") { _, _ ->
                }
                .show()
            allergyflag = false
        }
        if (agevalue.isNotEmpty()){
            val agevalueint = agevalue.toInt()
            val childgender = childgenderdropdown.text.toString()
            val childfirstname = childfirstName.text.toString()
            if(validchildfirstName && validgender && validdob && agevalueint < 18 && allergyChecker && allergyflag ){
                generateUniqueRandomString { uniqueString ->
                    val firestore = FirebaseFirestore.getInstance()
                    val collectionReference = firestore.collection("users")
                    val chipList = getAllChipValues()
                    val child = hashMapOf(
                        "childName" to childfirstName.text.toString(),
                        "childAge" to agevalue,
                        "childGender" to childgenderdropdown.text.toString(),
                        "pkIdentifier" to pkIdentifier,
                        "childIdentifier" to uniqueString,
                        "child_dob" to childdob.text.toString(),
                        "childLastName" to lastName,
                        "allergy_status" to selectedOption,
                        "allergy_values" to chipList


                    )
                    collectionReference.whereEqualTo("pkIdentifier", pkIdentifier).get().addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot){
                            val documentID = document.id
                            val childCollection = collectionReference.document(documentID).collection("child")
                            val updates = hashMapOf<String, Any>(
                                "child_state" to "TRUE",
                                "childIdentifier" to uniqueString
                            )
                            val editor = sharedPreferences.edit()
                            editor.putString("child_state" ,"TRUE")
                            editor.putString("childName" , childfirstname)
                            editor.putString("childAge" , agevalue)
                            editor.putString("childGender", childgender)
                            editor.putString("childIdentifier",uniqueString)
                            editor.putString("childdob",childdob.text.toString())
                            editor.apply()
                            val docRef = collectionReference.document(documentID)
                            docRef.update(updates).addOnSuccessListener {
                                childCollection
                                    .add(child)
                                    .addOnCompleteListener{
                                        AlertDialog.Builder(this)
                                            .setTitle("Child Account Created")
                                            .setMessage("An Account for your Child is Successfully created")
                                            .setPositiveButton("Ok"){ _,_ ->
                                                val intent = Intent(this,AccountPage::class.java)
                                                startActivity(intent)

                                            }
                                            .show()
                                        clear()
                                        addchildbtn.isEnabled = true
                                        addchildbtn.isClickable = true

                                    }


                            }
                        }


                    }
                }


            }
        }
        else{
            AlertDialog.Builder(this@AddChildActivity)
                .setTitle("Invalid Form")
                .setMessage("Please Ensure to fill out necessary fields")
                .setPositiveButton("Ok"){ _,_ ->
                }
                .show()
            addchildbtn.isEnabled = true
            addchildbtn.isClickable = true
        }




}
    private fun clear(){
        val firstName = findViewById<EditText>(R.id.childf_nameEditText)
        val dob = findViewById<TextInputEditText>(R.id.childdobEditText)
        val dropdown = findViewById<MaterialAutoCompleteTextView>(R.id.childgenderEditText)
        firstName.text = null
        dob.text = null
        dropdown.text = null

    }
    private fun generateUniqueRandomString(callback: (String) -> Unit) {
        val prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val prefix = "IM-C"
        val random = Random(System.currentTimeMillis())
        val chars = ('A'..'Z').toList() + ('0'..'9').toList()
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        collectionReference.whereEqualTo("pkIdentifier",pkidentifier).get().addOnSuccessListener { querySnapshot ->
            for (i in querySnapshot){
                val x = i.id
                val data_fromfirebase = collectionReference.document(x).collection("child")
                generateRandomString(data_fromfirebase, random, chars, prefix, callback)
            }

        }


    }

    private fun generateRandomString(
        collectionReference: CollectionReference,
        random: Random,
        chars: List<Char>,  // Change the type to List<Char>
        prefix: String,
        callback: (String) -> Unit
    ) {
        val randomString = (1..8)
            .map { chars[random.nextInt(chars.size)] }
            .joinToString("")

        collectionReference
            .whereEqualTo("childIdentifier", randomString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback("$prefix$randomString")
                } else {
                    generateRandomString(collectionReference, random, chars, prefix, callback)
                }
            }
    }
}