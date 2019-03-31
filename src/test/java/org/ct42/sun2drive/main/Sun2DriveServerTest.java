package org.ct42.sun2drive.main;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class Sun2DriveServerTest {
    private Vertx vertx;
    private int port;

    @Before
    public void setup(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                );

        vertx.deployVerticle(Sun2DriveServer.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void teardown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void shouldProvideWebPage(TestContext context) throws Exception {
        final Async async = context.async();
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/").send(ar -> {
            if(ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                context.assertTrue(response.body().toString().contains("Sun2Drive"));
                async.complete();
            } else {
                context.fail(ar.cause().getMessage());
                async.complete();
            }
        });
    }
}
