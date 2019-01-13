package org.ct42.sun2drive.vehicle.nissanconnect;

import solutions.tveit.nissanconnect.NissanConnectAsyncService;
import solutions.tveit.nissanconnect.Region;
import solutions.tveit.nissanconnect.api.batterystatusrecords.BatteryStatusRecordsResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class NissanConnectController {
    private String user;
    private String password;

    public static class ChargeState {
        public String pluginState;
        public String chargingStatus;
        public String operationTime;
        public String socPercent;
        public String remainingWh;
    }

    public NissanConnectController(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public ChargeState getChargeState() throws CommunicationException {
        ChargeState chargeState = new ChargeState();

        NissanConnectAsyncService service = new NissanConnectAsyncService();
        login(service);

        Future<BatteryStatusRecordsResponse> batteryStatusRecords = service.getBatteryStatusRecords();
        BatteryStatusRecordsResponse batteryStatusRecordsResponse = null;
        try {
            batteryStatusRecordsResponse = batteryStatusRecords.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CommunicationException("Failed to get status from Nissan Connect Server", e);
        }
        chargeState.socPercent = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getSoc().getValue();
        chargeState.operationTime = batteryStatusRecordsResponse.getBatteryStatusRecords().getOperationDateAndTime();
        chargeState.chargingStatus = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getBatteryChargingStatus();
        chargeState.remainingWh = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getBatteryRemainingAmountWH();
        chargeState.pluginState = batteryStatusRecordsResponse.getBatteryStatusRecords().getPluginState();

        return chargeState;
    }

    private void login(NissanConnectAsyncService service) {
        String userId = System.getenv("NISSANCONNECT_USER");
        String password = System.getenv("NISSANCONNECT_PASSWORD");
        service.login(Region.EUROPE, userId, password);
    }
}
