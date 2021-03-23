package com.madpay.madpay.AuthSystem;

import com.madpay.madpay.helper.AuthHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.RoutingContext;

import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AuthSystem extends AbstractVerticle {
  public static String user(JsonObject body) {
    final String secretKey = "/gUMUTGAdSlccs50XKdvVOMEM+GcH7HJErOMkCEefxM=";
    JsonObject jsonObject = body;
    jsonObject.put("date", new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(Calendar.getInstance().getTime()));
    String originalString = jsonObject.encode();
    String encryptedString = AuthHelper.encrypt(originalString, secretKey) ;
    String decryptedString = AuthHelper.decrypt(encryptedString, secretKey) ;
    // System.out.println(originalString);
    // System.out.println(encryptedString);
    // System.out.println(decryptedString);
    return encryptedString;

  }
}
