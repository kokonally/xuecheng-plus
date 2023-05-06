package com.xuecheng.content;

import com.xuecheng.content.service.TeachplanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TeachplanServiceTest {
    @Autowired
    private TeachplanService teachplanService;


    @Test
    void deleteTeachplanTest() {
        Long teachplanId = 292L;
        teachplanService.deleteTeachplan(teachplanId);
    }


}
