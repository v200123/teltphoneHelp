package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.databinding.ActivityBadgeRuleManageBinding
import com.u2tzjtne.telephonehelper.ui.adapter.BadgeRuleAdapter
import com.u2tzjtne.telephonehelper.util.BadgeRule
import com.u2tzjtne.telephonehelper.util.RingtoneBadgeRuleStore

class BadgeRuleManageActivity : BaseActivity() {
    private val binding: ActivityBadgeRuleManageBinding by lazy {
        ActivityBadgeRuleManageBinding.inflate(layoutInflater)
    }

    private val adapter: BadgeRuleAdapter by lazy {
        BadgeRuleAdapter(
            onEditClick = ::openEditor,
            onRenameClick = ::showRenameDialog,
            onDeleteClick = ::showDeleteDialog
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    override fun onResume() {
        super.onResume()
        loadRules()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnRuleBinding.setOnClickListener {
            startActivity(Intent(this, BadgeRuleBindingManageActivity::class.java))
        }
        binding.btnCreateRule.setOnClickListener { showCreateRuleDialog() }
        binding.rvRuleList.layoutManager = LinearLayoutManager(this)
        binding.rvRuleList.adapter = adapter
    }

    private fun loadRules() {
        val rules = RingtoneBadgeRuleStore.listRules(this)
        adapter.submitList(rules)
        binding.tvRuleCount.text = "规则数量：${rules.size}/20"
        binding.tvEmpty.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRuleList.visibility = if (rules.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showCreateRuleDialog() {
        val editText = EditText(this).apply {
            hint = "请输入规则名称"
            inputType = InputType.TYPE_CLASS_TEXT
            setText("新规则")
            setSelection(text?.length ?: 0)
        }
        AlertDialog.Builder(this)
            .setTitle("新建规则")
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text?.toString()?.trim().orEmpty().ifBlank { "新规则" }
                val rule = RingtoneBadgeRuleStore.createRule(this, name)
                if (rule == null) {
                    Toast.makeText(this, "规则数量已达上限（20）", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "规则已创建", Toast.LENGTH_SHORT).show()
                    openEditor(rule)
                }
            }
            .show()
    }

    private fun showRenameDialog(rule: BadgeRule) {
        val editText = EditText(this).apply {
            hint = "请输入规则名称"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(rule.name)
            setSelection(text?.length ?: 0)
        }
        AlertDialog.Builder(this)
            .setTitle("重命名规则")
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text?.toString()?.trim().orEmpty().ifBlank { rule.name }
                if (RingtoneBadgeRuleStore.renameRule(this, rule.id, newName)) {
                    Toast.makeText(this, "规则名称已更新", Toast.LENGTH_SHORT).show()
                    loadRules()
                } else {
                    Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteDialog(rule: BadgeRule) {
        AlertDialog.Builder(this)
            .setTitle("删除规则")
            .setMessage("确定删除规则「${rule.name}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                if (RingtoneBadgeRuleStore.deleteRule(this, rule.id)) {
                    Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
                    loadRules()
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openEditor(rule: BadgeRule) {
        startActivity(
            Intent(this, BadgeRuleEditorActivity::class.java)
                .putExtra(BadgeRuleEditorActivity.EXTRA_RULE_ID, rule.id)
        )
    }
}
