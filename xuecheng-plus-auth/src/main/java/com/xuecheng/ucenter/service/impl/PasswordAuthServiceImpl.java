package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * username 和 password认证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //1.校验验证码
        //远程调用验证码服务区校验验证码
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();
        if (StringUtils.isEmpty(checkcodekey) || StringUtils.isEmpty(checkcode)) {
            throw new RuntimeException("验证码输入错误");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (BooleanUtils.isFalse(verify)) {
            throw new RuntimeException("验证码输入错误");
        }

        //2.校验账号是否存在
        String username = authParamsDto.getUsername();
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getUsername, username);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);

        //3.用户不存在返回 null即可，spring security就会抛出异常用户不存在
        if (xcUser == null) {
            throw new RuntimeException("账号或密码错误");
        }

        //3.验证密码是否正确
        boolean matches = passwordEncoder.matches(authParamsDto.getPassword(), xcUser.getPassword());
        if (!matches) {
            throw new RuntimeException("账号或密码错误");
        }

        XcUserExt xcUserExt = new XcUserExt();
        xcUser.setPassword(null);
        BeanUtils.copyProperties(xcUser, xcUserExt);

        return xcUserExt;
    }
}
