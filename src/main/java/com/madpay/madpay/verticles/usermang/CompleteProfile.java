package com.madpay.madpay.verticles.usermang;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Set;

public class CompleteProfile extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(UserMang.class);

  JsonObject response = new JsonObject();
  JsonObject body = new JsonObject();
  private final PgPool db;
  private final Router api;
  public CompleteProfile(final PgPool db, Router api) {
    this.db = db;
    this.api = api;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    super.start(startPromise);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("Upload", handler -> {
      Promise<Void> uploadId = Promise.promise();
      Promise<Void> createUploadDB = Promise.promise();

      vertx.executeBlocking(promise -> {

        uploadIdFunc(uploadId, promise);

        final Future<Void> futureUploadId = uploadId.future();
//        final Future<Void> futureCreateUploadDB = createUploadDB.future();

        futureUploadId.onSuccess(firstSuc -> promise.complete(response));
//        futureUploadId.onSuccess(firstSuc -> createUploadDBFunc(createUploadDB, promise));
//        futureCreateUploadDB.onSuccess(firstSuc -> promise.complete(response));

      }, res -> {
        handler.reply(res.result().toString());
      });

      body = (JsonObject) handler.body();
    });
  }

  private void uploadIdFunc(Promise<Void> uploadId, Promise<Object> promise) {
    api.route().handler(ctx ->{
      uploadIdProcessFunc(ctx,uploadId,promise);
    });
  }

  private void uploadIdProcessFunc(RoutingContext context, Promise<Void> uploadId, Promise<Object> promise) {
    Set<FileUpload> fileUploadSet = context.fileUploads();
    Iterator<FileUpload> fileUploadIterator = fileUploadSet.iterator();
    while (fileUploadIterator.hasNext()){
      FileUpload fileUpload = fileUploadIterator.next();
      Buffer uploadedFile = vertx.fileSystem().readFileBlocking(fileUpload.uploadedFileName());
      LOG.debug(fileUpload.uploadedFileName().substring(13));
      try {
        JsonObject res = new JsonObject();
        res.put("fileId",fileUpload.uploadedFileName().substring(13));
        String fileName = URLDecoder.decode(fileUpload.fileName(), "UTF-8");
        response.put("statusCode","404");
        response.put("message","Error happened, please try again Later!");
        uploadId.complete();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        response.put("statusCode","404");
        response.put("message","Error happened, please try again Later!");
        promise.complete();
      }
    }
  }

  private void createUploadDBFunc(Promise<Void> createUploadDB, Promise<Object> promise) {
  }
}
