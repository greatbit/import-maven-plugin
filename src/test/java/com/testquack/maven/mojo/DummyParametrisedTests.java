package com.testquack.maven.mojo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;


@RunWith(Parameterized.class)
public class DummyParametrisedTests {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {0},
                {1},
                {3},
                {2}
        });
    }

    int intVal;

    public DummyParametrisedTests(int intVal) {
        this.intVal = intVal;
    }

    @Test
    public void parametrisedTest() {
        assertThat(intVal, lessThan(3));
    }
}
