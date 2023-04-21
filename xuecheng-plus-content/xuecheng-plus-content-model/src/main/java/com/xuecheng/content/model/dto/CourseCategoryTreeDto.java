package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    //子节点
    @ApiModelProperty("子节点")
    private List<CourseCategoryTreeDto> childrenTreeNodes;

}
