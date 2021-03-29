package com.madpay.madpay.usermang;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CreatePassword extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(Signup.class);
  private final PgPool db;
  private JsonObject response = new JsonObject();
  UUID uuid;
  private String hash;
  private JsonObject body = new JsonObject();



  public CreatePassword(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    super.start(startPromise);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("Signup", handler -> {
      body = (JsonObject) handler.body();

      Promise<Void> findUser = Promise.promise();
      Promise<Void> userValidatePhone = Promise.promise();
      Promise<Void> hashPassword = Promise.promise();
      Promise<Void> createUser = Promise.promise();
      Promise<Void> updateTwilio = Promise.promise();


      vertx.executeBlocking(promise -> {
//        findUserFunc(handler, findUser, promise);
//        final Future<Void> futureFindUser = findUser.future();
//        final Future<Void>  futureUserValidatePhone = userValidatePhone.future();
//        final Future<Void>  futureHashPassword = hashPassword.future();
//        final Future<Void>  futureCreateUser = createUser.future();
//        final Future<Void>  futureUpdateTwilio = updateTwilio.future();
//
//
//        futureFindUser.onSuccess(firstSuc -> userValidatePhoneFunc(handler, userValidatePhone, promise));
//        futureUserValidatePhone.onSuccess(firstSuc -> hashPasswordFunc(handler, hashPassword, promise));
//
//        futureHashPassword.onSuccess(firstSuc ->createUserFunc(handler, createUser, promise));
//        futureCreateUser.onSuccess(firstSuc -> updateTwilioFunc(handler, updateTwilio, promise));
//        futureUpdateTwilio.onSuccess(firstSuc -> promise.complete(response));

      },res -> {
        LOG.debug("response: {}",res.result().toString());
        handler.reply(res.result().toString());
      });
    });
    super.start(startPromise);
  }
}
