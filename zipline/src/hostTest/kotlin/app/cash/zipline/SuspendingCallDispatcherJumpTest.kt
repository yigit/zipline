/*
 * Copyright (C) 2025 Cash App
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
package app.cash.zipline

import app.cash.zipline.testing.EchoRequest
import app.cash.zipline.testing.SuspendingEchoService
import app.cash.zipline.testing.loadTestingJs
import app.cash.zipline.testing.singleThreadCoroutineDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SuspendingCallDispatcherJumpTest {
  private val dispatcher = singleThreadCoroutineDispatcher(
    name = "suspendingTest",
    stackSize = 8 * 1024 * 1024
  )
  private val zipline = Zipline.create(dispatcher)

  @BeforeTest
  fun setUp() = runBlocking(dispatcher) {
    zipline.loadTestingJs()
  }

  @AfterTest
  fun tearDown() = runBlocking(dispatcher) {
    zipline.close()
    dispatcher.close()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun callSuspendFunctionFromAnotherDispatcher() = runTest {
    val echoService = withContext(dispatcher) {
      zipline.quickJs.evaluate("testing.app.cash.zipline.testing.prepareSuspendingJsBridges()")
      zipline.quickJs.evaluate("testing.app.cash.zipline.testing.unblockSuspendingJs()")
      zipline.take<SuspendingEchoService>("jsSuspendingEchoService")
    }
    val response = withContext(Dispatchers.IO) {
      echoService.suspendingEcho(
        EchoRequest("buddy")
      )
    }
    assertEquals("hello from suspending JavaScript, buddy", response.message)
  }
}
