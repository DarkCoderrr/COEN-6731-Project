package com.DistributedSystems;

import javax.servlet.http.HttpServlet;

import org.springframework.boot.web.servlet.ServletRegistrationBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.DistributedSystems.controller.ResourceServlet;

@Configuration
public class WebConfig {
   @Bean	
   public ServletRegistrationBean<HttpServlet> resourceServlet() {
	   ServletRegistrationBean<HttpServlet> servRegBean = new ServletRegistrationBean<>();
	   servRegBean.setServlet(new ResourceServlet());
	   servRegBean.addUrlMappings("/skiers/*");
	   servRegBean.setLoadOnStartup(1);
	   return servRegBean;
   }
  

}
