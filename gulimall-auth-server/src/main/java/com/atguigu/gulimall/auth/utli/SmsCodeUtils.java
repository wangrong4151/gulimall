package com.atguigu.gulimall.auth.utli;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

public class SmsCodeUtils {

    public static int getSmsCode(int num) {
        Random random = new Random();
        String member = "";
        for (int i = 0; i < num; i++) {
            member += random.nextInt(10);
        }

        return Integer.parseInt(member);

    }

    public static void main(String[] args) {
        System.out.println(getSmsCode(6));
    }
}
