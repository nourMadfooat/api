package com.madpay.madpay.usermang;

import com.madpay.madpay.AuthSystem.AuthSystem;
import com.madpay.madpay.helper.AuthHelper;
import com.madpay.madpay.helper.HashingPassword;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

public class Signup extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(Signup.class);
  private final PgPool db;
  private JsonObject response = new JsonObject();
  UUID uuid;
  private String hash;
  private JsonObject body = new JsonObject();



  public Signup(PgPool db) {
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
          findUserFunc(handler, findUser, promise);
          final Future<Void>  futureFindUser = findUser.future();
          final Future<Void>  futureUserValidatePhone = userValidatePhone.future();
          final Future<Void>  futureHashPassword = hashPassword.future();
          final Future<Void>  futureCreateUser = createUser.future();
          final Future<Void>  futureUpdateTwilio = updateTwilio.future();


          futureFindUser.onSuccess(firstSuc -> userValidatePhoneFunc(handler, userValidatePhone, promise));
          futureUserValidatePhone.onSuccess(firstSuc -> hashPasswordFunc(handler, hashPassword, promise));

          futureHashPassword.onSuccess(firstSuc ->createUserFunc(handler, createUser, promise));
          futureCreateUser.onSuccess(firstSuc -> updateTwilioFunc(handler, updateTwilio, promise));
          futureUpdateTwilio.onSuccess(firstSuc -> promise.complete(response));

        },res -> {
          LOG.debug("response: {}",res.result().toString());
          handler.reply(res.result().toString());
        });
    });
  }



  private void findUserFunc(Message<Object> handler, Promise findUser, Promise<Object> promise) {
    String selectStatement = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"'";
    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
        findUser.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() >= 1){
          response.put("statusCode","404");
          response.put("message","User is already hase an account");
          findUser.fail("Something went wrong");
          promise.complete(response);
        }else{
          findUser.complete();
        }
      });

  }

  private void userValidatePhoneFunc(Message<Object> handler, Promise userValidatePhone, Promise promise) {
    String selectStatement = "SELECT * FROM public.\"twilio\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"' " +
      "AND \"countryCode\"='"+ body.getString("countryCode") +"' "+
      "AND \"code\"='"+ body.getString("code") +"' "+
      "AND \"isUsed\"=false "+
      "AND AGE('"+ LocalDateTime.now()+"',\"createdOn\") <= '00:02:00'";
    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
      })
      .onSuccess(res ->{
        if(res.rowCount() < 1){
          response.put("statusCode","404");
          response.put("message","Please enter valid OTP");
          userValidatePhone.fail("Something went wrong");
          promise.complete(response);
        }else{
          LOG.debug("res: {}", res);
          userValidatePhone.complete();
        }
      });
  }

  private void hashPasswordFunc(Message<Object> handler, Promise hashPassword, Promise promise) {
    String password = body.getString("password");
    hash = HashingPassword.getCryptoHash(password, "SHA-256");
    hashPassword.complete();
  }
  private void createUserFunc(Message<Object> handler, Promise createUser, Promise promise) {
    uuid = UUID.randomUUID();
    final String secretKey = "/gUMUTGAdSlccs50XKdvVOMEM+GcH7HJErOMkCEefxM=";
    JsonObject jsonObject = new JsonObject();

    jsonObject.put("phone",body.getString("phone"));
    jsonObject.put("fingerPrint",body.getString("fingerPrint"));
    jsonObject.put("countryCode",body.getString("countryCode"));
    jsonObject.put("userId", uuid);
    jsonObject.put("date", new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(Calendar.getInstance().getTime()));
    String originalString = jsonObject.encode();


    String encryptedString = AuthHelper.encrypt(originalString, secretKey) ;

    String insertStatement = "INSERT INTO public.\"user\"("+
      "username, password, \"userId\", phone, country, \"userToken\","+
  "\"lastLogin\", \"isDeleted\", \"isComplete\", email, \"createdAtOn\", \"updatedOn\", \"isActive\", \"fingerPrint\", \"isValidated\")"+
    "VALUES ("+
      "NULL,"+
      "'"+ hash +"',"+
      "'"+ uuid +"',"+
      "'"+ body.getString("phone") + "',"+
      "'"+body.getString("countryCode")+"',"+
      "'"+ encryptedString +"',"+
      " '"+ LocalDateTime.now() +"' ,"+
      " false,"+
      " false,"+
      "NULL,"+
      "'"+LocalDateTime.now()+"',"+
      "'"+LocalDateTime.now()+"',"+
      "false,"+
      "'"+body.getString("fingerPrint")+"',"+
      "true)";
    db.query(insertStatement)
      .execute()
      .onFailure(err -> {
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
      })
      .onSuccess(res -> {
        JsonObject data = new JsonObject();
        data.put("token",encryptedString);
        data.put("fingerPrint",body.getString("fingerPrint"));
        response.put("statusCode","200");
        response.put("message","Success");
        response.put("data",data);
        createUser.complete();
      });
  }

  private void updateTwilioFunc(Message<Object> handler, Promise updateTwilio, Promise promise) {
    String updateStatement = "UPDATE public.twilio\n" +
      "\tSET \"isUsed\"=true,\"updatedOn\"='"+LocalDateTime.now()+"', \"userId\"='"+uuid+"'\n" +
      "\tWHERE phone="+ body.getString("phone") +
      " AND  \"countryCode\"= '"+body.getString("countryCode")+"';";
    db.query(updateStatement)
      .execute()
      .onFailure(err -> {
        response.put("statusCode","404");
        response.put("message","Something went wrong");
        promise.complete(response);
      })
      .onSuccess(res -> {
        updateTwilio.complete();
      });
  }

}
