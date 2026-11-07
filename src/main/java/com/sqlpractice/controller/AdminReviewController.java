package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.QuizDtos;
import com.sqlpractice.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final QuizService quizService;

    @GetMapping
    public ApiResult<List<QuizDtos.ReviewQueueItem>> queue(
            @RequestParam(required = false, defaultValue = "PENDING_REVIEW") String status) {
        return ApiResult.ok(quizService.reviewQueue(status));
    }

    @GetMapping("/{attemptId}")
    public ApiResult<QuizDtos.ReviewDetail> detail(@PathVariable Long attemptId) {
        return ApiResult.ok(quizService.reviewDetail(attemptId));
    }

    @PostMapping("/{attemptId}/answers/{answerId}")
    public ApiResult<Void> review(@PathVariable Long attemptId, @PathVariable Long answerId,
                                  @RequestBody Map<String, Object> body) {
        Boolean correct = body.get("correct") == null ? null : Boolean.valueOf(String.valueOf(body.get("correct")));
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment"));
        quizService.reviewAnswer(attemptId, answerId, correct, comment);
        return ApiResult.ok();
    }

    @PostMapping("/{attemptId}/complete")
    public ApiResult<QuizDtos.ReviewDetail> complete(@PathVariable Long attemptId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String comment = body == null ? null : body.get("overallComment");
        return ApiResult.ok(quizService.completeReview(attemptId, comment));
    }
}
