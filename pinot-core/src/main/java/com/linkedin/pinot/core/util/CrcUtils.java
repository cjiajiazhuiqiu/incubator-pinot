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
package com.linkedin.pinot.core.util;

import com.google.common.base.Preconditions;
import com.linkedin.pinot.core.segment.creator.impl.V1Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;


@SuppressWarnings("Duplicates")
public class CrcUtils {
  private static final int BUFFER_SIZE = 65536;

  private final List<File> _files;

  private CrcUtils(List<File> files) {
    _files = files;
  }

  public static CrcUtils forAllFilesInFolder(File dir) {
    List<File> normalFiles = new ArrayList<>();
    getAllNormalFiles(dir, normalFiles);
    Collections.sort(normalFiles);
    return new CrcUtils(normalFiles);
  }

  /**
   * Helper method to get all normal (non-directory) files under a directory recursively.
   * <p>NOTE: do not include the segment creation meta file.
   */
  private static void getAllNormalFiles(File dir, List<File> normalFiles) {
    File[] files = dir.listFiles();
    Preconditions.checkNotNull(files);
    for (File file : files) {
      if (file.isFile()) {
        if (!file.getName().equals(V1Constants.SEGMENT_CREATION_META)) {
          normalFiles.add(file);
        }
      } else {
        getAllNormalFiles(file, normalFiles);
      }
    }
  }

  public long computeCrc() throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    Checksum checksum = new Adler32();

    for (File file : _files) {
      try (InputStream input = new FileInputStream(file)) {
        int len;
        while ((len = input.read(buffer)) > 0) {
          checksum.update(buffer, 0, len);
        }
      }
    }

    return checksum.getValue();
  }

  public String computeMD5() throws NoSuchAlgorithmException, IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    MessageDigest digest = MessageDigest.getInstance("md5");

    for (File file : _files) {
      try (InputStream input = new FileInputStream(file)) {
        int len;
        while ((len = input.read(buffer)) > 0) {
          digest.update(buffer, 0, len);
        }
      }
    }

    return toHexaDecimal(digest.digest());
  }

  public static String toHexaDecimal(byte[] bytesToConvert) {
    final char[] hexCharactersAsArray = "0123456789ABCDEF".toCharArray();
    final char[] convertedHexCharsArray = new char[bytesToConvert.length * 2];
    for (int j = 0; j < bytesToConvert.length; j++) {
      final int v = bytesToConvert[j] & 0xFF;
      convertedHexCharsArray[j * 2] = hexCharactersAsArray[v >>> 4];
      convertedHexCharsArray[j * 2 + 1] = hexCharactersAsArray[v & 0x0F];
    }
    return new String(convertedHexCharsArray);
  }
}
