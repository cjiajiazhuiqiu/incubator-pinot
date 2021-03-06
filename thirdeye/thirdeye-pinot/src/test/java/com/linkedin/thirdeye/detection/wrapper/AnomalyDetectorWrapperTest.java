/*
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

package com.linkedin.thirdeye.detection.wrapper;

import com.google.common.collect.ImmutableMap;
import com.linkedin.thirdeye.api.TimeSpec;
import com.linkedin.thirdeye.datalayer.dto.DatasetConfigDTO;
import com.linkedin.thirdeye.datalayer.dto.DetectionConfigDTO;
import com.linkedin.thirdeye.datalayer.dto.MetricConfigDTO;
import com.linkedin.thirdeye.detection.MockDataProvider;
import com.linkedin.thirdeye.detection.components.ThresholdRuleDetector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class AnomalyDetectorWrapperTest {
  private static final String PROP_METRIC_URN = "metricUrn";
  private static final String PROP_MOVING_WINDOW_DETECTION = "isMovingWindowDetection";
  private static final String PROP_DETECTOR = "detector";

  private MockDataProvider provider;
  private Map<String, Object> properties;
  private DetectionConfigDTO config;
  private Map<String, Object> stageSpecs;

  @BeforeMethod
  public void setUp() {
    this.properties = new HashMap<>();
    this.properties.put(PROP_METRIC_URN, "thirdeye:metric:1");
    this.properties.put(PROP_DETECTOR, "$testDetector");
    this.config = new DetectionConfigDTO();
    this.config.setComponents(ImmutableMap.of("testDetector", new ThresholdRuleDetector()));
    this.config.setProperties(properties);

    this.provider = new MockDataProvider();
    MetricConfigDTO metric = new MetricConfigDTO();
    metric.setId(1L);
    metric.setDataset("test");
    this.provider.setMetrics(Collections.singletonList(metric));
    DatasetConfigDTO dataset = new DatasetConfigDTO();
    dataset.setDataset("test");
    dataset.setTimeUnit(TimeUnit.DAYS);
    dataset.setTimeDuration(1);
    this.provider.setDatasets(Collections.singletonList(dataset));
  }

  @Test
  public void testMonitoringWindow() {
    AnomalyDetectorWrapper detectionPipeline =
        new AnomalyDetectorWrapper(this.provider, this.config, 1538418436000L, 1540837636000L);
    List<Interval> monitoringWindows = detectionPipeline.getMonitoringWindows();
    for (Interval window : monitoringWindows) {
      Assert.assertEquals(window, new Interval(1538418436000L, 1540837636000L));
    }
  }

  @Test
  public void testMovingMonitoringWindow() {
    this.properties.put(PROP_MOVING_WINDOW_DETECTION, true);
    AnomalyDetectorWrapper detectionPipeline =
        new AnomalyDetectorWrapper(this.provider, this.config, 1540147725000L, 1540493325000L);
    List<Interval> monitoringWindows = detectionPipeline.getMonitoringWindows();
    DateTimeZone timeZone = DateTimeZone.forID(TimeSpec.DEFAULT_TIMEZONE);
    Assert.assertEquals(monitoringWindows,
        Arrays.asList(new Interval(1540080000000L, 1540166400000L, timeZone), new Interval(1540166400000L, 1540252800000L, timeZone),
            new Interval(1540252800000L, 1540339200000L, timeZone), new Interval(1540339200000L, 1540425600000L, timeZone)));
  }

}
