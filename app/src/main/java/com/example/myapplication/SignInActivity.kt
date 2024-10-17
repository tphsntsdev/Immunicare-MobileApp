package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeFormatter
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SignInActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        FirebaseApp.initializeApp(this)
        val firestore = FirebaseFirestore.getInstance()
        AndroidThreeTen.init(application)
        val collectionReference = firestore.collection("users")
        var actionBar = supportActionBar
        val forgot_Password = findViewById(R.id.backbtn) as TextView
        val createAccount = findViewById(R.id.createAccount) as TextView
        val usernameInput = findViewById<EditText>(R.id.resetCode)
        val passwordInput = findViewById<EditText>(R.id.editTextText3)
        val prefkey = resources.getString(R.string.prefKey)
        val encryptionMethod = resources.getString(R.string.encryptionMethod)
        val encryptionHash = resources.getString(R.string.encryptionHash)

        val sign_inbtn = findViewById(R.id.button2) as Button
        actionBar?.hide()
        forgot_Password.setOnClickListener {
            val intent = Intent(this@SignInActivity, ForgotPassword::class.java)
            startActivity(intent)
            finish()

        }
        createAccount.setOnClickListener {
            val intent = Intent(this@SignInActivity, CreateAccountActivity::class.java)
            startActivity(intent)
            finish()
        }



        sign_inbtn.setOnClickListener {
            val username_Value = usernameInput.text.toString()
            val password_Value = passwordInput.text.toString()

            if (username_Value.isNotEmpty() && password_Value.isNotEmpty()) {
                collectionReference.whereEqualTo("username", username_Value).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (data_value in querySnapshot) {
                                try {
                                    val salt = data_value.get("salt") as String
                                    val spec: KeySpec = PBEKeySpec(
                                        password_Value.toCharArray(),
                                        salt.toByteArray(),
                                        65536,
                                        256
                                    )
                                    val secretKeyFactory =
                                        SecretKeyFactory.getInstance(encryptionHash)
                                    val secretKey: SecretKey = SecretKeySpec(
                                        secretKeyFactory.generateSecret(spec).encoded,
                                        encryptionMethod
                                    )
                                    val storedEncryptedPassword =
                                        data_value.get("password") as String
                                    val decryptedPassword =
                                        decrypt(storedEncryptedPassword, secretKey)
                                    if (decryptedPassword == password_Value) {
                                        val accountcreated =
                                            data_value.get("accountCreated") as String
                                        val firstName = data_value.get("firstName") as String
                                        val lastName = data_value.get("lastName") as String
                                        val email = data_value.get("email") as String
                                        val gender = data_value.get("gender") as String
                                        val pkIdentifier = data_value.get("pkIdentifier") as String
                                        val address = data_value.get("address") as String
                                        val child_state = data_value.get("child_state") as String
                                        val image_check = data_value.get("image_check") as String
                                        val passwordResetCode =
                                            data_value.get("passwordResetCode") as String
                                        val adultage = data_value.get("age") as String
                                        val adultdob = data_value.get("dateofbirth") as String
                                        val childimage_check =
                                            data_value.get("childimage_check") as String
                                        sharedPreferences =
                                            getSharedPreferences(prefkey, Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        val adultnewage = isBirthdayPassedByDay(adultdob)
                                        var y = adultnewage.toString()
                                        val adult_updates = hashMapOf<String, Any>(
                                            "age" to y,
                                        )
                                        var documentID = data_value.id
                                        firestore.collection("users").document(documentID)
                                            .update(adult_updates)
                                        editor.putString("firstName", firstName)
                                        editor.putString("lastName", lastName)
                                        editor.putString("email", email)
                                        editor.putString("gender", gender)
                                        editor.putString("pkIdentifier", pkIdentifier)
                                        editor.putString("full_name", firstName + " " + lastName)
                                        editor.putString("address", address)
                                        editor.putString("child_state", child_state)
                                        editor.putString("image_check", image_check)
                                        editor.putString("childimage_check", childimage_check)
                                        editor.putString("accountCreated", accountcreated)
                                        editor.putString("passwordResetCode", passwordResetCode)
                                        editor.putString("isloggedIn", "FALSE")
                                        editor.putString("adultAge", adultage)
                                        editor.apply()
                                        if (image_check.equals("TRUE")) {
                                            collectionReference.document(documentID)
                                                .collection("photos").get()
                                                .addOnSuccessListener { querySnapshot ->
                                                    if (!querySnapshot.isEmpty) {
                                                        for (x in querySnapshot) {
                                                            val user_photo_id = x.get("photo_url")
                                                            val editor = sharedPreferences.edit()
                                                            editor.putString(
                                                                "pictureURI",
                                                                user_photo_id.toString()
                                                            )
                                                            editor.apply()
                                                        }
                                                    }
                                                }

                                        }
                                        if (child_state.equals("TRUE")) {
                                            val collectionReference1 =
                                                firestore.collection("users/$documentID/child")
                                            collectionReference1.whereEqualTo(
                                                "pkIdentifier",
                                                pkIdentifier
                                            ).get().addOnSuccessListener { querySnapshot ->
                                                if (!querySnapshot.isEmpty) {
                                                    for (data_value1 in querySnapshot) {
                                                        val childName =
                                                            data_value1.get("childName") as String
                                                        val childGender =
                                                            data_value1.get("childGender") as String
                                                        val childAge =
                                                            data_value1.get("childAge") as String
                                                        val childIdentifier =
                                                            data_value1.get("childIdentifier") as String
                                                        val childdob =
                                                            data_value1.get("child_dob") as String

                                                        val childnewage =
                                                            isBirthdayPassedByDay(childdob)


                                                        val editor = sharedPreferences.edit()
                                                        var x = childnewage.toString()

                                                        val child_updates = hashMapOf<String, Any>(
                                                            "childAge" to x,

                                                            )
                                                        collectionReference1.document(data_value1.id)
                                                            .update(child_updates)

                                                        editor.putString("childName", childName)
                                                        editor.putString("childGender", childGender)
                                                        editor.putString("childAge", childAge)
                                                        editor.putString(
                                                            "childIdentifier",
                                                            childIdentifier
                                                        )
                                                        editor.putString("childdob", childdob)
                                                        editor.apply()

                                                    }
                                                }


                                            }

                                        }


                                    }
                                    val intent = Intent(this@SignInActivity, Homepage::class.java)
                                    startActivity(intent)
                                    finish()
                                } catch (e: BadPaddingException) {
                                    AlertDialog.Builder(this@SignInActivity)
                                        .setTitle("Invalid Credentials")
                                        .setMessage("Credentials Provided were Invalid")
                                        .setPositiveButton("Ok") { _, _ ->
                                            // do nothing
                                        }
                                        .show()
                                }
                            }

                        } else {
                            // User not found
                            AlertDialog.Builder(this@SignInActivity)
                                .setTitle("Invalid Credentials")
                                .setMessage("Credentials Provided were Invalid")
                                .setPositiveButton("Ok") { _, _ ->
                                    // do nothing
                                }
                                .show()
                        }
                    }
            } else {
                // Empty fields
                AlertDialog.Builder(this@SignInActivity)
                    .setTitle("Invalid Credentials")
                    .setMessage("Fields cannot be empty")
                    .setPositiveButton("Ok") { _, _ ->
                        // do nothing
                    }
                    .show()
            }
        }
    }

    private fun decrypt(encryptedText: String, secretKey: SecretKey): String {
        val decryptionMethod = resources.getString(R.string.decryptionMethod)
        val cipher = Cipher.getInstance(decryptionMethod)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText))
        return String(decryptedBytes)
    }


    private fun isBirthdayPassedByDay(childDob: String): Int? {
        val childBirthday = LocalDate.parse(childDob, DateTimeFormatter.ofPattern("d-M-yyyy"))
        val currentDateTime = LocalDate.now()
        if (currentDateTime.isAfter(childBirthday) || currentDateTime == childBirthday) {
            val age = Period.between(childBirthday, currentDateTime).years
            return age
        }
        return 0
    }

}
