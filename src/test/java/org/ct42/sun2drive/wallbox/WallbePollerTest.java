package org.ct42.sun2drive.wallbox;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class WallbePollerTest {
    public static final int PORT = 32000;
    public static final int UNIT_ID = 255;
    private static PhoenixContactChargeControllerMock chargeControllerMock;

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setup(TestContext context) throws Exception {
        chargeControllerMock = new PhoenixContactChargeControllerMock(PORT, UNIT_ID);
    }

    private void deploy(TestContext context) throws UnknownHostException {
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("wallbe.address", InetAddress.getLocalHost().getHostAddress())
                        .put("wallbe.port", PORT)
                        .put("wallbe.poll.delay", 1)
                );

        rule.vertx().deployVerticle(WallbePoller.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void teardown(TestContext context) {
        chargeControllerMock.shutdown();
    }

    @Test(timeout = 5000)
    public void shouldSendMessageOverEventBus(TestContext context) throws Exception {
        ArrayList<String> awaitedMessages = new ArrayList<>(2);
        awaitedMessages.add(WallbePoller.DEFAULT_ID + ":" + WallbePoller.STATUS_CONNECTED);
        awaitedMessages.add(WallbePoller.DEFAULT_ID + ":" + WallbePoller.STATUS_VEHICLE_STATUS + ":NOT_CONNECTED");
        awaitedMessages.add(WallbePoller.DEFAULT_ID + ":" + WallbePoller.STATUS_VEHICLE_STATUS + ":CONNECTED");

        AtomicInteger currentmessage = new AtomicInteger(0);
        Async async = context.async(awaitedMessages.size());

        rule.vertx().eventBus().consumer(WallbePoller.SUN2DRIVE_EVENT_ADDRESS, message -> {
            context.assertEquals(awaitedMessages.get(currentmessage.getAndIncrement()), message.body().toString());
            if(currentmessage.get() == 2) {
                chargeControllerMock.setStatus(VEHICLE_STATUS.CONNECTED.getCode());
            }
            async.countDown();
        });

        deploy(context);
    }
}
