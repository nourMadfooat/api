package com.madpay.madpay.configLoader;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DbConfig {
//  String host ="localhost";
//  int port = 5432;
//  String database = "madpay";
//  String user = "nour";
//  String password = "123456";
String host;
    int port;
  String database;
  String user;
  String password;

  @Override
  public String toString() {
    return "DbConfig{" +
      "host='" + host + '\'' +
      ", port=" + port +
      ", database='" + database + '\'' +
      ", user='" + user + '\'' +
      ", password='*****'" +
      '}';
  }
}
