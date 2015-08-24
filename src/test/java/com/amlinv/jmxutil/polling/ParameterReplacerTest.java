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

package com.amlinv.jmxutil.polling;

import com.amlinv.jmxutil.MBeanLocationParameterSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Validate operation of the ParameterReplacer.
 *
 * Created by art on 8/22/15.
 */
public class ParameterReplacerTest {
    private MBeanLocationParameterSource mockSource;
    private ParameterReplacer replacer;


    @Before
    public void setupTest() throws Exception {
        mockSource = Mockito.mock(MBeanLocationParameterSource.class);

        replacer = new ParameterReplacer();
    }

    /**
     * Verify replacement of parameter values.  Includes boundary testing of 1, 2, and >2 parameters.
     *
     * @throws Exception
     */
    @Test
    public void testReplaceObjectNameParameters() throws Exception {
        Mockito.when(this.mockSource.getParameter("x-param1-x")).thenReturn("x-value1-x");
        Mockito.when(this.mockSource.getParameter("x-param2-x")).thenReturn("x-value2-x");
        Mockito.when(this.mockSource.getParameter("x-param3-x")).thenReturn("x-value3-x");

        String result;

        // Match 1
        result = this.replacer.replaceObjectNameParameters("X${x-param1-x}X", mockSource);
        assertEquals("Xx-value1-xX", result);

        // Match 2
        result = this.replacer.replaceObjectNameParameters("X${x-param1-x}X${x-param2-x}X", mockSource);
        assertEquals("Xx-value1-xXx-value2-xX", result);

        // Match 3
        result = this.replacer.replaceObjectNameParameters("X${x-param1-x}X${x-param2-x}X${x-param3-x}X", mockSource);
        assertEquals("Xx-value1-xXx-value2-xXx-value3-xX", result);
    }

    /**
     * Test handling of a parameter which evaulates to a null value.
     *
     * @throws Exception
     */
    @Test
    public void testNullParameter() throws Exception {
        Mockito.when(this.mockSource.getParameter("x-param1-x")).thenReturn(null);

        String result = this.replacer.replaceObjectNameParameters("X${x-param1-x}X", mockSource);
        assertEquals("X${x-param1-x}X", result);
    }

    /**
     * Test a candidate string which contains zero replacement pattersn.
     *
     * @throws Exception
     */
    @Test
    public void testNoPatternMatch() throws Exception {
        String result = this.replacer.replaceObjectNameParameters("x-no-pattern-match-x", mockSource);

        assertEquals(result, "x-no-pattern-match-x");
        Mockito.verifyNoMoreInteractions(mockSource);
    }
}
