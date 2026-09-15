# 悦家服务

悦家服务平台单仓库快照，包含服务人员端 App、PC 管理端和用户端微信小程序三端应用，以及后端微服务。

## 目录

- `jzo2o-api`：微服务接口定义
- `jzo2o-aigc`：独立 AIGC 微服务骨架
- `jzo2o-customer`：用户与服务人员相关业务
- `jzo2o-foundations`：服务、区域等基础业务
- `jzo2o-framework`：公共框架组件
- `jzo2o-gateway`：API 网关
- `jzo2o-market`：营销业务
- `jzo2o-orders`：订单业务
- `jzo2o-publics`：公共业务服务
- `jzo2o-trade`：交易业务
- `project-xzb-app-uniapp-java`：服务人员端 App
- `project-xzb-pc-admin-vue3-java`：PC 管理端
- `project-xzb-xcx-uniapp-java`：用户端微信小程序

## 配置说明

本仓库不提交本地环境配置、私有小程序配置、密钥或构建产物。数据库、Nacos 和第三方服务凭证应通过部署环境或私有配置中心提供。
