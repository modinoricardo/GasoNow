package com.example.gasonow

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gasonow.adapter.GasStationAdapter
import com.example.gasonow.databinding.ActivityResultsBinding
import com.example.gasonow.model.FuelType
import com.example.gasonow.model.SearchFilters
import com.example.gasonow.model.SortBy
import com.example.gasonow.ui.StationDetailBottomSheet
import com.example.gasonow.viewmodel.ResultsUiState
import com.example.gasonow.viewmodel.ResultsViewModel

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private val viewModel: ResultsViewModel by viewModels()
    private lateinit var adapter: GasStationAdapter
    private lateinit var filters: SearchFilters

    private var loadingAnimatorSet: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filters = parseFiltersFromIntent()

        setupToolbar()
        setupRecyclerView()
        setupSortButton()
        observeViewModel()

        if (savedInstanceState == null) {
            viewModel.loadStations(filters)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoadingAnimation()
    }

    // ── Animación de carga ────────────────────────────────────────────────────

    private fun startLoadingAnimation() {
        if (loadingAnimatorSet?.isRunning == true) return

        val icon = binding.tvLoadingIcon

        // Pulso del icono: crece y encoge suavemente
        val iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.85f, 1.15f).apply {
            duration = 850
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }
        val iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.85f, 1.15f).apply {
            duration = 850
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }

        // Ondas que se expanden desde el icono hacia afuera (efecto sonar)
        fun rippleAnims(view: View, delay: Long): List<ObjectAnimator> = listOf(
            ObjectAnimator.ofFloat(view, View.SCALE_X, 0.15f, 1f).apply {
                duration = 1800
                repeatCount = ObjectAnimator.INFINITE
                startDelay = delay
                interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.15f, 1f).apply {
                duration = 1800
                repeatCount = ObjectAnimator.INFINITE
                startDelay = delay
                interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofFloat(view, View.ALPHA, 0.6f, 0f).apply {
                duration = 1800
                repeatCount = ObjectAnimator.INFINITE
                startDelay = delay
                interpolator = AccelerateInterpolator()
            }
        )

        val allAnimators = mutableListOf<Animator>(iconScaleX, iconScaleY)
        allAnimators.addAll(rippleAnims(binding.ripple1, 0L))
        allAnimators.addAll(rippleAnims(binding.ripple2, 600L))
        allAnimators.addAll(rippleAnims(binding.ripple3, 1200L))

        loadingAnimatorSet = AnimatorSet().apply {
            playTogether(allAnimators)
            start()
        }
    }

    private fun stopLoadingAnimation() {
        loadingAnimatorSet?.cancel()
        loadingAnimatorSet = null
        listOf(binding.ripple1, binding.ripple2, binding.ripple3).forEach {
            it.scaleX = 0.15f
            it.scaleY = 0.15f
            it.alpha = 0f
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.layoutLoading.visibility = View.GONE
            binding.layoutError.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE

            when (state) {
                is ResultsUiState.Loading -> {
                    binding.layoutLoading.visibility = View.VISIBLE
                    startLoadingAnimation()
                    // Actualiza el mensaje con fade suave
                    binding.tvLoadingMessage.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction {
                            binding.tvLoadingMessage.text = state.message
                            binding.tvLoadingMessage.animate()
                                .alpha(1f)
                                .setDuration(180)
                                .start()
                        }
                        .start()
                    binding.tvResultsCount.text = getString(R.string.results_loading)
                    val progress = when (state.message) {
                        "Localizando provincia…"    -> 15
                        "Descargando gasolineras…"  -> 40
                        "Calculando distancias…"    -> 70
                        "Aplicando filtros…"        -> 90
                        else                        -> 5
                    }
                    binding.loadingProgressBar.setProgressCompat(progress, true)
                }

                is ResultsUiState.Success -> {
                    binding.loadingProgressBar.setProgressCompat(100, true)
                    stopLoadingAnimation()
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.tvResultsCount.text = getString(R.string.results_count, state.totalCount)
                    adapter.submitList(state.stations)
                }

                is ResultsUiState.Error -> {
                    stopLoadingAnimation()
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                    binding.tvResultsCount.text = ""
                    binding.btnRetry.setOnClickListener { viewModel.loadStations(filters) }
                }

                is ResultsUiState.Empty -> {
                    stopLoadingAnimation()
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvResultsCount.text = getString(R.string.results_count, 0)
                }
            }
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun parseFiltersFromIntent(): SearchFilters {
        return SearchFilters(
            maxDistanceKm = intent.getIntExtra(EXTRA_MAX_DISTANCE, 10),
            fuelType = FuelType.valueOf(
                intent.getStringExtra(EXTRA_FUEL_TYPE) ?: FuelType.GASOLINA_95.name
            ),
            onlyOpen = intent.getBooleanExtra(EXTRA_ONLY_OPEN, true),
            onlyWithPrice = intent.getBooleanExtra(EXTRA_ONLY_WITH_PRICE, true),
            sortBy = SortBy.valueOf(
                intent.getStringExtra(EXTRA_SORT_BY) ?: SortBy.PRICE.name
            ),
            userLat = intent.getDoubleExtra(EXTRA_USER_LAT, 0.0),
            userLon = intent.getDoubleExtra(EXTRA_USER_LON, 0.0)
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = GasStationAdapter(filters.fuelType) { station ->
            StationDetailBottomSheet.newInstance(station, filters.fuelType)
                .show(supportFragmentManager, "detail")
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ResultsActivity)
            adapter = this@ResultsActivity.adapter
            setHasFixedSize(false)
        }
    }

    private fun setupSortButton() {
        updateSortButtonText(filters.sortBy)
        binding.btnSortToggle.setOnClickListener {
            val currentFilters = viewModel.getCurrentFilters() ?: filters
            val newSort = if (currentFilters.sortBy == SortBy.PRICE) SortBy.DISTANCE else SortBy.PRICE
            viewModel.changeSortOrder(newSort)
            updateSortButtonText(newSort)
        }
    }

    private fun updateSortButtonText(sortBy: SortBy) {
        binding.btnSortToggle.text = when (sortBy) {
            SortBy.PRICE -> getString(R.string.sort_by_price)
            SortBy.DISTANCE -> getString(R.string.sort_by_distance)
        }
    }

    companion object {
        const val EXTRA_MAX_DISTANCE = "extra_max_distance"
        const val EXTRA_FUEL_TYPE = "extra_fuel_type"
        const val EXTRA_ONLY_OPEN = "extra_only_open"
        const val EXTRA_ONLY_WITH_PRICE = "extra_only_with_price"
        const val EXTRA_SORT_BY = "extra_sort_by"
        const val EXTRA_USER_LAT = "extra_user_lat"
        const val EXTRA_USER_LON = "extra_user_lon"
    }
}
