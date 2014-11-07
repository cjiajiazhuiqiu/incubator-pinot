package com.linkedin.thirdeye.impl;

import com.linkedin.thirdeye.api.StarTreeConstants;
import com.linkedin.thirdeye.api.StarTreeRecord;
import com.linkedin.thirdeye.api.StarTreeRecordStore;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestStarTreeRecordStoreCircularBufferImpl
{
  private final List<String> dimensionNames = Arrays.asList("A", "B", "C");
  private final List<String> metricNames = Arrays.asList("M");
  private final Map<String, Map<String, Integer>> forwardIndex = new HashMap<String, Map<String, Integer>>();
  private final int numTimeBuckets = 4;
  private final int numRecords = 100;

  private File file;
  private StarTreeRecordStore recordStore;

  @BeforeClass
  public void beforeClass() throws Exception
  {
    file = new File(System.getProperty("java.io.tmpdir"), TestStarTreeRecordStoreCircularBufferImpl.class.getSimpleName());

    if (file.exists())
    {
      FileUtils.forceDelete(file);
    }

    Map<String, Integer> aValues = new HashMap<String, Integer>();
    aValues.put(StarTreeConstants.STAR, StarTreeConstants.STAR_VALUE);
    aValues.put(StarTreeConstants.OTHER, StarTreeConstants.OTHER_VALUE);
    aValues.put("A0", 2);
    aValues.put("A1", 3);
    aValues.put("A2", 4);
    aValues.put("A3", 5);

    Map<String, Integer> bValues = new HashMap<String, Integer>();
    bValues.put(StarTreeConstants.STAR, StarTreeConstants.STAR_VALUE);
    bValues.put(StarTreeConstants.OTHER, StarTreeConstants.OTHER_VALUE);
    bValues.put("B0", 2);
    bValues.put("B1", 3);
    bValues.put("B2", 4);

    Map<String, Integer> cValues = new HashMap<String, Integer>();
    cValues.put(StarTreeConstants.STAR, StarTreeConstants.STAR_VALUE);
    cValues.put(StarTreeConstants.OTHER, StarTreeConstants.OTHER_VALUE);
    cValues.put("C0", 2);
    cValues.put("C1", 3);

    forwardIndex.put("A", aValues);
    forwardIndex.put("B", bValues);
    forwardIndex.put("C", cValues);
  }

  @BeforeMethod
  public void beforeMethod() throws Exception
  {
    // Init
    recordStore = new StarTreeRecordStoreCircularBufferImpl(UUID.randomUUID(), file, dimensionNames, metricNames, forwardIndex, numTimeBuckets);

    // Generate records
    List<StarTreeRecord> records = new ArrayList<StarTreeRecord>();
    for (int i = 0; i < numRecords; i++)
    {
      StarTreeRecordImpl.Builder builder = new StarTreeRecordImpl.Builder()
              .setDimensionValue("A", "A" + (i % 4))
              .setDimensionValue("B", "B" + (i % 3))
              .setDimensionValue("C", "C" + (i % 2))
              .setMetricValue("M", 1L)
              .setTime((long) (i / (numRecords / numTimeBuckets)));
      records.add(builder.build());
    }

    // Fill a buffer and write to file
    ByteBuffer byteBuffer = ByteBuffer.allocate(numRecords * recordStore.getEntrySize()); // upper bound
    StarTreeRecordStoreCircularBufferImpl.fillBuffer(byteBuffer, dimensionNames, metricNames, forwardIndex, records, numTimeBuckets);
    byteBuffer.flip();
    FileChannel fileChannel = new FileOutputStream(file).getChannel();
    fileChannel.write(byteBuffer);
    fileChannel.close();

//    // Debug
//    fileChannel = new RandomAccessFile(file, "rw").getChannel();
//    MappedByteBuffer fromFile = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
//    StarTreeRecordStoreCircularBufferImpl.dumpBuffer(fromFile, System.out, dimensionNames, metricNames, numTimeBuckets);

    // Open
    recordStore.open();
  }

  @AfterMethod
  public void afterMethod() throws Exception
  {
    FileUtils.forceDelete(file);
  }

  @Test
  public void testIterator() throws Exception
  {
    long sum = 0;

    for (StarTreeRecord record : recordStore)
    {
      sum += record.getMetricValues().get("M");
    }

    Assert.assertEquals(sum, numRecords);
  }
}
