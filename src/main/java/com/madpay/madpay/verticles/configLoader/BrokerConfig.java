package com.madpay.madpay.verticles.configLoader;

import ch.qos.logback.classic.Logger;
import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.Objects;

@Builder
@Value
@ToString
public class BrokerConfig {
  int serverPort;
DbConfig dbConfig;
  public static BrokerConfig from(final JsonObject config){
    final Integer serverPort = config.getInteger(ConfigLoader.SERVER_PORT);
//    final Integer serverPort = 8888;

    if(Objects.isNull(serverPort)){
      throw new RuntimeException(ConfigLoader.SERVER_PORT + " Not configured!");
    }

    return BrokerConfig
      .builder()
      .serverPort(config.getInteger(ConfigLoader.SERVER_PORT))
      .dbConfig(parseDbConfig(config))
      .build();
  }

  private static DbConfig parseDbConfig(final JsonObject config) {
//    return DbConfig.builder()
//      .host("localhost")
//      .port(5432)
//      .database("madpay")
//      .user("nour")
//      .password("123456a")
//      .build();
    System.out.println(ConfigLoader.DB_PORT);
    return DbConfig.builder()
      .host(config.getString(ConfigLoader.DB_HOST))
      .port(config.getInteger(ConfigLoader.DB_PORT))
      .database(config.getString(ConfigLoader.DB_DATABASE))
      .user(config.getString(ConfigLoader.DB_USER))
      .password(config.getString(ConfigLoader.DB_PASSWORD))

      .build();
  }
}
