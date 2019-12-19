// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EventEventRASTest {
    ForeignEventRAS ev;

    @Before
    public void SetUp() {
        ev = new ForeignEventRAS();
    }

    @Test
    public void EventCanConvertToJSON() {
        ev.setTimestamp(581_248_298);
        ev.setLocation("c0-0c2s2n3");
        ev.setEventType("ec_l0_mod_dwn_rsp");
        ev.setBootImageId("imageId_1");
        assertEquals("imageId_1", ev.getBootImageId());
        System.out.println(ev.getJSON());
    }

}
