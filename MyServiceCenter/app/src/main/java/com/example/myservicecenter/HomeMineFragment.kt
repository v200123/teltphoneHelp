package com.example.myservicecenter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myservicecenter.databinding.FragmentHomeMineBinding

class HomeMineFragment : Fragment(R.layout.fragment_home_mine) {
    private var _binding: FragmentHomeMineBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeMineBinding.bind(view)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            applyHeaderInfo()
        }
    }

    private fun initViews() {
        binding.btnOpenDetail.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        Glide.with(this)
            .load(R.drawable.tab_mine_select_v2)
            .placeholder(android.R.drawable.sym_def_app_icon)
            .error(android.R.drawable.sym_def_app_icon)
            .transform(CircleCrop())
            .into(binding.ivAvatar)
        applyHeaderInfo()
    }

    private fun applyHeaderInfo() {
        val context = requireContext()
        val rawPhoneNumber = AppPreferences.getCustomPhoneNumber(context).trim()
        val region = AppPreferences.getCustomSelfRegion(context).trim()
        binding.tvHomePhone.text = if (rawPhoneNumber.isNotEmpty()) {
            maskPhoneNumber(rawPhoneNumber)
        } else {
            getString(R.string.home_phone_default)
        }
        binding.tvHomeRegion.text = region.ifEmpty { getString(R.string.home_region_default) }
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length < 7) return phoneNumber
        return buildString {
            append(phoneNumber.take(3))
            append("****")
            append(phoneNumber.takeLast(4))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
