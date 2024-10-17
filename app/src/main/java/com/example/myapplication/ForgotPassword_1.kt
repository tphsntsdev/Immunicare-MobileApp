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
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ForgotPassword_1 : AppCompatActivity() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var prefkey_cp: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password1)
        FirebaseApp.initializeApp(this)
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        val back_btn = findViewById(R.id.backbtn) as TextView
        val resetPassword = findViewById(R.id.button2) as Button
        val resetCode = findViewById(R.id.resetCode) as EditText
        val newPassword = findViewById(R.id.newPassword) as EditText
        prefkey_cp = resources.getString(R.string.prefKey_cp)
        sharedPreferences =
            getSharedPreferences(prefkey_cp, Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)!!



        back_btn.setOnClickListener {
            sharedPreferences.edit().clear().commit()
            val intent = Intent(this,ForgotPassword::class.java)
            startActivity(intent)
            finish()

        }
        resetPassword.setOnClickListener {
            resetPassword.isEnabled = false
            resetPassword.isClickable = false
            val newPasswordTxt = newPassword.text.toString()
            val resetCodeTxt = resetCode.text.toString()

            if (newPasswordTxt.isEmpty() || resetCodeTxt.isEmpty()) {
                AlertDialog.Builder(this@ForgotPassword_1)
                    .setTitle("Empty Fields")
                    .setMessage("Please enter the necessary information (Reset Code and New Password)")
                    .setPositiveButton("Ok") { _, _ ->
                    }
                    .show()
                resetPassword.isEnabled = true
                resetPassword.isClickable = true
            } else {
                collectionReference.whereEqualTo("passwordResetCode", resetCodeTxt).whereEqualTo("email",email).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (data_value in querySnapshot) {
                                val encryptionMethod = resources.getString(R.string.encryptionMethod)
                                val encryptionHash = resources.getString(R.string.encryptionHash)
                                val documentKey = data_value.id
                                val salt = generateSalt()
                                val spec: KeySpec = PBEKeySpec(newPasswordTxt.toCharArray(), salt.toByteArray(), 65536, 256)
                                val secretKeyFactory = SecretKeyFactory.getInstance(encryptionHash)
                                val secretKey: SecretKey = SecretKeySpec(secretKeyFactory.generateSecret(spec).encoded, encryptionMethod)// Replace with your salt generation logic
                                val encryptedPassword = encrypt(newPasswordTxt, secretKey)
                                val updates = hashMapOf<String, Any>(
                                    "salt" to salt,
                                    "password" to encryptedPassword
                                )
                                collectionReference.document(documentKey).update(updates)
                                    .addOnSuccessListener {
                                        AlertDialog.Builder(this@ForgotPassword_1)
                                            .setTitle("Success")
                                            .setMessage("Password updated successfully")
                                            .setPositiveButton("Ok") { _, _ ->
                                                val intent = Intent(this,SignInActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                            .show()


                                    }
                                    .addOnFailureListener {
                                        AlertDialog.Builder(this@ForgotPassword_1)
                                            .setTitle("Error")
                                            .setMessage("Failed to update password")
                                            .setPositiveButton("Ok") { _, _ ->
                                            }
                                            .show()
                                    }
                            }
                            resetPassword.isEnabled = true
                            resetPassword.isClickable = true
                        } else {
                            AlertDialog.Builder(this@ForgotPassword_1)
                                .setTitle("Error")
                                .setMessage("Invalid Reset Code, Please try again")
                                .setPositiveButton("Ok") { _, _ ->
                                }
                                .show()
                            resetPassword.isEnabled = true
                            resetPassword.isClickable = true
                        }
                    }
                    .addOnFailureListener {
                        AlertDialog.Builder(this@ForgotPassword_1)
                            .setTitle("Error")
                            .setMessage("Failed to fetch data")
                            .setPositiveButton("Ok") { _, _ ->
                            }
                            .show()
                        resetPassword.isEnabled = true
                        resetPassword.isClickable = true
                    }
                resetPassword.isEnabled = true
                resetPassword.isClickable = true
            }
        }
    }

    private fun encrypt(text: String, secretKey: SecretKey): String {
        val decryptionMethod = resources.getString(R.string.decryptionMethod)
        val cipher = Cipher.getInstance(decryptionMethod)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }
}