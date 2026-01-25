package com.touplus.billing_api.admin.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/batch")
public class BatchDashBoardController {

	@GetMapping("/dashboard")
	public String batch(Model model,  HttpServletRequest request) {
		model.addAttribute("currentPath", request.getRequestURI());
		return "batch-dashboard";
	}
}
