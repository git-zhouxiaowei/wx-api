package com.aisino.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aisino.common.Constant;
import com.aisino.pojo.Invoice;
import com.aisino.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @Description: 微信原生接口业务类
 * @Author Zhouxw
 * @Date 2020/9/28 0028 16:04
 **/
@Service
@Slf4j
public class WxServiceImpl implements WxService {
    @Value("${wx.appId}")
    private String appId;
    @Value("${wx.appSecret}")
    private String appSecret;
    @Value("${wx.redirectUri}")
    private String redirectUrl;

    @Override
    public synchronized String getAccessToken() {
        if (Constant.ACCESS_TOKEN == null || System.currentTimeMillis() - Constant.LAST_TOKEN_TIME > 7000 * 1000) {
            // 微信接口地址
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
            String resp = HttpUtil.get(url, 5000);
            JSONObject jsonObject = JSONUtil.parseObj(resp);
            // 输出access_token
            log.info(">>-ACCESS_TOKEN：" + jsonObject.get("access_token"));
            //给静态变量赋值，获取到access_token
            Constant.ACCESS_TOKEN = (String) jsonObject.get("access_token");
            //给获取access_token时间赋值，方便下此次获取时进行判断
            Constant.LAST_TOKEN_TIME = System.currentTimeMillis();
        }
        return Constant.ACCESS_TOKEN;
    }

    @Override
    public synchronized String getAuthTicket() {
        if (Constant.AUTH_TICKET == null || System.currentTimeMillis() - Constant.LAST_AUTH_TICKET_TIME > 7000 * 1000) {
            try {
                String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + getAccessToken() + "&type=wx_card";
                String resp = HttpUtil.get(url, 5000);
                log.info("领票authTicket返回:" + resp);
                //字符串转json
                JSONObject jsonObject = JSONUtil.parseObj(resp);
                Constant.AUTH_TICKET = jsonObject.get("ticket").toString();
            } catch (JSONException e) {
                log.error("领票authTicket异常：", e);
            }
        }
        return Constant.AUTH_TICKET;
    }

    @Override
    public synchronized String getJsapiTicket() {
        if (Constant.JS_API_TICKET == null || System.currentTimeMillis() - Constant.LAST_JS_API_TICKET_TIME > 7000 * 1000) {
            String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + getAccessToken() + "&type=jsapi";
            String resp = HttpUtil.get(url, 5000);
            log.info("JSTicket返回:" + resp);
            try {
                JSONObject jsonObject = JSONUtil.parseObj(resp);
                Constant.JS_API_TICKET = (String) jsonObject.get("ticket");
            } catch (Exception e) {
                log.error("获取JSTicket异常：", e);
            }
        }
        return Constant.JS_API_TICKET;
    }

    @Override
    public List<Map<String, String>> getJsSdk(String url) {
        // 随机字符串
        String nonceStr = IdUtil.simpleUUID();
        // 微信接收的时间戳是10位的所以咱们要除以1000
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        // 获取签名
        String signature = getJsSdkSign(url, nonceStr, timestamp);
        // 拼装返回数据
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = new HashMap(4);
        map.put("signature", signature);
        map.put("nonceStr", nonceStr);
        map.put("appId", appId);
        map.put("timestamp", timestamp);
        list.add(map);
        return list;
    }

    @Override
    public String getWxUserInfo(String code) {
        String openUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" +
                appId + "&secret=" + appSecret + "&code=" + code + "&grant_type=authorization_code";
        String result = "";
        try {
            String resp = HttpUtil.get(openUrl, 5000);
            JSONObject jsonObject = JSONUtil.parseObj(resp);
            // 获取到用户openId
            String openId = (String) jsonObject.get("openid");
            String userInfoUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=" +
                    getAccessToken() + "&openid=" + openId + "&lang=zh_CN";
            // 继续获取用户更多信息
            result = HttpUtil.get(userInfoUrl, 5000);
        } catch (Exception e) {
            log.error("获取微信用户信息异常：", e);
        }
        return result;
    }

    @Override
    public String silentAuth(String sessionId) {
        String url = null;
        try {
            String redirectUri = URLEncoder.encode(redirectUrl + "?p=" + sessionId, "UTF-8");
            url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" +
                    appId + "&redirect_uri=" + redirectUri + "&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
            log.info("静默授权地址：" + url);
        } catch (UnsupportedEncodingException e) {
            log.error("发起静默授权，编码时异常：", e);
        }
        return url;
    }

    @Override
    public String getSpappId() {
        String url = "https://api.weixin.qq.com/card/invoice/seturl?access_token=" + getAccessToken();
        String resp = HttpUtil.post(url, "{}");
        log.info("获取开票平台ID返回：" + resp);
        JSONObject jsonObject = JSONUtil.parseObj(resp);
        String invoiceUrl = (String) jsonObject.get("invoice_url");
        // 截取平台ID
        String spAppId = StrUtil.sub(invoiceUrl, invoiceUrl.indexOf("s_pappid=") + 9, invoiceUrl.length());
        return spAppId;
    }

    @Override
    public String setContact(String timeOut, String phone) {
        String url = "https://api.weixin.qq.com/card/invoice/setbizattr?action=set_contact&access_token=" + getAccessToken();
        Map<String, Object> contactMap = new HashMap<>(2);
        contactMap.put("time_out", timeOut);
        contactMap.put("phone", phone);
        Map<String, Object> dataMap = new HashMap<>(1);
        dataMap.put("contact", contactMap);
        String paramJson = JSONUtil.toJsonStr(dataMap);
        String resp = HttpUtil.post(url, paramJson);
        log.info("设置开票商户信息返回：" + resp);
        return resp;
    }

    @Override
    public String uploadImage(String filePath) {
        String urlStr = "https://api.weixin.qq.com/cgi-bin/media/uploadimg?access_token=" + getAccessToken();
        String resp = uploadCommon(filePath, urlStr, false);
        log.info("上传图片返回：" + resp);
        JSONObject jsonObject = JSONUtil.parseObj(resp);
        String logoUrl = (String) jsonObject.get("url");
        return logoUrl;
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 16:51
     * @Description 创建卡券模板
     * @Param [payee 收款方, type 发票类型, logoUrl logo图片地址, title 商户简称]
     * @Return java.lang.String
     */
    @Override
    public String createCardTemplate(String payee, String type, String logoUrl, String title) {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/card/invoice/platform/createcard?access_token=" + accessToken;
        // 按格式拼装参数
        Map<String, Object> baseMap = new HashMap<>(2);
        baseMap.put("logo_url", logoUrl);
        baseMap.put("title", title);

        Map<String, Object> invoiceMap = new HashMap<>(3);
        invoiceMap.put("base_info", baseMap);
        invoiceMap.put("payee", payee);
        // 最长9个汉字
        if (StrUtil.isBlank(type)) {
            type = "增值税电子普通发票";
        }
        invoiceMap.put("type", type);

        Map<String, Object> endMap = new HashMap<>(1);
        endMap.put("invoice_info", invoiceMap);
        String paramsJson = JSONUtil.toJsonStr(endMap);
        String resp = HttpUtil.post(url, paramsJson);
        log.info("创建卡券返回：" + resp);
        JSONObject jsonObject = JSONUtil.parseObj(resp);
        String cardId = (String) jsonObject.get("card_id");
        return cardId;
    }

    @Override
    public String openAuth(String spAppId, String orderId, String money) {
        String url = "https://api.weixin.qq.com/card/invoice/getauthurl?access_token=" + getAccessToken();
        Map<String, Object> tmpMap = new HashMap<>(8);
        String authUrl = "";
        try {
            tmpMap.put("s_pappid", spAppId);
            tmpMap.put("order_id", orderId);
            tmpMap.put("money", money);
            tmpMap.put("timestamp", DateToTimestamp(new Date()));
            tmpMap.put("source", "wap");
            tmpMap.put("redirect_url", "");
            tmpMap.put("ticket", getAuthTicket());
            tmpMap.put("type", "2");
            String paramsJson = JSONUtil.toJsonStr(tmpMap);
            String resp = HttpUtil.post(url, paramsJson);
            log.info("获取授权页返回：" + resp);
            JSONObject jsonObject = JSONUtil.parseObj(resp);
            authUrl = jsonObject.get("auth_url").toString();
        } catch (Exception e) {
            log.error("获取授权页地址异常：", e);
        }
        return authUrl;
    }

    @Override
    public String getAuthStatus(String spAppId, String orderId) {
        String url = "https://api.weixin.qq.com/card/invoice/getauthdata?access_token=" + getAccessToken();
        Map<String, Object> tmpMap = new HashMap<>(2);
        tmpMap.put("s_pappid", spAppId);
        tmpMap.put("order_id", orderId);
        String paramsJson = JSONUtil.toJsonStr(tmpMap);
        String resp = HttpUtil.post(url, paramsJson);
        return resp;
    }

    @Override
    public String uploadPdf(String filePath, boolean delFlag) {
        String urlStr = "https://api.weixin.qq.com/card/invoice/platform/setpdf?access_token=" + getAccessToken();
        String resp = uploadCommon(filePath, urlStr, delFlag);
        log.info("上传pdf发票返回：" + resp);
        JSONObject jsonObject = JSONUtil.parseObj(resp);
        String mediaId = (String) jsonObject.get("s_media_id");
        return mediaId;
    }

    @Override
    public String getPdf(String mediaId) {
        String url = "https://api.weixin.qq.com/card/invoice/platform/getpdf?action=get_url&access_token=" + getAccessToken();
        Map<String, Object> tmpMap = new HashMap<>();
        tmpMap.put("action", "get_url");
        tmpMap.put("s_media_id", mediaId);
        String paramsJson = JSONUtil.toJsonStr(tmpMap);
        String resp = HttpUtil.post(url, paramsJson);
        log.info("查看已上传pdf发票返回：" + resp);
        return resp;
    }

    @Override
    public String insertCard(String orderId, Invoice invoice, String cardId) {
        String url = "https://api.weixin.qq.com/card/invoice/insert?access_token=" + getAccessToken();
        // 第一层 商品信息
        // 第二层 发票详情
        Map<String, Object> invoiceUserDataMap = new HashMap<>(11);
        invoiceUserDataMap.put("fee", invoice.getFee());
        invoiceUserDataMap.put("title", invoice.getTitle());
        invoiceUserDataMap.put("billing_time", DateToTimestamp(new Date()));
        invoiceUserDataMap.put("billing_no", invoice.getBillingNo());
        invoiceUserDataMap.put("billing_code", invoice.getBillingCode());
        invoiceUserDataMap.put("fee_without_tax", invoice.getFeeWithoutTax());
        invoiceUserDataMap.put("tax", invoice.getTax());
        invoiceUserDataMap.put("s_pdf_media_id", invoice.getPdfMediaId());
        invoiceUserDataMap.put("check_code", invoice.getCheckCode());
        invoiceUserDataMap.put("seller_number", invoice.getSellerNumber());
        invoiceUserDataMap.put("info", invoice.getItems());

        // 第三层 用户信息结构体
        Map<String, Object> userCardMap = new HashMap<>(1);
        userCardMap.put("invoice_user_data", invoiceUserDataMap);

        // 第四层 发票具体内容
        Map<String, Object> cardExtMap = new HashMap<>(2);
        cardExtMap.put("nonce_str", System.currentTimeMillis());
        cardExtMap.put("user_card", userCardMap);

        // 第五层 插入卡包所需所有数据
        Map<String, Object> totalMap = new HashMap<>(4);
        totalMap.put("order_id", orderId);
        totalMap.put("card_id", cardId);
        totalMap.put("appid", appId);
        totalMap.put("card_ext", cardExtMap);

        String paramsJson = JSONUtil.toJsonStr(totalMap);
        String res = HttpUtil.post(url, paramsJson);
        log.info("插入卡包返回：" + res);
        try {
            JSONObject jsonObject = JSONUtil.parseObj(res);
            String msg = jsonObject.get("errmsg").toString();
            if (Constant.OK.equals(msg)) {
                res = msg;
            }
        } catch (JSONException e) {
            log.error("转换异常：", e);
        }
        return res;
    }


    private String getJsSdkSign(String url, String nonceStr, String timestamp) {
        String sign = "";
        try {
            String jsApiTicket = getJsapiTicket();
            String content = "jsapi_ticket=" + jsApiTicket + "&noncestr=" + nonceStr + "&timestamp=" + timestamp + "&url=" + url;
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(content.getBytes());
            StringBuffer signature = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                signature.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }
            sign = signature.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("签名生成异常：", e);
        }
        return sign;
    }

    private String uploadCommon(String filePath, String urlStr, boolean delFlag) {
        File file = new File(filePath);
        String res = "";
        HttpURLConnection conn = null;
        OutputStream out = null;
        BufferedReader reader = null;
        DataInputStream in = null;
        // boundary就是request头和上传文件内容的分隔符
        String BOUNDARY = "---------------------------123821742118716";
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            out = new DataOutputStream(conn.getOutputStream());

            String filename = file.getName();
            String contentType = new MimetypesFileTypeMap().getContentType(file);
            String name = "";
            if (filename.endsWith(Constant.PDF)) {
                contentType = "application/pdf";
                name = "pdf";
            } else if (filename.endsWith(Constant.PNG)) {
                contentType = "image/png";
                name = "png";
            } else if (filename.endsWith(Constant.JPG)) {
                contentType = "image/jpg";
                name = "jpg";
            }
            if (StrUtil.isBlank(contentType)) {
                contentType = "application/octet-stream";
            }

            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
            strBuf.append(
                    "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n");
            strBuf.append("Content-Type:" + contentType + "\r\n\r\n");
            out.write(strBuf.toString().getBytes());
            in = new DataInputStream(new FileInputStream(file));
            int bytes;
            byte[] bufferOut = new byte[1024];
            while ((bytes = in.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();
            out.write(endData);
            out.flush();

            // 读取返回数据
            StringBuffer readBuf = new StringBuffer();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                readBuf.append(line).append("\n");
            }
            res = readBuf.toString();
        } catch (Exception e) {
            log.error("上传时异常：", e);
        } finally {
            if (null == in) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("关闭流异常：", e);
                }
            }
            if (null == reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("关闭流异常：", e);
                }
            }
            if (null == out) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("关闭流异常：", e);
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
            if (delFlag) {
                try {
                    file.delete();
                } catch (Exception e) {
                    log.error("文件删除异常：", e);
                }
            }
        }
        return res;
    }

    /**
     * @Description Date类型转换为10位时间戳
     * @Param [time]
     * @Return java.lang.Integer
     */
    public Integer DateToTimestamp(Date time) {
        Timestamp ts = new Timestamp(time.getTime());
        return (int) ((ts.getTime()) / 1000);
    }
}
