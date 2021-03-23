package com.madpay.madpay.flywayMigration;

import com.madpay.madpay.MainVerticle;
import com.madpay.madpay.configLoader.DbConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayMigration {

  private static final Logger LOG = LoggerFactory.getLogger(FlywayMigration.class);

    public static Future<Void> migrate(final Vertx vertx, final DbConfig dbConfig) {
      LOG.debug("DB Config: {}", dbConfig);
      LOG.debug("DB Password: {}", dbConfig.getPassword());
      return vertx.<Void>executeBlocking(promise ->{
        extracted(dbConfig);
        promise.complete();
      }).onFailure(err ->LOG.error("Faild to migrate db schema with error {}", err));
    }

  private static void extracted(DbConfig dbConfig) {
    final String jdbUrl = String.format("jdbc:postgresql://%s:%d/%s",
      dbConfig.getHost()
      , dbConfig.getPort()
      , dbConfig.getDatabase());

    LOG.debug("MigrationDB schema using jdbc url: {}", jdbUrl);

    final Flyway flayway = Flyway.configure()
      .dataSource(jdbUrl, dbConfig.getUser(), dbConfig.getPassword())
      .schemas("public")
      .defaultSchema("public")
      .load();

    flayway.migrate();
  }
}
