package com.nnt.transferdataviabluetooth

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nnt.transferdataviabluetooth.databinding.ItemMyMessageBinding
import com.nnt.transferdataviabluetooth.databinding.ItemOtherMessageBinding

class MessageAdapter(private val messages: ArrayList<Message>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class MyMessageViewHolder(val binding: ItemMyMessageBinding): RecyclerView.ViewHolder(binding.root)
    class OtherMessageViewHolder(val binding: ItemOtherMessageBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = parent.context.getSystemService(LayoutInflater::class.java)
        return if(viewType == MY_MESSAGE_TYPE){
            MyMessageViewHolder((ItemMyMessageBinding.inflate(inflater)))

        } else {
            OtherMessageViewHolder((ItemOtherMessageBinding.inflate(inflater)))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is MyMessageViewHolder){
            holder.binding.message = message
        }
        else if(holder is OtherMessageViewHolder){
            holder.binding.message = message

        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMyMessage) MY_MESSAGE_TYPE
        else OTHER_MESSAGE_TYPE
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun appendMessage(message: Message){
        this.messages.add(message)
        notifyItemInserted(messages.size-1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData(){
        this.messages.clear()
        notifyDataSetChanged()
    }

    companion object {
        private const val MY_MESSAGE_TYPE = 1
        private const val OTHER_MESSAGE_TYPE = 2
    }
}