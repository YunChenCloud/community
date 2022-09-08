package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替Session对象
 * 需要实现线程隔离
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    //将当前线程作为key，user作为值存放
    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}
