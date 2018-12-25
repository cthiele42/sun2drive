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

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.net.ModbusTCPListener;
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalOut;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

public class PhoenixContactChargeControllerMock {

    public static final int DEFAULT_INIT_DELAY = 100;
    private final SimpleProcessImage simpleProcessImage;

    public PhoenixContactChargeControllerMock(int port, int unitId, int initDelay) throws ModbusException {
        simpleProcessImage = new SimpleProcessImage();

        addDummyInputRegisters(100);
        simpleProcessImage.addInputRegister(new SimpleInputRegister((int)'A'));
        simpleProcessImage.addDigitalOut(PhoenixContactChargeController.START_STOP_CHARGING_ADDRESS, new SimpleDigitalOut(false));

        ModbusSlave slave = ModbusSlaveFactory.createTCPSlave(port, 3);
        slave.addProcessImage(unitId, simpleProcessImage);
        slave.open();
    }

    public PhoenixContactChargeControllerMock(int port, int unitId) throws ModbusException {
        this(port, unitId, DEFAULT_INIT_DELAY);
    }

    public void setStatus(int statusCode) {
        simpleProcessImage.setInputRegister(100, new SimpleInputRegister(statusCode));
    }

    public boolean getChargeState() {
        return simpleProcessImage.getDigitalOut(PhoenixContactChargeController.START_STOP_CHARGING_ADDRESS).isSet();
    }

    public void shutdown() {
        ModbusSlaveFactory.close();
    }

    private void addDummyInputRegisters(int count) {
        for(int i = 0; i < count; i++) {
            simpleProcessImage.addInputRegister(new SimpleInputRegister());
        }
    }
}
