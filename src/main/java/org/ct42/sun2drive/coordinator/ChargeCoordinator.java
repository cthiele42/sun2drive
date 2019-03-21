package org.ct42.sun2drive.coordinator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.ct42.sun2drive.solaredge.SolarEdgePoller;
import org.ct42.sun2drive.wallbox.WallbePoller;

import static org.ct42.sun2drive.wallbox.WallbePoller.SUN2DRIVE_EVENT_ADDRESS;

public class ChargeCoordinator extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ChargeCoordinator.class.getName());
    private static final double THRESHOLD = .2;
    private static final double MIN_OVERHANG = 1.380;
    private static final double RATE_FACTOR = 23;
    private static final int MAX_CHARGE_RATE = 200;

    private boolean isCharging = false;

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(SUN2DRIVE_EVENT_ADDRESS).handler(message -> {
            String msg = message.body().toString();
            if(msg.startsWith(SolarEdgePoller.DEFAULT_ID)) {
                String[] values = msg.split(":");
                if(values.length == 5) {
                    try {
                        double homePower = Double.valueOf(values[1]);
                        double pvPower = Double.valueOf(values[2]);
                        double overhang = pvPower - THRESHOLD - homePower;
                        if(overhang > MIN_OVERHANG) {
                            int chargeRate = Math.min((int)(overhang / RATE_FACTOR), MAX_CHARGE_RATE);
                            LOG.info("Setting charge rate to " + chargeRate);
                            vertx.eventBus().send(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "setChargeCurrent100mA:" + chargeRate);

                            if(!isCharging) {
                                isCharging = true;
                                vertx.eventBus().send(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "startCharging");
                                LOG.info("Started charging...");
                            }
                        } else {
                            LOG.info("Not enough overhang power (" + overhang + "kW)");
                            if(isCharging) {
                                isCharging = false;
                                vertx.eventBus().send(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "stopCharging");
                                LOG.info("Stopped charging...");
                            }
                        }

                    } catch(NumberFormatException e) {
                        LOG.error("Received solaredge message of wrong format", e);
                    }
                } else {
                    LOG.warn("Received solaredge message of wrong format");
                }
            }
        });
    }
}