package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //1.查询数据库获得数据
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        List<CourseCategoryTreeDto> rs = new ArrayList<>();  //存储最终返回的
        //2.将list集合转成map集合
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos
                .stream().filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(CourseCategory::getId, value -> value, (key1, key2) -> key2));

        //3.处理元素
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            //获取父节点
            if (id.equals(item.getParentid())) {
                rs.add(item);
            }

            //将子节点存入父节点中
            CourseCategoryTreeDto parentNode = mapTemp.get(item.getParentid());  //获得当前节点的父节点
            if (parentNode != null) {
                if (parentNode.getChildrenTreeNodes() == null) {
                    parentNode.setChildrenTreeNodes(new ArrayList<>());
                }
                parentNode.getChildrenTreeNodes().add(item);
            }
        });
        return rs;
    }
}
