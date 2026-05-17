package com.example.myservicecenter

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myservicecenter.databinding.FragmentCallDetailBinding
import com.example.myservicecenter.databinding.LayoutCallDetailFiltersBinding
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class CallDetailFragment : Fragment(R.layout.fragment_call_detail) {
    private var _binding: FragmentCallDetailBinding? = null
    private val binding get() = _binding!!
    private var filterBinding: LayoutCallDetailFiltersBinding? = null
    private val adapter = CallRecordAdapter()
    private var allRecords: List<CallRecord> = emptyList()
    private var selectedMonth: YearMonth? = null
    private var displayedRecordCount: Int = 0
    private val monthZoneId: ZoneId = ZoneId.systemDefault()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCallDetailBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        setupFilterHeader()
        applyCustomSettingsToAdapter()
        binding.swipeRefresh.setOnRefreshListener { (activity as? MainActivity)?.loadCallRecords() }
        bindLoadCompleteHint()
        binding.btnCallAnalysis.setOnClickListener { (activity as? MainActivity)?.showCallAnalysisTip() }
        filterBinding?.btnSearch?.setOnClickListener {
            applyFilter()
        }
        filterBinding?.ivClearSearch?.setOnClickListener {
            filterBinding?.etSearchPhone?.text?.clear()
        }
        filterBinding?.etSearchPhone?.doAfterTextChanged {
            filterBinding?.ivClearSearch?.visibility = if (it.isNullOrBlank()) View.GONE else View.VISIBLE
            if (it.isNullOrBlank()) {
                applyFilter()
            }
        }
    }

    private fun setupFilterHeader() {
        adapter.removeAllHeaderView()
        val headerBinding = LayoutCallDetailFiltersBinding.inflate(layoutInflater, binding.recyclerView, false)
        adapter.addHeaderView(headerBinding.root)
        filterBinding = headerBinding
    }

    fun setRefreshing(refreshing: Boolean) {
        _binding?.swipeRefresh?.isRefreshing = refreshing
    }

    fun submitRecords(records: List<CallRecord>) {
        allRecords = records
        applyFilter()
    }

    fun setSelectedMonth(month: YearMonth?) {
        selectedMonth = month
        applyFilter()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        applyCustomSettingsToAdapter()
    }

    private fun applyFilter() {
        val currentBinding = _binding ?: return
        val keyword = normalizePhoneNumber(filterBinding?.etSearchPhone?.text?.toString().orEmpty())
        val filteredRecords = allRecords.filter { record ->
            val matchesMonth = selectedMonth?.let { getRecordYearMonth(record) == it } ?: true
            val matchesKeyword = keyword.isEmpty() || normalizePhoneNumber(record.phoneNumber.orEmpty()).contains(keyword)
            matchesMonth && matchesKeyword
        }
        displayedRecordCount = filteredRecords.size
        adapter.setData(filteredRecords)
        currentBinding.recyclerView.visibility = View.VISIBLE
        if (filteredRecords.isEmpty()) {
            currentBinding.tvEmpty.visibility = View.VISIBLE
            currentBinding.tvLoadComplete.visibility = View.GONE
        } else {
            currentBinding.tvEmpty.visibility = View.GONE
            updateLoadCompleteHint()
        }
    }

    private fun normalizePhoneNumber(raw: String): String = raw.filter { it.isDigit() }

    private fun getRecordYearMonth(record: CallRecord): YearMonth? {
        val timestamp = when {
            record.startTime > 0 -> record.startTime
            record.connectedTime > 0 -> record.connectedTime
            record.endTime > 0 -> record.endTime
            else -> 0L
        }
        if (timestamp <= 0L) return null
        return Instant.ofEpochMilli(timestamp).atZone(monthZoneId).toLocalDate().let { date ->
            YearMonth.of(date.year, date.monthValue)
        }
    }

    private fun bindLoadCompleteHint() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateLoadCompleteHint()
            }
        })
    }

    private fun applyCustomSettingsToAdapter() {
        val context = context ?: return
        adapter.setCustomSelfRegion(AppPreferences.getCustomSelfRegion(context))
        adapter.setOutgoingPackageInfo(AppPreferences.getOutgoingPackageInfo(context))
        adapter.setCustomCallTypes(
            AppPreferences.getCustomOutgoingCallType(context),
            AppPreferences.getCustomIncomingCallType(context)
        )
    }

    private fun updateLoadCompleteHint() {
        val hasData = displayedRecordCount > 0
        val atBottom = !binding.recyclerView.canScrollVertically(1)
        val hasScrolledDown = binding.recyclerView.canScrollVertically(-1)
        binding.tvLoadComplete.visibility = if (hasData && atBottom && hasScrolledDown) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.removeAllHeaderView()
        filterBinding = null
        _binding = null
    }
}
