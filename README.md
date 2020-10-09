# wx-api
微信开发中间服务

为了解决多个服务器端对接微信公众号后台配置的问题。

此服务为中间服务，对接微信接口，为多服务端提供分装后的微信接口。


首次部署时注意：

    1、删除根目录下的授权文件，导入最新的公众号授权文件MP_verify_XXX.txt；
    2、配置application.yml内wx节点下的参数信息，回调地址等；
    3、删除application.yml下spAppId的value值，重新获取后填充，可以在后期调用其他接口时免传该参数。
