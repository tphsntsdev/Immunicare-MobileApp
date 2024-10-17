package com.example.myapplication
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject

class MapsDisplay : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 506
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val back_btn = findViewById<ImageButton>(R.id.mapsback_btn)
        back_btn.setOnClickListener {
            val intent = Intent(this, Updatespage::class.java)
            startActivity(intent)
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)



    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    val receivedIntent = intent
                    val destination_lat = receivedIntent.getDoubleExtra("destination_lat", 0.0)
                    val destination_lng = receivedIntent.getDoubleExtra("destination_long", 0.0)
                    val destinationLatLng = LatLng(destination_lat,destination_lng)
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val origin = "${location.latitude},${location.longitude}"
                    val destination = "${destination_lat},${destination_lng}"
                    val markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    val destinationMarker = MarkerOptions()
                        .position(destinationLatLng)
                        .icon(markerIcon)

                    val originMarker = MarkerOptions()
                        .position(currentLatLng)

                    val apiKey = resources.getString(R.string.google_maps_key)



                    val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&key=$apiKey"
                    fetchNearestRoute(url,
                        { response ->
                            val routes = response.optJSONArray("routes")
                            if (routes != null && routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val polyline = route.getJSONObject("overview_polyline").getString("points")
                                val polylineOptions = PolylineOptions()
                                    .color(Color.GREEN)
                                decodePolyline(polyline).forEach {
                                    polylineOptions.add(it)
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                                    mMap.addPolyline(polylineOptions)
                                    mMap.addMarker(destinationMarker)
                                    mMap.addMarker(originMarker)

                                }

                            } else {

                            }
                        },
                        { error ->

                        }
                    )
                }
            }
    }
   private fun fetchNearestRoute(
        url: String,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ) {
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            listener, errorListener
        )
        requestQueue.add(jsonObjectRequest)
    }
    private fun decodePolyline(polyline: String): List<LatLng> {
        val points = ArrayList<LatLng>()
        var index = 0
        val len = polyline.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = polyline[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = polyline[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latitude = lat / 1E5
            val longitude = lng / 1E5
            val point = LatLng(latitude, longitude)
            points.add(point)
        }
        return points
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
            }
        }
    }
}


