package org.ct42.sun2drive.vehicle.nissanconnect;

import org.junit.Ignore;
import org.junit.Test;
import solutions.tveit.nissanconnect.NissanConnectAsyncService;
import solutions.tveit.nissanconnect.Region;
import solutions.tveit.nissanconnect.api.batterystatusrecords.BatteryStatusRecordsResponse;

import java.util.Date;
import java.util.concurrent.Future;

public class NissanConnectTest {
    @Test
    @Ignore("For manual tests only. Set system properties nissanconnect.user and nissanconnect.password .")
    public void shoudlGetSOC() throws Exception {
        while(true) {
            System.out.println(new Date());
            getChargeState();
            Thread.sleep(180000);
        }

    }

    private void getChargeState() throws InterruptedException, java.util.concurrent.ExecutionException {
        NissanConnectAsyncService service = new NissanConnectAsyncService();
        // Log in
        String userId = System.getenv("NISSANCONNECT_USER");
        String password = System.getenv("NISSANCONNECT_PASSWORD");
        service.login(Region.EUROPE, userId, password);

        // Get state of charge (SOC) from server
        Future<BatteryStatusRecordsResponse> batteryStatusRecords = service.getBatteryStatusRecords();
        BatteryStatusRecordsResponse batteryStatusRecordsResponse = batteryStatusRecords.get();
        String soc = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getSoc().getValue();
        String operationDateAndTime = batteryStatusRecordsResponse.getBatteryStatusRecords().getOperationDateAndTime();
        String batteryChargingStatus = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getBatteryChargingStatus();
        String batteryRemainingAmountWH = batteryStatusRecordsResponse.getBatteryStatusRecords().getBatteryStatus().getBatteryRemainingAmountWH();
        String pluginState = batteryStatusRecordsResponse.getBatteryStatusRecords().getPluginState();

        System.out.println(
                "pluginstate: " + pluginState +
                "\nbatteryRemainingAmountWH: " + batteryRemainingAmountWH +
                "\nchargingstatus: " + batteryChargingStatus +
                "\noperationtime: " + operationDateAndTime +
                "\nsoc: " + soc +
                "\n");
    }
}
