package org.ct42.sun2drive.wallbox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class WallbePoller extends AbstractVerticle {
    private static final long DEFAULT_POLL_DELAY = 20;
    public static final String SUN2DRIVE_EVENT_ADDRES = "org.ct42.sun2drive";
    public static final String DEFAULT_ID = "wallbe";
    public  static final String STATUS_CONNECTED = "connected";
    private static final String STATUS_CONNECTION_ERROR = "connection_failure";
    private static final String STATUS_COMMUNICATION_ERROR = "communication_error";
    public static final String STATUS_VEHICLE_STATUS = "vehicle_status";

    private static final Logger LOG = LoggerFactory.getLogger(WallbePoller.class.getName());

    private boolean state_connected = false;
    private VEHICLE_STATUS state_vehicle_status = null;

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
        action = id -> {
            poll();
            vertx.setTimer(delay, action);
        };
        vertx.setTimer(delay, action);
    }

    private void poll() {
        if(!state_connected) {
            try {
                wallbe.verifiedConnect();
                state_connected = true;
                vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRES, DEFAULT_ID + ":" + STATUS_CONNECTED);
            } catch (Exception e) {
                state_connected = false;
                vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRES, DEFAULT_ID + ":" + STATUS_CONNECTION_ERROR + ":" + e.getMessage());
            }
        } else { //connected - gathering information
            try {
                VEHICLE_STATUS status = wallbe.getStatus();
                if(!status.equals(state_vehicle_status)) {
                    state_vehicle_status = status;
                    vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRES, DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + status.name());
                }
            } catch(CommunicationException e) {
                vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRES, DEFAULT_ID + ":" + STATUS_COMMUNICATION_ERROR + ":" + e.getMessage());
            }
        }
    }
}
