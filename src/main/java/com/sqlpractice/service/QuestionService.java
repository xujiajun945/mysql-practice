package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.entity.Question;
import com.sqlpractice.mapper.QuestionMapper;
import com.sqlpractice.security.PermissionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final GradingService gradingService;
    private final PermissionGuard guard;

    public List<Question> list() {
        guard.requireCap(Caps.QUESTION_ADMIN);
        return questionMapper.findAll();
    }

    public Question create(AdminDtos.QuestionRequest req) {
        guard.requireCap(Caps.QUESTION_ADMIN);
        Question question = new Question();
        apply(question, req);
        questionMapper.insert(question);
        return question;
    }

    public Question update(Long id, AdminDtos.QuestionRequest req) {
        guard.requireCap(Caps.QUESTION_ADMIN);
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw BizException.notFound("题目不存在");
        }
        apply(question, req);
        questionMapper.update(question);
        return question;
    }

    public void delete(Long id) {
        guard.requireCap(Caps.QUESTION_ADMIN);
        questionMapper.deleteById(id);
    }

    public Map<String, Object> test(Long id) {
        guard.requireCap(Caps.QUESTION_ADMIN);
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw BizException.notFound("题目不存在");
        }
        try {
            List<List<String>> rows = gradingService.runReference(question.getReferenceSql(), 50);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("rowCount", rows.size());
            result.put("rows", rows);
            return result;
        } catch (SQLException e) {
            throw new BizException(400, "参考 SQL 执行失败：" + e.getMessage());
        }
    }

    private void apply(Question question, AdminDtos.QuestionRequest req) {
        question.setTitle(req.getTitle());
        question.setDescription(req.getDescription());
        question.setDifficulty(req.getDifficulty() == null ? "MEDIUM" : req.getDifficulty().toUpperCase());
        question.setTags(req.getTags());
        question.setReferenceSql(req.getReferenceSql());
        question.setExplanation(req.getExplanation());
        question.setOrderSensitive(req.getOrderSensitive() == null ? 0 : req.getOrderSensitive());
        question.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
    }
}
