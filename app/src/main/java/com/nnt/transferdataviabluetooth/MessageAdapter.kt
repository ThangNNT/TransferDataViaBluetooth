package com.nnt.transferdataviabluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nnt.transferdataviabluetooth.databinding.ItemMessageBinding

class MessageAdapter(private val messages: ArrayList<Message>): RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = parent.context.getSystemService(LayoutInflater::class.java)
        return ViewHolder((ItemMessageBinding.inflate(inflater)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.binding.message = message
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun appendMessage(message: Message){
        this.messages.add(message)
        notifyItemInserted(messages.size-1)
    }
}