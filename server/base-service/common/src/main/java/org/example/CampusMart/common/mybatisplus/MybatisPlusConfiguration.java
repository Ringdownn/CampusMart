package org.example.CampusMart.common.mybatisplus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.example.CampusMart.web.mapper")
public class MybatisPlusConfiguration  {
}
