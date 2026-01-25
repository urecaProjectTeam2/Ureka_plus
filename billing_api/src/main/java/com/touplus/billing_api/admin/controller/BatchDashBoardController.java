package com.touplus.billing_api.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/batch")
public class BatchDashBoardController {

	@GetMapping("/dashboard")
	public String batch() {
		return "batch-dashboard";
	}
}
