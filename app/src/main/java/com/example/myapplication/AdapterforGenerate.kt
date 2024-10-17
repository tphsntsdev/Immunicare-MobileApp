package com.example.myapplication
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream





class AdapterforGenerate(private val context: Context, private val userList_1 : ArrayList<fetchcompleteappointment>) : RecyclerView.Adapter<AdapterforGenerate.MyViewHolder>(){
    private val prefkey = context.getString(R.string.prefKey)
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(prefkey, Context.MODE_PRIVATE)
    private var isClicked : Boolean = false



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterforGenerate.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.generatecertificate,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val child_state = sharedPreferences.getString("child_state", null)!!
        val newItems: List<String?>
        val vaccine: String?
        val location: String?
        val datetime: String?
        val certificateURL: String?
        val appointmentKey: String?
        var itemsAdded: MutableList<String?> = mutableListOf()

        if (child_state == "TRUE") {

            if (isClicked) {
                val updatedUser = userList_1[position]
                vaccine = updatedUser.childvaccine?: ""
                location = updatedUser.location?: ""
                datetime = updatedUser.date + " " + updatedUser.time?: ""
                certificateURL = updatedUser.childCertificateURL?: ""
                appointmentKey = updatedUser.appointmentKey?: ""

                if (!vaccine.isBlank() && !vaccine.isNullOrEmpty() && !vaccine.equals("None") && !vaccine.equals("")){
                    newItems = listOfNotNull(vaccine, location, datetime, certificateURL, appointmentKey)
                    itemsAdded.clear()
                    itemsAdded.addAll(newItems)

                }

            }
            else {
                val updatedUser1 = userList_1[position]
                vaccine = updatedUser1.adultvaccine?: ""
                location = updatedUser1.location?: ""
                datetime = updatedUser1.date + " " + updatedUser1.time?: ""
                certificateURL = updatedUser1.adultCertificateURL?: ""
                appointmentKey = updatedUser1.appointmentKey?: ""
                if (!vaccine.isBlank() && !vaccine.isNullOrEmpty() && !vaccine.equals("None") && !vaccine.equals("")){
                    newItems = listOfNotNull(vaccine, location, datetime, certificateURL, appointmentKey)
                    itemsAdded.clear()
                    itemsAdded.addAll(newItems)


                }

            }
            holder.vaccineText.text = itemsAdded.getOrElse(0) { "" }
            holder.locationText.text = itemsAdded.getOrElse(1) { "" }
            holder.datetimeText.text = itemsAdded.getOrElse(2) { "" }
            holder.PDFURL.text = itemsAdded.getOrElse(3) { "" }
            holder.hiddenAppointmentID.text  = itemsAdded.getOrElse(4) { "" }
            holder.downloadPDF.setOnClickListener {
                val link = holder.PDFURL.text.toString()
                val vaccine = holder.vaccineText.text.toString()
                startDownload(link, vaccine)


            }




        } else if (child_state == "FALSE") {
            val updatedUser = userList_1[position]
            val vaccine = updatedUser.adultvaccine
            val location = updatedUser.location
            val datetime = updatedUser.date + " " + updatedUser.time
            val certificateURL = updatedUser.adultCertificateURL
            val appointmentKey = updatedUser.appointmentKey
            holder.vaccineText.text = vaccine
            holder.locationText.text = location
            holder.datetimeText.text = datetime
            holder.PDFURL.text = certificateURL
            holder.hiddenAppointmentID.text = appointmentKey
            holder.downloadPDF.setOnClickListener {
                val link = holder.PDFURL.text.toString()
                val vaccine = holder.vaccineText.text.toString()
                startDownload(link, vaccine)




            }

        }

    }

    private val client by lazy {
        OkHttpClient.Builder()
            .build()
    }

    private fun startDownload(link: String, vaccine: String) {
        try {
            val credentialsFile = context.getString(R.string.credentialsFile)
            val googleScope = context.getString(R.string.googleScope)
            CoroutineScope(Dispatchers.IO).launch {
                val inputStream: InputStream = context.assets.open(credentialsFile)
                val credentials: GoogleCredentials = GoogleCredentials.fromStream(inputStream)
                val token = credentials.createScoped(googleScope)
                    .refreshAccessToken().tokenValue

                val request = Request.Builder()
                    .url(link)
                    .header("Authorization", "Bearer $token")
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                showDownloadFailedToast("Download failed: ${response.code}")
                            }
                            return@use
                        }

                        val localFile = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$vaccine.pdf"
                        )

                        response.body?.byteStream()?.use { input ->
                            localFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            openPdfFile(localFile)
                            println("Local File Path: ${localFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showDownloadFailedToast("Download failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showDownloadFailedToast("Download failed: ${e.message}")
        }
    }

    private fun showDownloadFailedToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun openPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val pdfIntent = Intent(Intent.ACTION_VIEW)
        pdfIntent.setDataAndType(uri, "application/pdf")
        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(pdfIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }
    override fun getItemCount(): Int {
        return userList_1.size
    }
    fun setIsClicked(value: Boolean){
        isClicked = value
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vaccineText: TextView = itemView.findViewById(R.id.vaccineText_1)
        val locationText: TextView = itemView.findViewById(R.id.locationText_1)
        val datetimeText: TextView = itemView.findViewById(R.id.datetimeText_1)
        val PDFURL : TextView = itemView.findViewById(R.id.PDFURL_1)
        val hiddenAppointmentID : TextView = itemView.findViewById(R.id.hiddenAppointmentID_1)
        val downloadPDF : Button = itemView.findViewById(R.id.downloadPDF)





    }




}