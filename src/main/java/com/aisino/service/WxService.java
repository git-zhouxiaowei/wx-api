package com.aisino.service;

import com.aisino.pojo.Invoice;

import java.util.List;
import java.util.Map;

public interface WxService {
    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:25
     * @Description 全局accessToken获取
     * @Return java.lang.String
     */
    String getAccessToken();

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:26
     * @Description 获取领票权限Ticket
     * @Return java.lang.String
     */
    String getAuthTicket();

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:27
     * @Description 获取jssdk权限Ticket
     * @Return java.lang.String
     */
    String getJsapiTicket();

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:27
     * @Description 获取jssdk信息
     * @Param [url：使用sdk的网页地址]
     * @Return java.util.List<java.util.Map < java.lang.String, java.lang.String>>
     */
    List<Map<String, String>> getJsSdk(String url);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:28
     * @Description 获取微信用户信息
     * @Param [code：微信回调参数]
     * @Return java.lang.String
     */
    String getWxUserInfo(String code);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:29
     * @Description 获取静默授权地址
     * @Param [sessionId：数组key值]
     * @Return java.lang.String
     */
    String silentAuth(String sessionId);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:31
     * @Description 获取开票平台识别ID
     * @Return java.lang.String
     */
    String getSpappId();

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:31
     * @Description 设置商户信息，在领票页面超时后点击弹出联系方式
     * @Param [timeOut：超时时间（秒）, phone：联系方式，可以是座机号码]
     * @Return java.lang.String
     */
    String setContact(String timeout, String phone);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:33
     * @Description 上传图片到微信服务器
     * @Param [filePath：本地图片绝对路径]
     * @Return java.lang.String
     */
    String uploadImage(String filePath);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:33
     * @Description 创建卡券模板
     * @Param [payee：销方名称, type：发票类型,默认为增值税电子普通发票,
     * logoUrl：微信图片地址, title：商户简称，9字符以内]
     * @Return java.lang.String
     */
    String createCardTemplate(String payee, String type, String logoUrl, String title);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:36
     * @Description 获取授权页地址
     * @Param [spAppId：发票平台识别ID, orderId：订单号，业务系统唯一即可, money：金额（单位：分）]
     * @Return java.lang.String
     */
    String openAuth(String spAppId, String orderId, String money);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:36
     * @Description 查询领票授权状态
     * @Param [spAppId：发票平台识别ID, orderId：订单号，业务系统唯一即可]
     * @Return java.lang.String
     */
    String getAuthStatus(String spAppId, String orderId);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:36
     * @Description 上传PDF发票
     * @Param [filePath：pdf发票本地绝对路径, delFlag：是否上传完删除]
     * @Return java.lang.String
     */
    String uploadPdf(String filePath, boolean delFlag);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:38
     * @Description 查看已经上传的PDF
     * @Param [mediaId：微信附件ID]
     * @Return java.lang.String
     */
    String getPdf(String mediaId);

    /**
     * @Author Zhouxw
     * @Date 2020/10/09 10:39
     * @Description 插入卡包
     * @Param [orderId：订单号，业务系统唯一, invoice：发票信息, cardId：卡券ID]
     * @Return java.lang.String
     */
    String insertCard(String orderId, Invoice invoice, String cardId);
}
