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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.gasonow.databinding.ActivityMainBinding
import com.example.gasonow.model.FuelType
import com.example.gasonow.model.SortBy
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.viewModels
import com.example.gasonow.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDistanceSlider()
        setupSearchButton()
        setupWindowInsets()
    }

    private fun setupWindowInsets() {
        val dp = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Header: padding superior = altura status bar + 16dp (en vez de los 52dp fijos)
            binding.headerLayout.setPadding(
                (24 * dp).toInt(),
                systemBars.top + (16 * dp).toInt(),
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
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.addOnChangeListener { _, value, _ ->
            binding.tvDistanceValue.text = "${value.toInt()} km"
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
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    binding.btnSearch.isEnabled = true
                    binding.btnSearch.text = getString(R.string.btn_search)
                    if (location != null) {
                        startResultsActivity(location.latitude, location.longitude)
                    } else {
                        // Try last known location
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                startResultsActivity(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                Toast.makeText(
                                    this,
                                    "No se pudo obtener tu ubicación. Activa el GPS.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    binding.btnSearch.isEnabled = true
                    binding.btnSearch.text = getString(R.string.btn_search)
                    Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            binding.btnSearch.isEnabled = true
            binding.btnSearch.text = getString(R.string.btn_search)
            checkLocationPermissionAndSearch()
        }
    }

    private fun startResultsActivity(lat: Double, lon: Double) {
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
