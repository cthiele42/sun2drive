package org.ct42.sun2drive.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.ct42.sun2drive.solaredge.SolarEdgePoller;
import org.ct42.sun2drive.vehicle.nissanconnect.NissanConnectPoller;
import org.ct42.sun2drive.wallbox.WallbePoller;

public class Sun2DriveServer extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);

        BridgeOptions opts = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(WallbePoller.SUN2DRIVE_EVENT_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddress(WallbePoller.SUN2DRIVE_EVENT_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddress(WallbePoller.SUN2DRIVE_COMMANDS_ADDRESS));

        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
        router.route("/eventbus/*").handler(ebHandler);

        router.route().handler(StaticHandler.create());
        vertx
            .createHttpServer()
            .requestHandler(router).listen(config().getInteger("http.port", 80),
                result -> {
            if(result.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config());
        vertx.deployVerticle(WallbePoller.class.getName(), deploymentOptions);
        vertx.deployVerticle(NissanConnectPoller.class.getName(), deploymentOptions);
        vertx.deployVerticle(SolarEdgePoller.class.getName(), deploymentOptions);
    }
}
