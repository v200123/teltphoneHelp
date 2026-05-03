package com.example.myservicecenter

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.myservicecenter.databinding.FragmentModulePlaceholderBinding

class ModulePlaceholderFragment : Fragment(R.layout.fragment_module_placeholder) {
    private var _binding: FragmentModulePlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentModulePlaceholderBinding.bind(view)
        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        binding.tvPlaceholder.text = getString(R.string.module_placeholder_with_name, title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"

        fun newInstance(title: String): ModulePlaceholderFragment {
            return ModulePlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}
