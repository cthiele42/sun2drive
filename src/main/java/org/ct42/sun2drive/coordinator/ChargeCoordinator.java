package org.ct42.sun2drive.coordinator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.ct42.sun2drive.solaredge.SolarEdgePoller;
import org.ct42.sun2drive.vehicle.nissanconnect.NissanConnectPoller;
import org.ct42.sun2drive.wallbox.WallbePoller;

import static org.ct42.sun2drive.wallbox.WallbePoller.*;

public class ChargeCoordinator extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ChargeCoordinator.class.getName());
    private static final double THRESHOLD = .2;
    private static final double MIN_OVERHANG = 1.380;
    private static final double RATE_FACTOR = 23;
    private static final int MAX_CHARGE_RATE = 200;
    private static final double CUTOVER_LIMIT = 5.0;
    private static final double CUTOVER_USE = 3.9;
    private static final String DEFAULT_ID = "coordinator";
    private static final String CONSUME_CUTOVER = "consumecutover";
    private static final String MAX_SOC = "maxsoc";
    private static final int MIN_SOC = 25;

    private boolean isCharging = false;
    private int activeChargerate = 0;
    private boolean consumeCutOver = true;
    private int maxSoc = 100;
    private int currentSoc = 0;

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
                        double overhang = pvPower - THRESHOLD - (homePower - ((((double)activeChargerate) * RATE_FACTOR) / 1000));
                        if(consumeCutOver) {
                            double sureplus = pvPower - THRESHOLD - homePower;
                            if((sureplus > CUTOVER_LIMIT) && !maxSocReached()) {
                                int chargeRate = (int)(CUTOVER_USE * 1000 / RATE_FACTOR);
                                LOG.info("Using cutover, setting chargerate to " + chargeRate);
                                vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "setChargeCurrent100mA:" + chargeRate);
                                activeChargerate = chargeRate;
                                if(!isCharging) {
                                    isCharging = true;
                                    vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "startCharging");
                                    LOG.info("Started charging...");
                                }

                            } else {
                                if(maxSocReached()) {
                                    LOG.info("Max Soc of " + maxSoc + "% reached");
                                } else {
                                    LOG.info("No cutover (" + (sureplus - CUTOVER_LIMIT) + "kW)");
                                }
                                isCharging = false;
                                vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "stopCharging");
                                activeChargerate = 0;
                                LOG.info("Stopped charging...");
                            }
                        } else {
                            if(overhang > MIN_OVERHANG && !maxSocReached()) {
                                int chargeRate = Math.min((int)((overhang * 1000) / RATE_FACTOR), MAX_CHARGE_RATE);
                                LOG.info("Setting charge rate to " + chargeRate);
                                vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "setChargeCurrent100mA:" + chargeRate);
                                activeChargerate = chargeRate;
                                if(!isCharging) {
                                    isCharging = true;
                                    vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "startCharging");
                                    LOG.info("Started charging...");
                                }
                            } else {
                                if(maxSocReached()) {
                                    LOG.info("Max Soc of " + maxSoc + "% reached");
                                } else {
                                    LOG.info("Not enough overhang power (" + overhang + "kW)");
                                }
                                isCharging = false;
                                vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "stopCharging");
                                activeChargerate = 0;
                                LOG.info("Stopped charging...");
                            }
                        }
                    } catch(NumberFormatException e) {
                        LOG.error("Received solaredge message of wrong format", e);
                    }
                } else {
                    LOG.warn("Received solaredge message of wrong format");
                }
            } else if(msg.startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_CHARGE_RATE_WATT)) {
                String[] values = msg.split(":");
                if(values.length == 3) {
                    try {
                        double chargeWatt = Double.valueOf(values[2]);
                        activeChargerate = (int)(chargeWatt / RATE_FACTOR);
                        LOG.info("Set active charge rate to " + activeChargerate);
                    } catch(NumberFormatException e) {
                        LOG.error("Received wallbe message of wrong format", e);
                    }
                } else {
                    LOG.warn("Received wallbe message of wrong format");
                }
            } else if(msg.startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_CONNECTED) ||
                    msg.startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_NOT_CONNECTED) ||
                    msg.startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_CONNECTION_ERROR)) { //stop charging on wallbox if vehicle is disconnected or stopped charging
                LOG.info("Vehicle stopped charging");
                isCharging = false;
                vertx.eventBus().publish(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS, "stopCharging");
                activeChargerate = 0;
                LOG.info("Stopped charging...");
            } else if(msg.startsWith(NissanConnectPoller.DEFAULT_ID)) {
                String[] values = msg.split(":");
                if(values.length == 4) {
                    try {
                        currentSoc = Integer.valueOf(values[3]);
                        LOG.info("Set current Soc to " + currentSoc + "%");
                    } catch(NumberFormatException e) {
                        LOG.error("Received leaf message of wrong format", e);
                    }
                } else {
                    LOG.error("Received leaf message of wrong format");
                }
            }
        });

        vertx.eventBus().consumer(SUN2DRIVE_COMMANDS_ADDRESS).handler(message -> {
            if (message.body().toString().startsWith("switchCutoverUse")) {
                String[] values = message.body().toString().split(":");
                if (values.length == 2) {
                    boolean newConsumeCutOver = Boolean.valueOf(values[1]);
                    if (consumeCutOver != newConsumeCutOver) {
                        consumeCutOver = newConsumeCutOver;
                        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + CONSUME_CUTOVER + ":" + Boolean.toString(consumeCutOver));
                        LOG.info("Switched consumeCutover to " + Boolean.toString(consumeCutOver));
                    }
                } else {
                    LOG.warn("Received command message of wrong format");
                }
            } else if(message.body().toString().startsWith("setMaxSoc")) {
                String[] values = message.body().toString().split(":");
                if (values.length == 2) {
                    try {
                        int soc = Integer.valueOf(values[1]);
                        if (soc != maxSoc && soc >= MIN_SOC && soc <= 100) {
                            maxSoc = soc;
                            vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + MAX_SOC + ":" + maxSoc);
                            LOG.info("Set maxSoc to " + maxSoc);
                        }
                    } catch(NumberFormatException e) {
                        LOG.error("Received setSoc command of wrong format", e);
                    }
                } else {
                    LOG.warn("Received command message of wrong format");
                }
            }
        });
        vertx.eventBus().consumer(SUN2DRIVE_COMMANDS_ADDRESS).handler(message -> {
            if("getStatus".equals(message.body())) {
                vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + CONSUME_CUTOVER + ":" + Boolean.toString(consumeCutOver));
                vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + MAX_SOC + ":" + maxSoc);
            }
        });
    }

    private boolean maxSocReached() {
        return currentSoc < maxSoc;
    }
}
