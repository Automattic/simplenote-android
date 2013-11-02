package com.automattic.simplenote;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

    public void testThatFails() throws Exception {
        fail("Ha! It Failed!");
    }

}