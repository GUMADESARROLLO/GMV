package com.unimarksa.gmv.fragments

import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.Config

class PerfilFragment : Fragment() {

    private lateinit var nestedScroll: NestedScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nestedScroll = view.findViewById(R.id.nested_content)
        view.findViewById<View>(R.id.AboutUs).setOnClickListener { showDialogAbout() }
    }

    private fun showDialogAbout() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_about)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val versionName = try {
            requireContext().packageManager.getPackageInfo(
                requireContext().packageName, 0
            ).versionName ?: "?"
        } catch (_: PackageManager.NameNotFoundException) {
            "?"
        }

        dialog.findViewById<TextView>(R.id.tv_version).text =
            "Versi\u00f3n $versionName | ${Config.BASE_URL}"

        dialog.findViewById<View>(R.id.bt_close).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
