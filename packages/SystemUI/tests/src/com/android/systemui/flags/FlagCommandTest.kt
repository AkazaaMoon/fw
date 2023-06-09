/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.flags

import android.test.suitebuilder.annotation.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.util.mockito.any
import java.io.PrintWriter
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations

@SmallTest
class FlagCommandTest : SysuiTestCase() {

    @Mock private lateinit var featureFlags: FeatureFlagsDebug
    @Mock private lateinit var pw: PrintWriter
    private val flagMap = mutableMapOf<Int, Flag<*>>()
    private val flagA = UnreleasedFlag(500, "500", "test")
    private val flagB = ReleasedFlag(501, "501", "test")
    private val stringFlag = StringFlag(502, "502", "test", "abracadabra")
    private val intFlag = IntFlag(503, "503", "test", 12)

    private lateinit var cmd: FlagCommand

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        whenever(featureFlags.isEnabled(any(UnreleasedFlag::class.java))).thenReturn(false)
        whenever(featureFlags.isEnabled(any(ReleasedFlag::class.java))).thenReturn(true)
        whenever(featureFlags.getString(any(StringFlag::class.java))).thenAnswer { invocation ->
            (invocation.getArgument(0) as StringFlag).default
        }
        whenever(featureFlags.getInt(any(IntFlag::class.java))).thenAnswer { invocation ->
            (invocation.getArgument(0) as IntFlag).default
        }

        flagMap.put(flagA.id, flagA)
        flagMap.put(flagB.id, flagB)
        flagMap.put(stringFlag.id, stringFlag)
        flagMap.put(intFlag.id, intFlag)

        cmd = FlagCommand(featureFlags, flagMap)
    }

    @Test
    fun readBooleanFlagCommand() {
        cmd.execute(pw, listOf(flagA.id.toString()))
        Mockito.verify(featureFlags).isEnabled(flagA)
    }

    @Test
    fun readStringFlagCommand() {
        cmd.execute(pw, listOf(stringFlag.id.toString()))
        Mockito.verify(featureFlags).getString(stringFlag)
    }

    @Test
    fun readIntFlag() {
        cmd.execute(pw, listOf(intFlag.id.toString()))
        Mockito.verify(featureFlags).getInt(intFlag)
    }

    @Test
    fun setBooleanFlagCommand() {
        cmd.execute(pw, listOf(flagB.id.toString(), "on"))
        Mockito.verify(featureFlags).setBooleanFlagInternal(flagB, true)
    }

    @Test
    fun setStringFlagCommand() {
        cmd.execute(pw, listOf(stringFlag.id.toString(), "set", "foobar"))
        Mockito.verify(featureFlags).setStringFlagInternal(stringFlag, "foobar")
    }

    @Test
    fun setIntFlag() {
        cmd.execute(pw, listOf(intFlag.id.toString(), "put", "123"))
        Mockito.verify(featureFlags).setIntFlagInternal(intFlag, 123)
    }

    @Test
    fun toggleBooleanFlagCommand() {
        cmd.execute(pw, listOf(flagB.id.toString(), "toggle"))
        Mockito.verify(featureFlags).setBooleanFlagInternal(flagB, false)
    }

    @Test
    fun eraseFlagCommand() {
        cmd.execute(pw, listOf(flagA.id.toString(), "erase"))
        Mockito.verify(featureFlags).eraseFlag(flagA)
    }
}
