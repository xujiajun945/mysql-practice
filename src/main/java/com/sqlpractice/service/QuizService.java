package com.sqlpractice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.QuizDtos;
import com.sqlpractice.entity.Attempt;
import com.sqlpractice.entity.AttemptAnswer;
import com.sqlpractice.entity.Question;
import com.sqlpractice.entity.User;
import com.sqlpractice.mapper.AttemptAnswerMapper;
import com.sqlpractice.mapper.AttemptMapper;
import com.sqlpractice.mapper.QuestionMapper;
import com.sqlpractice.mapper.UserMapper;
import com.sqlpractice.security.CurrentUser;
import com.sqlpractice.security.PermissionGuard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final int QUESTIONS_PER_ATTEMPT = 5;
    private static final int PREVIEW_ROWS = 100;
    private static final int TEST_PREVIEW_ROWS = 300;

    private final QuestionMapper questionMapper;
    private final AttemptMapper attemptMapper;
    private final AttemptAnswerMapper answerMapper;
    private final UserMapper userMapper;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper;
    private final PermissionGuard guard;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultPayload {
        private List<String> columns;
        private List<List<String>> rows;
    }

    // ------------------------------------------------------------ 学生端

    public QuizDtos.AttemptView createAttempt() {
        List<Question> pool = questionMapper.listEnabled();
        if (pool.size() < QUESTIONS_PER_ATTEMPT) {
            throw BizException.badRequest("题库不足 " + QUESTIONS_PER_ATTEMPT + " 题，请联系管理员补充");
        }
        List<Question> picked = pick(pool);
        Attempt attempt = new Attempt();
        attempt.setUserId(guard.current().getId());
        attempt.setStatus("ANSWERING");
        attempt.setQuestionIds(picked.stream().map(q -> String.valueOf(q.getId()))
                .reduce((a, b) -> a + "," + b).orElse(""));
        attemptMapper.insert(attempt);
        return getAttempt(attempt.getId(), false);
    }

    private List<Question> pick(List<Question> pool) {
        List<Question> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random());
        List<Question> picked = new ArrayList<>();
        for (String difficulty : List.of("EASY", "EASY", "MEDIUM", "MEDIUM", "HARD")) {
            for (Question q : shuffled) {
                if (normalizeDifficulty(q.getDifficulty()).equals(difficulty) && !picked.contains(q)) {
                    picked.add(q);
                    break;
                }
            }
        }
        for (Question q : shuffled) {
            if (picked.size() >= QUESTIONS_PER_ATTEMPT) {
                break;
            }
            if (!picked.contains(q)) {
                picked.add(q);
            }
        }
        return picked;
    }

    public List<QuizDtos.AttemptSummary> myAttempts() {
        Long userId = guard.current().getId();
        List<QuizDtos.AttemptSummary> list = new ArrayList<>();
        for (Attempt attempt : attemptMapper.findByUserId(userId)) {
            list.add(toSummary(attempt));
        }
        return list;
    }

    public QuizDtos.AttemptView getAttempt(Long id, boolean adminView) {
        Attempt attempt = attemptMapper.findById(id);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        if (!adminView && !attempt.getUserId().equals(guard.current().getId())) {
            throw BizException.forbidden("无权查看该答题记录");
        }
        List<Question> questions = new ArrayList<>();
        for (String part : attempt.getQuestionIds().split(",")) {
            if (part.isBlank()) {
                continue;
            }
            Question q = questionMapper.findById(Long.valueOf(part.trim()));
            if (q != null) {
                questions.add(q);
            }
        }
        Map<Long, AttemptAnswer> answers = new HashMap<>();
        for (AttemptAnswer answer : answerMapper.findByAttempt(id)) {
            answers.put(answer.getQuestionId(), answer);
        }

        QuizDtos.AttemptView view = new QuizDtos.AttemptView();
        view.setId(attempt.getId());
        view.setStatus(attempt.getStatus());
        view.setStartedAt(attempt.getStartedAt());
        view.setSubmittedAt(attempt.getSubmittedAt());
        view.setReviewedAt(attempt.getReviewedAt());
        view.setScore(attempt.getScore());
        view.setOverallComment(attempt.getOverallComment());
        view.setTotal(questions.size());
        view.setAnswered((int) answers.values().stream().filter(a -> a.getUserSql() != null).count());

        boolean unlock = adminView || "REVIEWED".equals(attempt.getStatus());
        List<QuizDtos.QuestionView> questionViews = new ArrayList<>();
        for (Question question : questions) {
            QuizDtos.QuestionView qv = new QuizDtos.QuestionView();
            qv.setId(question.getId());
            qv.setTitle(question.getTitle());
            qv.setDescription(question.getDescription());
            qv.setDifficulty(question.getDifficulty());
            qv.setTags(question.getTags());
            qv.setOrderSensitive(question.getOrderSensitive());
            qv.setAnswer(toAnswerView(question, answers.get(question.getId()), unlock));
            questionViews.add(qv);
        }
        view.setQuestions(questionViews);
        return view;
    }

    public QuizDtos.AnswerView submitAnswer(Long attemptId, Long questionId, String sql) {
        Attempt attempt = attemptMapper.findById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        if (!attempt.getUserId().equals(guard.current().getId())) {
            throw BizException.forbidden("无权操作该答题记录");
        }
        if (!"ANSWERING".equals(attempt.getStatus())) {
            throw BizException.badRequest("本次答题已提交，不能再修改");
        }
        if (!attempt.getQuestionIds().contains(String.valueOf(questionId))) {
            throw BizException.badRequest("该题目不属于本次答题");
        }
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw BizException.notFound("题目不存在");
        }

        GradingService.GradeOutcome outcome = gradingService.grade(
                sql, question.getReferenceSql(), question.getOrderSensitive() != null && question.getOrderSensitive() == 1);

        AttemptAnswer answer = new AttemptAnswer();
        answer.setAttemptId(attemptId);
        answer.setQuestionId(questionId);
        answer.setUserSql(sql);
        answer.setAutoResult(outcome.getResult());
        answer.setAutoMessage(truncate(outcome.getMessage(), 500));
        answer.setElapsedMs((int) outcome.getElapsedMs());
        try {
            answer.setUserResultJson(objectMapper.writeValueAsString(preview(outcome.getUserColumns(), outcome.getUserRows())));
            answer.setExpectedResultJson(objectMapper.writeValueAsString(preview(outcome.getExpectedColumns(), outcome.getExpectedRows())));
        } catch (Exception ignored) {
            // 序列化失败不影响判题
        }
        answerMapper.insertOrUpdate(answer);
        return toAnswerView(question, answer, false);
    }

    public QuizDtos.TestRunResult testAnswer(Long attemptId, Long questionId, String sql) {
        Attempt attempt = attemptMapper.findById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        if (!attempt.getUserId().equals(guard.current().getId())) {
            throw BizException.forbidden("无权操作该答题记录");
        }
        if (!"ANSWERING".equals(attempt.getStatus())) {
            throw BizException.badRequest("本次答题已提交，不能再测试");
        }
        if (!attempt.getQuestionIds().contains(String.valueOf(questionId))) {
            throw BizException.badRequest("该题目不属于本次答题");
        }

        GradingService.RunOutcome outcome = gradingService.run(sql);
        QuizDtos.TestRunResult view = new QuizDtos.TestRunResult();
        view.setOk(outcome.isOk());
        view.setMessage(outcome.getMessage());
        view.setElapsedMs(outcome.getElapsedMs());
        view.setTruncated(outcome.isTruncated());
        view.setRowCount(outcome.getRowCount());
        if (outcome.isOk()) {
            List<List<String>> rows = new ArrayList<>();
            List<List<String>> all = outcome.getRows() == null ? List.of() : outcome.getRows();
            for (int i = 0; i < all.size() && i < TEST_PREVIEW_ROWS; i++) {
                rows.add(all.get(i));
            }
            view.setColumns(outcome.getColumns());
            view.setRows(rows);
            if (all.size() > TEST_PREVIEW_ROWS) {
                view.setMessage("执行成功，结果共 " + all.size() + " 行，右侧仅展示前 " + TEST_PREVIEW_ROWS + " 行");
            }
        }
        return view;
    }

    public QuizDtos.AttemptSummary submitAttempt(Long attemptId) {
        Attempt attempt = attemptMapper.findById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        if (!attempt.getUserId().equals(guard.current().getId())) {
            throw BizException.forbidden("无权操作该答题记录");
        }
        int updated = attemptMapper.submit(attemptId, guard.current().getId());
        if (updated == 0) {
            throw BizException.badRequest("本次答题已提交，请勿重复操作");
        }
        return toSummary(attemptMapper.findById(attemptId));
    }

    // ------------------------------------------------------------ 管理端

    public List<QuizDtos.ReviewQueueItem> reviewQueue(String status) {
        guard.requireCap(Caps.REVIEW);
        String filter = (status == null || status.isBlank()) ? "PENDING_REVIEW" : status;
        List<QuizDtos.ReviewQueueItem> list = new ArrayList<>();
        for (Attempt attempt : attemptMapper.listByStatus(filter, 100)) {
            QuizDtos.ReviewQueueItem item = new QuizDtos.ReviewQueueItem();
            item.setAttemptId(attempt.getId());
            item.setUserId(attempt.getUserId());
            User user = userMapper.findById(attempt.getUserId());
            if (user != null) {
                item.setUsername(user.getUsername());
                item.setNickname(user.getNickname());
            }
            item.setSubmittedAt(attempt.getSubmittedAt());
            item.setStatus(attempt.getStatus());
            item.setScore(attempt.getScore());
            List<AttemptAnswer> answers = answerMapper.findByAttempt(attempt.getId());
            item.setTotal(attempt.getQuestionIds().split(",").length);
            item.setAnswered((int) answers.stream().filter(a -> a.getUserSql() != null).count());
            item.setAutoPass((int) answers.stream().filter(a -> "PASS".equals(a.getAutoResult())).count());
            list.add(item);
        }
        return list;
    }

    public QuizDtos.ReviewDetail reviewDetail(Long attemptId) {
        guard.requireCap(Caps.REVIEW);
        Attempt attempt = attemptMapper.findById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        QuizDtos.ReviewDetail detail = new QuizDtos.ReviewDetail();
        detail.setAttemptId(attempt.getId());
        detail.setUserId(attempt.getUserId());
        User user = userMapper.findById(attempt.getUserId());
        if (user != null) {
            detail.setUsername(user.getUsername());
            detail.setNickname(user.getNickname());
        }
        detail.setStatus(attempt.getStatus());
        detail.setSubmittedAt(attempt.getSubmittedAt());
        detail.setOverallComment(attempt.getOverallComment());
        detail.setScore(attempt.getScore());
        detail.setQuestions(getAttempt(attemptId, true).getQuestions());
        return detail;
    }

    public void reviewAnswer(Long attemptId, Long answerId, Boolean correct, String comment) {
        guard.requireCap(Caps.REVIEW);
        if (correct == null) {
            throw BizException.badRequest("请给出是否正确");
        }
        int updated = answerMapper.review(answerId, attemptId, correct ? 1 : 0, comment, guard.current().getId());
        if (updated == 0) {
            throw BizException.notFound("答题记录不存在");
        }
    }

    public QuizDtos.ReviewDetail completeReview(Long attemptId, String overallComment) {
        guard.requireCap(Caps.REVIEW);
        Attempt attempt = attemptMapper.findById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("答题记录不存在");
        }
        if (!"PENDING_REVIEW".equals(attempt.getStatus())) {
            throw BizException.badRequest("该答题不处于待批阅状态");
        }
        List<AttemptAnswer> answers = answerMapper.findByAttempt(attemptId);
        int correctCount = 0;
        for (AttemptAnswer answer : answers) {
            if (answer.getCorrect() == null) {
                int autoCorrect = "PASS".equals(answer.getAutoResult()) ? 1 : 0;
                answerMapper.review(answer.getId(), attemptId, autoCorrect,
                        answer.getReviewComment() == null ? "系统按自动判题结论采纳" : answer.getReviewComment(),
                        guard.current().getId());
                answer.setCorrect(autoCorrect);
            }
            if (answer.getCorrect() != null && answer.getCorrect() == 1) {
                correctCount++;
            }
        }
        attemptMapper.completeReview(attemptId, guard.current().getId(), overallComment, correctCount);
        return reviewDetail(attemptId);
    }

    // ------------------------------------------------------------ 内部转换

    private QuizDtos.AnswerView toAnswerView(Question question, AttemptAnswer answer, boolean unlock) {
        QuizDtos.AnswerView view = new QuizDtos.AnswerView();
        view.setQuestionId(question.getId());
        if (answer != null) {
            view.setId(answer.getId());
            view.setUserSql(answer.getUserSql());
            view.setAutoResult(answer.getAutoResult());
            view.setAutoMessage(answer.getAutoMessage());
            view.setElapsedMs(answer.getElapsedMs());
            view.setCorrect(answer.getCorrect());
            view.setReviewComment(answer.getReviewComment());
            ResultPayload userPayload = parse(answer.getUserResultJson());
            if (userPayload != null) {
                view.setUserColumns(userPayload.getColumns());
                view.setUserRows(userPayload.getRows());
            }
            ResultPayload expectedPayload = parse(answer.getExpectedResultJson());
            if (expectedPayload != null) {
                view.setExpectedColumns(expectedPayload.getColumns());
                view.setExpectedRows(expectedPayload.getRows());
            }
        }
        if (unlock) {
            view.setReferenceSql(question.getReferenceSql());
            view.setExplanation(question.getExplanation());
        }
        return view;
    }

    private ResultPayload preview(List<String> columns, List<List<String>> rows) {
        List<List<String>> limited = new ArrayList<>();
        if (rows != null) {
            for (int i = 0; i < rows.size() && i < PREVIEW_ROWS; i++) {
                limited.add(rows.get(i));
            }
        }
        return new ResultPayload(columns, limited);
    }

    private ResultPayload parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ResultPayload.class);
        } catch (Exception e) {
            return null;
        }
    }

    private QuizDtos.AttemptSummary toSummary(Attempt attempt) {
        QuizDtos.AttemptSummary summary = new QuizDtos.AttemptSummary();
        summary.setId(attempt.getId());
        summary.setStatus(attempt.getStatus());
        summary.setStartedAt(attempt.getStartedAt());
        summary.setSubmittedAt(attempt.getSubmittedAt());
        summary.setReviewedAt(attempt.getReviewedAt());
        summary.setScore(attempt.getScore());
        summary.setTotal(attempt.getQuestionIds().split(",").length);
        List<AttemptAnswer> answers = answerMapper.findByAttempt(attempt.getId());
        summary.setAnswered((int) answers.stream().filter(a -> a.getUserSql() != null).count());
        return summary;
    }

    private static String normalizeDifficulty(String difficulty) {
        return difficulty == null ? "MEDIUM" : difficulty.toUpperCase();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
