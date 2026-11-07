package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.DdlDtos;
import com.sqlpractice.service.DdlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ddl")
@RequiredArgsConstructor
public class DdlController {

    private final DdlService ddlService;

    @PostMapping("/create/preview")
    public ApiResult<DdlDtos.DdlResult> previewCreate(@Valid @RequestBody DdlDtos.CreateTableRequest req) {
        return ApiResult.ok(ddlService.previewCreateTable(req));
    }

    @PostMapping("/create")
    public ApiResult<DdlDtos.DdlResult> create(@Valid @RequestBody DdlDtos.CreateTableRequest req) {
        return ApiResult.ok(ddlService.createTable(req));
    }

    @PostMapping("/drop/preview")
    public ApiResult<DdlDtos.DdlResult> previewDrop(@Valid @RequestBody DdlDtos.DropTableRequest req) {
        return ApiResult.ok(ddlService.previewDropTable(req));
    }

    @PostMapping("/drop")
    public ApiResult<DdlDtos.DdlResult> drop(@Valid @RequestBody DdlDtos.DropTableRequest req) {
        return ApiResult.ok(ddlService.dropTable(req));
    }

    @PostMapping("/column/preview")
    public ApiResult<DdlDtos.DdlResult> previewAddColumn(@Valid @RequestBody DdlDtos.AddColumnRequest req) {
        return ApiResult.ok(ddlService.previewAddColumn(req));
    }

    @PostMapping("/column/add")
    public ApiResult<DdlDtos.DdlResult> addColumn(@Valid @RequestBody DdlDtos.AddColumnRequest req) {
        return ApiResult.ok(ddlService.addColumn(req));
    }

    @PostMapping("/column/drop/preview")
    public ApiResult<DdlDtos.DdlResult> previewDropColumn(@Valid @RequestBody DdlDtos.DropColumnRequest req) {
        return ApiResult.ok(ddlService.previewDropColumn(req));
    }

    @PostMapping("/column/drop")
    public ApiResult<DdlDtos.DdlResult> dropColumn(@Valid @RequestBody DdlDtos.DropColumnRequest req) {
        return ApiResult.ok(ddlService.dropColumn(req));
    }

    @PostMapping("/index/add/preview")
    public ApiResult<DdlDtos.DdlResult> previewAddIndex(@Valid @RequestBody DdlDtos.AddIndexRequest req) {
        return ApiResult.ok(ddlService.previewAddIndex(req));
    }

    @PostMapping("/index/add")
    public ApiResult<DdlDtos.DdlResult> addIndex(@Valid @RequestBody DdlDtos.AddIndexRequest req) {
        return ApiResult.ok(ddlService.addIndex(req));
    }

    @PostMapping("/index/drop/preview")
    public ApiResult<DdlDtos.DdlResult> previewDropIndex(@Valid @RequestBody DdlDtos.DropIndexRequest req) {
        return ApiResult.ok(ddlService.previewDropIndex(req));
    }

    @PostMapping("/index/drop")
    public ApiResult<DdlDtos.DdlResult> dropIndex(@Valid @RequestBody DdlDtos.DropIndexRequest req) {
        return ApiResult.ok(ddlService.dropIndex(req));
    }
}
