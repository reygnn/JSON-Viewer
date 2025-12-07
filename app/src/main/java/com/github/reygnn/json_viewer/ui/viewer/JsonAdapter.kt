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
import com.github.reygnn.json_viewer.core.parser.JsonTokenizer
import com.github.reygnn.json_viewer.databinding.ItemJsonLineBinding
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.github.reygnn.json_viewer.domain.model.TokenType
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

        private val tokenizer = JsonTokenizer()

        private val defaultColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorOnSurface)

        private val colors = mapOf(
            TokenType.KEY to MaterialColors.getColor(binding.root, AppCompatR.attr.colorPrimary),
            TokenType.STRING to MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary),
            TokenType.NUMBER to MaterialColors.getColor(binding.root, AppCompatR.attr.colorError),
            TokenType.BOOLEAN to MaterialColors.getColor(binding.root, AppCompatR.attr.colorError),
            TokenType.NULL to MaterialColors.getColor(binding.root, AppCompatR.attr.colorError)
        )

        fun bind(line: String) {
            val spannable = SpannableString(line)

            tokenizer.tokenize(line).forEach { token ->
                colors[token.type]?.let { color ->
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        token.start,
                        token.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
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
}