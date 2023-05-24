package com.xuecheng.content;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * 测试页面静态化
 */
@SpringBootTest
public class FreemarkerTest {

    @Autowired
    private CoursePublishService coursePublishService;

    @Test
    void testGenerateHtmlByTemple() throws IOException, TemplateException {

        //拿到classpath路径
        String classpath = this.getClass().getResource("/").getPath();

        Configuration configuration = new Configuration(Configuration.getVersion());

        String path = classpath + "templates";

        //指定模板目录
        configuration.setDirectoryForTemplateLoading(new File(path));

        //指定编码
        configuration.setDefaultEncoding("utf-8");

        //得到模板
        Template template = configuration.getTemplate("course_template.ftl");
        //Template template 模板, Object model 数据模型
        CoursePreviewDto coursePreviewDto = coursePublishService.getCoursePreviewInfo(2L);
        Map<String, CoursePreviewDto> model = Collections.singletonMap("model", coursePreviewDto);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

        //将HTML写入文件
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        OutputStream outputStream = new FileOutputStream("C:\\Users\\sheng\\Desktop\\2.html");

        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

    }
}
