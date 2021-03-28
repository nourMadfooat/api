package com.madpay.madpay.usermang;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class OTPReq extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(Signup.class);

  JsonObject response = new JsonObject();
  JsonObject body = new JsonObject();
  private final PgPool db;

  public OTPReq(PgPool db) {
    this.db = db;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("OtpReq", handler -> {

      Promise<Void> validateOTP = Promise.promise();
      Promise<Void> createOtp = Promise.promise();

      vertx.executeBlocking(promise -> {
        validateOtpFunc(validateOTP, promise);
        final Future<Void> futureValidateOtp = validateOTP.future();
        final Future<Void> futureCreateOtp = createOtp.future();

        futureValidateOtp.onSuccess(firstSuc -> createOtpFunc(createOtp, promise));
        futureCreateOtp.onSuccess(firstSuc -> promise.complete(response));

      }, res -> {
        handler.reply(res.result().toString());
      });
      body = (JsonObject) handler.body();
    });

  }

  private void validateOtpFunc(Promise<Void> validateOTP, Promise<Object> promise) {
    LOG.debug("in First Promises---------------------------------------");
    String selectStatement = "SELECT * FROM public.\"twilio\"\n" +
      "WHERE \"phone\"='"+body.getString("phone")+"' " +
      "AND \"countryCode\"='"+ body.getString("countryCode") +"' "+
      "AND \"isUsed\"=false "+
      "AND AGE('"+ LocalDateTime.now()+"',\"createdOn\") <= '00:02:00'";

    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        LOG.debug("Something went wrong: {}", err);
        promise.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() >= 1){
          response.put("statusCode",404);
          response.put("message","Please Wait to our SMS OTP");
        }
        validateOTP.complete();
      });
  }

  private void createOtpFunc(Promise<Void> createOtp , Promise<Object> promise) {
    LOG.debug("in 2nd Promises---------------------------------------");
    UUID uuid = UUID.randomUUID();

    String selectStatement = "INSERT INTO public.twilio(\n" +
      "\tid, code, \"isUsed\", \"createdOn\", \"updatedOn\", phone, \"userId\", \"countryCode\")\n" +
      "\tVALUES ('"+ uuid +"' , '123456', false, '"+ LocalDateTime.now()+"', '"+LocalDateTime.now()+"', '"+body.getString("phone")+"'"+
      ", NULL , '"+ body.getString("countryCode") +"');";

    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        promise.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() >= 1 )
        {
          JsonObject data = new JsonObject();
          data.put("code","123456");
          response.put("statusCode", "200");
          response.put("Message", "OTP Sent Successfully");
          response.put("data", data);

          createOtp.complete();
        }else{
          response.put("statusCode", "404");
          response.put("Message", "Something went wrong, Please try again!");
          createOtp.complete();
        }
      });
  }
}
