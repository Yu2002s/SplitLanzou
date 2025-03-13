# SplitLanzou

突破上传文件限制，**分割上传文件，第三方蓝奏云**

> 特别说明：本项目为**蓝奏云(lanzou.com)的第三方客户端**，本项目不存储任何文件和资源。项目采用
> 分割文件方式达到上传储存目的，任何用户二次开发本项目行为，**本项目概不负责**。
**请勿将本项目用于非法用途，否则一切后果由使用者自己承担。如本项目侵犯了原作者的版权，请立即联系我删除。**

## 联系我

1. gmail：jiangdongyu54@gmail.com
2. qq: 2475058223@qq.com

## 下载

### 1.最新发行版(release)

> **以下版本不会自动更新，需要手动前往 github gitee 首页进行更新，如存在问题请提 issue**

1. [github release](https://github.com/Yu2002s/SplitLanzou/releases) **可能无法访问**

2. [gitee release](https://gitee.com/jdy2002/SplitLanzou/releases) **推荐**

### 2.其他版本下载

> 以下版本比本项目**功能更加完善**，可以辅助本项目使用。但是**可能存在问题**，不会发布正式更新，后期可能修复问题，可在
> App 内加入交流群获得更新

1. **可能存在问题** [LanzouCloud下载](https://github.com/Yu2002s/LanzouCloud)
   |
[云盘下载](https://jdy2002.lanzoue.com/b041496oj) (密码: 123456)

2. **下载目前有问题** [雨盘下载](https://jdy2002.lanzoue.com/b040cdb5g) (密码: hyf3)

## 项目

编译此项目需要的一些修改

### 本项目依赖环境：

1. jdk17
2. gradle8.0-bin (jdk不要超过17)
3. AndroidStudio 2024.2.2 (AS版本太高，jdk也高，需要手动设置jdk版本)

其他环境可能无法成功编译

### 修改签名

```gradle
// /app/build.gradle
android {
    signingConfigs {
        debug {
            // 修改签名信息
        }
        release {
            // ...
        }
    }   
}
```

## 问题

1. 没有文件选择器上传文件

> 解决办法: 使用外部文件管理器分享(可多选)文件，选择 SplitLanzou上传文件。
> 或者使用 [雨盘下载](https://jdy2002.lanzoue.com/b040cdb5g) (密码: hyf3)

2. 文件列表没有图标

> 懒得做了

3. 可以分享100M文件嘛?

> 支持，但是新用户注册的，只有一次下载机会，第二次网页下载会报错，需要对方也下载此App

## 截图

文件管理截图，支持上传、下载、分享并显示**100m+**文件

![image](https://s1.ax1x.com/2023/08/03/pPFeucj.png)
![image](https://s1.ax1x.com/2023/08/03/pPFen3Q.png)
![image](https://s1.ax1x.com/2023/08/03/pPFem9g.png)

## 关于

1. 开发时间较短，可能有一些bug，欢迎提 issue。
2. App不会自动更新，请前往本主页手动获取更新。
3. 代码全部开源，不用担心后门问题。