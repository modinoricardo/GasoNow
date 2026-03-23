package com.example.gasonow.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gasonow.BuildConfig
import com.example.gasonow.databinding.BottomSheetAcercaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AcercaDeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAcercaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAcercaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvVersion.text = "Versión ${BuildConfig.VERSION_NAME}"

        binding.rowEmail.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:modinoricardo@gmail.com")
            })
        }

        binding.rowGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/modinoricardo")))
        }

        binding.rowInstagram.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://instagram.com/rich.icodes")))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
