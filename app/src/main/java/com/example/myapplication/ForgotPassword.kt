package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ForgotPassword : AppCompatActivity() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var prefkey_cp: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotrd)
        FirebaseApp.initializeApp(this)
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        val back_btn = findViewById(R.id.backbtn) as TextView
        val changebtn = findViewById(R.id.button2) as Button
        val emailEditText = findViewById(R.id.email) as EditText
        prefkey_cp = resources.getString(R.string.prefKey_cp)
        sharedPreferences =
            getSharedPreferences(prefkey_cp, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()




        back_btn.setOnClickListener {
            sharedPreferences.edit().clear().commit()
            val intent = Intent(this,SignInActivity::class.java)
            startActivity(intent)
            finish()

        }
        changebtn.setOnClickListener {
            changebtn.isEnabled = false
            changebtn.isClickable = false
            val auth = FirebaseAuth.getInstance()
            val emailVal = emailEditText.text.toString()
            if (Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
                val passwordResetCode = generateRandomString(10)
                auth.fetchSignInMethodsForEmail(emailVal).addOnCompleteListener { authcheck ->
                    if (authcheck.isSuccessful) {
                        val result = authcheck.result
                        if (result?.signInMethods?.isNotEmpty() == false) {
                            collectionReference.whereEqualTo("email", emailVal).get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty) {
                                        for (data_value in querySnapshot) {
                                            val documentKey = data_value.id
                                            val email = data_value.get("email") as String
                                            collectionReference.document(documentKey)
                                                .update("passwordResetCode", passwordResetCode)
                                                .addOnSuccessListener {
                                                    editor.putString("email", emailVal)
                                                    editor.apply()
                                                    val passwordResetText =
                                                        "Your Password Reset Code is : $passwordResetCode"
                                                    sendEmail(email, passwordResetText)
                                                    val intent =
                                                        Intent(this, ForgotPassword_1::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                    changebtn.isEnabled = true
                                                    changebtn.isClickable = true
                                                }
                                        }
                                    } else {
                                        AlertDialog.Builder(this@ForgotPassword)
                                            .setTitle("Invalid Email")
                                            .setMessage("Email does not exist, Please Create a new Account")
                                            .setPositiveButton("Ok") { _, _ ->
                                            }
                                            .show()
                                        changebtn.isEnabled = true
                                        changebtn.isClickable = true
                                    }

                                }
                        }
                        else {
                            AlertDialog.Builder(this@ForgotPassword)
                                .setTitle("Invalid Email")
                                .setMessage("Email does not exist, Please Create a new Account")
                                .setPositiveButton("Ok") { _, _ ->
                                }
                                .show()
                            changebtn.isEnabled = true
                            changebtn.isClickable = true
                        }



                    }
                }




            } else {
                AlertDialog.Builder(this@ForgotPassword)
                    .setTitle("Invalid Email")
                    .setMessage("Please input a valid Email Address")
                    .setPositiveButton("Ok") { _, _ ->
                    }
                    .show()
                changebtn.isEnabled = true
                changebtn.isClickable = true
            }
        }
        var actionBar = supportActionBar
        actionBar?.hide()




    }
    fun generateRandomString(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = java.security.SecureRandom()

        return (1..length)
            .map { characters[random.nextInt(characters.length)] }
            .joinToString("")
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
                message.subject = "Password Reset Code"
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