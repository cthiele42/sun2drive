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

    private void getChargeState() throws CommunicationException {
        String userId = System.getenv("NISSANCONNECT_USER");
        String password = System.getenv("NISSANCONNECT_PASSWORD");
        NissanConnectController nissanConnectController = new NissanConnectController(userId, password);
        NissanConnectController.ChargeState chargeState = nissanConnectController.getChargeState();

        System.out.println(
                "pluginstate: " + chargeState.pluginState +
                "\nbatteryRemainingAmountWH: " + chargeState.remainingWh +
                "\nchargingstatus: " + chargeState.chargingStatus +
                "\noperationtime: " + chargeState.operationTime +
                "\nsoc: " + chargeState.socPercent +
                "\n");
    }
}
