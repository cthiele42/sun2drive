package org.ct42.sun2drive.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class Sun2DriveServer extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
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
    }
}
