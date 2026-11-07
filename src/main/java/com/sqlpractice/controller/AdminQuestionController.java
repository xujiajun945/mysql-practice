package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.entity.Question;
import com.sqlpractice.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ApiResult<List<Question>> list() {
        return ApiResult.ok(questionService.list());
    }

    @PostMapping
    public ApiResult<Question> create(@Valid @RequestBody AdminDtos.QuestionRequest req) {
        return ApiResult.ok(questionService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResult<Question> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.QuestionRequest req) {
        return ApiResult.ok(questionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResult<Map<String, Object>> test(@PathVariable Long id) {
        return ApiResult.ok(questionService.test(id));
    }
}
