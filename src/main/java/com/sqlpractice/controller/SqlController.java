package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.SqlDtos;
import com.sqlpractice.service.SqlExecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
public class SqlController {

    private final SqlExecService sqlExecService;

    @PostMapping("/execute")
    public ApiResult<SqlDtos.SqlExecuteResult> execute(@Valid @RequestBody SqlDtos.SqlExecuteRequest req) {
        return ApiResult.ok(sqlExecService.execute(req));
    }
}
