package com.testquack.maven.mojo;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DummyTests {

    @Test
    public void dummyTestPassed(){}

    @Test
    public void dummyTestPassed2(){}

    @Test
    public void dummyTestBroken(){
        throw new RuntimeException();
    }

    @Test
    public void dummyTestFailed(){
        assertFalse(true);
    }

    @Test
    @Ignore
    public void dummyTestSkipped(){
        assertFalse(true);
    }
}
