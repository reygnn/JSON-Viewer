package com.github.reygnn.json_viewer.ui.viewer

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import androidx.appcompat.R as AppCompatR
import com.github.reygnn.json_viewer.databinding.ItemJsonLineBinding
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.google.android.material.color.MaterialColors

class JsonAdapter : ListAdapter<JsonLine, JsonAdapter.JsonViewHolder>(JsonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JsonViewHolder {
        val binding = ItemJsonLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JsonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JsonViewHolder, position: Int) {
        holder.bind(getItem(position).content)
    }

    class JsonViewHolder(private val binding: ItemJsonLineBinding) : RecyclerView.ViewHolder(binding.root) {

        private val keyColor = MaterialColors.getColor(binding.root, AppCompatR.attr.colorPrimary)
        private val stringColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary)
        private val numberColor = MaterialColors.getColor(binding.root, AppCompatR.attr.colorError)
        private val defaultColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorOnSurface)

        fun bind(line: String) {
            val spannable = SpannableString(line)

            KEY_REGEX.findAll(line).forEach { match ->
                spannable.setSpan(
                    ForegroundColorSpan(keyColor),
                    match.range.first,
                    match.range.last,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            STRING_REGEX.findAll(line).forEach { match ->
                val isKey = (match.range.last + 1 < line.length && line[match.range.last + 1] == ':')
                if (!isKey) {
                    spannable.setSpan(
                        ForegroundColorSpan(stringColor),
                        match.range.first,
                        match.range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            LITERAL_REGEX.findAll(line).forEach { match ->
                spannable.setSpan(
                    ForegroundColorSpan(numberColor),
                    match.range.first,
                    match.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.lineText.text = spannable
            binding.lineText.setTextColor(defaultColor)
        }
    }

    class JsonDiffCallback : DiffUtil.ItemCallback<JsonLine>() {
        override fun areItemsTheSame(oldItem: JsonLine, newItem: JsonLine) =
            oldItem.index == newItem.index

        override fun areContentsTheSame(oldItem: JsonLine, newItem: JsonLine) =
            oldItem == newItem
    }

    companion object {
        private val KEY_REGEX = "\"[^\"]+\":".toRegex()
        private val STRING_REGEX = "\"[^\"]+\"".toRegex()
        private val LITERAL_REGEX = "\\b(true|false|null|-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\b".toRegex()
    }
}