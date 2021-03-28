package com.madpay.madpay.usermang;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsExist extends AbstractVerticle {

  JsonObject response = new JsonObject();
  JsonObject body = new JsonObject();
  private final PgPool db;
  private static final Logger LOG = LoggerFactory.getLogger(Login.class);


  public IsExist(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("IsExist", handler -> {
      vertx.executeBlocking(promise -> {
        isExistFunc(promise);
      }, res -> {
        handler.reply(res.result().toString());
      });
      body = (JsonObject) handler.body();
    });


    super.start(startPromise);
  }

  private void isExistFunc(Promise<Object> promise) {
    String selectStatement = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"'"+
      " AND \"country\"='"+body.getString("countryCode")+"'";
    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        LOG.debug("err: {}",err);
        response.put("statusCode","404");
        response.put("message","something went wrong");
        promise.complete(response);
      })
      .onSuccess(res ->{
        JsonObject data = new JsonObject();
        if(res.rowCount()>=1){
          data.put("isExit",true);
        }else{
          data.put("isExit",false);
        }
        response.put("statusCode","200");
        response.put("message","Success");
        response.put("data",data);
        promise.complete(response);

      });

  }
}
