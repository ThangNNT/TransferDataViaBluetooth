package com.nnt.transferdataviabluetooth.util

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

@BindingAdapter("shouldVisible")
fun shouldVisible(view: View, isVisible: Boolean){
    view.isVisible = isVisible
}