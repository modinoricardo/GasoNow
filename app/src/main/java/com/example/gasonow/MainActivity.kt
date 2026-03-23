package com.example.gasonow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.gasonow.databinding.ActivityMainBinding
import com.example.gasonow.model.FuelType
import com.example.gasonow.model.SortBy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.viewModels
import com.example.gasonow.viewmodel.MainViewModel
import com.example.gasonow.ui.MenuBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    companion object {
        private const val PREFS_FILTERS = "gasonow_search_prefs"
        private const val KEY_DISTANCE = "pref_distance"
        private const val KEY_FUEL_CHIP = "pref_fuel_chip"
        private const val KEY_ONLY_OPEN = "pref_only_open"
        private const val KEY_ONLY_WITH_PRICE = "pref_only_with_price"
        private const val KEY_SORT_PRICE = "pref_sort_price"
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LON = "last_lon"
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        when {
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                fetchLocationAndSearch()
            }
            else -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermissionRationaleDialog()
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lastLat = savedInstanceState?.let { if (it.containsKey(KEY_LAST_LAT)) it.getDouble(KEY_LAST_LAT) else null }
        lastLon = savedInstanceState?.let { if (it.containsKey(KEY_LAST_LON)) it.getDouble(KEY_LAST_LON) else null }

        mainViewModel // dispara el prefetch en background inmediatamente
        setupDistanceSlider()
        setupSearchButton()
        setupWindowInsets()
        setupMenuButton()
        restoreFilters()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences(PREFS_FILTERS, MODE_PRIVATE).edit()
            .putFloat(KEY_DISTANCE, binding.sliderDistance.value)
            .putInt(KEY_FUEL_CHIP, binding.chipGroupFuel.checkedChipId)
            .putBoolean(KEY_ONLY_OPEN, binding.switchOnlyOpen.isChecked)
            .putBoolean(KEY_ONLY_WITH_PRICE, binding.switchOnlyWithPrice.isChecked)
            .putBoolean(KEY_SORT_PRICE, binding.radioSortPrice.isChecked)
            .apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastLat?.let { outState.putDouble(KEY_LAST_LAT, it) }
        lastLon?.let { outState.putDouble(KEY_LAST_LON, it) }
    }

    private fun restoreFilters() {
        val prefs = getSharedPreferences(PREFS_FILTERS, MODE_PRIVATE)
        val savedDistance = prefs.getFloat(KEY_DISTANCE, -1f)
        if (savedDistance > 0) {
            binding.sliderDistance.value = savedDistance
        } else {
            binding.tvTimeEstimate.text = estimateDriveTime(binding.sliderDistance.value.toInt())
        }
        val chipId = prefs.getInt(KEY_FUEL_CHIP, -1)
        if (chipId != -1) binding.chipGroupFuel.check(chipId)
        binding.switchOnlyOpen.isChecked = prefs.getBoolean(KEY_ONLY_OPEN, true)
        binding.switchOnlyWithPrice.isChecked = prefs.getBoolean(KEY_ONLY_WITH_PRICE, true)
        if (!prefs.getBoolean(KEY_SORT_PRICE, true)) binding.radioSortDistance.isChecked = true
    }

    private var lastLat: Double? = null
    private var lastLon: Double? = null

    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener {
            MenuBottomSheet.newInstance(lastLat, lastLon).show(supportFragmentManager, "menu")
        }
    }

    private fun setupWindowInsets() {
        val dp = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Header: padding superior = altura status bar + 16dp (en vez de los 52dp fijos)
            binding.headerLayout.setPadding(
                (24 * dp).toInt(),
                systemBars.top + (20 * dp).toInt(),
                (24 * dp).toInt(),
                (32 * dp).toInt()
            )

            // Botón: margen inferior = altura nav bar + 16dp
            binding.btnSearch.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = systemBars.bottom + (16 * dp).toInt()
            }

            // ScrollView: padding inferior = altura nav bar + altura botón + margen extra
            val scrollBottomPadding = systemBars.bottom + (88 * dp).toInt()
            binding.nestedScrollView.setPadding(
                binding.nestedScrollView.paddingLeft,
                binding.nestedScrollView.paddingTop,
                binding.nestedScrollView.paddingRight,
                scrollBottomPadding
            )

            insets
        }
        // Fuerza el re-despacho de insets por si ya se emitieron antes de registrar el listener
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.addOnChangeListener { _, value, _ ->
            val km = value.toInt()
            binding.tvDistanceValue.text = "$km km"
            binding.tvTimeEstimate.text = estimateDriveTime(km)
        }
    }

    private fun estimateDriveTime(km: Int): String {
        val minutes = km // ~60 km/h → 1 min por km
        return if (minutes < 60) {
            "~$minutes min en coche"
        } else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "~${h}h en coche" else "~${h}h ${m}min en coche"
        }
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            checkLocationPermissionAndSearch()
        }
    }

    private fun checkLocationPermissionAndSearch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            fineGranted || coarseGranted -> fetchLocationAndSearch()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                showPermissionRationaleDialog()
            else -> requestPermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocationAndSearch() {
        binding.btnSearch.isEnabled = false
        binding.btnSearch.text = "Obteniendo ubicación…"

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            // 1. lastLocation es instantáneo: ya lo tiene el sistema cacheado
            fusedClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        resetSearchButton()
                        startResultsActivity(lastLoc.latitude, lastLoc.longitude)
                    } else {
                        // 2. Sin caché: pide ubicación con precisión balanceada (red/WiFi, ~2-3 seg)
                        requestFreshLocation(fusedClient)
                    }
                }
                .addOnFailureListener { requestFreshLocation(fusedClient) }
        } catch (e: SecurityException) {
            resetSearchButton()
            checkLocationPermissionAndSearch()
        }
    }

    private fun requestFreshLocation(fusedClient: FusedLocationProviderClient) {
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    resetSearchButton()
                    if (location != null) {
                        startResultsActivity(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(
                            this,
                            "No se pudo obtener tu ubicación. Activa el GPS.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    resetSearchButton()
                    Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            resetSearchButton()
            checkLocationPermissionAndSearch()
        }
    }

    private fun resetSearchButton() {
        binding.btnSearch.isEnabled = true
        binding.btnSearch.text = getString(R.string.btn_search)
    }

    private fun startResultsActivity(lat: Double, lon: Double) {
        lastLat = lat
        lastLon = lon
        val fuelType = getSelectedFuelType()
        val sortBy = if (binding.radioSortPrice.isChecked) SortBy.PRICE else SortBy.DISTANCE
        val maxDistance = binding.sliderDistance.value.toInt()

        val intent = Intent(this, ResultsActivity::class.java).apply {
            putExtra(ResultsActivity.EXTRA_USER_LAT, lat)
            putExtra(ResultsActivity.EXTRA_USER_LON, lon)
            putExtra(ResultsActivity.EXTRA_FUEL_TYPE, fuelType.name)
            putExtra(ResultsActivity.EXTRA_MAX_DISTANCE, maxDistance)
            putExtra(ResultsActivity.EXTRA_ONLY_OPEN, binding.switchOnlyOpen.isChecked)
            putExtra(ResultsActivity.EXTRA_ONLY_WITH_PRICE, binding.switchOnlyWithPrice.isChecked)
            putExtra(ResultsActivity.EXTRA_SORT_BY, sortBy.name)
        }
        startActivity(intent)
    }

    private fun getSelectedFuelType(): FuelType {
        return when (binding.chipGroupFuel.checkedChipId) {
            R.id.chipGasolina95 -> FuelType.GASOLINA_95
            R.id.chipGasolina98 -> FuelType.GASOLINA_98
            R.id.chipDiesel -> FuelType.GASOLEO_A
            R.id.chipDieselPlus -> FuelType.GASOLEO_PREMIUM
            R.id.chipGLP -> FuelType.GLP
            R.id.chipGNC -> FuelType.GNC
            else -> FuelType.GASOLINA_95
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_location_title))
            .setMessage(getString(R.string.permission_location_message))
            .setPositiveButton("Permitir") { _, _ ->
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_location_title))
            .setMessage(getString(R.string.permission_denied_message))
            .setPositiveButton(getString(R.string.btn_go_settings)) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
