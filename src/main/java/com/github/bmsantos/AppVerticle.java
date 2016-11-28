package com.github.bmsantos;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import static io.vertx.core.logging.LoggerFactory.getLogger;

public class AppVerticle extends AbstractVerticle {

  private static final Logger log = getLogger(AppVerticle.class);

  @Override
  public void start() throws Exception {
    final Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    final BridgeOptions options = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("to_reply"))
      .addInboundPermitted(new PermittedOptions().setAddress("to_send"))
      .addInboundPermitted(new PermittedOptions().setAddress("to_publish"))
      .addOutboundPermitted(new PermittedOptions().setAddress("to_client"));

    final SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(options);
    router.route("/eventbus/*").handler(ebHandler);

    vertx.eventBus().consumer("to_send").handler(msg -> {
      log.info("Received Request to Send: " + msg.body());
      vertx.eventBus().send("to_client", "Send Requested");
    });

    vertx.eventBus().consumer("to_publish").handler(msg -> {
      log.info("Received Request to Publish: " + msg.body());
      vertx.eventBus().publish("to_client", "Publish Requested");
    });

    vertx.eventBus().consumer("to_reply").handler(msg -> {
      log.info("Received Request to Reply: " + msg.body());
      msg.reply("Reply Requested");
    });

    router.route().handler(StaticHandler.create());

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }
}