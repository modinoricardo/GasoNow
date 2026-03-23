package com.example.gasonow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gasonow.databinding.BottomSheetMenuBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMenuBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_LAT = "lat"
        private const val ARG_LON = "lon"

        fun newInstance(lat: Double?, lon: Double?) = MenuBottomSheet().apply {
            arguments = Bundle().apply {
                lat?.let { putDouble(ARG_LAT, it) }
                lon?.let { putDouble(ARG_LON, it) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lat = arguments?.getDouble(ARG_LAT)?.takeIf { arguments?.containsKey(ARG_LAT) == true }
        val lon = arguments?.getDouble(ARG_LON)?.takeIf { arguments?.containsKey(ARG_LON) == true }

        binding.itemAcercaDe.setOnClickListener {
            dismiss()
            AcercaDeBottomSheet().show(parentFragmentManager, "acerca")
        }

        binding.itemSugerencias.setOnClickListener {
            dismiss()
            SugerenciasBottomSheet.newInstance(lat, lon).show(parentFragmentManager, "sugerencias")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
