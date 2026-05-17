package com.largeevent.management.model

enum class VerificationResultType(@JvmField val code: Int) {
    // 1-未识读出证件：通道设备未佩戴证件人员通行和闸机设备未注册的人脸比对产生
    NOT_DETECTED(1),
    
    // 2-无效证件：读到芯片，在此次活动中证件库查询不到
    INVALID_CERT(2),
    
    // 3-限制通行：关注人员或黑名单，BlackSign=1
    BLACKLIST(3),
    
    // 4-证件已注销：证件已注销（EventStatus=6）
    CANCELED(4),
    
    // 5-证件未激活：证件未激活（EventStatus<=4），该判断是可选
    NOT_ACTIVATED(5),
    
    // 6-无权通行：权限不足
    PERMISSION_DENIED(6),
    
    // 8-通过：正常通行
    PASS(8),
    
    // 9-请检查证件：人证不合一
    FACE_MISMATCH(9),
    
    // 10-未实名绑定：TP未实名绑定
    UNBOUND(10),
    
    // 已过期（状态码同无效证件）
    EXPIRED(2),
    
    // 需要绑定
    BIND_REQUIRED(12)
}

