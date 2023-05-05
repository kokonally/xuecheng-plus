package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划管理相关接口
 */
@RestController
@Api(value = "课程计划管理相关接口", tags = "课程计划管理相关接口")
public class TeachplanController {
    @Autowired
    private TeachplanService teachplanService;

    /**
     * 查询对应课程id的课程计划
     * @param courseId 课程id
     * @return List<TeachplanDto>
     */
    @ApiOperation("查询对应课程id的课程计划")
    @ApiImplicitParams(@ApiImplicitParam(value = "课程id", name = "courseId", required = true, dataType = "Long", paramType = "path"))
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable("courseId") Long courseId) {
        return teachplanService.findTeachplanTree(courseId);
    }

    @ApiOperation("课程计划创建和修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto saveTeachplanDto) {
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    @ApiOperation("删除课程计划")
    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan(@PathVariable("teachplanId") Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
    }
}
