package com.madpay.madpay.usermang;

import com.madpay.madpay.AuthSystem.AuthSystem;
import com.madpay.madpay.helper.HashingPassword;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Login extends AbstractVerticle {
private final PgPool db;
  private static final Logger LOG = LoggerFactory.getLogger(Login.class);

  public Login(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    EventBus eventBus = vertx.eventBus();

    eventBus.consumer("Login.vertx.addr", handler -> {
      vertx.executeBlocking(promise -> {
        applyLogin(handler, promise);
      }, res -> {
        handler.reply(res.result().toString());
      });
    });
    super.start(startPromise);
  }

  private void applyLogin(Message<Object> handler, Promise<Object> promise) {
    JsonObject response = new JsonObject();

    JsonObject body = (JsonObject) handler.body();
    String selectStatement = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"'";
    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        failuerDb(promise, response, err);
      })
      .onSuccess(res ->{
        successDb(promise, response, body, res);

      });
  }

  private void failuerDb(Promise<Object> promise, JsonObject response, Throwable err) {
    LOG.debug("Selection Error: {}", err);
    response.put("statusCode",404);
    response.put("message","Something went wrong");
    promise.complete(response);
  }

  private void successDb(Promise<Object> promise, JsonObject response, JsonObject body, io.vertx.sqlclient.RowSet<Row> res) {
    if(res.rowCount() < 1){
      response.put("statusCode",404);
      response.put("message","Cannot Find User");
      promise.complete(response);
    }else{
      JsonObject user = new JsonObject();
      for (Row row : res) {
        user = row.toJson();
      };
      String password = body.getString("password");
      String hash = HashingPassword.getCryptoHash(password, "SHA-256");
      String DatabasePassword = user.getString("password");
      if(!DatabasePassword.equals(hash)){
        response.put("statusCode",404);
        response.put("message","Please enter valid mobile number or password");
        promise.complete(response);
      }else{
        JsonObject data = new JsonObject();
        String auth = AuthSystem.user(body);
        data.put("token",auth);
        data.put("fingerPrint",body.getString("fingerPrint"));
        response.put("statusCode",200);
        response.put("message","Success");
        response.put("data",data);
        promise.complete(response);
      }
    }
  }
}
