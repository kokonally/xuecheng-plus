package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;

import java.util.List;

/**
 * 课程分类信息业务层
 */
public interface CourseCategoryService extends IService<CourseCategory> {

    /**
     * 查询分类节点
     * @return list
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
