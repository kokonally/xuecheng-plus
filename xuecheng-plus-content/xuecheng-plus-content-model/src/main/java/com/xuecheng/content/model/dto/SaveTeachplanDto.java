package com.xuecheng.content.model.dto;

import com.xuecheng.base.exception.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 新增大章节小章节修改章节信息模型类
 */
@ApiModel
@Data
public class SaveTeachplanDto {
    /***
     * 教学计划id
     */
    @NotNull(message = "教学计划id不能为空", groups = {ValidationGroups.Update.class, ValidationGroups.Delete.class})
    @ApiModelProperty(value = "教学计划id", required = false)
    private Long id;

    /**
     * 课程计划名称
     */
    @NotBlank(message = "课程计划名称不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Update.class})
    @ApiModelProperty(value = "课程计划名称", required = true)
    private String pname;

    /**
     * 课程计划父级Id
     */
    @NotNull(message = "所属章节不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Update.class})
    @ApiModelProperty(value = "课程计划父级Id", required = true)
    private Long parentid;

    /**
     * 层级，分为1、2、3级
     */
    @NotNull(message = "层级不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Update.class})
    @ApiModelProperty(value = "层级，分为1、2、3级", required = true, example = "1")
    private Integer grade;

    /**
     * 课程类型:1视频、2文档
     */
    @ApiModelProperty(value = "课程类型:1视频、2文档", required = true)
    private String mediaType;


    /**
     * 课程标识
     */
    @NotNull(message = "所属课程不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Update.class})
    @ApiModelProperty(value = "课程标识", required = true)
    private Long courseId;

    /**
     * 课程发布标识
     */
    @ApiModelProperty("课程发布标识")
    private Long coursePubId;


    /**
     * 是否支持试学或预览（试看）
     */
    @NotBlank(message = "试学或浏览选项不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Update.class})
    @ApiModelProperty(value = "是否支持试学或预览（试看", required = true)
    private String isPreview;

}
