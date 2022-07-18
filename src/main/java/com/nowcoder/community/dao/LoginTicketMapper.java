package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LoginTicketMapper {
    //在该类中使用注解的方式实现mapper

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    @Update({
            "update login_ticket set status=#{status} where ticket=#{ticket}"
    })
    int updateStatus(String ticket, int status);

    // //仅用于演示动态sql脚本的编写
    // @Update({
    //         "<script>",
    //         "update login_ticket set status=#{status} where ticket=#{ticket} ",
    //         "<if test=\"ticket!=null\">",
    //         "and 1=1",
    //         "</if>",
    //         "</script>"
    // })

}
