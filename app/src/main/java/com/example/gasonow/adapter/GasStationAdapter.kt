package com.example.gasonow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gasonow.R
import com.example.gasonow.databinding.ItemGasStationBinding
import com.example.gasonow.model.FuelType
import com.example.gasonow.model.GasStation

class GasStationAdapter(
    private val fuelType: FuelType,
    private val onItemClick: (GasStation) -> Unit
) : ListAdapter<GasStation, GasStationAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemGasStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: GasStation) {
            binding.apply {
                tvStationName.text = station.nombre
                tvBrandInitial.text = station.nombre.firstOrNull()?.toString() ?: "?"
                tvAddress.text = station.getDireccionCompleta()
                tvDistance.text = station.getDistanciaDisplay()
                tvFuelLabel.text = fuelType.displayName

                val precio = station.getPrecio(fuelType)
                tvPrice.text = if (precio != null) {
                    String.format("%.3f €/L", precio)
                } else {
                    root.context.getString(R.string.no_price)
                }

                when (station.estaAbierta) {
                    true -> {
                        tvOpenStatus.text = root.context.getString(R.string.status_open)
                        tvOpenStatus.setBackgroundResource(R.drawable.bg_status_open)
                        tvOpenStatus.setTextColor(
                            root.context.getColor(R.color.color_open)
                        )
                    }
                    false -> {
                        tvOpenStatus.text = root.context.getString(R.string.status_closed)
                        tvOpenStatus.setBackgroundResource(R.drawable.bg_status_closed)
                        tvOpenStatus.setTextColor(
                            root.context.getColor(R.color.color_closed)
                        )
                    }
                    null -> {
                        tvOpenStatus.text = root.context.getString(R.string.status_unknown)
                        tvOpenStatus.setBackgroundResource(R.drawable.bg_status_unknown)
                        tvOpenStatus.setTextColor(
                            root.context.getColor(R.color.color_unknown)
                        )
                    }
                }

                root.setOnClickListener { onItemClick(station) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGasStationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GasStation>() {
            override fun areItemsTheSame(old: GasStation, new: GasStation) = old.id == new.id
            override fun areContentsTheSame(old: GasStation, new: GasStation) = old == new
        }
    }
}
