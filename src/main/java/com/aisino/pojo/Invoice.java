package com.aisino.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Description: 发票信息
 * @Author Zhouxw
 * @Date 2020/9/30 12:11
 **/
@Data
@ApiModel(value = "Invoice", description = "发票信息")
public class Invoice {
    /**
     * 开票金额
     */
    @ApiModelProperty(value = "开票金额", dataType = "String", required = true, example = "1000表示10元")
    private String fee;
    /**
     * 购方抬头
     */
    @ApiModelProperty(value = "购方抬头", dataType = "String", required = true)
    private String title;
    /**
     * 发票代码
     */
    @ApiModelProperty(value = "发票代码", dataType = "String", required = true)
    private String billingNo;
    /**
     * 发票号码
     */
    @ApiModelProperty(value = "发票号码", dataType = "String", required = true)
    private String billingCode;
    /**
     * 不含税金额
     */
    @ApiModelProperty(value = "不含税金额", dataType = "String", required = true)
    private String feeWithoutTax;
    /**
     * 税额
     */
    @ApiModelProperty(value = "税额", dataType = "String", required = true)
    private String tax;
    /**
     * 发票PDF地址ID
     */
    @ApiModelProperty(value = "发票PDF地址ID", dataType = "String", required = true)
    private String pdfMediaId;
    /**
     * 校验码
     */
    @ApiModelProperty(value = "校验码", dataType = "String", required = true)
    private String checkCode;
    /**
     * 销售方纳税人识别号
     */
    @ApiModelProperty(value = "销方纳税人识别号", dataType = "String", required = true)
    private String sellerNumber;
    /**
     * 商品明细列表
     */
    @ApiModelProperty(value = "商品明细列表", dataType = "List", required = true)
    private List<Item> items;
}
