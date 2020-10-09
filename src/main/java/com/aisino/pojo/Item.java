package com.aisino.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 发票明细项
 * @Author Zhouxw
 * @Date 2020/9/30 0030 14:47
 **/
@Data
@ApiModel(value = "Item", description = "商品明细项")
public class Item {
    /**
     * 名称
     */
    @ApiModelProperty(value = "商品名称", dataType = "String", required = true)
    private String name;
    /**
     * 单价
     */
    @ApiModelProperty(value = "单价", dataType = "int", required = true, example = "1000表示10元")
    private Integer price;
    /**
     * 单位
     */
    @ApiModelProperty(value = "单位", dataType = "String")
    private String unit;
    /**
     * 数量
     */
    @ApiModelProperty(value = "数量", dataType = "int")
    private Integer num;
}
