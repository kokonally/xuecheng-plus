package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/**
 * 自定义UserDetailService
 */
@Component
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private XcMenuMapper xcMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将传过来的转为AuthParamDto对象
        AuthParamsDto authParamsDto;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证的参数不符合要求");
        }

        //1.获取认证类型
        String authType = authParamsDto.getAuthType();
        //根据类型取出指定的bean
        String beanName = authType + "_authservice";
        AuthService authService = (AuthService) applicationContext.getBean(beanName);

        //2.调用方法
        XcUserExt xcUserExt = authService.execute(authParamsDto);
        //将xcUserExt封装成UserDetails
        return this.xcUserExt2userDetails(xcUserExt);
    }

    private UserDetails xcUserExt2userDetails(XcUserExt xcUserExt) {
        String xcUserDetailsJson = JSON.toJSONString(xcUserExt);
        //授权
        String userId = xcUserExt.getId();
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(userId);
        if (xcMenus.isEmpty()) {
            xcMenus = Collections.emptyList();
        }
        return User.withUsername(xcUserDetailsJson).password("password").authorities(xcMenus.stream().map(XcMenu::getCode).toArray(String[]::new)).build();
    }
}
