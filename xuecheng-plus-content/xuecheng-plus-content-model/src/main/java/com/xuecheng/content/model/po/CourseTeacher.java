package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.*;
import com.xuecheng.base.exception.ValidationGroups;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程-教师关系表
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("course_teacher")
public class CourseTeacher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull(message = "id参数异常", groups = ValidationGroups.Update.class)
    private Long id;

    /**
     * 课程标识
     */
    @NotNull(message = "课程参数异常", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    private Long courseId;

    /**
     * 教师标识
     */
    @NotBlank(message = "教师名称不能为空", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    @Size(max = 20, message = "教师名称过长", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    private String teacherName;

    /**
     * 教师职位
     */
    @NotBlank(message = "教师职位不能为空", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    @Size(max = 200, message = "教师职位过长", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    private String position;

    /**
     * 教师简介
     */
    @NotBlank(message = "教师简介不能为空", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    @Size(max = 1024, message = "教师简介过长", groups = {ValidationGroups.Update.class, ValidationGroups.Inster.class})
    private String introduction;

    /**
     * 照片
     */
    private String photograph;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createDate;


}
