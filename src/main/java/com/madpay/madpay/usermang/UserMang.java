package com.madpay.madpay.usermang;


import com.madpay.madpay.MainVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMang extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(UserMang.class);
  private final Router api;
private final PgPool db;
  public UserMang(Router api,final PgPool db) {
    this.db = db;
    this.api = api;
  }

  @Override
  public void start() {
  vertx.deployVerticle(new Login(db));
  vertx.deployVerticle(new Signup(db));

  api.post("/login").handler(this::loginHandler);
  api.post("/signup").handler(this::signup);

  }

  private void signup(RoutingContext context) {
    doReq(context, "Signup");
  }

  private void loginHandler(RoutingContext context) {
    doReq(context, "Login.vertx.addr");
  }


  private void doReq(RoutingContext context , String address) {
    JsonObject message = context.getBodyAsJson();
    vertx.eventBus().request(address, message, reply -> {
      JsonObject res = new JsonObject((String) reply.result().body());
      int statusCode = Integer.parseInt(res.getString("statusCode"));
      context.request().response()
        .setStatusCode(statusCode)
        .putHeader("Content-Type", "application/json; charset=utf-8")
        .end((String) reply.result().body());
    });
  }
}
