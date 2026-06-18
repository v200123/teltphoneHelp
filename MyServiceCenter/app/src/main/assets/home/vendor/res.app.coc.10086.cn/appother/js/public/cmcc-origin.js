/**
 * js-sdk
 *
 * 错误码只进行大的分类，详细错误通过错误描述字段表述。
 * -1： 系统错误（系统接口错误，内存错误，异常错误）
 * -2： 网络错误（网络接口错误，网络数据错误）
 * -3： 数据错误（非合法数据）
 * -4： 用户取消操作（部分接口用户干预的操作）
 */
(function () {
  var u = navigator.userAgent;
  var android = u.indexOf("Android") > -1;
  var isGtVersion = (function () {
    // 判断版本是否大于等于7.8  是返回值为true 否则为false
    var matchStr = u.match(/leadeon\/[\d\.]+\/CMCCIT/gi);
    if (matchStr) {
      return Number(
        String(matchStr[0].replace(/(leadeon\/)([\d\.]+)(\/CMCCIT)/gi, "$2"))
          .split(".")
          .splice(0, 3)
          .join("")
      ) >= 780
        ? true
        : false;
    }
    return false;
  })();
  //用于对外暴露,init只有在客户端为4.0版本才有
  var statementObj = function () {
    return {
      init: function () {},
      voiceRecogResult: function () {},
      load: function () {
        window.location.reload();
      },
    };
  };
  var leadeon = (cmcc = statementObj());
  // var cmcc = statementObj();
  //判断为ios的wkwebview就吧leadeon.init赋空
  if (u.indexOf("wkwebview") > -1) {
    leadeon.init = cmcc.init = null;
  }
  //各种能力的集合
  var businessNameS = [
    "userStatus", //获取用户登录状态
    "checkSessionIsvalid", //新登录状态接口（会话校验）--V4.0新增
    "getUserInfo", //获取用户基础信息
    "getCallDetail", //获取详单数据
    "qrScan", //直接进入扫一扫功能
    "getContacts", //调用手机通信录
    "sendSMS", //调用系统发短信功能
    "getContactName", //获取通信录联系人姓名
    "newWebview", //打开新页面
    "playSound", //手机振动
    "getNetStatus", //获取网络状态
    "pickPhoto", //调用照相机
    "callPhone", //拨打手机电话
    "goNativePage", //H5调用native功能页面
    "getUserMarketInfo", //查询用户营销活动信息
    "getImei", //获取 IMEI
    "enableShared", //是否显示分享按钮
    "shareMessage", //分享功能
    "payHistory", //查询交费历史
    "getOCR", //扫描充值卡密码(OCR)
    "searchBizHall", //查询实体营业厅
    "nearBizHall", // 附近营业厅
    "encryptString", //加密功能
    "newGuide", //新手引导
    "giveMeScore", //给我评分
    "checkVersion", //版本检测
    "showAuthentication", //调用二次鉴权,说明：用户需要操作敏感用户信息时，需要进行二次鉴权操作
    "getContactAll", //获取通信录信息,说明：获取通信录中所有人的手机号码和姓名
    "setWaterFlowerState", //通知客户端浇花签到状态
    "savePhoto", //保存图片到相册
    "digitalSignature", //网络请求体数字签名
    "showLogin", //调用登录接口
    "getNewPay", //拉起统一支付
    "setTitle", //设置webView标题-V3.8新加
    "assistiveControl", //二级页面情感化插件-V3.8新加
    "showNavBackAndCloseBtn", //导航栏返回及关闭按钮的显示状态-V3.8新加
    "getCatchInfo", //客户端信息采集-V3.8新加
    "closeCurrentWebView", //关闭当前webview-V3.8新加
    "cleanCache", //清除缓存webview-V3.8新加
    "navigationColorSetting", //设置导航栏颜色 -V4.0新增
    "getClientApplicationList", //获得本机所有应用的列表（-V4.0新增，只有安卓有这能力）
    "openApplication", //打开应用（-V4.0新增，只有安卓有这能力）
    "downloadApplication", //调用下载（-V4.0新增）
    "startPlugin", //调起客户端插件（V4.0新增）
    "backToRootView", //用于任意二级页面返回一级页面 -V4.1新增
    "overTime", //会话超时处理接口--V4.0新增,
    "tollSingle", //单点统计 -V4.1新增
    "mainPageUpdate", //首页刷新 -V4.1新增
    "imagesShare", //多图分享 -V4.1新增
    "quitLogin", //注销登录 -V4.1新增
    "voiceRecognizer", //设定是否支持语音识别，在键盘弹出时提供入口 -V4.2新增
    "showNavCloseBtn", //导航栏关闭按钮的显示状态-V4.3新加
    "showNavBackBtn", //导航栏返回按钮的显示状态-V4.3新加
    "getIDMPLoginToken", // 获取一键登录token(获取当前登录号码的token)-V4.5新加
    "getLocalNumberLoginToken", // 获取一键登录token(获取当前本机号码的token)-V4.5新加
    "openOtherAPP", // scheme打开其他应用-V4.5新加
    "checkUID", //Uid有效性校验 V4.7.0新增
    "package4G", //4G+终端套餐返回  V4.7.0新增
    "getLoginInfo", //获取登录信息-V4.8 新增
    "cmccSetUp", //CMCC配置-V4.8 新增
    "rescanQRCode", //PC 扫码登录功能专用JS能力,重新扫描--V4.8 新增
    "backToHostApp", //关闭当前页面--V4.8 新增
    "hejiaqin", //打开和家亲功能页面--V5.0 新增
    "hiddenShareButton", //隐藏分享按钮-V5.1 新增
    "checkSessionIsvalidForJFLib", //H5单独调用新登录状态接口（会话校验）--V5.2 新增
    "hebaoPay", //拉起和包支付--V5.3 新增
    "speechBroadcast", //语音播报-V5.5 新增
    "forbiddenWebviewScrollStyle", //禁止浏览器上滑回弹效果-V5.5 新增
    "hiddenNavigationBar", //是否隐藏导航栏（即全屏webview）-V5.5 新增
    "oneKeyAlarm", //一键报警sdk初始化
    "getDeviceMultiSimInfo", //一号一终端 获取设备硬件信息--V5.6 新增
    "newVoiceRecognizer", //语音识别（无界面） -V5.6 新增
    "openMiniProgram", //H5页面打开小程序-V5.6 新增
    "alipaySDKauthV2", //支付宝授权登录 -V5.6 新增
    "AISpeechUnderStander", //智能语义理解 -V5.7 新增
    "audioRecord", //本地录音 -V5.7 新增
    "isInstall", //判断应用是否安装 -V5.8 新增
    "openHeBaoURL", //打开和包下载地址 -V5.8 新增
    "forbiddenWebviewScrollBack", //禁止webview滑动返回效果-V5.8.2 新增,只iOS可用
    "imageRecognitionLists", //获取客户端图像识别支持的识别种类 -V6.0新增
    "imageRecognition", //调用客户端图像识别 -V6.0新增
    "trashRecognition", //AR垃圾分类-V6.0新增
    "startVoipVoiceOrVideo", //宽带视频客服sdk 调用(6.2新增)
    "forbiddenWebp", //禁止webphttps及webphttp拦截 调用(6.1.5新增)
    "imageRecognitionConfig", //通知客户端本次识别目标 -V6.2新增
    "showQuestionnaire", //问卷调查调用与展示 (6.3新增)
    "replyAfterQuestionnaireSummit", //问卷调查提交成功反馈 (6.3新增)
    "deviceLoginAnalyse", //设备登录风控分析-6.3新增
    "userLoginAnalyse", //用户标签风控分析-6.3新增
    "getNetInfo", //获取网络及运营商信息-6.3新增
    "getTitleHeight", //获取页面标题栏加状态栏高度-V6.4 新增
    "openLiveRoom", //用户端打开直播间-6.4新增
    "getFloatWindowStatus", //获取浮窗任务完成情况 -V6.4 新增
    "setNavigationFuncAtSecRightBtn", //导航栏定制指定的功能-V6.5新增
    "openLivePlayback", //直播回放-V6.5新增
    "getFileDiskInfo", //和彩云SDK获取个人云空间 -V6.6 新增
    "getFamilyDiskInfo", //和彩云SDK获取家庭云空间 -V6.6 新增
    "queryUserBenefits", //和彩云SDK获取用户权益 -V6.6 新增
    "pauseVolumeWhenPageDisappear", //页面不可见时暂停声音-V6.6 新增
    "arForHtml5", //Ar活动页面调用-V6.7 新增
    "userHeadIcon", //用户头像获取-V7.1 新增
    "openOnlineService", //打开在线客服 调用(V7.1新增)
    "savePDFCloud", //保存到和彩云盘(7.3新增)
    "isGotoneGuideShow", //全球通H5首页引导蒙版展示开关(7.3新增)
    "getCareModeSetting", //长辈版页面支持设置播放语速以及更改字号大小。(7.4新增)
    "isAccessibilityOpen", //判断系统无障碍功能是否开启(7.4新增)
    "getNetSetting", // 拉起系统移动网络设置(7.5新增)
    "CMCCFloatWindow", //签到反向引流增加浮窗(7.5新增)
    "isVoipVoiceOrVideoCanuse", //音视频场景是否可用(7.6新增)
    "getYDRZToken", //大网认证  （V7.5.6新增）
    "openMiguAR", //拉起咪咕AR(7.6新增)
    "getLocalHomeWebViewVersion", //获取首页H5蒙层版本信息(7.7新增)
    "setLocalHomeWebViewVersion", //设置首页H5蒙层版本信息(7.7新增)
    "getPromptInfo", //客户端故障说明提示内容(7.8新增)
    "saveScreenShotToAlbum", //保存当前页面截图到相册(7.8新增)
    "addCalendarSchedule", //添加日历日程(7.8新增)
    "queryCalendarSchedule", //查询日历日程(7.8新增)
    "deleteCalendarSchedule", //删除日历日程(7.8新增)
    "checkNotificationEnabled", //检查系统通知权限开关状态(7.8新增)
    "showRequestNotificationDialog", //申请开启系统通知权限弹窗(7.8新增)
    "showRealTimeScenePopup", //场景弹框(9.2.0新增)
    "jumpHCYTelephoneBillPage", // 跳转到我的卡包
    "createPassWordCode", //口令码生成(7.8新增)
    "copyToClipboard", //写入剪贴板(7.8新增)
    "hcyCheckLogin", //检测和彩云是否登录成功(7.9新增)
    "hcyQueryDynamic", //查询和彩云卡片描述文案(7.9新增)
    "videoVerify", //视频核验(8.0新增，小程序用)
    "jsADReport", //江苏广告上报(8.0.0新增)
    "openCmccPayChannel", //拉起支付渠道
    "navigationToFirstPageTab", //跳转到APP一级页面指定Tab(8.1.0新增)
    "clearAnnouncementUnreadNumber", //消息中心一键清除移动公告未读数(8.1.0新增)
    "saveDataToNative", //H5存储数据至原生（8.1.0新增）
    "getDataFromNative", //H5获取存储在原生的数据（8.1.0新增）
    "jsADAPIDownloadOpenApp", //江苏广告下载(8.0新增)
    "onPageActive", //客户端在H5页面重新展示给用户时回调(8.1.0新增)
    "fetchStepCount", //获取步数
    "getShake", // 摇一摇
    "addCalendarScheduleReminder", // 分省活动添加日历日程(8.3.0新增)
    "queryCalendarScheduleReminder", // 分省活动查询日历日程(8.3.0新增)
    "deleteCalendarScheduleReminder", // 分省活动删除日历日程(8.3.0新增)
    "gotoOtherSdkWithRouter", //(8.3.0新增，仅安卓)暂只支持和彩铃
    "facialVerification", //人脸识别实名认证JS能力支持(8.4.0新增)
    "saveFileCloud", //保存文件到中国移动云盘(8.4.0新增)
    "updateBalanceStatus", //通知原生探索页刷新和包余额展示状态(8.4.0新增)
    "enableBusinessCorner", // 设置二级H5页面客服按钮是否显示退订角标(8.4.0新增)
    "getCmccSimInfo", // 获取卡槽号码端能力(8.4.0新增)
    "electronicInvoiceOpeningMethod", // 电子发票系统打开方式(8.50新增)
    "videoPlay", // 视频播放器js能力需求JS接口
    "getSearchFlag", //首页搜索返回能力
    "showNavCustomBtn", //二级页面顶部导航栏改造
    "intentSztSdkPage", //江.苏(苏证通SDK调用)S能力支持
    "fetchMiguVideoPlayUrl", //首页视频专区 获取播放地址（8.7.0新增）
    "toggleCustomFloor", //设置页面开启个性化模块入口(8.8.0新增)
    "openBusNFCWritePage", //公交卡读卡
    "networkBackup", // 账户信息容灾能力(8.6.0新增)
    "toggleDigitalMan", // 数字人生(8.5.0新增)
    "showTitleBarRightIcons", //标题栏右上角触点支持配置
    "fetchMiguVideoData", // 首页获取视频数据（8.7.0新增）
    "pushMiguVideoPlay", // 跳转播放器页面（8.7.0新增）
    "isPageUrlCollected", //查询页面是否已收藏
    "addOrDeleteCollect", //页面添加或取消收藏
    "lookFile", //客户端下载查看能力
    "enableRefresh", //二级页面下拉刷新
    "disableScreenCapture", // 敏感信息界面禁止截录屏(9.0.0新增)
    "permissionIsEnable", // 获取权限是否开启 (9.0.0新增)
    "jumpMiguVideoListActivityByAreaId", //跳转咪咕视频SDK列表集合页面 (9.0.5新增)
    "addQuickWay", //添加桌面快捷方式
    "getSystemPermissionOpenOrNot", // 获取权限是否开启
    "getRiskLevel", // 风控(9.0.5新增)
    "showBottomFloatView", // 智能调度浮层(9.1新增)
    "webViewLog",
    "isActivated",
    "goOpenCardView",
    "goRechargeView",
    "goTradeRecordView",
    "goOrderListView",
    "getCardNumber",
    "getCardBalance",
    "isBlueCardPro",
    "getUpdateInfo",
    "goRightsActivity",
    "getAppLaunchLog",
    "getMultipleInfo", //获取多个js能力聚合信息
    "openDuoWanGame", //拉起掼蛋游戏sdk(9.1.0新增)
    "playRewardVideo", //播放穿山甲、优量汇激励视频(9.1.0新增)
    "jumpVideoEnginePage", //跳转江苏5G云台席SDK销广号码排队取号页面(9.2.新增)
    "openBrowser", // 打开系统浏览器（9.1.0新增）
    "getLastPageCode", //江苏用户获取历史页面访问记录(9.3.0新增)
    "startMetaverseGame", // 920江苏元宇宙游戏SDK
    "openSettingForCalendarOfPermissions", // 日历权限申请(9.3.0新增)
    "quickShare", // 视频分享 V9.5
    "navigationToExplore", //探索页活动引流数据区分(9.0.0新增)
    "jumpAuditDetectPrivacyPage", // 跳转甘肃启明星辰- 隐查查SDK页面 (9.6.新增)
    "getStatusBarHeight", // 获取手机状态栏高度 (9.6.0新增)
    "switchNormalPage", // 英文版H5切回app普通页 (9.6.0新增)
    "jumpHCYPhotoBackupPage", //APP V9.5版本和彩云(云盘)SDK
    "getIDFA", //获取IDFA信息 (9.5.0新增)
    "getConcatenationInfo", // 串联需求,获取串联信息(9.8.0新增)
    "getLocation", // 串联需求,获取串联信息(9.8.5新增)
    "getTempAuthInfo", // 登录授权获取临时codeis能力,获取授权信息
    "fetchCameraCaptureData", // 浙江探索页更新SDK 997
    "getTypePaymentPassesInfo", // 中国移动掌厅接入NFC公交卡(996)
    "canAddPaymentPass", // 中国移动掌厅接入NFC公交卡(996)
    "switchLogin",//切换登录状态
    "presentAddPaymentPass", // 中国移动掌厅接入NFC公交卡(996)
    "verifyCardIssuerData", // 中国移动掌厅接入NFC公交卡(996)
    "customerServiceGuidanceToggle", // 智能调度策略客服引导浮条展开收缩(v10.0.0 新增)
    "setOrientationAndFullScreen", // 设置页面横竖屏模式(10.1.0新增)
    "closeWindow", // 关闭当前网页窗口
    "getMobileCardSlotInformation", // 获取移动手机卡槽信息(10.2.0新增)
    "getRecordingOrScreenshotStatus", // 监听获取录制或截图状态(10.2.0新增)
    "jumpMcsCloudContactsBackUpActivity", //跳转中国移动云盘通讯录功能
    "setConcatenationInfo", //串联轨迹日志采集数据优化:端能力接口
    "webMsgUnread", //消息中心web更新未读数 (10.3.0 新增)
    "isShowMessageReceivingReminder", // 是否展示未开启消息接收管理提示
    "saveLastTimeData", // 数据存储js 9.6
    "getLastTimeData", // 数据查询js 9.6
    "getPersonalInformationCollectData", //获取个人信息收集数
    "jumpMGMusicHomePage", //跳转咪咕音乐SDK首页JS能力(10.4 新增)
    "welfareWidgetAddStateMessage",//新增桌面福利小组件及刷新机制优化需求
		"exitTinyScene",//小程序退出拦截增加弹窗需求
		"addWidgetInApp",//添加小组件到桌面的js能力(v10.4.0 新增)
		"getHasCityChoose",//app首页是否为切换城市
		"getMultiLanguageInfo",//获取多语言的信息
		"multiLanguageGoBack",//多语言页面返回 (10.4.0 新增)
    "getBottomNavigationHeight",//H5新增获取底部导航栏高度能力
    "openJsPayChannel",//(8.5新增)
    "showRealTimeScenePopup",//场景弹窗showRealTimeScenePopup能力(9.2.0新增)
    "cloudPullBroadcast",//传递下载地址至客户端
    "showMsgSubscribeDialog",//新建消息订阅公共能力(v11.3.0新增)
    "getMsgSubscribeData",//同步订阅能力JS
    "getMNNNativeDataProcess",//端智能业务场景触发推理(11.5新增)
    "handleDynamicIslandMessage",//ios实时活动时序图 
    "dataPlanAccountUpdatedWithInfo",//一号一终端测试用
    "getIsOtherNetworkStatus",//当前用户是否为异网用户
    "operateNativeData",//新增清除原生缓存的端能力
    "isAvailableEsim",//检验设备是否支持 esim
    "provisioningEsim",//esim能力提供
    "serviceModule",//服务功能端能力
    "makePhoneCallnew",//首页视频和直播js
    "multAccountLogin",//多账户H5
    "getNativeTitleBarStatus",//权益获取导航栏高度及状态
    "installApplication",// 安装指定路径的apk
    "mapPOISearch",//V11.9.4.0 获取POI周边搜索数据
    "pickMaskPhoto",//调用照相机能力优化
    "getIsFoldable",//鸿蒙折叠屏端能力
    "openJKLiveFace",//跳转金科活体检测功能
    "nearbyCell",//英文版页面用户信息采集
    "getVPNStatus",//获取设备VPN状态端能力
    "getOCRContent",//获取图片资源识别结果
    "getAttachedDeviceESIMInfo", // android新增从厂商获取ESIM功能
    "downloadESIMProfile", // android将卡数据的激活码传输给eSIM⼿机
    "getEsimDeviceInfo", // hamony 获取esim信息
    "downloadEsimConfig", // hamony 将卡数据的激活码传输给eSIM⼿机
    "enterSwitchLogin", //V12.0.3.0 进⼊切换账号⻚⾯进⾏账号切换
    "openShandongPrint", // 山东排队打印(12.0.4 新增)
    "showNotificationPopup", // 消息中心开启通知弹窗
    "openNotificationTypes", // 消息中心提醒开启通知
    "navigateTo", //V11.9.6.0 统一跳转
    "jumpSystemPermissionSetting", // 跳转手机系统权限设置
    "showLoading", // 加载展示顶部进度条
    "hideLoading", // 加载去除顶部进度条
    "addContactWithAddressBook", // 添加单个联系人到通讯录（拉起通讯录，包含新建联系人和添加到已有联系人，根据是否传id来区分）(12.1.0 新增)
    "addContacts", // 批量添加联系人到通讯录(12.1.0 新增)
    "openAppSystemSet", // 新增打开app系统设置页面(12.0.8 新增)
    "takeScreenshotsListener", //新增监控录屏截屏JS能力(12.0.6 新增)
    "phoneNumberStatusChange", // 新增号码状态变更查询(12.1.0 新增)
    "mcsGetCameraPermission", // 新增获取相机权限能力，没权限弹出申请弹窗，点击跳转设置页面(12.1.2 新增)
    "readDataFromMessageNotificationCache", // 新增消息通知缓存读取JS能力(12.0.8 新增)
    "writeDataToMessageNotificationCache", // 新增消息通知缓存写入JS能力(12.0.8 新增)
  ];
  /**
   * 设置回调成功的函数,并给客户端发送json数据,并构造leadeon结构体
   * @param {Object} os 前端传递的对象
   * @param {String} st 业务名称
   */
  function callNative(os, st) {
    if ("undefined" === typeof os) {
      os = {};
    }
    os.businessName = st;
    var callbackRandom = ''
    if (os.businessName === 'saveDataToNative' || os.businessName === 'getDataFromNative' || os.businessName === 'getYDRZToken' || os.businessName === 'serviceModule'|| os.businessName ==='getUserInfo'||os.businessName ==='userStatus'||os.businessName ==='checkSessionIsvalid') {
      callbackRandom = "_" + String(Date.now()) + Math.floor(Math.random() * 1000);
    }
    os.callbackSuccess = "callbackSuccess_" + os.businessName + callbackRandom;
    os.callbackError = "callbackError_" + os.businessName + callbackRandom;
    if ("undefined" === typeof os.debug) {
      os.debug = false;
    }
    if ("undefined" === typeof os.success) {
      os.success = function () {};
    }
    if ("undefined" === typeof os.error) {
      os.error = function () {};
    }
    leadeon["callbackChangeSuccess_" + os.businessName + callbackRandom] = cmcc[
      "callbackChangeSuccess_" + os.businessName + callbackRandom
    ] = os.success;
    leadeon["callbackChangeError_" + os.businessName + callbackRandom] = cmcc[
      "callbackChangeError_" + os.businessName + callbackRandom
    ] = os.error;
    var commomSuccess = function (dates) {
      var jsonstr = dates;
      if (!!jsonstr && typeof jsonstr == "string") {
        if (android) {
          //android系统
          if (os.businessName != "hebaoPay") {
            try {
              if (!!jsonstr) {
                jsonstr = JSON.parse(jsonstr); //安卓不支持传递对象，需要对传递的字符串转换
              }
            } catch (error) {}
          }
        }
      }
      if (
        os.businessName == "getUserInfo" &&
        jsonstr.version &&
        parseFloat(jsonstr.version) >= 7.7
      ) {
        try {
          if (
            jsonstr.accessibilityOpen &&
            jsonstr.accessibilityOpen == "1" &&
            !!window._tag
          ) {
            // 无障碍模式开启
            _tag.setOtherParameter({
              cmd: "BarrierFree",
            });
          } else if (!!window._tag) {
            // 无障碍模式关闭
            _tag.setOtherParameter({
              cmd: "",
            });
          }
        } catch (error) {}
      }
      return jsonstr;
    };
    var commomError = function (dates) {
      var jsonstr = dates;
      if (!!jsonstr && typeof jsonstr == "string") {
        if (android) {
          //android系统
          if (os.businessName != "hebaoPay") {
            try {
              if (!!jsonstr) {
                jsonstr = JSON.parse(jsonstr); //安卓不支持传递对象，需要对传递的字符串转换
              }
            } catch (error) {}
          }
        }
      }
      return jsonstr;
    };
    var arr = ["leadeon", "cmcc"];
    for (var i = 0; i < arr.length; i++) {
        eval(arr[i])["callbackSuccess_" + os.businessName + callbackRandom] = function (dates) { 
          var jsonstrSuc =commomSuccess(dates);
          this["callbackChangeSuccess_" + os.businessName + callbackRandom](jsonstrSuc);
          // console.log(this.init)
        }.bind(eval(arr[i]));
        eval(arr[i])["callbackError_" + os.businessName + callbackRandom] = function (dates) {
          var jsonstrSuc = commomError(dates);
          this["callbackChangeError_" + os.businessName + callbackRandom](jsonstrSuc);
        }.bind(eval(arr[i]));
      }
        //传值完毕以后就删除多余的属性，没必要给客户端
  delete os.success;
  delete os.error;
  if (android) {
      newSdkInterface[os.businessName](JSON.stringify(os));
    } else {
      if (u.indexOf("wkwebview") > -1) {
        window.webkit.messageHandlers.newSdkInterface.postMessage(os);
      } else {
        newSdkInterface[os.businessName](os);
      }
    }
  
  }
  businessNameS.forEach(function (businessName) {
    leadeon[businessName] = cmcc[businessName] = function (options) {
      callNative(options, businessName);
    };
  });
  window.leadeon = leadeon;
  window.cmcc = cmcc;
})();
