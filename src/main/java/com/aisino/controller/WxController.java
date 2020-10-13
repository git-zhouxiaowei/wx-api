package com.aisino.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aisino.common.Constant;
import com.aisino.pojo.Invoice;
import com.aisino.service.WxService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * @Description: 微信原生接口，封装接口
 * @Author Zhouxw
 * @Date 2020/9/28 0028 15:23
 **/
@Api(tags = "微信相关接口")
@Slf4j
@RestController
@RequestMapping("/")
public class WxController {
    @Resource
    private WxService wxService;

    @Value("${wx.invoice.spAppId}")
    private String wxInvoiceSpAppId;

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 11:06
     * @Description 全局accessToken获取
     * @Return java.lang.String
     */
    @ApiOperation(value = "全局accessToken获取")
    @GetMapping("accessToken")
    public String getAccessToken() {
        return wxService.getAccessToken();
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 10:56
     * @Description 获取jssdk信息
     * @Param [response, url]
     * @Return java.util.List<java.util.Map < java.lang.String, java.lang.String>>
     */
    @ApiOperation(value = "获取jssdk信息")
    @ApiImplicitParam(name = "url", value = "encode编码后的H5页面地址", required = true, dataType = "String")
    @GetMapping("jsSdk")
    public List<Map<String, String>> getJsSdk(HttpServletResponse response, @RequestParam String url) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        try {
            url = URLDecoder.decode(url, "UTF-8");
            return wxService.getJsSdk(url);
        } catch (UnsupportedEncodingException e) {
            log.error("解码异常：", e);
        }
        return null;
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 14:57
     * @Description 静默授权，获取微信用户信息
     * @Param [request, response, callbackUrl]
     * @Return void
     */
    @ApiOperation(value = "静默授权获取微信用户信息")
    @ApiImplicitParam(name = "callbackUrl", value = "回调地址", required = true, dataType = "String")
    @GetMapping("silentAuth")
    public void getSilentAuth(HttpServletRequest request, HttpServletResponse response, @RequestParam String callbackUrl) {
        // 存到缓存，然后回调后异步推送
        String sessionId = IdUtil.simpleUUID();
        Constant.sessionPool.put(sessionId, callbackUrl);
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try {
            request.setCharacterEncoding("UTF-8");
            String url = wxService.silentAuth(sessionId);
            response.sendRedirect(url);
        } catch (IOException e) {
            log.error("重定向静默授权时异常：", e);
        }
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 14:58
     * @Description 回调接收接口，跳转客户端地址
     * @Param [response, code, p]
     * @Return void
     */
    @ApiOperation(value = "微信回调接收接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value = "用户code", required = true, dataType = "String"),
            @ApiImplicitParam(name = "p", value = "自定义参数", required = true, dataType = "String")
    })
    @GetMapping("receive")
    public void receive(HttpServletResponse response, @RequestParam String code, @RequestParam String p) {
        String callbackUrlTemp = Constant.sessionPool.get(p);
        try {
            String callbackUrl = new String(Base64.decodeBase64(callbackUrlTemp));
            String wxUserInfoJson = wxService.getWxUserInfo(code);
            String userInfo = Base64.encodeBase64String(wxUserInfoJson.getBytes("utf-8"));
            String returnUrl = callbackUrl + "&userInfo=" + userInfo;
            log.info("回调地址：" + returnUrl);
            response.sendRedirect(returnUrl);
        } catch (IOException e) {
            log.error("返回OpenId时异常：", e);

        }
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:13
     * @Description 首次获取开票平台ID，用于后续业务使用
     * @Param []
     * @Return java.lang.String
     */
    @ApiOperation(value = "获取开票平台ID")
    @GetMapping("spAppId")
    public String spAppId() {
        return wxService.getSpappId();
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:13
     * @Description 设置开票商户信息, 在领票页面超时后提示联系商户，点击直接吊起拨号
     * @Param [timeout, phone]
     * @Return java.lang.String
     */
    @ApiOperation(value = "设置开票商户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "timeout", value = "超时时间(秒)", required = true, dataType = "String"),
            @ApiImplicitParam(name = "phone", value = "联系方式", required = true, dataType = "String")
    })
    @GetMapping("setContact")
    public String setContact(@RequestParam String timeout, @RequestParam String phone) {
        return wxService.setContact(timeout, phone);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:54
     * @Description 上传图片到微信服务器
     * @Param [filePath]
     * @Return java.lang.String
     */
    @ApiOperation(value = "上传图片到微信服务器")
    @ApiImplicitParam(name = "filePath", value = "图片绝对路径", required = true, dataType = "String")
    @GetMapping("uploadImage")
    public String uploadImage(@RequestParam String filePath) {
        return wxService.uploadImage(filePath);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/29 16:51
     * @Description 创建卡券模板
     * @Param [payee 收款方, type 发票类型, logoUrl logo图片地址, title 商户简称]
     * @Return java.lang.String
     */
    @ApiOperation(value = "创建卡券模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "payee", value = "收款方", required = true, dataType = "String"),
            @ApiImplicitParam(name = "type", value = "发票类型,默认为增值税电子普通发票", dataType = "String"),
            @ApiImplicitParam(name = "logoUrl", value = "微信图片地址", required = true, dataType = "String"),
            @ApiImplicitParam(name = "title", value = "商户简称", required = true, dataType = "String")
    })
    @PostMapping("createCardTemplate")
    public String createCardTemplate(@RequestParam String payee, String type, @RequestParam String logoUrl, @RequestParam String title) {
        return wxService.createCardTemplate(payee, type, logoUrl, title);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 16:33
     * @Description 获取授权页地址
     * @Param [spAppId, orderId, money]
     * @Return java.lang.String
     */
    @ApiOperation(value = "领票获取授权页地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "spAppId", value = "开票平台ID", dataType = "String"),
            @ApiImplicitParam(name = "orderId", value = "订单号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "money", value = "金额（单位：分）", required = true, dataType = "String")
    })
    @GetMapping("authUrl")
    public String authUrl(@RequestParam(required = false) String spAppId, @RequestParam String orderId, @RequestParam String money) {
        if (StrUtil.isBlank(spAppId)) {
            if (StrUtil.isBlank(wxInvoiceSpAppId)) {
                return "error：开票平台ID为空，请传入参数或者初始化服务配置！";
            } else {
                spAppId = wxInvoiceSpAppId;
            }
        }
        return wxService.openAuth(spAppId, orderId, money);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 16:33
     * @Description 查询发票领取授权状态
     * @Param [orderId]
     * @Return java.lang.String
     */
    @ApiOperation(value = "领票获取授权页地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "spAppId", value = "开票平台ID", required = true, dataType = "String"),
            @ApiImplicitParam(name = "orderId", value = "订单号", required = true, dataType = "String")
    })
    @GetMapping("authStatus")
    public String authStatus(@RequestParam String spAppId, @RequestParam String orderId) {
        if (StrUtil.isBlank(spAppId)) {
            if (StrUtil.isBlank(wxInvoiceSpAppId)) {
                return "error：开票平台ID为空，请传入参数或者初始化服务配置！";
            } else {
                spAppId = wxInvoiceSpAppId;
            }
        }
        return wxService.getAuthStatus(spAppId, orderId);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:30
     * @Description 上传PDF发票文件
     * @Param [filePath, delFlag]
     * @Return java.lang.String
     */
    @ApiOperation(value = "上传PDF发票")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "filePath", value = "PDF绝对路径", required = true, dataType = "String"),
            @ApiImplicitParam(name = "delFlag", value = "上传后是否删除", required = true, dataType = "String")
    })
    @PostMapping("uploadPdf")
    public String uploadPdf(@RequestParam String filePath, @RequestParam boolean delFlag) {
        return wxService.uploadPdf(filePath, delFlag);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:44
     * @Description 获取已上传发票（很少用）
     * @Param [mediaId]
     * @Return java.lang.String
     */
    @ApiOperation(value = "获取已上传发票")
    @ApiImplicitParam(name = "mediaId", value = "微信文件ID", required = true, dataType = "String")
    @GetMapping("getPdf")
    public String getPdf(@RequestParam String mediaId) {
        return wxService.getPdf(mediaId);
    }

    /**
     * @Author Zhouxw
     * @Date 2020/09/30 14:44
     * @Description 插入微信卡包操作
     * @Param [orderId, invoice, cardId, goods]
     * @Return java.lang.String
     */
    @ApiOperation(value = "插入卡包")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderId", value = "订单号码", required = true, dataType = "String"),
            @ApiImplicitParam(name = "invoice", value = "发票信息", required = true, dataType = "Invoice"),
            @ApiImplicitParam(name = "cardId", value = "卡券模板ID", required = true, dataType = "String")
    })
    @PostMapping("insertCard")
    public String insertCard(@RequestParam String orderId, @RequestBody Invoice invoice, @RequestParam String cardId) {
        return wxService.insertCard(orderId, invoice, cardId);
    }
}
