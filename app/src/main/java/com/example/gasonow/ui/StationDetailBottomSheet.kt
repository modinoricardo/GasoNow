package com.example.gasonow.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.gasonow.R
import com.example.gasonow.databinding.BottomSheetStationDetailBinding
import com.example.gasonow.model.FuelType
import com.example.gasonow.model.GasStation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StationDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStationDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var station: GasStation
    private lateinit var selectedFuel: FuelType

    override fun getTheme() = R.style.Theme_GasoNow_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDetailName.text = station.nombre

        // Open/closed status
        when (station.estaAbierta) {
            true -> {
                binding.tvDetailStatus.text = getString(R.string.status_open)
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_open)
                binding.tvDetailStatus.setTextColor(requireContext().getColor(R.color.color_open))
            }
            false -> {
                binding.tvDetailStatus.text = getString(R.string.status_closed)
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_closed)
                binding.tvDetailStatus.setTextColor(requireContext().getColor(R.color.color_closed))
            }
            null -> {
                binding.tvDetailStatus.text = getString(R.string.status_unknown)
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_unknown)
                binding.tvDetailStatus.setTextColor(requireContext().getColor(R.color.color_unknown))
            }
        }

        binding.tvDetailAddress.text = station.getDireccionCompleta()
        binding.tvDetailSchedule.text = station.horario.ifBlank {
            getString(R.string.detail_unknown_schedule)
        }
        binding.tvDetailDistance.text = station.getDistanciaDisplay()

        // Populate prices
        populatePrices()

        // Directions button
        binding.btnDirections.setOnClickListener {
            openGoogleMaps(station.latitud, station.longitud, station.nombre)
        }
    }

    private fun populatePrices() {
        val container = binding.pricesContainer
        container.removeAllViews()

        val fuelOrder = listOf(
            FuelType.GASOLINA_95,
            FuelType.GASOLINA_98,
            FuelType.GASOLEO_A,
            FuelType.GASOLEO_PREMIUM,
            FuelType.GLP,
            FuelType.GNC
        )

        fuelOrder.forEach { fuel ->
            val precio = station.getPrecio(fuel) ?: return@forEach
            val row = layoutInflater.inflate(R.layout.item_price_row, container, false)
            val tvFuel = row.findViewById<TextView>(R.id.tvPriceFuel)
            val tvValue = row.findViewById<TextView>(R.id.tvPriceValue)

            tvFuel.text = fuel.displayName
            tvValue.text = String.format("%.3f €/L", precio)

            // Highlight selected fuel
            if (fuel == selectedFuel) {
                tvFuel.setTextColor(requireContext().getColor(R.color.color_primary))
                tvValue.setTextColor(requireContext().getColor(R.color.color_primary))
                tvFuel.setTypeface(null, android.graphics.Typeface.BOLD)
                tvValue.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            container.addView(row)
        }

        if (container.childCount == 0) {
            val tvNone = TextView(requireContext()).apply {
                text = getString(R.string.no_price)
                setTextColor(requireContext().getColor(R.color.color_outline))
                textSize = 14f
            }
            container.addView(tvNone)
        }
    }

    private fun openGoogleMaps(lat: Double, lon: Double, name: String) {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(name)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback: open in browser
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(station: GasStation, fuelType: FuelType): StationDetailBottomSheet {
            return StationDetailBottomSheet().also {
                it.station = station
                it.selectedFuel = fuelType
            }
        }
    }
}
