package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    //域名
    @Value("${community.path.domain}")
    private String domain;

    //项目名
    @Value("${server.servlet.context-path}")
    private String contextPath;

    // @Autowired
    // private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id) {
        // return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if(user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！"); //前一个参数对应的是LoginController中的map.get("usernameMsg")
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u != null) {
            map.put("usernameMsg", "该账号已存在！");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null) {
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5)); //获取一个5位的随机字符串作为salt，用于给密码加密
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID()); //生成一个随机的激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000))); //生成一个随机头像
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        // id是mybatis自动生成的
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1) { //重复激活
            return ACTIVATION_REPEAT;
        } else if(user.getActivationCode().equals(code)) { //激活成功
            userMapper.updateStatus(userId, 1); //将状态码改为1
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else { //激活失败
            return ACTIVATION_FAILURE;
        }
    }

    /**
     *
     * @param username
     * @param password 未加密的密码
     * @param expireSeconds 过期秒数
     * @return
     */
    public Map<String, Object> login(String username, String password, int expireSeconds) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }

        //验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }

        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expireSeconds * 1000L)); //转换为long类型，以免超范围
        // loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket); //Redis会把LoginTicket对象自动序列化成一个JSON格式的字符串

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket) {
        // loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey); //取出对象
        loginTicket.setStatus(1); //修改状态
        redisTemplate.opsForValue().set(redisKey, loginTicket); //存回去
    }

    public LoginTicket findLoginTicket(String ticket) {
        // return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId, String headerUrl) throws InterruptedException {
        //先更新数据库再删除缓存
        // return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        sleep(5000);
        clearCache(userId);
        return rows;
    }

    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空！");
            return map;
        }

        //验证原密码
        User user = userMapper.selectById(userId);
        String salt = user.getSalt();
        oldPassword = CommunityUtil.md5(oldPassword + salt);
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "密码不正确！");
            return map;
        }

        //修改密码
        newPassword = CommunityUtil.md5(newPassword + salt);
        userMapper.updatePassword(userId, newPassword);

        return map;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    //1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS); //1小时后失效
        return user;
    }

    //3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

}
