package com.workflow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
//경고메시지 : For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))
//or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.
// -> Page<DTO> 방식은 내부적으로 PageImpl이라서 Spring이 그대로 직렬화해버리면 JSON구조가 버전에 따라 바뀔 수 있어서 경고를 띄움
// -> 부트 3버전부터 PageImpl을 그대로 직렬화하는건 안정적이지 않다고 공식적으로 막기 시작
// 밑의 어노테이션을 쓰면 PageImpl을 내부 DTO 구조로 변환해서 안정적인 JSON으로 만들어 줌.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
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
