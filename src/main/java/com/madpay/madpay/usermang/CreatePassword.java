package com.madpay.madpay.usermang;

import com.madpay.madpay.helper.HashingPassword;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreatePassword extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(Signup.class);
  private final PgPool db;
  private JsonObject response = new JsonObject();
  private JsonObject userData = new JsonObject();
  UUID uuid;
  private String hash;
  private JsonObject body = new JsonObject();

  public CreatePassword(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("CreatePassword", handler -> {
      body = (JsonObject) handler.body();
      Promise<Void> findUser = Promise.promise();
      Promise<Void> createPassword = Promise.promise();
      Promise<Void> updateUser = Promise.promise();

      vertx.executeBlocking(promise -> {

        final Future<Void>  futureFindUserPassword = findUser.future();
        final Future<Void> futureCreatePasswordUser = createPassword.future();
        final Future<Void>  futureUpdateUserPhone = updateUser.future();

        findUserFunc(findUser, promise);
        futureFindUserPassword.onSuccess(firstSuc -> createPasswordFunc(createPassword, promise));
        futureCreatePasswordUser.onSuccess(firstSuc -> updateUserFunc(updateUser, promise));
        futureUpdateUserPhone.onSuccess(firstSuc ->promise.complete(response));
      },res -> {
        LOG.debug("response: {}",res.result().toString());
        handler.reply(res.result().toString());
      });
    });
    super.start(startPromise);
  }


  private void findUserFunc(Promise<Void> findUser, Promise<Object> promise) {
    String selectStatement = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"userToken\"='"+ body.getString("token") +"' "+
      " AND \"fingerPrint\"='"+ body.getString("fingerPrint") +"' "+
      " AND \"hasPassword\"=false ";
    LOG.debug("statement: {}", selectStatement);
    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        LOG.debug("err: {}", err);
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
      })
      .onSuccess(res ->{
        if(res.rowCount() < 1){
          response.put("statusCode","404");
          response.put("message","Cannot Found user");
          promise.complete(response);
        }else{
          for (Row row : res) {
            userData = row.toJson();
          };
          findUser.complete();
        }
      });
  }

  private void createPasswordFunc(Promise<Void> createPassword, Promise<Object> promise) {
    String password = body.getString("password");
    hash = HashingPassword.getCryptoHash(password, "SHA-256");
    createPassword.complete();
  }

  private void updateUserFunc(Promise<Void> updateUser, Promise<Object> promise) {
String updateStatement = "UPDATE public.\"user\"\n" +
  "\tSET password='"+hash+"', \"hasPassword\"=true\n" +
  "\tWHERE \"userId\" = '"+userData.getString("userId")+"';";
    db.query(updateStatement)
      .execute()
      .onFailure(err -> {
        LOG.debug("error: {}",err);
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
      })
      .onSuccess(res ->{
        if(res.rowCount() != 1){
          response.put("statusCode","404");
          response.put("message","Cannot Found user");
          promise.complete(response);
        }else{
          response.put("statusCode","200");
          response.put("message","Success");
          updateUser.complete();
        }
      });
  }
}
