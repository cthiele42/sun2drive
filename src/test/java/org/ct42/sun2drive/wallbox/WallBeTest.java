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
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetAddress;

public class WallBeTest {
    private static final String WALLBE_ADDRESS = "wallbe";
    private Wallbe wallbe;

    @Before
    public void setup() throws Exception {
        wallbe = new Wallbe(InetAddress.getByName(WALLBE_ADDRESS));
        wallbe.verifiedConnect();
    }

    @After
    public void teardown() {
        wallbe.disconnect();
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldGetStatus() throws Exception {
        System.out.println("Wallbe status: " + wallbe.getStatus());
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldStartCharging() throws Exception {
        wallbe.startCharging();
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldStopCharging() throws Exception {
        wallbe.stopCharging();
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldGetChargingRatePreset() throws Exception {
        System.out.println("Wallbe charging rate preset: " + (double)wallbe.getChargeCurrentPreset() / 10 + " Ampere");
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldGetChargeCurrentPWM() throws Exception {
        System.out.println("Wallbe charge current PWM: " + (double)wallbe.getChargeCurrentPWM() / 10  + " Ampere");
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldSetChargeCurrent() throws Exception {
        wallbe.setChargeCurrent(200);
        System.out.println("Wallbe charge current PWM: " + (double)wallbe.getChargeCurrentPWM() / 10 + " Ampere");
    }

    @Test
    @Ignore("For manual testing only. Change WALLBE_ADDRESS to your needs")
    public void shouldGetChargingTime() throws Exception {
        System.out.println("Wallbe charging time: " + wallbe.getChargingTimeSeconds() + " Seconds");
    }
}
