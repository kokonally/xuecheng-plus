package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划管理相关业务
 */
public interface TeachplanService {
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程id
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增修改保存课程计划信息
     * @param saveTeachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     * @param teachplanId 课程id
     */
    public void deleteTeachplan(Long teachplanId);

    /**
     * 向上向下移动
     * @param type movedown 下移 moveup上移
     * @param teachplanId 课程计划id
     */
    public void moveDownAndUp(String type, Long teachplanId);

}
