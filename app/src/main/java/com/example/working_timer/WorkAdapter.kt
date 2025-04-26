package com.example.working_timer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.example.working_timer.data.Work
import com.example.working_timer.databinding.ItemWorkBinding

class WorkAdapter(
    private var workList: List<Work>, private val onDeleteClickListener: (Work) -> Unit, private val onEditClickListener: (Work) -> Unit) :
    RecyclerView.Adapter<WorkAdapter.WorkViewHolder>() {

    inner class WorkViewHolder(private val binding: ItemWorkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(work: Work) {
            binding.startTime.text = "開始  ${work.start_time}"
            binding.endTime.text = "終了  ${work.end_time}"
            val h = work.elapsed_time / 3600
            val m = (work.elapsed_time % 3600) / 60
            binding.elapsedTime.text = buildSpannedString {
                append("活動時間  ")
                append(String.format("%02d", h))
                scale(0.7f) { append("時間 ") }
                append(String.format("%02d", m))
                scale(0.7f) { append("分") }
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClickListener(work)
            }

            binding.editButton.setOnClickListener {
                onEditClickListener(work)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        holder.bind(workList[position])
    }

    override fun getItemCount(): Int = workList.size

    fun updateData(newList: List<Work>) {
        workList = newList
        notifyDataSetChanged()
    }
}
