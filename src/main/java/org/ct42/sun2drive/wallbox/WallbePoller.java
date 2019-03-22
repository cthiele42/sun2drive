package org.ct42.sun2drive.wallbox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class WallbePoller extends AbstractVerticle {
    private static final long DEFAULT_POLL_DELAY = 20;
    public static final String SUN2DRIVE_EVENT_ADDRESS = "org.ct42.sun2drive";
    public static final String SUN2DRIVE_COMMANDS_ADDRESS = "org.ct42.sun2drive.commands";
    public static final String DEFAULT_ID = "wallbe";
    public  static final String STATUS_CONNECTED = "connected";
    public  static final String STATUS_NOT_CONNECTED = "not_connected";
    public static final String STATUS_CONNECTION_ERROR = "connection_failure";
    public static final String STATUS_COMMUNICATION_ERROR = "communication_error";
    public static final String STATUS_VEHICLE_STATUS = "vehicle_status";
    public static final String STATUS_CHARGE_RATE_WATT = "charge_rate_watt";
    public static final String STATUS_CHARGING_TIME_SECONDS = "charging_time_seconds";

    public static final double CHARGE_VOLT = 230;

    private static final Logger LOG = LoggerFactory.getLogger(WallbePoller.class.getName());

    private boolean state_connected = false;
    private VEHICLE_STATUS state_vehicle_status = null;
    private int chargeCurrent100mA = -1;
    private long chargingTimeSeconds = -1;

    private Handler<Long> action;
    private Wallbe wallbe;

    @Override
    public void start() throws Exception {
        wallbe = new Wallbe(InetAddress.getByName(
                config().getString("wallbe.address", "wallbe")),
                config().getInteger("wallbe.port", Wallbe.WALLBE_DEFAULT_PORT)
        );

        long delay = TimeUnit.SECONDS.toMillis(
                config().getLong("wallbe.poll.delay", DEFAULT_POLL_DELAY));
        LOG.debug("Using a poll delay of " + delay + " seconds");
        action = id -> {
            poll();
            vertx.setTimer(delay, action);
        };
        vertx.setTimer(delay, action);

        vertx.eventBus().consumer(SUN2DRIVE_COMMANDS_ADDRESS).handler(message -> {
            try {
                if ("getStatus".equals(message.body())) {
                    LOG.debug("Received getStatus");
                    if (state_connected) {
                        publishConnected();
                        if (state_vehicle_status != null) {
                            publishVehicleStatus();
                            if (vehicleIsCharging()) {
                                publishChargeRate();
                                publishChargingTime();
                            }
                        }
                    } else {
                        publishNotConnected();
                    }

                } else if ("startCharging".equals(message.body())) {
                    LOG.debug("Received startCharging");
                    if (VEHICLE_STATUS.CONNECTED.equals(state_vehicle_status)) {
                        wallbe.startCharging();
                    }
                } else if ("stopCharging".equals(message.body())) {
                    LOG.debug("Received stopCharging");
                    if (vehicleIsCharging()) {
                        wallbe.stopCharging();
                    }

                } else if(message.body().toString().startsWith("setChargeCurrent100mA")) {
                    String[] msgParts = message.body().toString().split(":");
                    if(msgParts.length == 2) {
                        LOG.debug("Received setChargeCurrent " + msgParts[1]);
                        try {
                            wallbe.setChargeCurrent(Integer.valueOf(msgParts[1]));
                        } catch(NumberFormatException e) {
                            LOG.info("Received unparsaeble message (wrong number format): " + message.body());
                        }
                    } else {
                        LOG.info("Received unparsaeble message: " + message.body());
                    }
                }
            } catch(CommunicationException e) {
                LOG.error("Communication error", e);
                publishCommunicationError(e.getMessage());
                state_vehicle_status = null;
            }
        });
    }

    private void poll() {
        if(!state_connected) {
            try {
                wallbe.verifiedConnect();
                state_connected = true;
                LOG.info("Successfully connected");
                publishConnected();
            } catch (Exception e) {
                LOG.error("Failed to connect", e);
                state_connected = false;
                publishConnectionError(e.getMessage());
            }
        } else { //connected - gathering information
            try {
                VEHICLE_STATUS status = wallbe.getStatus();
                if(!status.equals(state_vehicle_status)) {
                    LOG.info("Vehicle status change detected. Going from " + (state_vehicle_status == null ? "UNKNOWN" : state_vehicle_status.name()) + " to " + status.name());
                    state_vehicle_status = status;
                    publishVehicleStatus();
                }
                if(vehicleIsCharging()) {
                    int wallbeChargeCurrentPWM = wallbe.getChargeCurrentPWM();
                    if(wallbeChargeCurrentPWM != chargeCurrent100mA) {
                        chargeCurrent100mA = wallbeChargeCurrentPWM;
                        publishChargeRate();
                    }
                    long wallbeChargingTimeSeconds = wallbe.getChargingTimeSeconds();
                    if(wallbeChargingTimeSeconds != chargingTimeSeconds) {
                        chargingTimeSeconds = wallbeChargingTimeSeconds;
                        publishChargingTime();
                    }
                }
            } catch(CommunicationException e) {
                LOG.error("Communication error", e);
                publishCommunicationError(e.getMessage());
                state_vehicle_status = null;
            }
        }
    }

    private void publishCommunicationError(String message) {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_COMMUNICATION_ERROR + ":" + message);
    }

    private boolean vehicleIsCharging() {
        return VEHICLE_STATUS.CHARGING.equals(state_vehicle_status) || VEHICLE_STATUS.CHARGING_VENTILATED.equals(state_vehicle_status);
    }

    private void publishChargingTime() {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_CHARGING_TIME_SECONDS + ":" + chargingTimeSeconds);
    }

    private void publishChargeRate() {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_CHARGE_RATE_WATT + ":" + chargeCurrent100mA * CHARGE_VOLT / 10L);
    }

    private void publishConnectionError(String message) {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_CONNECTION_ERROR + ":" + message);
    }

    private void publishVehicleStatus() {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + state_vehicle_status.name());
    }

    private void publishNotConnected() {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_NOT_CONNECTED);
    }

    private void publishConnected() {
        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + STATUS_CONNECTED);
    }
}
