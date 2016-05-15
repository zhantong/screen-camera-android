# Screen-Camera Android
## Screen-Camera简介
Screen-Camera可以利用手机摄像头传输文件。其原理是将需要传输的文件编码为动态二维码（即多个二维码组成的视频），由手机摄像头捕获到这些二维码后，解码还原为文件。由于整个传输过程只需要用到手机摄像头，因此而免去了如WiFi和蓝牙传输文件需要网络协议的支持，显得更为简单和便捷。
## Android版本
Android版本用来对接收到的二维码流进行解码，编码部分在[JAVA版本]中实现。

Android版本需要用到外部依赖包OpenRQ。
## 项目导入
Android采用Android Studio作为集成开发环境，可以在Android Studio中使用GitHub插件直接clone本项目，也可以在本项目页面下载zip包，解压后用Android Studio打开。

## Reference
- ReedSolomon编解码部分用到了[zxing]项目的代码
- RaptorQ的实现用到了[OpenRQ]项目

[JAVA版本]:https://github.com/zhantong1994/screen-camera-java
[zxing]:https://github.com/zxing/zxing
[OpenRQ]:https://github.com/openrq-team/OpenRQ
