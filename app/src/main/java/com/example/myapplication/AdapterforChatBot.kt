import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ChatSessionManager
import com.example.myapplication.R
import com.example.myapplication.chatbot_data
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.PlacesApi
import com.google.maps.model.GeocodingResult
import com.google.maps.model.Geometry
import com.google.maps.model.PlaceType
import com.google.maps.model.PlacesSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class AdapterforChatBot(
    private val context: Context,
    private val messageList: MutableList<chatbot_data>,
    private val lifecycleScope: CoroutineScope
) :
    RecyclerView.Adapter<AdapterforChatBot.MessageViewHolder>() {
    private var fusedLocationClient: FusedLocationProviderClient
    private val prefkey =  context.getString(R.string.prefKey)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefkey, Context.MODE_PRIVATE)
    private var accountselected = false
    private var vaccineselected = false
    private var adultselected = false
    private var childselected = false
    private var nochild = false

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (messageList.isEmpty()) {
            val initialBotMessage = chatbot_data(
                "Welcome! I am your virtual assistant. How can I assist you today?, use Keywords such as  \"Account\", \"Vaccines\",\"Locations\",\"About Us\"",
                false
            )
            messageList.add(initialBotMessage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = when (viewType) {
            VIEW_TYPE_USER -> {
                inflater.inflate(R.layout.item_user_message, parent, false)
            }

            VIEW_TYPE_BOT -> {
                inflater.inflate(R.layout.item_bot_message, parent, false)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message)
        if (message.isFromUser && !message.isProcessed) {
            message.isProcessed = true

            lifecycleScope.launch(Dispatchers.Main) {
                val botResponse = generateBotResponse(message.text)
                val botMessage = botResponse
                messageList.add(botMessage)
                notifyDataSetChanged()
                ChatSessionManager.getInstance(context).saveMessages(messageList)
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isFromUser) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_BOT
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userMessageTextView: TextView? = itemView.findViewById(R.id.userMessageTextView)
        private val botMessageTextView: TextView? = itemView.findViewById(R.id.botMessageTextView)


        fun bind(message: chatbot_data) {
            if (message.isFromUser) {
                userMessageTextView?.text = message.text
            } else {
                botMessageTextView?.text = message.text
            }
        }
    }

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_BOT = 2
    }

    private suspend fun generateBotResponse(userMessage: String): chatbot_data {
        val apiKey = context.getString(R.string.google_maps_key)
        val date_account_created = sharedPreferences.getString("accountCreated", null)!!
        val child_state = sharedPreferences.getString("child_state", null)!!
        val geoApiContext = GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()
        if (child_state.equals("FALSE")) {
            nochild = true
        }
        if (child_state.equals("TRUE")) {
            nochild = false
        }
        return when {
            userMessage.contains("locations", true) -> {
                val userLocation = getUserLocation(context)
                var sampleString = ""
                if (userLocation != null) {
                    val currentLatitude = userLocation.latitude
                    val currentLongitude = userLocation.longitude
                    val nearbyHospitals = getAddressesWithinRadiusWithNearbyPlaces(
                        geoApiContext,
                        currentLatitude,
                        currentLongitude
                    )
                    if (nearbyHospitals != null && nearbyHospitals.isNotEmpty()) {
                        val top3NearestLocations = getTop3NearestLocations(
                            nearbyHospitals,
                            currentLatitude,
                            currentLongitude
                        )
                        sampleString =
                            "The top 3 nearest hospital/clinic(s) by rating are:\n$top3NearestLocations"
                    } else {
                        sampleString = "No nearby hospitals or clinics found."
                    }
                } else {
                    sampleString = "Unable to determine your location."
                }
                chatbot_data(sampleString, false)
            }

            userMessage.contains("hello", true) ->
                chatbot_data(
                    "Hello! How can I assist you today?, use Keywords such as  \"Account\", \"Vaccines\",\"Locations\",\"About Us\"",
                    false
                )

            userMessage.contains("account", true) -> {
                accountselected = true
                chatbot_data(
                    "Sure, Let me assist you, what would you like to know about your Account, type \"Appointments\" for ongoing Appointment, \"Date \" to know when your Account is Created and \"Return\" to return from the previous selection",
                    false
                )
            }

            userMessage.contains("vaccines", true) -> {
                vaccineselected = true
                var stringGenerated =
                    "Sure, Let me assist you, what would you like to know in terms of the Vaccines that most hospital offers, to know more about the Available Vaccine may i ask, for whom exactly is the Vaccine, Kindly Type \"Adult\" for you or \"Child\" for your child"
                chatbot_data(
                    stringGenerated,
                    false
                )
            }

            userMessage.contains("About us", true) -> {
                chatbot_data(
                    "Immunicare is Quick access from nearby Health Center for immunization schedules, complete with booking details.\n" +
                            "Intended for healthcare professionals recommending and administering vaccines and immunization to infants, children, adolescents, and adults. \n",
                    false
                )
            }

            accountselected && userMessage.contains("appointments", true) -> {
                val appointmentResult = appointmentChecker()
                chatbot_data(appointmentResult, false)
            }

            accountselected && userMessage.contains("Date", true) -> {
                chatbot_data(
                    "Your Account is created on $date_account_created, please select from the following options \"Appointments\", \"Return\"",
                    false
                )
            }

            userMessage.contains("Return", true) -> {
                accountselected = false
                vaccineselected = false
                childselected = false
                adultselected = false

                chatbot_data(
                    "Hello! How can I assist you today?, use Keywords such as  \"Account\", \"Vaccines\",\"Locations\",\"About Us\"",
                    false
                )
            }

            vaccineselected && userMessage.contains("Child", true) -> {
                childselected = true
                chatbot_data(
                    "As per Department of Health, The Vaccines Applicable for your child are Bacillus Calmette-Guérin Vaccine(BCG),Hepatitis B(Hep B),Tetanus Diptheria(TD),Oral Polio Vaccine(OPV),PENTA Vaccine(PENTA),Pneumococcal Conjugate Vaccine(PCV),Rotavirus Vaccine(RV),Inactivated Polio Vaccine(IPV),Measles–Mumps-Rubella Vaccine(MMR),Japanese Encephalitis(JE),Measles-Rubella Vaccine(MR),Human Papilloma Vaccine(HPV), To know more about the said vaccine you may enter the Vaccine that you need to know by its initials which is enclosed with a Parenthesis ",
                    false
                )
            }

            vaccineselected && userMessage.contains("Adult", true) -> {
                adultselected = true
                chatbot_data(
                    "As per Department of Health, The Vaccines Applicable for you are Pneumococcal Polysaccharide Vaccine (PPV),Influenza Vaccine(Flu),COVID Vaccine(COVID),To know more about the said vaccine you may enter the Vaccine that you need to know by its initials which is enclosed with a Parenthesis",
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("BCG", true) -> {
                chatbot_data(
                    """
BCG Vaccine : 
A vaccine primarily used against tuberculosis (TB) it is part of routine childhood vaccination programs. 
The vaccine is generally given shortly after birth. While its effectiveness in preventing pulmonary TB 
in adults is variable, it is effective in preventing severe TB in young children.

Side Effects : 
- Severe allergic reactions
- Pustule or ulceration at site of injection, 
- Pain, redness and swelling around the injection site
- Swelling of the glands in the armpit and/ or neck

Route of Administration : Into the Skin
Schedule  : At Birth

To go back please enter "Return" or you may type a different vaccine from the following "Hep B","TD","OPV","PENTA","PCV","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("Hep B", true) -> {
                chatbot_data(
                    """
Hepatitis B (HepB) Vaccine: 
A vaccine designed to protect against hepatitis B virus (HBV) infection, 
which can cause chronic liver disease and increase the risk of liver cancer. 
It contains purified proteins from the virus that stimulate the immune system to develop protection.

Side Effects : 
- Fever of 37.7 degrees C (100 degrees F) or higher
- Aches or pain in the joints, fever, or skin rash or welts (may occur days or weeks after receiving the vaccine)
- dizziness, faintness, or lightheadedness when getting up suddenly from a lying or sitting position
- Muscle weakness
- Sweating

Route of Administration : Into a Muscle
Schedule  : At Birth

To go back please enter "Return" or you may type a different vaccine from the following "BCG","TD","OPV","PENTA","PCV","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("TD", true) -> {
                chatbot_data(
                    """
Tetanus-Diphtheria (Td) Vaccine: 
A preventive shot that protects against two serious bacterial infections: tetanus and diphtheria. 
Tetanus, often called lockjaw, is caused by bacteria that enter through wounds, 
while diphtheria affects the throat and can cause severe breathing problems.

Side Effects: 
- Redness, swelling, or tenderness
- Mild fever
- Fatigue or general discomfort
- Headache or muscle aches

Route of Administration: Into a muscle
Schedule: 
- Grade 1 and 7 for Children
- 1st Dose - As Early as Pregnancy
- 2nd Dose - 4 Weeks after 1st Dose
- 3rd Dose - 6 months after 2nd Dose
- 4th Dose - 1 year after 3rd Dose
- 5th Dose - 1 year after 4th Dose

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("OPV", true) -> {
                chatbot_data(
                    """
Oral Polio Vaccine (OPV):
A live, attenuated vaccine used to prevent poliomyelitis, commonly known as polio. Administered orally
OPV contains weakened poliovirus strains that stimulate an immune response without causing the disease

Side Effects:
- Fever
- Irritability
- Redness or mild swelling

Route of Administration: Oral
Schedule:
- 1st Dose - 6 Weeks of Age
- 2nd Dose - 10 Weeks of Age
- 3rd Dose - 14 Weeks of Age

To go back please enter "Return" or you may type a different vaccine from the following "TD","BCG","Hep B","PENTA","PCV","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("PENTA", true) -> {
                chatbot_data(
                    """
PENTA Vaccine (DPT-HepB+Hib):
A combination vaccine that protects against five serious diseases: diphtheria, tetanus, pertussis (whooping cough), hepatitis B, and Haemophilus influenzae type b (Hib).
Administered as a single shot, PENTA streamlines the vaccination process, reducing the number of injections needed during early childhood.

Side Effects:
- Redness, swelling, or tenderness at the injection site
- Fever
- Irritability or fussiness
- Decreased appetite
- Fatigue or drowsiness

Route of Administration: Into a Muscle
Schedule:
- 1st Dose - 6 Weeks of Age
- 2nd Dose - 10 Weeks of Age
- 3rd Dose - 14 Weeks of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","TD","PCV","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("PCV", true) -> {
                chatbot_data(
                    """
Pneumococcal Conjugate (PCV) Vaccine:
Protects against infections caused by the bacterium Streptococcus pneumoniae, which can lead to serious illnesses such as pneumonia, meningitis, and bloodstream infections.
Administered in a series of doses during early childhood, PCV helps to prevent these potentially severe diseases by stimulating the immune system to recognize and fight the bacteria.

Side Effects:
- Redness, swelling, or tenderness at the injection site
- Fever
- Irritability or fussiness (in infants)
- Decreased appetite

Route of Administration: Into a Muscle
Schedule:
- 1st Dose - 6 Weeks of Age
- 2nd Dose - 10 Weeks of Age
- 3rd Dose - 14 Weeks of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","TD","RV","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("RV", true) -> {
                chatbot_data(
                    """
Rotavirus Vaccine:
Protects against rotavirus, a common virus that causes severe diarrhea, vomiting, and dehydration in infants and young children.
Administered orally, this vaccine helps prevent rotavirus infections, reducing the risk of hospitalizations and severe illness associated with the virus.

Side Effects:
- Fussiness or irritability (in infants)
- Mild diarrhea
- Vomiting

Route of Administration: Oral
Schedule:
- 1st Dose - 6 Weeks of Age
- 2nd Dose - 10 Weeks of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","TD","IPV","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("IPV", true) -> {
                chatbot_data(
                    """
Inactivated Polio Vaccine (IPV):
A shot that protects against poliomyelitis, or polio, a debilitating viral disease that can cause paralysis.
IPV contains inactivated (killed) poliovirus, which stimulates the immune system to build protection against polio without causing the disease.

Side Effects:
- Redness, swelling, or tenderness at the injection site
- Mild fever
- Allergic reactions (such as rash, itching, or swelling)

Route of Administration: Into the Muscle
Schedule:
- 14 Weeks of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","TD","MMR","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("MMR", true) -> {
                chatbot_data(
                    """
Measles-Mumps-Rubella (MMR) Vaccine:
Protects against three contagious viral diseases: measles, mumps, and rubella (German measles). It stimulates the immune system to build protection against these infections, 
which can cause serious complications such as pneumonia, encephalitis, and birth defects.
The MMR vaccine is typically given in two doses during early childhood, ensuring effective immunity and contributing to the prevention of outbreaks.

Side Effects:
- Redness or swelling at the injection site
- Fever
- Rash (a mild, non-contagious rash that typically appears 7-10 days after vaccination)
- Mild swelling of the glands in the cheeks or neck

Route of Administration: Under the Skin
Schedule:
- 1st Dose - 9 Months of Age
- 2nd Dose - 12 Months of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","IPV","TD","JE","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("JE", true) -> {
                chatbot_data(
                    """
Japanese Encephalitis Vaccine:
Protects people against viral encephalitis caused by Japanese Encephalitis virus (JEV). JEV is the leading cause of viral encephalitis in Asia.
A serious viral infection transmitted by mosquitoes that can cause inflammation of the brain.

Side Effects:
- Redness, swelling, or tenderness at the injection site
- Fever
- Headache
- Fatigue

Route of Administration: Under the Skin
Schedule:
- 9 Months of Age

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","IPV","MMR","TD","MR","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("MR", true) -> {
                chatbot_data(
                    """
Measles, Rubella (MR) Vaccine:
Protects against two contagious viral diseases: measles and rubella (German measles),
it stimulates the immune system to build protection against these diseases, which can lead to serious complications such as pneumonia, encephalitis, and birth defects.

Side Effects:
- Redness or swelling at the injection site
- Fever
- Rash (a mild, non-contagious rash that can appear 7-10 days after vaccination)
- Irritability or fussiness (especially in younger children)

Route of Administration: Under the Skin
Schedule:
- 1st Dose - 5 Years Old
- 2nd Dose - 7 Years Old

To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","IPV","MMR","JE","TD","HPV"
""".trimIndent(),
                    false
                )
            }

            childselected && vaccineselected && userMessage.contains("HPV", true) -> {
                chatbot_data(
                    """
    Human Papilloma Virus (HPV) Vaccine:
    Protects against certain types of cancer and genital warts caused by Human Papillomavirus (HPV).
    
    Side Effects:
    - Pain, redness, or swelling at the injection site
    - Fever
    - Headache
    - Fatigue
    - Nausea
    
    Route of Administration: Into the Muscle
    Schedule: 
    - For Female : 9-10 Years Old
    
    To go back please enter "Return" or you may type a different vaccine from the following "OPV","BCG","Hep B","PENTA","PCV","RV","IPV","MMR","JE","MR","TD"
""".trimIndent(),
                    false
                )
            }

            adultselected && vaccineselected && userMessage.contains("PPV", true) -> {
                chatbot_data(
                    """
Pneumo-Polysaccharide Vaccine or PPV:
PPV targets a broader range of pneumococcal strains and is typically recommended for those over 65 or with certain health conditions, 
PPV helps reduce the risk of serious pneumococcal diseases by stimulating the immune system to recognize and combat the bacteria.

Side Effects:
- Mild fever
- Fatigue
- Muscle aches

Route of Administration: Into the Muscle
Schedule: 
- 1st Dose - 60 Years Old
- 2nd Dose - 65 Years Old

To go back please enter "Return" or you may type a different vaccine from the following ,"Flu","COVID"
""".trimIndent(),
                    false
                )
            }

            adultselected && vaccineselected && userMessage.contains("Flu", true) -> {
                chatbot_data(
                    """
Influenza (flu) vaccines:
protects against seasonal influenza, a viral infection that can cause fever, cough, sore throat, body aches, and fatigue. 
Administered annually, the vaccine helps to reduce the risk of severe flu-related complications, such as pneumonia and hospitalization.

Side Effects:
- Redness, swelling, or tenderness at the injection site
- Mild fever
- Fatigue
- Headache
- Muscle aches

Route of Administration: Into the Muscle
Schedule: 
- 60 Years Old Above

To go back please enter "Return" or you may type a different vaccine from the following ,"PPV","COVID"
""".trimIndent(),
                    false
                )
            }

            adultselected && vaccineselected && userMessage.contains("COVID", true) -> {
                chatbot_data(
                    "The prioritization framework for COVID-19 vaccination was formulated due to the limited global supply of COVID-19 vaccine products. This will depend on the vaccine. For those currently available, Sinovac can be given to clinically healthy individuals 18 to 59 years old, while AstraZeneca can be given to those 18 years old and above, including senior citizens and children 6 months old above.. To go back please enter \"Return\" or you may type a different vaccine from the following ,\"PPV\",\"FLU\"",
                    false
                )
            }

            else -> chatbot_data(
                "I'm sorry, it seems that you have entered a value that is not included on the said choices, to return, please type \"Return\"",
                false
            )
        }
    }

    private fun checkLocationPermission(context: Context): Boolean {
        return (ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }


    private suspend fun checkLocationSettings(context: Context): Boolean {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(context)

        return try {
            settingsClient.checkLocationSettings(locationSettingsRequest).await()
            true
        } catch (e: ResolvableApiException) {
            try {
                e.startResolutionForResult(context as Activity, 123)
            } catch (sendEx: IntentSender.SendIntentException) {
            }
            false
        }
    }

    private suspend fun getUserLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return if (checkLocationPermission(context)) {
            if (checkLocationSettings(context)) {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                506
            )

            null
        }
    }


    private fun getAddressesWithinRadiusWithNearbyPlaces(
        apiContext: GeoApiContext,
        currentLatitude: Double,
        currentLongitude: Double
    ): List<Place>? {
        try {
            val location = com.google.maps.model.LatLng(currentLatitude, currentLongitude)
            val geocodingResults = GeocodingApi.reverseGeocode(apiContext, location).await()
            val validResults =
                geocodingResults.filter { it.geometry?.location?.lat != null && it.geometry.location.lng != null }
            val nearbyPlaces = getNearbyPlaces(
                apiContext, currentLatitude, currentLongitude, 5000, listOf(
                    PlaceType.HOSPITAL, PlaceType.LOCAL_GOVERNMENT_OFFICE
                )
            )
            val combinedResults = validResults + nearbyPlaces
            val uniqueResults = combinedResults.toSet().toList() // Remove duplicates

            val filteredResults = uniqueResults.filter {
                val distance = calculateDistance(
                    currentLatitude,
                    currentLongitude,
                    it.geometry.location.lat,
                    it.geometry.location.lng
                )
                val isWithinRadius = distance <= 5000
                val containsKeywords = containsHospitalOrClinicKeywords(it)
                isWithinRadius && containsKeywords
            }
            val places = filteredResults.map { convertGeocodingResultToPlace(it) }

            return places

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getNearbyPlaces(
        apiContext: GeoApiContext,
        currentLatitude: Double,
        currentLongitude: Double,
        radius: Int,
        placeTypes: List<PlaceType>
    ): List<GeocodingResult> {
        val location = com.google.maps.model.LatLng(currentLatitude, currentLongitude)
        val allResults = mutableListOf<GeocodingResult>()
        for (placeType in placeTypes) {
            val nearbySearchRequest = PlacesApi.nearbySearchQuery(apiContext, location)
                .radius(radius)
                .type(placeType)
                .await()
            val geocodingResults =
                nearbySearchRequest.results.map { convertPlacesSearchResultToGeocodingResult(it) }

            allResults.addAll(geocodingResults)
        }

        return allResults
    }

    private fun convertPlacesSearchResultToGeocodingResult(placeResult: PlacesSearchResult): GeocodingResult {
        val geocodingResult = GeocodingResult()
        geocodingResult.formattedAddress = placeResult.name
        geocodingResult.geometry = Geometry().apply {
            location = placeResult.geometry.location
        }
        return geocodingResult
    }


    private fun containsHospitalOrClinicKeywords(geocodingResult: GeocodingResult): Boolean {
        val keywords = listOf("hospital", "clinic", "health", "medical")
        val placeName = geocodingResult.formattedAddress ?: ""
        val placeTypes: List<String> = geocodingResult.types?.map { it.toString() } ?: emptyList()
        val containsKeywords = keywords.any { keyword ->
            placeName.contains(keyword, ignoreCase = true)
        }

        val containsPlaceTypes = placeTypes.any { type ->
            listOf("hospital", "doctor", "health").contains(type)
        }

        return containsKeywords || containsPlaceTypes
    }


    private fun convertGeocodingResultToPlace(geocodingResult: GeocodingResult): Place {
        val placeName = geocodingResult.formattedAddress ?: ""
        val geometry = geocodingResult.geometry
        val location = geometry?.location

        val placeLatLng = location?.let {
            com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
        }

        return Place.builder()
            .setName(placeName)
            .setLatLng(placeLatLng)
            .build()
    }

    private fun getTop3NearestLocations(
        nearbyPlaces: List<Place>?,
        currentLatitude: Double,
        currentLongitude: Double
    ): String {
        if (nearbyPlaces.isNullOrEmpty()) {
            return "No nearby hospitals or clinics found."
        }

        val sortedPlaces = nearbyPlaces.sortedWith(
            compareBy(
                {
                    calculateDistance(
                        currentLatitude,
                        currentLongitude,
                        it.latLng?.latitude,
                        it.latLng?.longitude
                    )
                },
            )
        ).take(3)

        val top3LocationsString = StringBuilder(sortedPlaces.size * 50)

        for ((index, place) in sortedPlaces.withIndex()) {
            top3LocationsString.append(
                "${index + 1}. ${place.name} , Distance: ${
                    calculateDistance(
                        currentLatitude,
                        currentLongitude,
                        place.latLng?.latitude,
                        place.latLng?.longitude
                    ).roundToInt()
                } meters)\n"
            )
        }

        return top3LocationsString.toString()
    }

    private fun calculateDistance(
        currentLatitude: Double,
        currentLongitude: Double,
        placeLatitude: Double?,
        placeLongitude: Double?
    ): Double {
        if (placeLatitude == null || placeLongitude == null) {
            return Double.MAX_VALUE
        }

        val R = 6371000.0

        val lat1 = Math.toRadians(currentLatitude)
        val lon1 = Math.toRadians(currentLongitude)
        val lat2 = Math.toRadians(placeLatitude)
        val lon2 = Math.toRadians(placeLongitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    private suspend fun appointmentChecker(): String {
        val appointmentStringBuilder = StringBuilder()
        val firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("users")
        val pkIdentifier = sharedPreferences.getString("pkIdentifier", null) ?: return "No identifier found"

        try {
            val querySnapshot = collectionReference
                .whereEqualTo("pkIdentifier", pkIdentifier)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                val userID = document.id
                val appointmentList = document.get("appointmentList") as? MutableMap<String, Any>
                val vaccinationCountStr = document.getString("vaccination_count")
                val vaccinationCount = vaccinationCountStr?.toIntOrNull() ?: 0
                val childState = sharedPreferences.getString("child_state", null) ?: ""

                if (vaccinationCount == 0) {
                    appointmentStringBuilder.append("It seems that you have no appointment, please go ahead and book an appointment under the navigation bar bottom part of your Screen. Please take note that the maximum appointment you can make is 2. \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                }

                if (childState == "TRUE") {
                    if (vaccinationCount == 1) {
                        val field1 = appointmentList?.get("1")
                        val querySnapshot1 = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field1)
                            .get()
                            .await()

                        for (y in querySnapshot1.documents) {
                            val childVaccine = y.getString("childvaccine") ?: ""
                            val vaccine = y.getString("adultvaccine") ?: ""
                            val date = y.getString("date") ?: ""
                            val location = y.getString("location") ?: ""
                            val time = y.getString("time") ?: ""
                            appointmentStringBuilder.append("You have 1 Appointment that will be held in $location, for the Vaccine: $vaccine & $childVaccine, on $date, $time. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                        }
                    }

                    if (vaccinationCount == 2) {
                        val field1 = appointmentList?.get("1")
                        val field2 = appointmentList?.get("2")

                        val querySnapshot1Task = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field1)
                            .get()

                        val querySnapshot2Task = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field2)
                            .get()

                        val results = Tasks.whenAllSuccess<QuerySnapshot>(querySnapshot1Task, querySnapshot2Task).await()
                        val querySnapshot1 = results[0] as QuerySnapshot
                        val querySnapshot2 = results[1] as QuerySnapshot

                        for (x in querySnapshot1.documents) {
                            for (y in querySnapshot2.documents) {
                                val vaccine1 = x.getString("adultvaccine") ?: ""
                                val childVaccine1 = x.getString("childvaccine") ?: ""
                                val date1 = x.getString("date") ?: ""
                                val location1 = x.getString("location") ?: ""
                                val time1 = x.getString("time") ?: ""

                                val vaccine = y.getString("adultvaccine") ?: ""
                                val childVaccine = y.getString("childvaccine") ?: ""
                                val date = y.getString("date") ?: ""
                                val location = y.getString("location") ?: ""
                                val time = y.getString("time") ?: ""

                                if (location == location1 && vaccine == vaccine1 && childVaccine == childVaccine1) {
                                    appointmentStringBuilder.append("You have 2 Appointments that will be held in $location, for the Vaccine: $vaccine, for your child: $childVaccine on the following dates: $date1, $date, at the time: $time1, $time respectively. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                                } else {
                                    appointmentStringBuilder.append("You have 2 Appointments that will be held in the locations: $location1 and $location, for the following Vaccines: $vaccine1, $vaccine, for your child: $childVaccine1, $childVaccine on the following dates: $date1, $date, at the time: $time1, $time respectively. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                                }
                            }
                        }
                    }
                } else if (childState == "FALSE") {
                    if (vaccinationCount == 1) {
                        val field1 = appointmentList?.get("1")
                        val querySnapshot1 = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field1)
                            .get()
                            .await()

                        for (y in querySnapshot1.documents) {
                            val vaccine = y.getString("adultvaccine") ?: ""
                            val date = y.getString("date") ?: ""
                            val location = y.getString("location") ?: ""
                            val time = y.getString("time") ?: ""
                            appointmentStringBuilder.append("You have 1 Appointment that will be held in $location, for the Vaccine: $vaccine, on $date, $time. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                        }
                    }

                    if (vaccinationCount == 2) {
                        val field1 = appointmentList?.get("1")
                        val field2 = appointmentList?.get("2")

                        val querySnapshot1Task = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field1)
                            .get()

                        val querySnapshot2Task = collectionReference.document(userID).collection("appointments")
                            .whereEqualTo("appointmentKey", field2)
                            .get()

                        val results = Tasks.whenAllSuccess<QuerySnapshot>(querySnapshot1Task, querySnapshot2Task).await()
                        val querySnapshot1 = results[0] as QuerySnapshot
                        val querySnapshot2 = results[1] as QuerySnapshot

                        for (x in querySnapshot1.documents) {
                            for (y in querySnapshot2.documents) {
                                val vaccine1 = x.getString("adultvaccine") ?: ""
                                val date1 = x.getString("date") ?: ""
                                val location1 = x.getString("location") ?: ""
                                val time1 = x.getString("time") ?: ""

                                val vaccine = y.getString("adultvaccine") ?: ""
                                val date = y.getString("date") ?: ""
                                val location = y.getString("location") ?: ""
                                val time = y.getString("time") ?: ""

                                if (location == location1 && vaccine == vaccine1) {
                                    appointmentStringBuilder.append("You have 2 Appointments that will be held in $location, for the Vaccine: $vaccine, on the following dates: $date1, $date, at the time: $time1, $time respectively. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                                } else {
                                    appointmentStringBuilder.append("You have 2 Appointments that will be held in the locations: $location1 and $location, for the following Vaccines: $vaccine1, $vaccine, on the following dates: $date1, $date, at the time: $time1, $time respectively. Type \"Date\" to know when your Account is Created and \"Return\" to return from the previous selection.")
                                }
                            }
                        }
                    }
                }
            }
            return appointmentStringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "An error occurred."
        }
    }
}



