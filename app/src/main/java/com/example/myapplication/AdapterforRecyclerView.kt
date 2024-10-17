package com.example.myapplication
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class AdapterforRecyclerView(private val context: Context, private val userList : ArrayList<fetchdatafromFirebase>) : RecyclerView.Adapter<AdapterforRecyclerView.MyViewHolder>() {
    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val prefkey = context.getString(R.string.prefKey)
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(prefkey, Context.MODE_PRIVATE)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterforRecyclerView.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.appointmentdata,parent,false)
        return MyViewHolder(itemView)
    }

    fun updateData(newList: List<fetchdatafromFirebase>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: AdapterforRecyclerView.MyViewHolder, position: Int) {
        val db = FirebaseFirestore.getInstance()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val pkidentifier = sharedPreferences.getString("pkIdentifier", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!
        val email = sharedPreferences.getString("email", null)!!
        var vaccination_count = 0
        db.collection("users").whereEqualTo("pkIdentifier", pkidentifier).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val userID = document.id
                    db.collection("users").document(userID).get().addOnSuccessListener { ref ->
                        val vaccinationCountStr = ref.getString("vaccination_count")
                        vaccination_count = vaccinationCountStr?.toIntOrNull() ?: 0
                        val appointmentList = ref?.get("appointmentList") as? MutableMap<String, Any> ?: mutableMapOf()
                        val containsKey2 = appointmentList.containsKey("2")
                        val containsKey1 = appointmentList.containsKey("1")
                        if (containsKey2 && !containsKey1) {
                            vaccination_count = 1
                            db.collection("users").document(userID)
                                .update("vaccination_count", vaccination_count)
                        }
                    }

                }



            }

        val user : fetchdatafromFirebase = userList[position]
        if(child_state == "TRUE"){
            if (user.adultvaccine.equals("")){
                holder.vaccineText.text =  user.childvaccine
            }
            else if (user.childvaccine.equals("")){
                holder.vaccineText.text = user.adultvaccine
            }
            else {
                holder.vaccineText.text = user.adultvaccine + " & " + user.childvaccine
            }
        }
        else if (child_state == "FALSE"){
            holder.vaccineText.text = user.adultvaccine
        }
        holder.locationText.text = user.location
        holder.datetimeText.text = user.date + " " + user.time
        val appointmentKeyID = user.appointmentKey
        holder.delete_btn.setOnClickListener {
            var appointmentDate = ""
            var appointmentLocation = ""
            var appointmentTime = ""
            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("Confirm Deletion")
            alertDialogBuilder.setMessage("Are you sure you want to delete this appointment?")
            alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
                db.collection("users").whereEqualTo("pkIdentifier", pkidentifier)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (userData in querySnapshot) {
                            val userDataID = userData.id
                            db.collection("users/$userDataID/appointments")
                                .whereEqualTo("appointmentKey", appointmentKeyID.toString())
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (x in querySnapshot) {
                                        var myHashMap = hashMapOf<String, Any>()
                                        val appointmentID = x.id
                                        if (child_state.equals("FALSE")) {
                                            appointmentDate = x.get("date") as String
                                            appointmentLocation = x.get("location") as String
                                            appointmentTime = x.get("time") as String
                                            val appointmentAdultVaccine =
                                                x.get("adultvaccine") as String
                                            myHashMap = hashMapOf(
                                                "date" to appointmentDate,
                                                "location" to appointmentLocation,
                                                "time" to appointmentTime,
                                                "adultvaccine" to appointmentAdultVaccine,
                                                "status" to "Cancelled",
                                                "pkIdentifier" to pkidentifier
                                            )

                                        }
                                        if (child_state.equals("TRUE")) {
                                            appointmentDate = x.get("date") as String
                                            appointmentLocation = x.get("location") as String
                                            appointmentTime = x.get("time") as String
                                            val appointmentAdultVaccine =
                                                x.get("adultvaccine") as String
                                            val appointmentChildVaccine =
                                                x.get("childvaccine") as String
                                            myHashMap = hashMapOf(
                                                "date" to appointmentDate,
                                                "location" to appointmentLocation,
                                                "time" to appointmentTime,
                                                "adultvaccine" to appointmentAdultVaccine,
                                                "childvaccine" to appointmentChildVaccine,
                                                "status" to "Cancelled",
                                                "pkIdentifier" to pkidentifier
                                            )
                                        }


                                        db.collection("users/$userDataID/deletedAppointments")
                                            .add(myHashMap).addOnSuccessListener {
                                            db.collection("users/$userDataID/appointments")
                                                .document(appointmentID).delete()
                                                .addOnSuccessListener {
                                                    vaccination_count--
                                                    val updates = hashMapOf(
                                                        "vaccination_count" to vaccination_count.toString()
                                                    )

                                                    val editor = sharedPreferences.edit()
                                                    editor.putString(
                                                        "vaccination_count",
                                                        vaccination_count.toString()
                                                    )
                                                    editor.apply()

                                                    db.collection("users")
                                                        .document(userDataID)
                                                        .get()
                                                        .addOnSuccessListener { documentSnapshot ->
                                                            if (documentSnapshot.exists()) {
                                                                val data = documentSnapshot.data
                                                                val appointmentList =
                                                                    data?.get("appointmentList") as? MutableMap<String, Any>
                                                                        ?: mutableMapOf()

                                                                val keytoRemove =
                                                                    appointmentKeyID?.let { it1 ->
                                                                        findKeyByValue(appointmentList,
                                                                            it1
                                                                        )
                                                                    }
                                                                appointmentList.remove(keytoRemove);
                                                                    val modifiedMap =
                                                                        keytoRemove?.let { key ->
                                                                            subtractOneFromKeys(
                                                                            appointmentList,
                                                                            key
                                                                        )
                                                                    }
                                                                    db.collection("users")
                                                                        .document(userDataID)
                                                                        .update("appointmentList", modifiedMap)
                                                                        .addOnSuccessListener {
                                                                            db.collection("users")
                                                                                .whereEqualTo(
                                                                                    "pkIdentifier",
                                                                                    pkidentifier
                                                                                )
                                                                                .get()
                                                                                .addOnSuccessListener { userQuerySnapshot ->
                                                                                    for (userDocument in userQuerySnapshot) {
                                                                                        val userDocumentID =
                                                                                            userDocument.id
                                                                                        db.collection(
                                                                                            "users"
                                                                                        )
                                                                                            .document(
                                                                                                userDocumentID
                                                                                            )
                                                                                            .update(
                                                                                                updates as Map<String, Any>
                                                                                            )
                                                                                            .addOnSuccessListener {
                                                                                                notifyDataSetChanged()
                                                                                                AlertDialog.Builder(
                                                                                                    context
                                                                                                )
                                                                                                    .setTitle(
                                                                                                        "Appointment Deleted"
                                                                                                    )
                                                                                                    .setMessage(
                                                                                                        "Your Appointment has been deleted, if you happen to have a current Chat Transaction just type Return, to Restart the Chat Session"
                                                                                                    )
                                                                                                    .setPositiveButton(
                                                                                                        "Ok"
                                                                                                    ) { _, _ ->
                                                                                                    }
                                                                                                    .show()
                                                                                                val stringSample = "Your Appointment that was Scheduled on $appointmentDate , $appointmentTime at $appointmentLocation has been Cancelled"
                                                                                                sendEmail(email,stringSample)

                                                                                            }
                                                                                            .addOnFailureListener { e ->
                                                                                                Log.w(
                                                                                                    "Firestore",
                                                                                                    "Error updating user document",
                                                                                                    e
                                                                                                )
                                                                                            }
                                                                                    }
                                                                                }
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            Log.w(
                                                                                "Firestore",
                                                                                "Error updating appointmentList",
                                                                                e
                                                                            )
                                                                        }





                                                            }
                                                        }
                                                }
                                        }
                                    }
                                }

                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error deleting appointment", e)
                                }
                        }
                    }
            }
            alertDialogBuilder.show()
        }
        holder.view_routes.setOnClickListener {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 505)
            } else {
                geocodeAddress(user.location)
            }
        }
    }
    fun geocodeAddress(address: String?) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(address.toString(), 1)
            println(addresses)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val latitude = addresses[0]?.latitude
                    val longitude = addresses[0]?.longitude
                    val intent = Intent(context, MapsDisplay::class.java)
                    intent.putExtra("destination_lat", latitude)
                    intent.putExtra("destination_long", longitude)
                    context.startActivity(intent)

                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
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

override fun getItemCount(): Int {
      return userList.size
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vaccineText: TextView = itemView.findViewById(R.id.vaccineText)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val datetimeText: TextView = itemView.findViewById(R.id.datetimeText)
        val delete_btn : Button = itemView.findViewById(R.id.delete_button)
        val view_routes : Button = itemView.findViewById(R.id.view_routes)



    }
    private fun sendEmail(receiverEmail: String, txtContent: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val smtp_host = context.getString(R.string.smtp_host)
                val smtp_starttls_state = context.getString(R.string.smtp_starttls_state)
                val smtp_auth = context.getString(R.string.smtp_auth)
                val smtp_port = context.getString(R.string.smtp_port)
                val no_reply_email = context.getString(R.string.no_reply_email)
                val no_reply_password = context.getString(R.string.no_reply_password)
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