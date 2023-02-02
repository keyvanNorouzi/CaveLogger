package com.cave.cavelogger.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cave.cavelogger.R
import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import com.cave.cavelogger.databinding.ItemCaveLoggerListBinding

class CaveLoggerAdapter(private val itemSelected: (CaveLogger) -> Unit) :
    ListAdapter<CaveLogger, CaveLoggerAdapter.LogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        return LogViewHolder(
            ItemCaveLoggerListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            itemSelected
        )
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(
        val binding: ItemCaveLoggerListBinding,
        private val itemSelected: (CaveLogger) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CaveLogger) {
            binding.tvURL.text = item.url
            binding.tvDuringTime.text = item.during
            item.statusCode?.let {
                binding.tvStatusCode.text = it.toString()
                when (item.statusCode) {
                    in 200..300 -> {
                        binding.tvStatusCode.setTextColor(
                            ContextCompat.getColor(
                                binding.tvStatusCode.context,
                                R.color.greenblue
                            )
                        )
                    }
                    in 300..500 -> {
                        binding.tvStatusCode.setTextColor(
                            ContextCompat.getColor(
                                binding.tvStatusCode.context,
                                R.color.red
                            )
                        )
                    }
                    else -> {
                        binding.tvStatusCode.setTextColor(
                            ContextCompat.getColor(
                                binding.tvStatusCode.context,
                                R.color.black
                            )
                        )
                    }
                }
            } ?: kotlin.run {
                binding.tvStatusCode.text = "Waiting"
                binding.tvStatusCode.setTextColor(
                    ContextCompat.getColor(
                        binding.tvStatusCode.context,
                        R.color.black
                    )
                )
            }
            binding.tvMethod.text = item.method
            binding.parentLayout.setOnClickListener {
                itemSelected(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CaveLogger>() {
        override fun areItemsTheSame(oldItem: CaveLogger, newItem: CaveLogger): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CaveLogger, newItem: CaveLogger): Boolean {
            return oldItem == newItem
        }
    }
}
