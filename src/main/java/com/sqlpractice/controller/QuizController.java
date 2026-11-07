package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.QuizDtos;
import com.sqlpractice.security.PermissionGuard;
import com.sqlpractice.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final PermissionGuard guard;

    @PostMapping("/attempts")
    public ApiResult<QuizDtos.AttemptView> create() {
        return ApiResult.ok(quizService.createAttempt());
    }

    @GetMapping("/attempts")
    public ApiResult<List<QuizDtos.AttemptSummary>> mine() {
        return ApiResult.ok(quizService.myAttempts());
    }

    @GetMapping("/attempts/{id}")
    public ApiResult<QuizDtos.AttemptView> detail(@PathVariable Long id) {
        boolean adminView = guard.hasCap(Caps.REVIEW);
        return ApiResult.ok(quizService.getAttempt(id, adminView));
    }

    @PostMapping("/attempts/{id}/answers/{questionId}")
    public ApiResult<QuizDtos.AnswerView> answer(@PathVariable Long id, @PathVariable Long questionId,
                                                @Valid @RequestBody QuizDtos.SubmitAnswerRequest req) {
        return ApiResult.ok(quizService.submitAnswer(id, questionId, req.getSql()));
    }

    @PostMapping("/attempts/{id}/questions/{questionId}/test")
    public ApiResult<QuizDtos.TestRunResult> test(@PathVariable Long id, @PathVariable Long questionId,
                                                  @Valid @RequestBody QuizDtos.SubmitAnswerRequest req) {
        return ApiResult.ok(quizService.testAnswer(id, questionId, req.getSql()));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResult<QuizDtos.AttemptSummary> submit(@PathVariable Long id) {
        return ApiResult.ok(quizService.submitAttempt(id));
    }
}
