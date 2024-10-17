package com.example.myapplication
import AdapterforChatBot
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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

class AccountPage : AppCompatActivity() {
    private var isClicked = false
    private lateinit var storageRef: StorageReference
    private lateinit var db: FirebaseFirestore
    private lateinit var chatSessionManager: ChatSessionManager
    private var isChatSessionManagerInitialized = false
    private lateinit var completedAppointments: ArrayList<fetchcompleteappointment>
    private lateinit var certificateAdapter : AdapterforGenerate
    private var messages = mutableListOf<chatbot_data>()
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefkey : String
    override fun onCreate(savedInstanceState: Bundle?) {
        chatSessionManager = ChatSessionManager.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_page)
        FirebaseApp.initializeApp(this)
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val picture_checker = sharedPreferences.getString("image_check", null)!!
        val profile_picture = findViewById<ImageView>(R.id.Profilepicture)
        val childprofile_picture = findViewById<ImageView>(R.id.childProfilepicture)
        val child_state = sharedPreferences.getString("child_state", null)!!
        val chatInterface = findViewById<RelativeLayout>(R.id.chatBot_UI)
        val chatbot_btn = findViewById<ImageButton>(R.id.chatbot_btn)
        val hidechatbot = findViewById<ImageView>(R.id.hideChatbot)
        val usermessage = findViewById<EditText>(R.id.user_response)
        val sendMessageButton: ImageButton = findViewById(R.id.submit_message)
        val vaccineRecyclerView = findViewById<RecyclerView>(R.id.vaccineRecyclerView)
        completedAppointments = arrayListOf()
        vaccineRecyclerView.layoutManager = LinearLayoutManager(this)
        vaccineRecyclerView.setHasFixedSize(true)
        certificateAdapter = AdapterforGenerate(this, completedAppointments)
        vaccineRecyclerView.adapter = certificateAdapter
        if (child_state.equals("FALSE")){
            childprofile_picture.visibility = View.INVISIBLE
        }
        if (picture_checker == "TRUE") {
                val pictureURI = sharedPreferences.getString("pictureURI", null)!!
                Glide.with(this)
                    .load(pictureURI)
                    .apply(
                        RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                            .override(250, 250) // Set target width and height
                    )
                    .into(profile_picture)
            }

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        val add_photobtn = findViewById<Button>(R.id.photobtn)
        val logout_btn = findViewById<Button>(R.id.logoutbtn)
        val nav = findViewById<BottomNavigationView>(R.id.nav_bar)
        val full_name = findViewById<TextView>(R.id.NameText)
        val email = findViewById<TextView>(R.id.emailText)
        val switch_btn = findViewById<Button>(R.id.switchacct)
        val addchild_btn = findViewById<Button>(R.id.addchild_btn)
        val updateAllergy = findViewById<Button>(R.id.addAllergy)
        val gender = findViewById<TextView>(R.id.genderText)
        val address = findViewById<TextView>(R.id.addressText)
        prefkey = resources.getString(R.string.prefKey)
        sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val full_nametxt = sharedPreferences.getString("full_name", null)!!
        val emailtxt = sharedPreferences.getString("email", null)!!
        val gender_txt = sharedPreferences.getString("gender", null)!!
        val address_txt = sharedPreferences.getString("address", null)!!
        val primaryKey = sharedPreferences.getString("pkIdentifier", null)!!
        deleteExpiredAppointments(primaryKey,child_state)
        nav.menu.findItem(R.id.account_info).setChecked(true)
        EventChangeListener_1(isClicked)
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
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.book_appointment -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, AppointmentPage::class.java)
                    startActivity(intent)
                    true

                }

                R.id.homepage -> {
                    chatSessionManager.resetMessages(messages)
                    val intent = Intent(this, Homepage::class.java)
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
        full_name.setText("Name : " + full_nametxt)
        email.setText("Email : " + emailtxt)
        gender.setText("Gender : " + gender_txt)
        address.setText("Address : " + address_txt)


        logout_btn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            sharedPreferences.edit().clear().commit()
            chatSessionManager.clearChatSession()

        }

        if (child_state.equals("FALSE")) {
            switch_btn.visibility = View.GONE
            addchild_btn.visibility = View.VISIBLE
            addchild_btn.setOnClickListener {
                val intent = Intent(this, AddChildActivity::class.java)
                startActivity(intent)
            }
        }
        else if (child_state.equals("TRUE")) {
            addchild_btn.visibility = View.GONE
            switch_btn.visibility = View.VISIBLE
            childprofile_picture.visibility = View.INVISIBLE
            val full_nametxt = sharedPreferences.getString("full_name", null)!!
            val emailtxt = sharedPreferences.getString("email", null)!!
            val gender_txt = sharedPreferences.getString("gender", null)!!
            val lastname = sharedPreferences.getString("lastName", null)!!
            val childfirstname = sharedPreferences.getString("childName", null)!!
            val childage = sharedPreferences.getString("childAge", null)!!
            val childgender = sharedPreferences.getString("childGender", null)!!
            val address_txt = sharedPreferences.getString("address", null)!!
            val pkIdentifier = sharedPreferences.getString("pkIdentifier", null)!!
            switch_btn.setOnClickListener {
                val childimagechecker = sharedPreferences.getString("childimage_check", null)!!
                if (!isClicked) {

                    full_name.setText("Name : " + childfirstname + " " + lastname)
                    email.setText("Age : " + childage)
                    gender.setText("Gender : " + childgender)
                    address.setText("Address : " + address_txt)
                    childprofile_picture.visibility = View.VISIBLE
                    profile_picture.visibility = View.INVISIBLE
                    nav.menu.findItem(R.id.account_info).setChecked(true)

                    if (childimagechecker.equals("FALSE")){
                        Glide.with(this).clear(childprofile_picture)
                    }
                    else{
                        db.collection("users")
                            .whereEqualTo("pkIdentifier",pkIdentifier)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (i in querySnapshot){
                                    val j = i.id
                                    db.collection("users")
                                        .document(j)
                                        .collection("child_photos")
                                        .get()
                                        .addOnSuccessListener {
                                                querySnapshot ->
                                            if (!querySnapshot.isEmpty)    {
                                                for (x in querySnapshot){
                                                    val user_photo_id = x.get("photo_url")
                                                    val editor = sharedPreferences.edit()
                                                    editor.putString("pictureURI",user_photo_id.toString())
                                                    editor.apply()
                                                    Glide.with(this)
                                                        .load(user_photo_id.toString())
                                                        .apply(
                                                            RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                                                .override(250, 250) // Set target width and height
                                                        )
                                                        .into(childprofile_picture)

                                                }
                                            }
                                        }

                                }
                            }

                        }

                    isClicked = true
                }

                else {

                    db.collection("users")
                        .whereEqualTo("pkIdentifier",pkIdentifier)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (i in querySnapshot){
                                val j = i.id
                                db.collection("users")
                                    .document(j)
                                    .collection("photos")
                                    .get()
                                    .addOnSuccessListener {
                                            querySnapshot ->
                                        if (!querySnapshot.isEmpty)    {
                                            for (x in querySnapshot){
                                                val user_photo_id = x.get("photo_url")
                                                val editor = sharedPreferences.edit()
                                                editor.putString("pictureURI",user_photo_id.toString())
                                                editor.apply()
                                                Glide.with(this)
                                                    .load(user_photo_id.toString())
                                                    .apply(
                                                        RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                                            .override(250, 250) // Set target width and height
                                                    )
                                                    .into(profile_picture)

                                            }
                                        }
                                    }

                            }
                        }
                    full_name.setText("Name : " + full_nametxt)
                    email.setText("Email : " + emailtxt)
                    gender.setText("Gender : " + gender_txt)
                    address.setText("Address : " + address_txt)
                    nav.menu.findItem(R.id.account_info).setChecked(true)
                    childprofile_picture.visibility = View.INVISIBLE
                    profile_picture.visibility = View.VISIBLE
                    isClicked = false

                }

                certificateAdapter.setIsClicked(isClicked)
                EventChangeListener_1(isClicked)





            }


        }
        add_photobtn.setOnClickListener label@{
            if (!isClicked) {

                val pickImageIntent = Intent(Intent.ACTION_PICK)
                pickImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                pickImageIntent.type = "image/*"
                startActivityForResult(pickImageIntent, 503)
            } else {
                val pickImageIntent = Intent(Intent.ACTION_PICK)
                pickImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                pickImageIntent.type = "image/*"
                startActivityForResult(pickImageIntent, 502)
            }
        }

        updateAllergy.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.updateallergy, null)
            val firestore = FirebaseFirestore.getInstance()
            var userID = ""
            var childID = ""
            val collectionReference = firestore.collection("users")
            val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.updateAllergyEditText)
            val chipGroup = dialogView.findViewById<ChipGroup>(R.id.updateAllergychipGroup)
            var initialChipValues: MutableList<String> = mutableListOf()
            collectionReference.whereEqualTo("pkIdentifier", primaryKey).get().addOnSuccessListener { querySnapshot ->
                for (x in querySnapshot){
                    userID = x.id
                    var allergy_state = x.getString("allergy_status")
                    val allergyValues = x.get("allergy_values") as? List<String>
                    if (child_state == "TRUE") {
                        if (isClicked){
                            collectionReference.document(userID).collection("child")
                                .get()
                                    .addOnSuccessListener { childDocument ->
                                        for (document in childDocument) {
                                            childID = document.id
                                            val childAllergyValues =
                                                document.get("allergy_values") as? List<String>
                                            val childAllergyState = document.getString("allergy_status")
                                            if (childAllergyState == "No") {
                                                chipGroup.removeAllViews()
                                            }
                                            else {
                                                if (childAllergyValues != null) {
                                                    chipGroup.removeAllViews()
                                                    for (n in childAllergyValues) {
                                                        addCategoryChip(n, chipGroup)
                                                        initialChipValues.add(n)


                                                    }
                                                } else {
                                                    chipGroup.removeAllViews()
                                                }
                                            }
                                        }
                                    }
                            }
                            else{
                                if (allergy_state == "No"){
                                    chipGroup.removeAllViews()
                                }
                                else{
                                    if(allergyValues != null) {
                                        for (xm in allergyValues) {
                                            addCategoryChip(xm, chipGroup)
                                            initialChipValues.add(xm)
                                        }
                                    }
                                }

                            }

                        }

                        else{
                        if (allergy_state == "No"){
                            chipGroup.removeAllViews()
                        }
                            if(allergyValues != null) {
                                for (xz in allergyValues) {
                                    addCategoryChip(xz, chipGroup)
                                    initialChipValues.add(xz)
                                }
                            }
                        }



                }


            }

            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView)
                .setTitle("Update Allergy")
                .setPositiveButton("Update") { dialog, _ ->
                    val chips = getAllChipValues(chipGroup)
                    val updates = hashMapOf(
                        "allergy_values" to chips,
                        "allergy_status" to "Yes"
                    )
                    if(chips.isEmpty()){
                        AlertDialog.Builder(this)
                            .setTitle("Allergy Updates")
                            .setMessage("No Allergy are Added, Kindly Add an Allergy")
                            .setPositiveButton("Ok") { _, _ -> }
                            .show()
                    }
                    if (child_state == "TRUE" && isClicked) {
                        collectionReference.document(userID).collection("child").document(childID).update(updates).addOnSuccessListener {
                            AlertDialog.Builder(this)
                                .setTitle("Allergy Updates")
                                .setMessage("Allergy for Child has been updated")
                                .setPositiveButton("Ok") { _, _ -> }
                                .show()
                        }
                    }
                    if(initialChipValues == chips){
                        AlertDialog.Builder(this)
                            .setTitle("Allergy Updates")
                            .setMessage("Allergy is the same, No updates were made")
                            .setPositiveButton("Ok") { _, _ -> }
                            .show()

                    }
                    else if(child_state == "TRUE" && !isClicked){
                        collectionReference.document(userID).update(updates).addOnSuccessListener {
                            AlertDialog.Builder(this)
                                .setTitle("Allergy Updates")
                                .setMessage("Allergy for Adult has been updated")
                                .setPositiveButton("Ok") { _, _ -> }
                                .show()
                        }

                    }
                    else if(child_state == "FALSE") {
                        collectionReference.document(userID).update(updates).addOnSuccessListener {
                            AlertDialog.Builder(this)
                                .setTitle("Allergy Updates")
                                .setMessage("Allergy has been updated")
                                .setPositiveButton("Ok") { _, _ -> }
                                .show()

                        }
                    }



                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }


            val dialog = builder.create()
            dialog.show()






            textInputEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    s?.let {
                        val text = it.toString()
                        val lastChar = text.lastOrNull()
                        if (lastChar == ' ' || lastChar == '\n') {
                            val category = text.trim()
                            if (category.isNotEmpty()) {
                                addCategoryChip(category, chipGroup)
                                textInputEditText.text?.clear()
                            }
                        }
                    }
                }
            })
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null)!!
        if ((requestCode == 503 || requestCode == 502) && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri = data?.data ?: return
            val storagePath = if (requestCode == 503) {
                "Images/Users/$pkIdentifier"
            } else {
                val childIdentifier = sharedPreferences.getString("childIdentifier", null)!!
                "Images/Child/$childIdentifier"

            }

            val imageRef = storageRef.child(storagePath)

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show()
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        updateFirestoreDocument(imageUrl, storagePath,imageUri)
                    }

                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun addCategoryChip(category: String, chipGroup: ChipGroup) {
        val chip = Chip(this)
        chip.text = category
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip) }
        chipGroup.addView(chip)
    }
    private fun getAllChipValues(chipGroup: ChipGroup): List<String> {
        val chipValues = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let {
                chipValues.add(it.text.toString())
            }
        }
        return chipValues
    }

    private fun updateFirestoreDocument(newImageUrl: String, storagePath: String, imageUri: Uri) {
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val profile_picture = findViewById<ImageView>(R.id.Profilepicture)
        val childprofile_picture = findViewById<ImageView>(R.id.childProfilepicture)
        val updates = hashMapOf<String, Any>(
            "photo_url" to newImageUrl
        )
        db.collection("users")
            .whereEqualTo("pkIdentifier", pkIdentifier)
            .get()
            .addOnSuccessListener { userSnapshot ->
                for (user_data in userSnapshot) {
                    val documentID_user = user_data.id
                    val photosCollectionPath = if (storagePath.contains("Users")) {
                        "photos"
                    } else {
                        "child_photos"
                    }
                    val photosCollectionRef = db.collection("users")
                        .document(documentID_user)
                        .collection(photosCollectionPath)
                    photosCollectionRef.get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                photosCollectionRef.add(updates)
                                val updates1 = hashMapOf<String, Any>(
                                    if (photosCollectionPath == "photos") "image_check" to "TRUE" else "childimage_check" to "TRUE"
                                )
                                db.collection("users").whereEqualTo("pkIdentifier", pkIdentifier).get()
                                    .addOnSuccessListener { querySnapshot ->
                                        for (t in querySnapshot) {
                                            val pkid = t.id
                                            db.collection("users").document(pkid).update(updates1)
                                            Glide.with(this)
                                                .load(newImageUrl)
                                                .apply(
                                                    RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                                        .override(250, 250) // Set target width and height
                                                )
                                                .into(if (photosCollectionPath == "photos") profile_picture else childprofile_picture)
                                            val editor = sharedPreferences.edit()
                                            editor.putString(
                                                if (photosCollectionPath == "photos") "image_check" else "childimage_check",
                                                "TRUE"
                                            )
                                            editor.putString("pictureURI", newImageUrl)
                                            editor.apply()
                                        }
                                    }
                            }
                            else {
                                for (photoDoc in querySnapshot) {
                                    val documentID = photoDoc.id
                                    val currentImageUrl = photoDoc.getString("photo_url")
                                    if (currentImageUrl != null) {
                                        db.collection("users")
                                            .document(documentID_user)
                                            .collection(photosCollectionPath)
                                            .document(documentID)
                                            .delete()
                                        val previousImageRef =
                                            FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl)
                                        previousImageRef.delete()
                                            .addOnSuccessListener {
                                                val user = hashMapOf("photo_url" to newImageUrl)
                                                db.collection("users")
                                                    .document(documentID_user)
                                                    .collection(photosCollectionPath)
                                                    .add(user)
                                                    .addOnSuccessListener { documentReference ->
                                                        val newDocumentID = documentReference.id
                                                        uploadNewFile(
                                                            storagePath,
                                                            newDocumentID,
                                                            imageUri
                                                        )
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Toast.makeText(
                                                            this,
                                                            "Firestore update failed: ${exception.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(
                                                    this,
                                                    "File deletion failed: ${exception.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                }
                            }
                        }
                }
            }
    }

    private fun uploadNewFile(storagePath: String, newDocumentID: String, imageUri: Uri) {
        val newImageRef = storageRef.child(storagePath)
        val childIdentifier = sharedPreferences.getString("childIdentifier", null)!!
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val profile_picture = findViewById<ImageView>(R.id.Profilepicture)
        val childprofile_picture = findViewById<ImageView>(R.id.childProfilepicture)
        val photosCollectionPath = if (storagePath.contains("Users")) {
            "photos"
        } else {
            "child_photos"
        }

        newImageRef.putFile(imageUri)
            .addOnSuccessListener { newTaskSnapshot ->
                newTaskSnapshot.storage.downloadUrl.addOnSuccessListener { newUri ->
                    val newImageUrl = newUri.toString()
                    val editor = sharedPreferences.edit()
                    editor.putString("pictureURI", newImageUrl)
                    editor.apply()

                    if (photosCollectionPath == "photos") {
                        Glide.with(this)
                            .load(newImageUrl)
                            .apply(
                                RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                    .override(250, 250) // Set target width and height
                            )
                            .into(profile_picture)
                    } else if (photosCollectionPath == "child_photos") {
                        Glide.with(this)
                            .load(newImageUrl)
                            .apply(
                                RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                    .override(250, 250) // Set target width and height
                            )
                            .into(childprofile_picture)
                    }

                    val user = if (photosCollectionPath == "photos") {
                        hashMapOf(
                            "photo_url" to newImageUrl,
                            "pkIdentifier" to pkIdentifier
                        )
                    } else  {
                        hashMapOf(
                            "photo_url" to newImageUrl,
                            "childIdentifier" to childIdentifier
                        )
                    }


                    db.collection("users").whereEqualTo("pkIdentifier", pkIdentifier).get()
                        .addOnSuccessListener { querySnapshot ->
                            for (x in querySnapshot) {
                                val documentID = x.id
                                db.collection("users").document(documentID)
                                    .collection(photosCollectionPath)
                                    .document(newDocumentID)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Image Uploaded",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Firestore document update failed: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener { newException ->
                Toast.makeText(
                    this,
                    "New file upload failed: ${newException.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
    private fun EventChangeListener_1(isClicked: Boolean) {
        val db = FirebaseFirestore.getInstance()
        prefkey = resources.getString(R.string.prefKey)
        val sharedPreferences = getSharedPreferences(prefkey, Context.MODE_PRIVATE)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!

        db.collection("users").whereEqualTo("pkIdentifier", pkidentifier).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val userID = document.id

                    val collectionPath =
                        if (child_state == "TRUE" && isClicked) "childCompletedAppointments" else "adultCompletedAppointments"

                    db.collection("users/$userID/$collectionPath")
                        .whereEqualTo("pkIdentifier", pkidentifier).addSnapshotListener(
                            object : EventListener<QuerySnapshot> {
                                override fun onEvent(
                                    value: QuerySnapshot?,
                                    error: FirebaseFirestoreException?
                                ) {
                                    if (error != null) {
                                        Log.e("Firestore Error", error.message.toString())
                                        return
                                    }

                                    completedAppointments.clear()

                                    for (dc: DocumentChange in value?.documentChanges!!) {
                                        if (dc.type == DocumentChange.Type.ADDED) {
                                            val appointment =
                                                dc.document.toObject(fetchcompleteappointment::class.java)
                                            if ("Cancelled".equals(appointment.status) || "Expired".equals(
                                                    appointment.status
                                                )
                                            ) {
                                                continue
                                            }
                                            completedAppointments.add(appointment)
                                        }
                                    }

                                    certificateAdapter.notifyDataSetChanged()
                                }
                            }
                        )
                }
            }
    }
    private fun scrollRecyclerViewToBottom(adapter: AdapterforChatBot) {
        val chatbotRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        chatbotRecyclerView.post {
            chatbotRecyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }
}













