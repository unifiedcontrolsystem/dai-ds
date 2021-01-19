package com.intel.dai.control;

import com.intel.logging.Logger;
import com.intel.dai.dsapi.WorkQueue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ControlApiTest {
    ControlApi underTest_;

    @Before
    public void setUp() throws Exception {
        underTest_ = new ControlApi("TEST", "TEST", mock(WorkQueue.class), mock(Logger.class));
    }

    @Test
    public void powerOnNode() {
        try {
            underTest_.powerOnNode(null, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void powerOffNode() {
        try {
            underTest_.powerOffNode(null, null, null, 0L, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void powerOffNode2() {
        try {
            underTest_.powerOffNode(null, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void shutdownNode() {
        try {
            underTest_.shutdownNode(null, null, null, 0L, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void shutdownNode2() {
        try {
            underTest_.shutdownNode(null, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void powerCycleNode() {
        try {
            underTest_.powerCycleNode(null, null, null, 0L, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void resetNodes() {
        try {
            underTest_.resetNodes(null, null, null, 0L, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }

    @Test
    public void increaseNodeFanSpeed() {
        try {
            underTest_.increaseNodeFanSpeed(null, null, null, 0L, null, null, null, 0L);
        } catch(InterruptedException | IOException e) {/**/}
    }
}
