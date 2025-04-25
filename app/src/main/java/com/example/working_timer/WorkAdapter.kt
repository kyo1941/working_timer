package com.example.working_timer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.working_timer.data.Work
import com.example.working_timer.databinding.ItemWorkBinding

class WorkAdapter(private var workList: List<Work>) :
    RecyclerView.Adapter<WorkAdapter.WorkViewHolder>() {

    inner class WorkViewHolder(private val binding: ItemWorkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(work: Work) {
            binding.startTime.text = "開始  ${work.start_time}"
            binding.endTime.text = "終了  ${work.end_time}"
            val h = work.elapsed_time / 3600
            val m = (work.elapsed_time % 3600) / 60
            val s = work.elapsed_time % 60
            binding.elapsedTime.text = "活動時間  %02d:%02d:%02d".format(h, m, s)
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
