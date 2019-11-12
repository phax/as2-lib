/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
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
package com.helger.as2lib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Test class for class {@link CAS2Version}
 *
 * @author Philip Helger
 */
public final class CAS2VersionTest
{
  @Test
  public void testBasic ()
  {
    assertNotEquals ("undefined", CAS2Version.BUILD_VERSION);
    assertNotEquals ("undefined", CAS2Version.BUILD_TIMESTAMP);

    // Check variable resolution
    assertFalse (CAS2Version.BUILD_VERSION.contains ("${"));
    assertFalse (CAS2Version.BUILD_TIMESTAMP.contains ("${"));
  }
}
