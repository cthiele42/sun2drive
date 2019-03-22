package org.ct42.sun2drive.vehicle.nissanconnect;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.ct42.sun2drive.wallbox.VEHICLE_STATUS;
import org.ct42.sun2drive.wallbox.WallbePoller;

import java.util.concurrent.TimeUnit;

import static org.ct42.sun2drive.wallbox.WallbePoller.*;

public class NissanConnectPoller extends AbstractVerticle {
    private static final long DEFAULT_POLL_DELAY = 180;
    private static final String DEFAULT_ID = "leaf";

    private static final Logger LOG = LoggerFactory.getLogger(NissanConnectPoller.class.getName());

    private Handler<Long> action;
    private boolean wallBoxIsConnected = false;
    private NissanConnectController controller;

    @Override
    public void start() throws Exception {
        controller = new NissanConnectController(
            config().getString("nissanconnect.user"),
            config().getString("nissanconnect.password")
        );

        long delay = TimeUnit.SECONDS.toMillis(
                config().getLong("nissanconnect.poll.delay", DEFAULT_POLL_DELAY));
        LOG.debug("Using a poll delay of " + delay + " seconds");

        action = id -> {
            poll();
            vertx.setTimer(delay, action);
        };
        vertx.setTimer(delay, action);

        vertx.eventBus().consumer(SUN2DRIVE_EVENT_ADDRESS).handler(message -> {
            if((WallbePoller.DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + VEHICLE_STATUS.CONNECTED).equals(message.body()) ||
               (WallbePoller.DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + VEHICLE_STATUS.CHARGING).equals(message.body()) ||
               (WallbePoller.DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + VEHICLE_STATUS.CHARGING_VENTILATED).equals(message.body())) {
                wallBoxIsConnected = true;
                LOG.info("Vehicle connected to wallbox, enable charge state polling...");
            } else if((WallbePoller.DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":" + VEHICLE_STATUS.NOT_CONNECTED).equals(message.body()) ||
                      message.body().toString().startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_VEHICLE_STATUS + ":ERROR") ||
                      message.body().toString().startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_COMMUNICATION_ERROR) ||
                      message.body().toString().startsWith(WallbePoller.DEFAULT_ID + ":" + STATUS_CONNECTION_ERROR) ||
                      (WallbePoller.DEFAULT_ID + ":" + STATUS_NOT_CONNECTED).equals(message.body())) {
                wallBoxIsConnected = false;
                LOG.info("Vehicle disconnected from wallbox, disable charge state polling...");
            }
        });
    }

    private void poll() {
        if(wallBoxIsConnected) {
            vertx.executeBlocking(future -> {
                        try {

                            NissanConnectController.ChargeState chargeState = controller.getChargeState();
                            String message = DEFAULT_ID + ":" +
                                    chargeState.pluginState + ":" +
                                    chargeState.chargingStatus + ":" +
                                    chargeState.socPercent;
                            vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, message);
                            LOG.info("Charge state: " + message);
                        } catch (CommunicationException e) {
                            LOG.error("Getting charge status from nissan connect failed", e);
                        }
                        future.complete();
                    }, res -> {});
        }
    }
}
