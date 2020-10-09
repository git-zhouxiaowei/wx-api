package com.aisino.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 公共声明
 * @Author Zhouxw
 * @Date 2020/9/28 0028 15:00
 **/
public class Constant {
    /**
     * 全局token 所有与微信有交互的前提
     */
    public static String ACCESS_TOKEN;
    /**
     * 全局token上次获取时间
     */
    public static long LAST_TOKEN_TIME;
    /**
     * 全局领取发票授权页ticket
     */
    public static String AUTH_TICKET;
    /**
     * 全局领取发票授权页ticket上次获取时间
     */
    public static long LAST_AUTH_TICKET_TIME;
    /**
     * 全局领取JSSDK授权页ticket
     */
    public static String JS_API_TICKET;
    /**
     * 全局领取JSSDK授权页ticket上次获取时间
     */
    public static long LAST_JS_API_TICKET_TIME;

    /**
     * session池
     */
    public static Map<String, String> sessionPool = new HashMap<>();

    /**
     * 文件后缀
     */
    public static final String PNG = ".png", PDF = ".pdf", JPG = ".jpg";

    /**
     * 微信返回信息标识
     */
    public static final String OK = "ok", ERR = "err";

}
