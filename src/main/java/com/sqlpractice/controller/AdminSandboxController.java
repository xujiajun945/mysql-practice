package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sandbox")
@RequiredArgsConstructor
public class AdminSandboxController {

    private final SandboxService sandboxService;

    @PostMapping("/reset")
    public ApiResult<Map<String, Object>> reset() {
        return ApiResult.ok(sandboxService.reset());
    }
}
