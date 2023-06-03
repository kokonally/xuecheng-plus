package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 微信扫码认证
 */
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.secret}")
    private String secret;

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private XcUserRoleMapper xcUserRoleMapper;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //得到账号
        String username = authParamsDto.getUsername();
        if (StringUtils.isEmpty(username)) {
            throw new RuntimeException("用户不存在");
        }
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getUsername, username);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if (xcUser == null) {
            throw new RuntimeException("用户不存在");
        }

        //查询用户信息
        XcUserExt xcUserExt = new XcUserExt();
        xcUser.setPassword(null);
        BeanUtils.copyProperties(xcUser, xcUserExt);

        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {
        //1.申请令牌
        Map<String, String> access_tokenMap = this.getAccess_token(code);
        String access_token = access_tokenMap.get("access_token");
        String openid = access_tokenMap.get("openid");

        //2.携带令牌，查询用户信息
        Map<String, String> userinfo = this.getUserinfo(access_token, openid);

        //3.保存用户信息
        WxAuthServiceImpl proxy = (WxAuthServiceImpl) AopContext.currentProxy();
        return proxy.addWxUser(userinfo);
    }

    @Transactional
    public XcUser addWxUser(Map<String, String> userinfo_map) {
        //1.查询用户信息
        String unionid = userinfo_map.get("unionid");
        String nickname = userinfo_map.get("nickname");
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getWxUnionid, unionid);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if (xcUser != null) {
            return xcUser;
        }

        //新增
        xcUser = new XcUser();
        xcUser.setId(UUID.randomUUID().toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setWxUnionid(unionid);
        xcUser.setNickname(nickname);
        xcUser.setName(nickname);
        xcUser.setUtype("101001");
        xcUser.setStatus("1");
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        //向用户角色关系表插入数据
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(xcUserRole.getId());
        xcUserRole.setRoleId("17");  //学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);

        return xcUser;
    }

    /**
     * 通过令牌查询用户信息
     * @param access_token 令牌
     * @param openid 微信用户唯一id
     *    https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * @return
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {
        String url_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(url_template, access_token, openid);

        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return JSON.parseObject(result, Map.class);
    }

    /**
     * 获取令牌
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * @param code 授权码
     * @return
     */
    private Map<String, String> getAccess_token(String code) {
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url = String.format(url_template, appid, secret, code);

        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();
        return JSON.parseObject(result, Map.class);
    }
}
