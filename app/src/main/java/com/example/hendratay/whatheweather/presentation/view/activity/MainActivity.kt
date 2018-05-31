package com.example.hendratay.whatheweather.presentation.view.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.example.hendratay.whatheweather.R
import com.example.hendratay.whatheweather.presentation.data.Resource
import com.example.hendratay.whatheweather.presentation.data.ResourceState
import com.example.hendratay.whatheweather.presentation.model.*
import com.example.hendratay.whatheweather.presentation.view.adapter.ForecastAdapter
import com.example.hendratay.whatheweather.presentation.view.utils.WeatherIcon
import com.example.hendratay.whatheweather.presentation.viewmodel.CurrentWeatherViewModel
import com.example.hendratay.whatheweather.presentation.viewmodel.CurrentWeatherViewModelFactory
import com.example.hendratay.whatheweather.presentation.viewmodel.WeatherForecastViewModel
import com.example.hendratay.whatheweather.presentation.viewmodel.WeatherForecastViewModelFactory
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dagger.android.AndroidInjection
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*

// Todo: add swipe to refresh view

const val TAG = "MainActivity"
const val REQUEST_ACCESS_FINE_LOCATION = 111
const val REQUEST_CHECK_SETTINGS = 222
class MainActivity : AppCompatActivity() {

    @Inject lateinit var currentWeatherViewModelFactory: CurrentWeatherViewModelFactory
    @Inject lateinit var weatherForecastViewModelFactory: WeatherForecastViewModelFactory

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var currentWeatherViewModel: CurrentWeatherViewModel
    private lateinit var weatherForecastViewModel: WeatherForecastViewModel
    private lateinit var adapter: ForecastAdapter

    private var forecastList: MutableList<ForecastView> = ArrayList()
    // Todo: follow display a location address guide
    private lateinit var geocoder: Geocoder
    private var address: List<Address> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidInjection.inject(this)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyErrorButtonClick()

        checkLocationPermission()
        createLocationRequest()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // initiate location callback first for pass into requestlocationupdates at `startLocationUpdates`
        receiveLocationUpdates()
        startLocationUpdates()

        geocoder = Geocoder(this, Locale.getDefault())

        currentWeatherViewModel = ViewModelProviders.of(this, currentWeatherViewModelFactory).get(CurrentWeatherViewModel::class.java)
        weatherForecastViewModel = ViewModelProviders.of(this, weatherForecastViewModelFactory).get(WeatherForecastViewModel::class.java)
    }

    private fun checkLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest().apply {
            interval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnFailureListener {
            if (it is ResolvableApiException){
                try {
                    it.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error at LocationSettingsResponse.addOnFailureListener")
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun receiveLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                p0 ?: return
                for(location in p0.locations) {
                    currentWeatherViewModel.setlatLng("${location.latitude},${location.longitude}")
                    weatherForecastViewModel.setlatLng("${location.latitude},${location.longitude}")

                    // Todo: Follow display a location addresss guide at android develoepr
                    address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                }
                swipe_refresh_layout.isRefreshing = false
                stopLocationUpdates()
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        getCurrentWeather()
        getTodayWeatherForecast()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        rv_weather_forecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = ForecastAdapter(forecastList)
        rv_weather_forecast.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipe_refresh_layout.setOnRefreshListener {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                startLocationUpdates()
            } else {
                currentWeatherViewModel.setlatLng("0,0")
                swipe_refresh_layout.isRefreshing = false
            }
        }
    }

    private fun setupEmptyErrorButtonClick() {
        empty_button.setOnClickListener {
            startLocationUpdates()
        }
        error_button.setOnClickListener {
            startLocationUpdates()
        }
    }

    private fun getCurrentWeather() {
        currentWeatherViewModel.getCurrentWeather().observe(this,
                Observer<Resource<CurrentWeatherView>> {
                    it?.let { handleCurrentWeatherViewState(it.status, it.data, it.message) }
                })
    }

    private fun handleCurrentWeatherViewState(resoureState: ResourceState, data: CurrentWeatherView?, message: String?) {
        when(resoureState) {
            ResourceState.LOADING -> setupScreenForLoadingState()
            ResourceState.SUCCESS -> setupScreenForSucess(data)
            ResourceState.ERROR -> setupScreenForError(message)
        }
    }

    // Todo: Appbar layout text to progress
    private fun setupScreenForLoadingState() {
        progress_bar.visibility = View.VISIBLE
        data_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        error_view.visibility = View.GONE
    }

    private fun setupScreenForSucess(it: CurrentWeatherView?) {
        progress_bar.visibility = View.GONE
        error_view.visibility = View.GONE
        if (it != null) {
            city_name_text_view.text = "${address[0].thoroughfare} \n".capitalize() +
                    "${address[0].locality}, ${address[0].countryName}".capitalize()

            weather_icon_image_view.setImageResource(WeatherIcon.getWeatherId(it.weatherList[0].id, it.weatherList[0].icon))
            temp_text_view.text = it.main.temp.roundToInt().toString() + " \u2103"
            weather_desc_text_view.text = it.weatherList[0].description.toUpperCase()

            pressure_text_view.text = it.main.pressure.roundToInt().toString() + "hPa"
            humidity_text_view.text = it.main.humidity.toString() + " %"
            cloud_text_view.text = it.clouds.cloudiness.toString() + " %"

            val tempMin = it.main.tempMin.toString() + " \u2103"
            val tempMax = it.main.tempMax.toString() + " \u2103"
            min_max_temp_text_view.text = "$tempMin / $tempMax"
            wind_text_view.text = it.wind.speed.toString() + " m/s"
            val sunrise = it.sys.sunriseTime
            val sunset = it.sys.sunsetTime
            val sdf = SimpleDateFormat("H:mm")
            val sunriseTime = sdf.format(Date(sunrise * 1000))
            val sunsetTime = sdf.format(Date(sunset * 1000))
            sunrise_sunset_text_view.text = "$sunriseTime / $sunsetTime"

            data_view.visibility = View.VISIBLE
        } else {
            empty_view.visibility = View.VISIBLE
        }
    }

    private fun setupScreenForError(message: String?) {
        progress_bar.visibility = View.GONE
        data_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        error_view.visibility = View.VISIBLE
    }

    private fun getTodayWeatherForecast() {
        weatherForecastViewModel.getWeatherForecast().observe(this,
                Observer<WeatherForecastView> {
                    forecastList.clear()
                    it?.forecastList?.forEach {
                        val sdf = SimpleDateFormat("dd MM yyyy")
                        val dateTime = sdf.format(Date(it.dateTime * 1000))
                        val currentDateTime = sdf.format(Calendar.getInstance().time)
                        if (dateTime == currentDateTime) {
                            forecastList.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                })
    }

}
