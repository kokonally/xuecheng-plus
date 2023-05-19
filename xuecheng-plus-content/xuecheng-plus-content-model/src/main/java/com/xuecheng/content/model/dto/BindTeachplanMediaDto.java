package com.xuecheng.content.model.dto;
import com.xuecheng.base.exception.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 绑定媒资和课程计划的模型类
 */
@Data
@ApiModel(description = "教学计划-媒资绑定提交数据")
public class BindTeachplanMediaDto {

    @ApiModelProperty(value = "媒资文件id", required = true)
    @NotBlank(message = "媒体文件id不能为空", groups = ValidationGroups.Inster.class)
    private String mediaId;

    @ApiModelProperty(value = "媒体文件名称", required = true)
    @NotBlank(message = "文件名称不能为空", groups = ValidationGroups.Inster.class)
    @Length(max = 150, message = "文件名称过长", groups = ValidationGroups.Inster.class)
    private String fileName;

    @ApiModelProperty(value = "课程计划标识", required = true)
    @NotBlank(message = "课程计划id不能为空", groups = {ValidationGroups.Inster.class, ValidationGroups.Delete.class})
    private Long teachplanId;
}
