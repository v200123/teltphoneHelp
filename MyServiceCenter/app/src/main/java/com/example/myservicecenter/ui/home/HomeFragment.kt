package com.example.myservicecenter.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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

        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/7efac0c5395a41fc811856473284b7c0.png?fmt=webp").into(_binding!!.ivShowTopGif);
        Glide.with(this).load("https://res.app.coc.10086.cn/qwhdcdn_cmcc-cs_cn/prd-mgcenter/a752f551f6f34cb0a55d15bce93e5fc7.png?fmt=webp").into(_binding!!.ivTopBarSearch);
   }


    override fun onResume() {
        super.onResume()

    }

}
