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

package com.amlinv.jmxutil.connection.impl;

import com.sun.tools.attach.AttachNotSupportedException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the DefaultVirtualMachineAttacher: this test is intended to show line coverage and is not intended as a true
 * functional test since the implementation is a simple pass-through to a static method call.
 *
 * Created by art on 8/18/15.
 */
public class DefaultVirtualMachineAttacherTest {

    @Test(expected = AttachNotSupportedException.class)
    public void testAttach() throws Exception {
        DefaultVirtualMachineAttacher attacher = new DefaultVirtualMachineAttacher();

        attacher.attach("x-no-such-thing-x");
    }
}