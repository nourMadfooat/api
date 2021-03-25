package com.madpay.madpay.usermang;

import com.madpay.madpay.AuthSystem.AuthSystem;
import com.madpay.madpay.helper.HashingPassword;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.reactivex.ext.unit.Async;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Future;

public class Signup extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(Signup.class);
  private final PgPool db;

  public Promise<Void> findUser = null;
  public Promise<Void> handleQuery = null;
  public Signup(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("Signup", handler -> {

//      var findUser = applyFindUser(handler);
//      var handleQuery = hashPassword(handler);
//
//      var futureFindUser = findUser.future();
//      var futureHandleQuery = handleQuery.future();
//
//
//      CompositeFuture.all(Arrays.asList(futureFindUser,futureHandleQuery)).onFailure(err -> {
//          LOG.debug("Error Future");
//        })
//        .onSuccess(res ->{
//          LOG.debug("res: {}",res.result().toString());
//          handler.reply(res.result().toString());
//        });
//      applyFindUser(handler, findUser);
//      hashPassword(handler, handleQuery);
        vertx.executeBlocking(promise -> {
          findUser(handler,promise);
        },res -> {
          handler.reply(res.result().toString());
        });
//
//      vertx.executeBlocking(promise -> {
//        hashPassword(handler,promise);

//      },true, res -> {});
    });

//    super.start(startPromise);
  }



  private void findUser(Message<Object> handler,Promise promise) {
    LOG.debug("in applyFindUser -------------------------------------------------");
    JsonObject response = new JsonObject();

    JsonObject body = (JsonObject) handler.body();
    String selectStatmennt = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"'";
    db.query(selectStatmennt)
      .execute()
      .onFailure(err -> {
        findUser.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() < 1){
          response.put("statusCode",404);
          response.put("message","Cannot Find User");
          promise.complete(response);
        }else{
          LOG.debug("in finish apply -------------------------------------------------");
//          hashPassword(handler,promise,body,response );
          userValidatePhone(handler,promise,body,response );
        }
      });

  }

  private void userValidatePhone(Message<Object> handler,Promise promise,JsonObject body, JsonObject response) {
    LOG.debug("in userValidatePhone -------------------------------------------------");

    String selectStatmennt = "SELECT * FROM public.\"twilio\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"' " +
      "AND \"code\"='"+ body.getString("code") +"' "+
      "AND \"isUsed\"=false "+
      "AND \"createdAt\"=age("+ System.currentTimeMillis() +")";
    LOG.debug(selectStatmennt);
    db.query(selectStatmennt)
      .execute()
      .onFailure(err -> {
        LOG.debug("Something went wrong: {}", err);
        promise.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() < 1){
          response.put("statusCode",404);
          response.put("message","Please enter valid OTP");
          promise.complete(response);
        }else{
          LOG.debug("res: {}", res);
          hashPassword(handler,promise,body,response );
        }
      });

    LOG.debug("in finish userValidatePhone -------------------------------------------------");
  }

  private void hashPassword(Message<Object> handler,Promise promise,JsonObject body,JsonObject response) {
    LOG.debug("in Hashd -------------------------------------------------");
    String password = body.getString("password");
    String hash = HashingPassword.getCryptoHash(password, "SHA-256");
    LOG.info("Hashing: {}",hash);
    response.put("statusCode",404);
    response.put("message","Cannot Find User");
    promise.complete(response);
  }

}
