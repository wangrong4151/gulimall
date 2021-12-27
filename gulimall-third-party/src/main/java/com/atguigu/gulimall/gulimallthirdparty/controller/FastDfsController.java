package com.atguigu.gulimall.gulimallthirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.gulimallthirdparty.utlis.FastDfsApiOpr;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
public class FastDfsController {
    @PostMapping("/fastdfs/policy")
    public R uploadF(@RequestParam("file") MultipartFile file) {
        log.info(file.getOriginalFilename() + ":" + file.getSize());
        String originalFilename = file.getOriginalFilename();
        // xxx.jpg
        String extName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        log.info(extName);
        String filePath = null;
        try {
            filePath = FastDfsApiOpr.upload(file.getBytes(), extName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return R.ok("上传成功").put("data", filePath);
    }
}
