package com.automattic.simplenote;

import junit.framework.TestCase;

public class FailingTest extends TestCase {

    public testThatFails() throws Exception {
        fail("Ha! It Failed!");
    }

}