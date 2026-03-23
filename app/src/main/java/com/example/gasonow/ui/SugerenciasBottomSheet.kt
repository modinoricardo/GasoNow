package com.example.gasonow.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.gasonow.BuildConfig
import com.example.gasonow.R
import com.example.gasonow.databinding.BottomSheetSugerenciasBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SugerenciasBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSugerenciasBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "gasonow_prefs"
        private const val KEY_EMAIL = "sugerencias_email"
        private const val ARG_LAT = "lat"
        private const val ARG_LON = "lon"

        fun newInstance(lat: Double?, lon: Double?) = SugerenciasBottomSheet().apply {
            arguments = Bundle().apply {
                lat?.let { putDouble(ARG_LAT, it) }
                lon?.let { putDouble(ARG_LON, it) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSugerenciasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.etEmail.setText(prefs.getString(KEY_EMAIL, ""))

        binding.btnEnviar.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val sugerencia = binding.etSugerencia.text.toString().trim()

            if (sugerencia.isEmpty()) {
                showError("Por favor, escribe tu sugerencia.")
                return@setOnClickListener
            }

            if (email.isNotEmpty()) {
                prefs.edit().putString(KEY_EMAIL, email).apply()
            }

            val ctx = requireContext().applicationContext
            setLoading(true)
            lifecycleScope.launch {
                try {
                    sendEmail(
                        sugerencia = sugerencia,
                        emailUsuario = email.ifEmpty { "Anónimo" },
                        ctx = ctx
                    )
                    dismiss()
                    Toast.makeText(ctx, "¡Gracias! Tu sugerencia ha sido enviada.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    setLoading(false)
                    showError("Error al enviar. Comprueba tu conexión.")
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnEnviar.isEnabled = !loading
        binding.btnEnviar.text = if (loading) "Enviando…" else "Enviar sugerencia"
        if (loading) binding.tvStatus.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.setTextColor(requireContext().getColor(R.color.color_error))
        binding.tvStatus.text = message
    }

    private suspend fun sendEmail(
        sugerencia: String,
        emailUsuario: String,
        ctx: android.content.Context
    ) = withContext(Dispatchers.IO) {
        val titulo = sugerencia.take(55).let { if (sugerencia.length > 55) "$it…" else it }

        val lat = arguments?.getDouble(ARG_LAT)?.takeIf { arguments?.containsKey(ARG_LAT) == true }
        val lon = arguments?.getDouble(ARG_LON)?.takeIf { arguments?.containsKey(ARG_LON) == true }
        val ubicacion = if (lat != null && lon != null)
            "%.4f, %.4f".format(lat, lon)
        else
            "No disponible"

        val appVersion = try {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            "${info.versionName} (build ${info.longVersionCode})"
        } catch (e: Exception) { "Desconocida" }

        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.trust", "smtp.gmail.com")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "15000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(
                BuildConfig.GMAIL_SENDER,
                BuildConfig.GMAIL_PASSWORD.replace(" ", "")
            )
        })

        val body = buildString {
            appendLine("--- MENSAJE USUARIO ---")
            appendLine(sugerencia)
            appendLine()
            appendLine("========================================")
            appendLine("USUARIO    : $emailUsuario")
            appendLine("DISPOSITIVO: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            appendLine("APP        : GasoNow v$appVersion")
            appendLine("UBICACIÓN  : $ubicacion")
            appendLine("FECHA      : $fecha")
        }

        MimeMessage(session).apply {
            setFrom(InternetAddress(BuildConfig.GMAIL_SENDER))
            setRecipient(Message.RecipientType.TO, InternetAddress(BuildConfig.GMAIL_RECEIVER))
            subject = "[GasoNow] $titulo"
            setText(body, "UTF-8")
        }.also { Transport.send(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
