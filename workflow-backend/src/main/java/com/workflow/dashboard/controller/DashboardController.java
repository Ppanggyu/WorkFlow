package com.workflow.dashboard.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController {
	
	private final DashboardService dashboardService;

  // 확인용
//  @GetMapping("/health")
//  public Map<String, String> health() {
//    return Map.of("status", "ok");
//  }

  // KPI 카드 값 내려주기
//  @GetMapping("/kpi")
//  public Map<String, Integer> kpi() {
//    return Map.of(
//      "TODO", 3,
//      "IN_PROGRESS", 2,
//      "REVIEW", 1,
//      "DONE", 10,
//      "ON_HOLD", 0,
//      "CANCELED", 0
//    );
//  }
  
  @GetMapping("/kpi")
  public ResponseEntity<?> kpiType(@AuthenticationPrincipal User user, @RequestParam("type") String type) {
	  
	  Long id = Long.parseLong(user.getUsername());
	  
	  return ResponseEntity.ok(dashboardService.kpiType(id, type));
	  
  }
  
}
