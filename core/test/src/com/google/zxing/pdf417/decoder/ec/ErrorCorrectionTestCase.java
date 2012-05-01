/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.pdf417.decoder.ec;

import com.google.zxing.ChecksumException;
import org.junit.Test;

import java.util.Random;

/**
 * @author Sean Owen
 */
public final class ErrorCorrectionTestCase extends AbstractErrorCorrectionTestCase {

  /** See ISO 15438, Annex Q */
  private static final int[] PDF417_TEST =
      { 5, 453, 178, 121, 239 };
  private static final int[] PDF417_TEST_WITH_EC =
      { 5, 453, 178, 121, 239, 452, 327, 657, 619 };
  private static final int ECC_BYTES = PDF417_TEST_WITH_EC.length - PDF417_TEST.length;
  // Example is EC level 1 (s=1). The number of erasures (l) and substitutions (f) must obey:
  // l + 2f <= 2^(s+1) - 3
  private static final int EC_LEVEL = 1;
  private static final int ERROR_LIMIT = (1 << (EC_LEVEL + 1)) - 3;
  private static final int MAX_ERRORS = ERROR_LIMIT / 2;
  private static final int MAX_ERASURES = ERROR_LIMIT;

  private final ErrorCorrection ec = new ErrorCorrection();

  @Test
  public void testNoError() throws ChecksumException {
    int[] received = PDF417_TEST_WITH_EC.clone();
    // no errors
    checkDecode(received);
  }

  @Test
  public void testOneError() throws ChecksumException {
    Random random = getRandom();
    for (int i = 0; i < PDF417_TEST_WITH_EC.length; i++) {
      int[] received = PDF417_TEST_WITH_EC.clone();
      received[i] = random.nextInt(256);
      checkDecode(received);
    }
  }

  @Test
  public void testMaxErrors() throws ChecksumException {
    Random random = getRandom();
    for (int test : PDF417_TEST) { // # iterations is kind of arbitrary
      int[] received = PDF417_TEST_WITH_EC.clone();
      corrupt(received, MAX_ERRORS, random);
      checkDecode(received);
    }
  }

  @Test
  public void testTooManyErrors() {
    int[] received = PDF417_TEST_WITH_EC.clone();
    Random random = getRandom();
    corrupt(received, MAX_ERRORS + 3, random); // +3 since the algo can actually correct 2 more than it should here
    try {
      checkDecode(received);
      fail("Should not have decoded");
    } catch (ChecksumException ce) {
      // good
    }
  }

  @Test
  public void testMaxErasures() throws ChecksumException {
    Random random = getRandom();
    for (int test : PDF417_TEST) { // # iterations is kind of arbitrary
      int[] received = PDF417_TEST_WITH_EC.clone();
      int[] erasures = erase(received, MAX_ERASURES, random);
      checkDecode(received, erasures);
    }
  }

  @Test
  public void testTooManyErasures() {
    Random random = getRandom();
    int[] received = PDF417_TEST_WITH_EC.clone();
    int[] erasures = erase(received, MAX_ERASURES + 1, random);
    try {
      checkDecode(received, erasures);
      fail("Should not have decoded");
    } catch (ChecksumException ce) {
      // good
    }
  }

  @Test
  public void testErasureAndError() throws ChecksumException {
    // Not sure this is valid according to the spec but it's correctable
    Random random = getRandom();
    for (int i = 0; i < PDF417_TEST_WITH_EC.length; i++) {
      int[] received = PDF417_TEST_WITH_EC.clone();
      received[i] = random.nextInt(256);
      for (int j = 0; j < PDF417_TEST_WITH_EC.length; j++) {
        if (i == j) {
          continue;
        }
        received[j] = 0;
        int[] erasures = {j};
        checkDecode(received, erasures);
      }
    }
  }

  private void checkDecode(int[] received) throws ChecksumException {
    checkDecode(received, new int[0]);
  }

  private void checkDecode(int[] received, int[] erasures) throws ChecksumException {
    ec.decode(received, ECC_BYTES, erasures);
    for (int i = 0; i < PDF417_TEST.length; i++) {
      assertEquals(received[i], PDF417_TEST[i]);
    }
  }

}