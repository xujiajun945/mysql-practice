package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.MetaDtos;
import com.sqlpractice.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    @GetMapping("/schemas")
    public ApiResult<List<MetaDtos.SchemaInfo>> schemas() {
        return ApiResult.ok(metaService.listSchemas());
    }

    @GetMapping("/schemas/{schema}/tables")
    public ApiResult<List<MetaDtos.TableInfo>> tables(@PathVariable String schema) {
        return ApiResult.ok(metaService.listTables(schema));
    }

    @GetMapping("/schemas/{schema}/tables/{table}")
    public ApiResult<MetaDtos.TableDetail> detail(@PathVariable String schema, @PathVariable String table) {
        return ApiResult.ok(metaService.detail(schema, table));
    }

    @GetMapping("/schemas/{schema}/tables/{table}/preview")
    public ApiResult<MetaDtos.PreviewResult> preview(@PathVariable String schema, @PathVariable String table,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(metaService.preview(schema, table, limit));
    }
}
