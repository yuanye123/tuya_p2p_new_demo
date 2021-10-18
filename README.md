## P2P SDK说明
p2p sdk集成了iot功能与p2p功能

## P2pJniApi 说明
### 初始化sdk
```java
 /**
 * sdk初始化
 * @param pid 产品id
 * @param uid  uid
 * @param key key
 * @param path  存储路径,类似"/sdcard/"
 * @param version  版本名
 * @param netcardName 网卡名
 * @return 0 成功
 *         非0 失败
 */
    external fun initSdk(
            pid: String, uid: String, key: String, path: String,
            version: String, netcardName: String
            ): Int
```
### 初始化p2p
```java
    /**
 * 初始化p2p
 * @return 0 成功
 *         非0 失败
 */
    external fun initTransP2p(): Int
  ```
### 关闭p2p连接
```java
    /**
 * 关闭p2p连接
 */
    external fun closeTransP2p()
```
### 多个dp上传接口
```java
 	/**
 * 多个dp上传接口
 * @param hasTime 是否使用时间戳
 * @param events  上传dp数组
 * @return 0 成功
 *        非0 失败
 */
    external fun do_report(hasTime: Boolean, events: Array<DPEvent>): Int
```
### 上传单个dp
```java
	/**
 * 上传单个dp
 * @param dpi dip
 * @param type dp类型
 * @param value dp数据
 * @param time 时间戳
 * @return 0 成功
 *         非0 失败
 */
    external fun do_report(dpid: Int, type: Int, value: Any?, time: Int): Int
```
### 监听的mq协议类型
```java
	 /**
 * 监听的mq协议类型
 * @param protocol  协议类型
 * @return 0 成功
 *        非0 失败
 */
    external fun regist_mqtt_msg(protocol: Int): Int
```
### 上传mq message
```java
/**
 * 上传mq message
 * @param protocol 协议类型
 * @param msg 消息
 * @return 0 成功
 *        非0 失败
 */
    external fun send_mqtt_msg(protocol: Int, msg: String): Int
```
### Http请求
```java
	/**
 * Http请求
 * @param apiName
 * @param  apiVersion
 * @param jsonMsg
 */
    external fun http_request(apiName: String, apiVersion: String, jsonMsg: String): HttpResponse
```
### 获取 deviceId
```java
	/**
 * get deviceId
 * @return
 */
    external fun getDeviceId(): String
```
###重置设备
```java
 	/**
 * unreset device
 * @return ret
 */
    external fun unActive(): Int
```
### p2p sdk回调
```java
interface P2pCallBack {
    /**
     * 云端解绑设备回调
     */
    fun onReset()

    //
    /**
     * 收到配网二维码短链
     * @param url 二维码连接
     */
    fun onShorturl(url: String?)

    //接收到的图视频数据
    fun recvVideData(
            sessionId: Int,
            status: RecvStatus,
            buf: ByteArray,
            len: Int,
            downloadAlbumHead: Download_Album_Head,
            c2cCmdIoCtrlAlbumDownloadStart: C2C_CMD_IO_CTRL_ALBUM_DOWNLOAD_START
    )

    /**
     * mq网络状态变化
     * @param
     */
    fun onNetStauts(status: Int)

    /**
     * mqtt消息回调
     * @param protocol 协议号
     * @param msgObj 消息
     */
    fun onMqttMsg(protocol: Int, msgObj: JSONObject?)

    /**
     * dp事件接收
     *
     * @param event 事件类型
     * DPEvent.Type.PROP_BOOL
     * DPEvent.Type.PROP_VALUE
     * DPEvent.Type.PROP_STR
     * DPEvent.Type.PROP_ENUM
     * DPEvent.Type.PROP_BITMAP
     * DPEvent.Type.PROP_RAW
     */
    fun onDpEvent(event: DPEvent)
}
```

### p2p sdk升级回调
```java
interface UpgradeEventCallback {

    /**
     * sdk 接收到后端的升级推送的时候，会触发此接口 附带升级信息
     * @param version
     */
    void onUpgradeInfo(String version);

    /**
     * 升级文件开始下载
     */
    void onUpgradeDownloadStart();

    /**
     * 升级文件下载进度
     */
    void onUpgradeDownloadUpdate(int progress);

    /**
     * sdk 下载升级文件下载完成触发此接口
     */
    void upgradeFileDownloadFinished(int resultCode, String file);
}
···

### 发送多个dp事件
```java
	/**
     * 发送多个dp事件
     *
     * @param events 多个dp类型
     * @return 0 成功
     *         非0 失败 
     */
    fun sendDP(events: List<DPEvent>): Int
```

### 开始升级
```java
/**
     * start upgrade download
     * @return 0 sucess
     *         not 0 failed
     */
    external fun start_upgradeDownload():Int
```


