package com.madpay.madpay.configLoader;

import com.madpay.madpay.MainVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class ConfigLoader {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);
  public static final String SERVER_PORT = "SERVER_PORT";
  public static final String DB_HOST = "DB_HOST";
  public static final String DB_PORT = "DB_PORT";
  public static final String DB_DATABASE = "DB_DATABASE";
  public static final String DB_USER = "DB_USER";
  public static final String DB_PASSWORD = "DB_PASSWORD";
  static final List<String> EXPOSED_ENVIROMENT_VARIBLES =
    Arrays.asList(SERVER_PORT, DB_HOST, DB_PORT, DB_DATABASE, DB_USER,DB_PASSWORD);
//  SERVER_PORT=8888;DB_HOST=localhost;DB_PORT=5432;DB_DATABASE=madpay;DB_USER=nour;DB_PASSWORD=123456a
  public static Future<BrokerConfig> load(Vertx vertx){
    final var exposedKeys = new JsonArray();
    EXPOSED_ENVIROMENT_VARIBLES.forEach(exposedKeys::add);
    LOG.debug("Fetch configration for {}", exposedKeys.encode() );
    var envStore = new ConfigStoreOptions()
      .setType("env")
      .setConfig(new JsonObject().put("key", exposedKeys));

    var retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(envStore));

    return retriever.getConfig().map(BrokerConfig::from);
  };
}
