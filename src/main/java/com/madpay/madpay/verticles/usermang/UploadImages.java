package com.madpay.madpay.verticles.usermang;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class UploadImages extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(UserMang.class);
  UUID uuid;
  JsonObject response = new JsonObject();
  int statusCode;
JsonObject user = new JsonObject();
  JsonObject res = new JsonObject();
  JsonArray resArray = new JsonArray();
  private FileUpload fileUpload;
  JsonObject body = new JsonObject();
  private final PgPool db;
  private final Router api;
  public UploadImages(Router api, PgPool db) {
    this.db = db;
    this.api = api;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    api.post("/uploadId").handler(context -> {
//      context.queryParam("token")
      uploadHandler(context);
    });
  }

  private void userValidateFunc(RoutingContext context, Promise<Void> userValidate, Promise<Object> promise){
    String userToken = context.request().getHeader("token");
    String fingerPrint = context.request().getHeader("fingerPrint");

    String selectStatement = "SELECT * FROM public.\"user\"\n" +
      "WHERE \"userToken\"='"+userToken+"' " +
      "AND \"fingerPrint\"='"+ fingerPrint +"' "+
      "AND AGE('"+ LocalDateTime.now()+"',\"lastLogin\") <= '12:00:00'";

    db.query(selectStatement)
      .execute()
      .onFailure(err -> {
        LOG.debug("Something went wrong: {}", err);
        promise.fail("Something went wrong");
      })
      .onSuccess(res ->{
        if(res.rowCount() < 1){
          response.put("statusCode",404);
          response.put("message","Please login again...");
        }
        for(Row row:res){
          user = row.toJson();
        }
        userValidate.complete();
      });

  }
  private void uploadHandler(RoutingContext context) {
    Promise<Void> userValidate = Promise.promise();
    Promise<Void> uploadId = Promise.promise();
    Promise<Void> createUploadDB = Promise.promise();

    vertx.executeBlocking(promise -> {
      userValidateFunc(context,userValidate, promise);

      final Future<Void> futureUserValidate = userValidate.future();
      final Future<Void> futureUploadId = uploadId.future();
//        final Future<Void> futureCreateUploadDB = createUploadDB.future();

      futureUserValidate.onSuccess(firstSuc -> uploadIdProcessFunc(context,uploadId, promise));
      futureUploadId.onSuccess(firstSuc -> promise.complete(response));
    }, res -> {
      context.request().response()
        .setStatusCode(statusCode)
        .putHeader("Content-Type", "application/json; charset=utf-8")
        .end(response.encode());
    });
//      body = (JsonObject) context.getBodyAsJson();
  }
  private void uploadIdProcessFunc(RoutingContext context, Promise<Void> uploadId, Promise<Object> promise) {
    uuid = UUID.randomUUID();
    Set<FileUpload> fileUploadSet = context.fileUploads();
    Iterator<FileUpload> fileUploadIterator = fileUploadSet.iterator();

    int i = 0;
    while (fileUploadIterator.hasNext()){
      fileUpload = fileUploadIterator.next();
      Buffer uploadedFile = vertx.fileSystem().readFileBlocking(fileUpload.uploadedFileName());
      LOG.debug(fileUpload.uploadedFileName().substring(13));
      res.put("fileId"+i,fileUpload.uploadedFileName().substring(13));
      resArray.add(fileUpload.uploadedFileName().substring(13));
      i++;
    }
    if(!fileUploadIterator.hasNext()){
      try {
        String fileName = URLDecoder.decode(fileUpload.fileName(), "UTF-8");
        LOG.debug(fileName);
        statusCode = 200;

        res.put("uploadId",uuid);
        response.put("statusCode",200);
        response.put("message","Success");
        response.put("data",res);
        uploadId.complete();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        statusCode = 404;
        response.put("statusCode",404);
        response.put("message","Error happened, please try again Later!");
        promise.complete();
      }
    }
  }
  private void createUploadDBFunc(Promise<Void> createUploadDB, Promise<Object> promise) {
    LOG.debug("in First Promises---------------------------------------");

    String insertStatement ="INSERT INTO public.uploadedids(\n" +
      "\t\"userId\", \"uploadId\", \"uploadedNames\", \"createdOn\", \"updatedOn\")\n" +
      "\tVALUES ('"+user.getString("userId")+"', '"+ uuid +"', '"+ resArray +"', '"+ LocalDateTime.now()+"', '"+ LocalDateTime.now()+"');";
    db.query(insertStatement)
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
        createUploadDB.complete();
      });
  }
}
