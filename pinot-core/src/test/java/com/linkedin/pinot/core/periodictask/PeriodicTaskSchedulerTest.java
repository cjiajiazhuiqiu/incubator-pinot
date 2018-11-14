/**
 * Copyright (C) 2014-2018 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.core.periodictask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class PeriodicTaskSchedulerTest {

  @Test
  public void testTaskWithInvalidInterval() throws Exception {
    AtomicBoolean initCalled = new AtomicBoolean();
    AtomicBoolean runCalled = new AtomicBoolean();

    List<PeriodicTask> periodicTasks = Collections.singletonList(new BasePeriodicTask("TestTask", 0L, 0L) {
      @Override
      public void init() {
        initCalled.set(true);
      }

      @Override
      public void run() {
        runCalled.set(true);
      }
    });

    PeriodicTaskScheduler taskScheduler = new PeriodicTaskScheduler();
    taskScheduler.start(periodicTasks);
    Thread.sleep(100L);
    taskScheduler.stop();

    assertFalse(initCalled.get());
    assertFalse(runCalled.get());
  }

  @Test
  public void testScheduleMultipleTasks() throws Exception {
    int numTasks = 3;
    AtomicInteger numTimesInitCalled = new AtomicInteger();
    AtomicInteger numTimesRunCalled = new AtomicInteger();

    List<PeriodicTask> periodicTasks = new ArrayList<>(numTasks);
    for (int i = 0; i < numTasks; i++) {
      periodicTasks.add(new BasePeriodicTask("Task", 1L, 0L) {
        @Override
        public void init() {
          numTimesInitCalled.getAndIncrement();
        }

        @Override
        public void run() {
          numTimesRunCalled.getAndIncrement();
        }
      });
    }

    PeriodicTaskScheduler taskScheduler = new PeriodicTaskScheduler();
    taskScheduler.start(periodicTasks);
    Thread.sleep(1100L);
    taskScheduler.stop();

    assertEquals(numTimesInitCalled.get(), numTasks);
    assertEquals(numTimesRunCalled.get(), numTasks * 2);
  }
}
