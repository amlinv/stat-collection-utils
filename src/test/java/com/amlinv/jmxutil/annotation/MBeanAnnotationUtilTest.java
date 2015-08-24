/*
 *   Copyright 2015 AML Innovation & Consulting LLC
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.amlinv.jmxutil.annotation;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by art on 8/18/15.
 */
public class MBeanAnnotationUtilTest {

  private Object withoutAnnotation;
  private TestAnnotation1 testAnnotation1;

  @Before
  public void setupTest() throws Exception {
    this.withoutAnnotation = new Object();
    this.testAnnotation1 = new TestAnnotation1();
  }

  @Test
  public void testGetLocationONamePattern() throws Exception {
    assertEquals("x-oname-pattern-x",
                 MBeanAnnotationUtil.getLocationONamePattern(this.testAnnotation1));

    assertNull(MBeanAnnotationUtil.getLocationONamePattern(this.withoutAnnotation));
  }

  @Test
  public void testGetAttributes() throws Exception {
    Map<String, Method> atts = MBeanAnnotationUtil.getAttributes(this.testAnnotation1);

    assertEquals(2, atts.size());
    assertEquals("setAttribute1", atts.get("x-att1-name-x").getName());
    assertEquals("setAttribute2", atts.get("x-att2-name-x").getName());

    assertEquals(0, MBeanAnnotationUtil.getAttributes(this.withoutAnnotation).size());
  }

  // Test the constructor for completeness (code coverage).
  @Test
  public void testConstructor() throws Exception {
    new MBeanAnnotationUtil();
  }

  @MBeanLocation(onamePattern = "x-oname-pattern-x")
  protected static class TestAnnotation1 {
    private String attribute1;
    private String attribute2;

    @MBeanAttribute(name = "x-att1-name-x", type = String.class)
    public void setAttribute1 (String value) {
      this.attribute1 = value;
    }

    @MBeanAttribute(name = "x-att2-name-x", type = String.class)
    public void setAttribute2 (String value) {
      this.attribute2 = value;
    }
  }
}
