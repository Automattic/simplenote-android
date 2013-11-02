package com.automattic.simplenote;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

    /**
     * This test should fail the build
     * 
     * Unfortunately the emulator doesn't run very well
     */
    public void testThatFails() throws Exception {
        fail("Ha! It Failed!");
    }

}
