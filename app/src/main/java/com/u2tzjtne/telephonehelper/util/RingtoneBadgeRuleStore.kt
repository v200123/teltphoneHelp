package com.u2tzjtne.telephonehelper.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.base.App
import java.util.UUID

enum class RuleSourceType {
    PRESET,
    CUSTOM
}

enum class BadgeId(val key: String, @DrawableRes val drawableRes: Int, val displayName: String) {
    BADGE_1("badge_1", R.drawable.ic_call_1_badge, "图标1"),
    BADGE_2("badge_2", R.drawable.ic_call_2_badge, "图标2"),
    BADGE_3("badge_3", R.drawable.ic_call_3_badge, "图标3"),
    BADGE_4("badge_4", R.drawable.ic_call_4_badge, "图标4"),
    BADGE_5("badge_5", R.drawable.ic_call_5, "图标5");

    companion object {
        fun fromKey(key: String): BadgeId? = entries.firstOrNull { it.key == key }
    }
}

data class BadgeItemState(
    val added: Boolean = false,
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f,
    val scale: Float = 1f,
    val zIndex: Int = 0
)

data class BadgeRule(
    val id: String,
    val name: String,
    val items: MutableMap<String, BadgeItemState>,
    val updatedAt: Long,
    val sourceType: RuleSourceType
) {
    fun countAdded(): Int = items.values.count { it.added }
}

data class BadgeRuleStoreState(
    val version: Int = 1,
    val rules: MutableList<BadgeRule> = mutableListOf(),
    val nextPlayIndex: Int = 0
)

object RingtoneBadgeRuleStore {
    private const val PREF_NAME = "ringtone_badge_rule_store"
    private const val KEY_RULE_STORE_STATE = "key_rule_store_state"
    private const val KEY_PRESET_INIT_DONE = "key_preset_init_done"
    private const val MAX_RULE_COUNT = 20

    private val gson = Gson()
    private val type = object : TypeToken<BadgeRuleStoreState>() {}.type
    private val lock = Any()

    fun ensureInitIfEmpty(context: Context) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val initDone = prefs.getBoolean(KEY_PRESET_INIT_DONE, false)
            val state = readState(context)
            if (state.rules.isNotEmpty()) {
                return
            }
            if (initDone) {
                return
            }
            val presetRules = PresetRuleInitializer.createPresetRules()
            writeState(context, state.copy(rules = presetRules.toMutableList(), nextPlayIndex = 0))
            prefs.edit().putBoolean(KEY_PRESET_INIT_DONE, true).apply()
        }
    }

    fun listRules(context: Context): List<BadgeRule> {
        ensureInitIfEmpty(context)
        return readState(context).rules
    }

    fun getRule(context: Context, ruleId: String): BadgeRule? {
        ensureInitIfEmpty(context)
        return readState(context).rules.firstOrNull { it.id == ruleId }
    }

    fun createRule(context: Context, name: String): BadgeRule? {
        synchronized(lock) {
            ensureInitIfEmpty(context)
            val state = readState(context)
            if (state.rules.size >= MAX_RULE_COUNT) {
                return null
            }
            val now = System.currentTimeMillis()
            val rule = BadgeRule(
                id = UUID.randomUUID().toString(),
                name = name,
                items = createEmptyItems(),
                updatedAt = now,
                sourceType = RuleSourceType.CUSTOM
            )
            state.rules.add(rule)
            writeState(context, state)
            return rule
        }
    }

    fun renameRule(context: Context, ruleId: String, name: String): Boolean {
        synchronized(lock) {
            ensureInitIfEmpty(context)
            val state = readState(context)
            val index = state.rules.indexOfFirst { it.id == ruleId }
            if (index < 0) {
                return false
            }
            val old = state.rules[index]
            state.rules[index] = old.copy(name = name, updatedAt = System.currentTimeMillis())
            writeState(context, state)
            return true
        }
    }

    fun saveRuleLayout(context: Context, rule: BadgeRule): Boolean {
        synchronized(lock) {
            ensureInitIfEmpty(context)
            val state = readState(context)
            val index = state.rules.indexOfFirst { it.id == rule.id }
            if (index < 0) {
                return false
            }
            state.rules[index] = rule.copy(updatedAt = System.currentTimeMillis())
            writeState(context, state)
            return true
        }
    }

    fun deleteRule(context: Context, ruleId: String): Boolean {
        synchronized(lock) {
            ensureInitIfEmpty(context)
            val state = readState(context)
            val removed = state.rules.removeAll { it.id == ruleId }
            if (!removed) {
                return false
            }
            val nextIndex = if (state.rules.isEmpty()) {
                0
            } else {
                state.nextPlayIndex.coerceIn(0, state.rules.lastIndex)
            }
            writeState(context, state.copy(nextPlayIndex = nextIndex))
            return true
        }
    }

    fun getNextRuleForPlayback(context: Context): BadgeRule? {
        synchronized(lock) {
            ensureInitIfEmpty(context)
            val state = readState(context)
            if (state.rules.isEmpty()) {
                return null
            }
            val currentIndex = state.nextPlayIndex.coerceIn(0, state.rules.lastIndex)
            val currentRule = state.rules[currentIndex]
            val nextIndex = (currentIndex + 1) % state.rules.size
            writeState(context, state.copy(nextPlayIndex = nextIndex))
            return currentRule
        }
    }

    fun createEmptyItems(): MutableMap<String, BadgeItemState> {
        val result = mutableMapOf<String, BadgeItemState>()
        BadgeId.entries.forEachIndexed { index, badgeId ->
            result[badgeId.key] = BadgeItemState(
                added = false,
                xPercent = 0.5f,
                yPercent = 0.5f,
                scale = 1f,
                zIndex = index
            )
        }
        return result
    }

    private fun readState(context: Context): BadgeRuleStoreState {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RULE_STORE_STATE, null)
            ?: return BadgeRuleStoreState()
        return runCatching { gson.fromJson<BadgeRuleStoreState>(raw, type) }
            .getOrElse { BadgeRuleStoreState() }
            .normalize()
    }

    private fun writeState(context: Context, state: BadgeRuleStoreState) {
        val normalized = state.normalize()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RULE_STORE_STATE, gson.toJson(normalized))
            .apply()
    }

    private fun BadgeRuleStoreState.normalize(): BadgeRuleStoreState {
        val normalizedRules = rules.map { rule ->
            val normalizedItems = createEmptyItems()
            rule.items.forEach { (key, value) ->
                if (BadgeId.fromKey(key) != null) {
                    normalizedItems[key] = value
                }
            }
            rule.copy(items = normalizedItems)
        }.toMutableList()
        val normalizedIndex = if (normalizedRules.isEmpty()) {
            0
        } else {
            nextPlayIndex.coerceIn(0, normalizedRules.lastIndex)
        }
        return copy(rules = normalizedRules, nextPlayIndex = normalizedIndex)
    }
}

object PresetRuleInitializer {
    fun ensureInitIfEmpty(context: Context = App.getContext()) {
        RingtoneBadgeRuleStore.ensureInitIfEmpty(context)
    }

    fun createPresetRules(): List<BadgeRule> {
        val now = System.currentTimeMillis()
        return listOf(
            presetRule(
                id = "preset_rule_1",
                name = "预设规则1",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_3.key] = BadgeItemState(true, 0.87f, 0.88f, 1f, 0)
                }
            ),
            presetRule(
                id = "preset_rule_2",
                name = "预设规则2",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_1.key] = BadgeItemState(true, 0.82f, 0.12f, 1f, 0)
                    it[BadgeId.BADGE_3.key] = BadgeItemState(true, 0.87f, 0.88f, 1f, 1)
                }
            ),
            presetRule(
                id = "preset_rule_3",
                name = "预设规则3",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_4.key] = BadgeItemState(true, 0.18f, 0.12f, 1f, 0)
                    it[BadgeId.BADGE_2.key] = BadgeItemState(true, 0.16f, 0.52f, 1f, 1)
                    it[BadgeId.BADGE_3.key] = BadgeItemState(true, 0.87f, 0.88f, 1f, 2)
                }
            ),
            presetRule(
                id = "preset_rule_4",
                name = "预设规则4",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_1.key] = BadgeItemState(true, 0.50f, 0.12f, 1f, 0)
                    it[BadgeId.BADGE_3.key] = BadgeItemState(true, 0.87f, 0.88f, 1f, 1)
                }
            ),
            presetRule(
                id = "preset_rule_5",
                name = "预设规则5",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_4.key] = BadgeItemState(true, 0.18f, 0.12f, 1f, 0)
                    it[BadgeId.BADGE_2.key] = BadgeItemState(true, 0.16f, 0.52f, 1f, 1)
                }
            ),
            presetRule(
                id = "preset_rule_6",
                name = "预设规则6",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_4.key] = BadgeItemState(true, 0.18f, 0.12f, 1f, 0)
                }
            ),
            presetRule(
                id = "preset_rule_7",
                name = "预设规则7",
                now = now,
                customizer = {
                    it[BadgeId.BADGE_2.key] = BadgeItemState(true, 0.16f, 0.52f, 1f, 0)
                    it[BadgeId.BADGE_3.key] = BadgeItemState(true, 0.87f, 0.88f, 1f, 1)
                }
            )
        )
    }

    private fun presetRule(
        id: String,
        name: String,
        now: Long,
        customizer: (MutableMap<String, BadgeItemState>) -> Unit
    ): BadgeRule {
        val items = RingtoneBadgeRuleStore.createEmptyItems()
        customizer(items)
        return BadgeRule(
            id = id,
            name = name,
            items = items,
            updatedAt = now,
            sourceType = RuleSourceType.PRESET
        )
    }
}
