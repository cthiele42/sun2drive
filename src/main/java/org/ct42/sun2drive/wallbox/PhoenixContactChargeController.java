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

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Adapting the charge controller from Phoenix Contact (https://www.phoenixcontact.com/online/portal/us/?uri=pxc-oc-itemdetail:pid=1018701&library=usen&pcck=P-29-04-02-01&tab=1&selectedCategory=ALL)
 * using ModbusTCP
 */
public class PhoenixContactChargeController {
    public static final int STATUS_ADDRESS = 100;
    public static final int START_STOP_CHARGING_ADDRESS = 400;

    private InetAddress address;
    private int unitID;
    private int port;
    private TCPMasterConnection connection;

    public PhoenixContactChargeController(InetAddress address, int unitID, int port) throws UnknownHostException {
        this.address = address;
        this.port = port;
        this.unitID = unitID;
    }

    public PhoenixContactChargeController(InetAddress address, int unitID) throws UnknownHostException {
        this(address, unitID, Modbus.DEFAULT_PORT);
    }

    public void verifiedConnect() throws Exception {
        connection = new TCPMasterConnection(address);
        connection.setPort(port);
        connection.connect();
        try {
            VEHICLE_STATUS status = getStatus();
        } catch(IllegalStateException e) {
            throw new IllegalStateException("Successfully connected to device, but seems to be an unsupported type, returned an unknown status code: " + e.getMessage());
        }
    }

    public VEHICLE_STATUS getStatus() throws CommunicationException {
        ReadInputRegistersRequest readStatus = new ReadInputRegistersRequest(STATUS_ADDRESS, 1);
        readStatus.setUnitID(unitID);
        ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
        tx.setRequest(readStatus);
        try {
            tx.execute();
        } catch (ModbusException e) {
            throw new CommunicationException("Failed to get controller status", e);
        }
        ReadInputRegistersResponse statusResponse = (ReadInputRegistersResponse) tx.getResponse();
        ;
        int statusCode = statusResponse.getRegister(0).getValue();
        VEHICLE_STATUS status = VEHICLE_STATUS.getStatusForCode(statusCode);
        if(status == null) {
            throw new IllegalStateException("Unknown status code " + statusCode);
        }
        return status;
    }

    /**
     * Start charging if vehicle is connected and ready for charge
     *
     * @return ture if charging was really started, false otherwise
     * @throws CommunicationException
     */
    public boolean startCharging()  throws CommunicationException {
        if(VEHICLE_STATUS.CONNECTED.equals(getStatus())) { //Vehicle is connected and ready for charge
            WriteCoilRequest enableCharging = new WriteCoilRequest(START_STOP_CHARGING_ADDRESS, true);
            enableCharging.setUnitID(unitID);
            ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
            tx.setRequest(enableCharging);
            try {
                tx.execute();
            } catch (ModbusException e) {
                throw new CommunicationException("Failed to start charging", e);
            }
            return true;
        }
        return false;
    }

    /**
     * Stop charging
     *
     * @throws CommunicationException
     */
    public void stopCharging() throws CommunicationException {
        WriteCoilRequest disableCharging = new WriteCoilRequest(START_STOP_CHARGING_ADDRESS, false);
        disableCharging.setUnitID(unitID);
        ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
        tx.setRequest(disableCharging);
        try {
            tx.execute();
        } catch (ModbusException e) {
            throw new CommunicationException("Failed to stop charging", e);
        }
    }

    public void disconnect() {
        if(connection != null) {
            connection.close();
            connection = null;
        }
    }
}