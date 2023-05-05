package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.DeleteErrorException;
import com.xuecheng.base.exception.DeleteErrorResponse;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.xuecheng.base.exception.CommonError.PARAMS_ERROR;
import static com.xuecheng.base.exception.CommonError.UNKOWN_ERROR;
import static com.xuecheng.base.exception.ErrorCode.DELETE_TEACHPLAN_ERR;

@Service
@Slf4j
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        Long teachplanId = saveTeachplanDto.getId();
        //1.新增
        if (teachplanId == null) {
            insertTeachplan(saveTeachplanDto);
        } else {
            //2.修改
            updateTeachplan(saveTeachplanDto);
        }
    }

    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null) {
            XueChengPlusException.cast(PARAMS_ERROR);
        }
        if (teachplan.getParentid() == 0) {
            //1.删除根节点
            //查询是否存在子节点
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid, teachplanId);
            int nodeCount = teachplanMapper.selectCount(queryWrapper);
            if (nodeCount == 0) {
                //没有子节点可以删除
                teachplanMapper.deleteById(teachplanId);
            } else {
                //有子节点不能删除
                throw new DeleteErrorException("课程计划信息还有子级信息，无法操作", DELETE_TEACHPLAN_ERR);
            }
        } else {
            //2.删除子节点
            //删除视频信息
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            teachplanMediaMapper.delete(queryWrapper);

            //删除课程计划
            teachplanMapper.deleteById(teachplanId);
        }
    }

    //新增课程计划
    private void insertTeachplan(@Validated(ValidationGroups.Inster.class) SaveTeachplanDto saveTeachplanDto) {
        Teachplan teachplan = new Teachplan();
        BeanUtils.copyProperties(saveTeachplanDto, teachplan);

        //确定排序字段
        Long parentid = saveTeachplanDto.getParentid();
        Long courseId = saveTeachplanDto.getCourseId();
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid, parentid);
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper);
        //通过遍历来获得排序字段的最大值
        int maxOrderBy = 0;
        for (Teachplan t : teachplans) {
            maxOrderBy = Math.max(t.getOrderby(), maxOrderBy);
        }
        teachplan.setOrderby(maxOrderBy + 1);

        teachplanMapper.insert(teachplan);
    }

    //修改课程计划
    private void updateTeachplan(@Validated(ValidationGroups.Update.class) SaveTeachplanDto saveTeachplanDto) {
        //1.先查出来
        Teachplan teachplan = teachplanMapper.selectById(saveTeachplanDto.getId());
        if (teachplan == null) {
            XueChengPlusException.cast("该课程计划不存在");
        }

        //2.覆盖信息
        BeanUtils.copyProperties(saveTeachplanDto, teachplan);


        //3.修改
        int i = teachplanMapper.updateById(teachplan);
        if (i <= 0) {
            XueChengPlusException.cast(UNKOWN_ERROR);
        }
    }
}
