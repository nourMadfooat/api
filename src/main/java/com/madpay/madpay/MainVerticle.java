package com.madpay.madpay;

import com.madpay.madpay.verticles.AuthSystem.AuthSystem;
import com.madpay.madpay.verticles.configLoader.BrokerConfig;
import com.madpay.madpay.verticles.configLoader.ConfigLoader;
import com.madpay.madpay.verticles.flywayMigration.FlywayMigration;
import com.madpay.madpay.verticles.usermang.UserMang;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);


  public static void main(String[] args) {
    var vertx = Vertx.vertx();
    vertx.exceptionHandler(error ->
      LOG.error("Unhandled: ", error)
    );
    vertx.deployVerticle(MainVerticle .class.getName(),
      new DeploymentOptions().setInstances(avilableProcessors()))
      .onFailure(err ->{
        LOG.error("Failed to deploy: {}",err);
      })
      .onSuccess(ar ->{
        LOG.info("Deployed {} with id {}!", MainVerticle.class.getSimpleName(), ar);
      });

  }

  private static  Future<Void> migrateDatabase(Vertx vertx) {
    return ConfigLoader.load(vertx).compose(config ->{
       return FlywayMigration.migrate(vertx, config.getDbConfig());
    });
  }

  private static int avilableProcessors() {
//    return Math.max(1, Runtime.getRuntime().availableProcessors());
    return 1;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigLoader.load(vertx)
      .onFailure(startPromise::fail)
      .onSuccess(configration ->{
        LOG.info("Configration {}", configration);
        startHttpServer(startPromise, configration);
      });

  }

  private void startHttpServer(Promise<Void> startPromise, final BrokerConfig configration) {

    PgPool db = pollCreation(configration);

    final Router api = Router.router(vertx);
    api.route()
      .handler(rtx ->{
        LOG.debug("in Midelware ");
        vertx.executeBlocking(promise -> {
          LOG.debug(rtx.request().path());
          AuthSystem.getAuthUser(db);
        }, res -> {
          rtx.next();
        });
      })
//    .handler(CorsHandler.create("*"))
      .handler(BodyHandler.create())
      .failureHandler(handlerFailure());


    vertx.deployVerticle(new UserMang(api, db))
      .onFailure(err ->{
        LOG.error("Failed to deploy: {}",err);
      })
      .onSuccess(ar ->{
        LOG.info("Deployed {} with id {}!", UserMang.class.getSimpleName(), ar);
      });


    vertx.createHttpServer()
      .requestHandler(api).listen(configration.getServerPort(), http -> {
      if (http.succeeded()) {
        startPromise.complete();
        LOG.info("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private PgPool pollCreation(BrokerConfig configration) {
    // Create DB Pool
    PgConnectOptions connectOptions = new PgConnectOptions()
    .setHost(configration.getDbConfig().getHost())
      .setHost(configration.getDbConfig().getHost())
      .setPort(configration.getDbConfig().getPort())
      .setDatabase(configration.getDbConfig().getDatabase())
      .setUser(configration.getDbConfig().getUser())
      .setPassword(configration.getDbConfig().getPassword());

    final var poolOption = new PoolOptions().setMaxSize(4);

    PgPool db = PgPool.pool(vertx, connectOptions, poolOption);
    return db;
  }


  private Handler<RoutingContext> handlerFailure() {
    return err -> {
      if (err.response().ended()) {
        return;
      }
      LOG.error("Router Error: {}", err.failure());
      err.response()
        .setStatusCode(500)
        .end(new JsonObject().put("message", "something went wrong : "+err.failure()).toBuffer());
    };
  }
}
