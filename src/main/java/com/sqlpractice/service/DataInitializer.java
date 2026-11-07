package com.sqlpractice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlpractice.common.Caps;
import com.sqlpractice.config.AppProperties;
import com.sqlpractice.entity.Question;
import com.sqlpractice.entity.Role;
import com.sqlpractice.entity.User;
import com.sqlpractice.mapper.QuestionMapper;
import com.sqlpractice.mapper.RoleMapper;
import com.sqlpractice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final BCryptPasswordEncoder encoder;
    private final ObjectMapper objectMapper;
    private final AppProperties props;

    @Override
    public void run(String... args) throws Exception {
        ensureRole("READER", "只读用户",
                "QUERY,RAW_SQL",
                "employees,employees_lab,practice,demo01,doubao_shop");
        ensureRole("EDITOR", "可建表用户",
                "QUERY,RAW_SQL,CREATE_TABLE,DROP_TABLE,ALTER_COLUMN,INDEX",
                "employees,employees_lab,practice,demo01,doubao_shop");
        ensureRole("ADMIN", "管理员",
                String.join(",", Caps.QUERY, Caps.RAW_SQL, Caps.CREATE_TABLE, Caps.DROP_TABLE,
                        Caps.ALTER_COLUMN, Caps.INDEX, Caps.REVIEW, Caps.USER_ADMIN,
                        Caps.QUESTION_ADMIN, Caps.AUDIT_VIEW),
                "employees,employees_lab,practice,demo01,doubao_shop");
        ensureAdmin();
        ensureQuestions();
    }

    private void ensureRole(String code, String name, String caps, String schemas) {
        if (roleMapper.findByCode(code) != null) {
            return;
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setCapabilities(caps);
        role.setAllowedSchemas(schemas);
        role.setBuiltIn(1);
        roleMapper.insert(role);
        log.info("初始化角色：{}", code);
    }

    private void ensureAdmin() {
        String username = props.getInitialAdmin().getUsername();
        if (userMapper.countByUsername(username) > 0) {
            return;
        }
        Role admin = roleMapper.findByCode("ADMIN");
        if (admin == null) {
            log.warn("ADMIN 角色缺失，跳过管理员初始化");
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(props.getInitialAdmin().getPassword()));
        user.setNickname(props.getInitialAdmin().getNickname());
        user.setRoleId(admin.getId());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        log.info("已创建初始管理员账号：{}（密码见 application.yml）", username);
    }

    private void ensureQuestions() throws Exception {
        int count = questionMapper.countAll();
        if (count > 0) {
            log.info("题库已存在 {} 道题，跳过初始化", count);
            return;
        }
        ClassPathResource resource = new ClassPathResource("data/questions.json");
        if (!resource.exists()) {
            log.warn("未找到 data/questions.json，题库为空");
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            List<Question> questions = objectMapper.readValue(in, new TypeReference<List<Question>>() {
            });
            for (Question question : questions) {
                if (question.getDifficulty() == null) {
                    question.setDifficulty("MEDIUM");
                }
                if (question.getOrderSensitive() == null) {
                    question.setOrderSensitive(0);
                }
                if (question.getEnabled() == null) {
                    question.setEnabled(1);
                }
                questionMapper.insert(question);
            }
            log.info("已导入 {} 道题目", questions.size());
        }
    }
}
