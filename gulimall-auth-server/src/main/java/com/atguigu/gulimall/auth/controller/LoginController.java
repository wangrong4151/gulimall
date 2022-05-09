package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.MemberResponseVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.Vo.UserLoginVo;
import com.atguigu.gulimall.auth.Vo.UserRegisterVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.utli.SmsCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class LoginController {
    /*@Autowired
    private ThirdPartFeignService thirdPartFeignService;*/

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping(value = "/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {

        //1、接口防刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            String time = redisCode.split("_")[1];
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - Long.parseLong(time) < 60) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }


        //2、验证码的再次效验 redis.存key-phone,value-code
//        String code = UUID.randomUUID().toString().substring(0, 5);
//        String redisValue = code+"_"+System.currentTimeMillis();
        //Double smsCode = Math.random() * 1000000;
        Integer smsCode = SmsCodeUtils.getSmsCode(6);
        String redisStrore = smsCode.toString() + "_" + System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisStrore, 5, TimeUnit.MINUTES);


        //todo 第三方工具发送短信
        //thirdPartFeignService.sendCode(phone, smsCode.toString());
        log.info("验证码:{}", smsCode);
        return R.ok();
    }


    /**
     * TODO: 重定向携带数据【request域失效】：利用session原理，将数据放在session中。
     * TODO: 只要跳转到下一个页面取出这个数据以后，session里面的数据就会删掉
     * TODO: 分布式下session问题
     * RedirectAttributes：重定向也可以保留数据，不会丢失
     * 用户注册
     *
     * @return
     */
    @PostMapping(value = "/register")
    public String register(@Valid UserRegisterVo vos, BindingResult result, RedirectAttributes attributes) {

        //如果有错误回到注册页面
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            attributes.addFlashAttribute("errors", errors);

            //效验出错回到注册页面
            return "redirect:http://auth.gulimall.com/reg.html";
            // 1、return "reg"; 请求转发【使用Model共享数据】【异常：，405 POST not support】
            // 2、"redirect:http:/reg.html"重定向【使用RedirectAttributes共享数据】【bug：会以ip+port来重定向】
            // 3、redirect:http://auth.gulimall.com/reg.html重定向【使用RedirectAttributes共享数据】
        }

        //1、效验验证码
        String code = vos.getCode();

        //获取存入Redis里的验证码
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String codeRedis = valueOperations.get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
        // 判断验证码是否正确【有BUG，如果字符串存储有问题，没有解析出code，数据为空，导致验证码永远错误】
        if (!StringUtils.isEmpty(codeRedis) && code.equals(codeRedis.split("_")[0])) {


            //删除验证码（不可重复使用）;令牌机制
            stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
            //验证码通过，真正注册，调用远程服务进行注册【会员服务】
            R r = memberFeignService.register(vos);
            //成功
            if (r.getCode() == 0) {
                return "redirect:http://auth.gulimall.com/login.html";
            } else {
                Map<String, String> errors = new HashMap<>();
                //调用失败，返回注册页并显示错误信息

                errors.put("msg", r.getData("msg", new TypeReference<String>() {
                }));
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        } else {
            // redis中验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码过期");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }


        //验证码错误

    }

    @PostMapping(value = "/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {
        //远程登录
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0) {
            MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {});
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            log.info("保存session{}", data);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> map = new HashMap<>();
            map.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            attributes.addFlashAttribute(map);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    /**
     * 判断session是否有loginUser，没有就跳转登录页面，有就跳转首页
     */
    @GetMapping({"/login.html", "/", "/index", "/index.html"})
    public String loginPage(HttpSession session) {
        //从session先取出来用户的信息，判断用户是否已经登录过了
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        //如果用户没登录那就跳转到登录页面
        if (attribute == null) {
            return "login";
        } else {
            return "redirect:http://gulimall.com";
        }
    }

    @GetMapping(value = "/loguot.html")
    public String logout(HttpServletRequest request) {
        request.getSession().removeAttribute(AuthServerConstant.LOGIN_USER);
        request.getSession().invalidate();
        return "redirect:http://gulimall.com";
    }

}
