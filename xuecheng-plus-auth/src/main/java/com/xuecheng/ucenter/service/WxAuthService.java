package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * 微信扫码登录向微信服务器发送请求
 */
public interface WxAuthService {

    /**
     * 微信扫码认证、申请令牌、携带令牌查询用户信息，保存用户信息到数据库库
     * @param code
     * @return
     */
    XcUser wxAuth(String code);
}
