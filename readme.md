# 背景分割特效集成
基于推流端，关于依赖和活动参考OpenCV分支
## 所做修改
- 目前背景分割特效集成到了MainLandScapeActivity上
- 为了方便测试，修改了默认Activity为SettingActivity，跳转到MainLandScapeActivity
- 修改了第三方库libWSLive中的代码
- 将segmentor（背景分割器），bgd（背景图片）作为参数传入RESVideoClient，实现背景替换
- 目前使用软解方式实现背景替换特效，帧数较低


