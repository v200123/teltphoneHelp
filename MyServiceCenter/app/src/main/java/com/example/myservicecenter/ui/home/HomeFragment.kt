package com.example.myservicecenter.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.R
import com.example.myservicecenter.SettingsActivity
import com.example.myservicecenter.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        applyWindowInsets()
        bindActions()
        bindHomeData()
    }

    override fun onResume() {
        super.onResume()
        bindHomeData()
    }

    private fun applyWindowInsets() {
        val originalTopPadding = binding.homeContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeContent) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = originalTopPadding + statusTop)
            insets
        }
    }

    private fun bindActions() {
        binding.balanceCard.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    private fun bindHomeData() {
        val context = context ?: return
        binding.tvHomeGreeting.text = getString(
            R.string.home_greeting_template,
            greetingText(),
            displayPhone(AppPreferences.getCustomPhoneNumber(context))
        )
        binding.tvDataValue.text = displayValue(
            AppPreferences.getMineStatData(context),
            oldDefault = "42.92",
            reference = "--"
        )
        binding.tvBalanceValue.text = displayValue(
            AppPreferences.getMineStatBalance(context),
            oldDefault = "724.48",
            reference = "338.27"
        )
        binding.tvCallValue.text = displayValue(
            AppPreferences.getWebViewHomeCallMinutes(context),
            oldDefault = "200",
            reference = "30"
        )
        binding.tvBeanValue.text = displayValue(
            AppPreferences.getMineStatBean(context),
            oldDefault = "1833",
            reference = "170"
        )
    }

    private fun displayValue(stored: String, oldDefault: String, reference: String): String {
        val value = stored.trim()
        return if (value.isEmpty() || value == oldDefault) reference else value
    }

    private fun displayPhone(rawPhone: String): String {
        val phone = rawPhone.trim()
        if (phone.isEmpty()) return getString(R.string.home_reference_phone)
        if ('*' in phone) return phone
        val digits = phone.filter(Char::isDigit)
        return if (digits.length >= 7) {
            "${digits.take(3)}****${digits.takeLast(4)}"
        } else {
            phone
        }
    }

    private fun greetingText(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> getString(R.string.home_greeting_morning)
            in 11..13 -> getString(R.string.home_greeting_noon)
            in 14..18 -> getString(R.string.home_greeting_afternoon)
            else -> getString(R.string.home_greeting_evening)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
