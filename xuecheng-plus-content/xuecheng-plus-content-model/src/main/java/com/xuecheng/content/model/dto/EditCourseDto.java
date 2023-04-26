package com.xuecheng.content.model.dto;

import com.xuecheng.base.exception.ValidationGroups;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 更新课程信息dto
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EditCourseDto extends AddCourseDto {

    @ApiModelProperty(value = "课程id", required = true)
    @NotNull(message = "课程id不能为空", groups = ValidationGroups.Update.class)
    private Long id;
}
