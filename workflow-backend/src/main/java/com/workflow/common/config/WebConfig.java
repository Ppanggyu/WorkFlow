package com.workflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
//		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			registry.addResourceHandler("/temp/**").addResourceLocations("file:///C:/WorkFlow/temp/");			
			registry.addResourceHandler("/**").addResourceLocations("file:///C:/WorkFlow");			
//		}else {
//			
//		}
	}

}
