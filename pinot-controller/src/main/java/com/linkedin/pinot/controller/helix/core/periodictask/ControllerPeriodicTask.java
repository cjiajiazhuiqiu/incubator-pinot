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
package com.linkedin.pinot.controller.helix.core.periodictask;

import com.linkedin.pinot.controller.helix.core.PinotHelixResourceManager;
import com.linkedin.pinot.core.periodictask.BasePeriodicTask;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The base periodic task for pinot controller only. It uses <code>PinotHelixResourceManager</code> to determine
 * which table resources should be managed by this Pinot controller.
 */
public abstract class ControllerPeriodicTask extends BasePeriodicTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerPeriodicTask.class);
  private static final Random RANDOM = new Random();

  public static final int MIN_INITIAL_DELAY_IN_SECONDS = 120;
  public static final int MAX_INITIAL_DELAY_IN_SECONDS = 300;

  protected final PinotHelixResourceManager _pinotHelixResourceManager;

  private boolean _wasLeader = false;

  public ControllerPeriodicTask(String taskName, long runFrequencyInSeconds, long initialDelayInSeconds,
      PinotHelixResourceManager pinotHelixResourceManager) {
    super(taskName, runFrequencyInSeconds, initialDelayInSeconds);
    _pinotHelixResourceManager = pinotHelixResourceManager;
  }

  public ControllerPeriodicTask(String taskName, long runFrequencyInSeconds,
      PinotHelixResourceManager pinotHelixResourceManager) {
    this(taskName, runFrequencyInSeconds, getRandomInitialDelayInSeconds(), pinotHelixResourceManager);
  }

  private static long getRandomInitialDelayInSeconds() {
    return MIN_INITIAL_DELAY_IN_SECONDS + RANDOM.nextInt(MAX_INITIAL_DELAY_IN_SECONDS - MIN_INITIAL_DELAY_IN_SECONDS);
  }

  @Override
  public void init() {
  }

  @Override
  public void run() {
    if (!_pinotHelixResourceManager.isLeader()) {
      skipLeaderTask();
    } else {
      List<String> allTableNames = _pinotHelixResourceManager.getAllTables();
      processLeaderTask(allTableNames);
    }
  }

  private void skipLeaderTask() {
    if (_wasLeader) {
      LOGGER.info("Current pinot controller lost leadership.");
      onBecomeNotLeader();
      _wasLeader = false;
    }
    LOGGER.info("Skip running periodic task: {} on non-leader controller", _taskName);
  }

  private void processLeaderTask(List<String> tables) {
    if (!_wasLeader) {
      LOGGER.info("Current pinot controller became leader. Starting {} with running frequency of {} seconds.",
          _taskName, _intervalInSeconds);
      onBecomeLeader();
      _wasLeader = true;
    }
    long startTime = System.currentTimeMillis();
    int numTables = tables.size();
    LOGGER.info("Start processing {} tables in periodic task: {}", numTables, _taskName);
    process(tables);
    LOGGER.info("Finish processing {} tables in periodic task: {} in {}ms", numTables, _taskName,
        (System.currentTimeMillis() - startTime));
  }

  /**
   * Does the following logic when losing the leadership. This should be done only once during leadership transition.
   */
  public void onBecomeNotLeader() {
  }

  /**
   * Does the following logic when becoming lead controller. This should be done only once during leadership transition.
   */
  public void onBecomeLeader() {
  }

  /**
   * Processes the task on the given tables.
   *
   * @param tables List of table names
   */
  public abstract void process(List<String> tables);
}
