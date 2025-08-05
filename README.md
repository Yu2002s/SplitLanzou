# SplitLanzou

**第三方蓝奏云**

> 特别说明：本项目为**蓝奏云(lanzou.com)的第三方客户端**，本项目不存储任何文件和资源。
> 任何用户二次开发本项目行为，**本项目概不负责**。
> **请勿将本项目用于非法用途，否则一切后果由使用者自己承担。如本项目侵犯了原作者的版权，请立即联系我删除。**

## 下载

### 1.最新版

> **以下版本不会自动更新，需要手动前往 github gitee 首页进行更新，如存在问题请提 issue**

1. [网盘下载](https://lzy.jdynb.xyz/share/folder/b041xpw2d/2fgt) *推荐* (密码：2fgt)
2. [github release](https://github.com/Yu2002s/SplitLanzou/releases) **可能无法访问**
3. [gitee release](https://gitee.com/jdy2002/SplitLanzou/releases) 国内仓库

### 2.其他版本下载

> 以下版本比本项目**功能更加完善**，可以辅助本项目使用。但是**可能存在问题**，不会发布正式更新，后期可能修复问题，可在
> App 内加入交流群获得更新

1. **可能存在问题** [LanzouCloud 下载](https://github.com/Yu2002s/LanzouCloud)
   |
   [云盘下载](https://jdy2002.lanzoue.com/b041496oj) (密码: 123456)

2. **下载目前有问题** [雨盘下载](https://jdy2002.lanzoue.com/b040cdb5g) (密码: hyf3)

## 项目

编译此项目需要的一些修改

### 本项目依赖环境：

1. jdk21
2. gradle8.11.1 +
3. AndroidStudio 2024.2.2 + (AS 版本太高，jdk 也高，需要手动设置 jdk 版本)

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

~~1. 没有文件选择器上传文件~~

> 解决办法: 使用外部文件管理器分享(可多选)文件，选择 SplitLanzou 上传文件。
> 或者使用 [雨盘下载](https://jdy2002.lanzoue.com/b040cdb5g) (密码: hyf3)

~~2. 文件列表没有图标~~

> 懒得做了

~~3. 可以分享 100M 文件嘛?~~

> 支持，但是新用户注册的，只有一次下载机会，第二次网页下载会报错，需要对方也下载此 App

其他方面问题，请通过 issue 方式告诉我，并尽量附带问题截图

## 截图

文件管理截图，支持上传、下载、分享并显示**100m+**文件

<img src="https://s1.ax1x.com/2023/08/03/pPFeucj.png"  align = "center" width="30%"  alt="文件列表"/>
<img src="https://s1.ax1x.com/2023/08/03/pPFen3Q.png"  align = "center"  width="30%" alt="" />
<img src="https://s1.ax1x.com/2023/08/03/pPFem9g.png"  align = "center"  width="30%" alt="" />

## 分享文件

App 内置了自定义分享功能，由于目前蓝奏云限制手机端分享文件，需要开通会员，
再加上**100M以上文件不能直接通过分享文件方式下载**，所以需要使用内置的分享地址

分享地址参考: `http://lzy.jdynb.xyz/share/file/分享id/分享密码(没有留空)`

> 分享id：原始分享地址末尾最后的几个字符(最后/后面的字符)

> 参考实际地址: http://lzy.jdynb.xyz/share/file/ik9Af2y9iaji

> 注意：这里的分享可能**不太稳定**，还是建议接收者也下载 `SplitLanzou`

## 分享文件夹

文件夹目前也支持

分享地址参考: `http://lzy.jdynb.xyz/share/folder/分享id/分享密码(没有留空)`

> 分享id：原始分享地址末尾最后的几个字符(最后/后面的字符)

> 参考实际地址: http://lzy.jdynb.xyz/share/folder/ik9Af2y9iaji

## 关于

1. 开发时间较短，可能有一些 bug，欢迎提 issue。
2. App 更新地址为 Github 的地址，网络被墙将无法获取更新，所以请手动前往本页更新
3. 代码全部开源，不用担心后门问题。
