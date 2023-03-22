package com.atguigu.gulimall.product;


import com.atguigu.gulimall.product.entity.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {


    public static void main(String[] args) {
        List<User> listUser = new ArrayList<User>();
        User user1 = new User("张三","18");
        listUser.add(user1);
        User user2 = new User("李四","19");
        listUser.add(user2);
        User user3 = new User("王五","20");
        listUser.add(user3);
        User user4 = new User("赵六","21");
        listUser.add(user4);
        System.out.println(listUser);		//创建一个LIst<User>对象数组


        User user5 = new User("张三","18");	//张三 130 是否在List<User>中   在这个数组中返回true  没有返回false
        if(listUser.contains(user5)){
            System.out.println(true);
        }else{
            System.out.println(false);
        }
    }


    @Test
    public void Test1(){



    }


}
