package com.workflow.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.auth.entity.AuthEntity;
import com.workflow.auth.repository.AuthRepository;

@RestController
@RequestMapping("/api")
public class AuthController {
	
//	@Autowired
//	AuthRepository lr;	
//	
//	@PostMapping("/login")
//	public void insterUsers(@RequestBody AuthEntity le) {
//		System.out.println("컨트롤러 진입");
//		System.out.println("출력" + le.getPasswordHash());
//	}
	
	@PostMapping("/login")
	public Map<String, String> login(@RequestBody Map<String, String> body){
		
		System.out.println(body.get("email"));
		System.out.println(body.get("password"));
		
		return Map.of(body.get("email"), body.get("password"));
		
	}

}
