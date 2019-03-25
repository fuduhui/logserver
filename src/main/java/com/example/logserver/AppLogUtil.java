package com.example.logserver;


import java.util.HashMap;
import java.util.Map;


public class AppLogUtil {

    public static final Map<String,String> appLogMap=new HashMap<String, String>();

    static {
        appLogMap.put("boot-test","logger.boot");
    }


}
