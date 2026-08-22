package com.prismspace.container.utils.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PuildCompatTest {

    @Test
    public void android13Gate_isApi33() {
        assertFalse(PuildCompat.isAtLeast(32, 0, 33));
        assertTrue(PuildCompat.isAtLeast(32, 1, 33));
        assertTrue(PuildCompat.isAtLeast(33, 0, 33));
    }

    @Test
    public void android14Gate_isApi34() {
        assertFalse(PuildCompat.isAtLeast(33, 0, 34));
        assertTrue(PuildCompat.isAtLeast(33, 1, 34));
        assertTrue(PuildCompat.isAtLeast(34, 0, 34));
    }

    @Test
    public void android15To17Gates_areMonotonic() {
        assertTrue(PuildCompat.isAtLeast(35, 0, 35));
        assertFalse(PuildCompat.isAtLeast(35, 0, 36));
        assertTrue(PuildCompat.isAtLeast(35, 1, 36));
        assertTrue(PuildCompat.isAtLeast(36, 0, 36));
        assertFalse(PuildCompat.isAtLeast(36, 0, 37));
        assertTrue(PuildCompat.isAtLeast(36, 1, 37));
        assertTrue(PuildCompat.isAtLeast(37, 0, 37));
    }

    @Test
    public void newerSdk_satisfiesOlderGate() {
        assertTrue(PuildCompat.isAtLeast(37, 0, 34));
        assertTrue(PuildCompat.isAtLeast(37, 0, 35));
        assertTrue(PuildCompat.isAtLeast(37, 0, 36));
    }
}
