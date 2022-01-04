package com.example.map.googlemap.ui.dialog

import android.view.WindowManager
import com.example.map.googlemap.R
import com.example.map.googlemap.base.ui.BaseDialogFragment
import com.example.map.googlemap.databinding.LoadingDialogBinding


class LoadingDialog : BaseDialogFragment<LoadingDialogBinding>(R.layout.loading_dialog) {

    override fun onStart() {
        super.onStart()
        val window = requireDialog().window
        val windowParams = window!!.attributes
        windowParams.dimAmount = 0.5f
        windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window.attributes = windowParams
    }

    companion object {
        fun newInstance() = LoadingDialog()
    }
}