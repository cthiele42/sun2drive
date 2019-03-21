package org.ct42.sun2drive.solaredge;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.ct42.sun2drive.wallbox.WallbePoller;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@RunWith(VertxUnitRunner.class)
public class SolarEdgePollerTest {
    private static final String TEST_SITEID = "1004242";
    private static final String TEST_ACCESSTOKEN = "08150815081508150815";

    private Vertx vertx;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicPort());

    @Before
    public void setup(TestContext context) throws Exception {
        stubFor(get(urlPathMatching("/site/" + TEST_SITEID + "/currentPowerFlow.*"))
                .withQueryParam("api_key", equalTo(TEST_ACCESSTOKEN))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"siteCurrentPowerFlow\": {\n" +
                                "    \"updateRefreshRate\": 3,\n" +
                                "    \"unit\": \"kW\",\n" +
                                "    \"connections\": [\n" +
                                "      {\n" +
                                "        \"from\": \"STORAGE\",\n" +
                                "        \"to\": \"Load\"\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"GRID\": {\n" +
                                "      \"status\": \"Active\",\n" +
                                "      \"currentPower\": 0.0\n" +
                                "    },\n" +
                                "    \"LOAD\": {\n" +
                                "      \"status\": \"Active\",\n" +
                                "      \"currentPower\": 0.28\n" +
                                "    },\n" +
                                "    \"PV\": {\n" +
                                "      \"status\": \"Idle\",\n" +
                                "      \"currentPower\": 0.0\n" +
                                "    },\n" +
                                "    \"STORAGE\": {\n" +
                                "      \"status\": \"Discharging\",\n" +
                                "      \"currentPower\": 0.28,\n" +
                                "      \"chargeLevel\": 84,\n" +
                                "      \"critical\": false\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")));

    }

    @After
    public void teardown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void shouldWork(TestContext context) throws Exception {
        final Async async = context.async();

        vertx = Vertx.vertx();

        vertx.eventBus().consumer(WallbePoller.SUN2DRIVE_EVENT_ADDRESS, message -> {
            context.assertEquals(SolarEdgePoller.DEFAULT_ID + ":0.28:0.0:0.28:84", message.body().toString());
            async.complete();
        });

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("solaredge.port", wireMockRule.port())
                        .put("siteid", TEST_SITEID)
                        .put("accesstoken", TEST_ACCESSTOKEN)
                );
        vertx.deployVerticle(SolarEdgePoller.class.getName(), options, context.asyncAssertSuccess());

        async.awaitSuccess();

        verify(getRequestedFor(urlPathMatching("/site/" + TEST_SITEID + "/currentPowerFlow.*")));
    }
}
