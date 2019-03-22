package org.ct42.sun2drive.solaredge;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.ct42.sun2drive.wallbox.VEHICLE_STATUS;
import org.ct42.sun2drive.wallbox.WallbePoller;

import java.util.concurrent.TimeUnit;

import static org.ct42.sun2drive.wallbox.WallbePoller.*;

public class SolarEdgePoller extends AbstractVerticle {
    public static final String DEFAULT_ID = "solaredge";
    private static final long DEFAULT_POLL_DELAY = 300;
    private static final Logger LOG = LoggerFactory.getLogger(SolarEdgePoller.class.getName());

    private WebClient webClient;
    private String url;

    private Handler<Long> action;
    private boolean wallBoxIsConnected = false;

    @Override
    public void start() throws Exception {
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost(config().getString("solaredge.host", "localhost"))
                .setDefaultPort(config().getInteger("solaredge.port", 80))
                .setSsl(config().getBoolean("solaredge.usessl", false));
        webClient = WebClient.create(vertx, options);
        url = "/site/" + config().getString("siteid") + "/currentPowerFlow";

        long delay = TimeUnit.SECONDS.toMillis(
                config().getLong("solaredge.poll.delay", DEFAULT_POLL_DELAY));
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
            webClient.get(url)
                    .setQueryParam("api_key", config().getString("accesstoken"))
                    .putHeader("Accept", "application/json")
                    .send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> response = ar.result();

                            if (response.statusCode() == 200 && response.getHeader("content-type").startsWith("application/json")) {
                                JsonObject body = response.bodyAsJsonObject();
                                JsonObject siteCurrentPowerFlow = body.getJsonObject("siteCurrentPowerFlow");
                                Double homePower = null;
                                Double pvPower = null;
                                Double storagePower = null;
                                Integer chargeLevel = null;

                                if (siteCurrentPowerFlow != null) {
                                    JsonObject home = siteCurrentPowerFlow.getJsonObject("LOAD");
                                    if (home != null) {
                                        homePower = home.getDouble("currentPower");
                                    }
                                    JsonObject pv = siteCurrentPowerFlow.getJsonObject("PV");
                                    if (pv != null) {
                                        pvPower = pv.getDouble("currentPower");
                                    }
                                    JsonObject storage = siteCurrentPowerFlow.getJsonObject("STORAGE");
                                    if (storage != null) {
                                        storagePower = storage.getDouble("currentPower");
                                        chargeLevel = storage.getInteger("chargeLevel");
                                    }
                                    if (homePower != null && pvPower != null && storagePower != null && chargeLevel != null) {
                                        LOG.info("Recieved power state. homePower: " +  homePower + ", pvPower: " + pvPower + ", storagePower: " + storagePower + ", chargeLevel: " +chargeLevel);
                                        vertx.eventBus().publish(SUN2DRIVE_EVENT_ADDRESS, DEFAULT_ID + ":" + homePower + ":" + pvPower + ":" + storagePower + ":" + chargeLevel);
                                        return;
                                    }
                                }
                                LOG.error("Unparseable powerflow object");

                            } else {
                                LOG.error("Error getting SolarEdge power flow, statuscode: " + response.statusCode());
                            }
                        } else {
                            LOG.error("Error getting SolarEdge power flow: ", ar.cause());
                        }
                    });
        }
    }
}
