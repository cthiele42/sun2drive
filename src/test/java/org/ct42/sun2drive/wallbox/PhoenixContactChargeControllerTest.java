/*
 * Copyright 2018 Claas Thiele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.ct42.sun2drive.wallbox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;

public class PhoenixContactChargeControllerTest {
    public static final int PORT = 32000;
    public static final int UNIT_ID = 255;
    private PhoenixContactChargeControllerMock chargeControllerMock;

    @Before
    public void setup() throws Exception {
        chargeControllerMock = new PhoenixContactChargeControllerMock(PORT, UNIT_ID);
    }

    @After
    public void teardown() {
        chargeControllerMock.shutdown();
    }

    @Test
    public void shouldGetStatusAByDefault() throws Exception {
        PhoenixContactChargeController phoenixContactChargeController = new PhoenixContactChargeController(InetAddress.getLocalHost(), UNIT_ID, PORT);
        phoenixContactChargeController.verifiedConnect();
        assertEquals(VEHICLE_STATUS.NOT_CONNECTED, phoenixContactChargeController.getStatus());
        phoenixContactChargeController.disconnect();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailForUnknownStatus() throws Exception {
        chargeControllerMock.setStatus((int)'G'); // invalid status code
        PhoenixContactChargeController phoenixContactChargeController = new PhoenixContactChargeController(InetAddress.getLocalHost(), UNIT_ID, PORT);
        phoenixContactChargeController.verifiedConnect();
        assertEquals(VEHICLE_STATUS.NOT_CONNECTED, phoenixContactChargeController.getStatus());
        phoenixContactChargeController.disconnect();
    }

    @Test
    public void shouldSetStartStopChargingCoilForConnectedVehicle() throws Exception {
        PhoenixContactChargeController phoenixContactChargeController = new PhoenixContactChargeController(InetAddress.getLocalHost(), UNIT_ID, PORT);
        phoenixContactChargeController.verifiedConnect();
        chargeControllerMock.setStatus(VEHICLE_STATUS.CONNECTED.getCode());
        phoenixContactChargeController.startCharging();
        assertTrue(chargeControllerMock.getChargeState());
        phoenixContactChargeController.disconnect();
    }
}
