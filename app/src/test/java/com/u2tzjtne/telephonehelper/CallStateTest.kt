package com.u2tzjtne.telephonehelper

import com.u2tzjtne.telephonehelper.ui.activity.newCallActivity.CallState
import org.junit.Assert.*
import org.junit.Test

/**
 * 通话状态枚举测试
 * 验证状态枚举的完整性与正确性
 */
class CallStateTest {

    // ===================== 枚举值完整性测试 =====================

    @Test
    fun `验证所有通话状态枚举值存在`() {
        val values = CallState.values()
        assertEquals("状态枚举应有6个值", 6, values.size)
        assertTrue(values.contains(CallState.DIALING))
        assertTrue(values.contains(CallState.RINGING))
        assertTrue(values.contains(CallState.CONNECTED))
        assertTrue(values.contains(CallState.BUSY))
        assertTrue(values.contains(CallState.NO_ANSWER))
        assertTrue(values.contains(CallState.HUNG_UP))
    }

    @Test
    fun `通过名称解析枚举值_DIALING`() {
        val state = CallState.valueOf("DIALING")
        assertEquals(CallState.DIALING, state)
    }

    @Test
    fun `通过名称解析枚举值_RINGING`() {
        val state = CallState.valueOf("RINGING")
        assertEquals(CallState.RINGING, state)
    }

    @Test
    fun `通过名称解析枚举值_CONNECTED`() {
        val state = CallState.valueOf("CONNECTED")
        assertEquals(CallState.CONNECTED, state)
    }

    @Test
    fun `通过名称解析枚举值_BUSY`() {
        val state = CallState.valueOf("BUSY")
        assertEquals(CallState.BUSY, state)
    }

    @Test
    fun `通过名称解析枚举值_NO_ANSWER`() {
        val state = CallState.valueOf("NO_ANSWER")
        assertEquals(CallState.NO_ANSWER, state)
    }

    @Test
    fun `通过名称解析枚举值_HUNG_UP`() {
        val state = CallState.valueOf("HUNG_UP")
        assertEquals(CallState.HUNG_UP, state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `无效枚举名称应抛出异常`() {
        CallState.valueOf("INVALID_STATE")
    }

    // ===================== 枚举序号测试 =====================

    @Test
    fun `枚举序号顺序正确`() {
        assertEquals(0, CallState.DIALING.ordinal)
        assertEquals(1, CallState.RINGING.ordinal)
        assertEquals(2, CallState.CONNECTED.ordinal)
        assertEquals(3, CallState.BUSY.ordinal)
        assertEquals(4, CallState.NO_ANSWER.ordinal)
        assertEquals(5, CallState.HUNG_UP.ordinal)
    }

    // ===================== 枚举状态语义分类测试 =====================

    @Test
    fun `预连接状态集合正确`() {
        // 预连接阶段：DIALING 和 RINGING
        val preConnectStates = setOf(CallState.DIALING, CallState.RINGING)
        assertTrue(preConnectStates.contains(CallState.DIALING))
        assertTrue(preConnectStates.contains(CallState.RINGING))
        assertFalse(preConnectStates.contains(CallState.CONNECTED))
    }

    @Test
    fun `终结状态集合正确`() {
        // 终结状态：BUSY / NO_ANSWER / HUNG_UP
        val terminalStates = setOf(CallState.BUSY, CallState.NO_ANSWER, CallState.HUNG_UP)
        assertFalse(terminalStates.contains(CallState.DIALING))
        assertFalse(terminalStates.contains(CallState.RINGING))
        assertFalse(terminalStates.contains(CallState.CONNECTED))
        assertTrue(terminalStates.contains(CallState.BUSY))
        assertTrue(terminalStates.contains(CallState.NO_ANSWER))
        assertTrue(terminalStates.contains(CallState.HUNG_UP))
    }

    @Test
    fun `枚举名称与valueOf互逆`() {
        for (state in CallState.values()) {
            assertEquals(state, CallState.valueOf(state.name))
        }
    }
}
