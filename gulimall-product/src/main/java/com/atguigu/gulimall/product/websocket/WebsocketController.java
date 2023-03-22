package com.atguigu.gulimall.product.websocket;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.websocket.Session;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebsocketController {
    @GetMapping({"/chatroom.html"})
    public String chatroom(Model model) {

        return "chatroom";
    }
    @GetMapping({"/chatroom2.html"})
    public String chatroom2(Model model) {

        return "chatroom2";
    }

    @RequestMapping("/onlineusers")
    @ResponseBody
    public Set<String> onlineusers(@RequestParam("currentuser") String currentuser) {
        ConcurrentHashMap<String, Session> map = WebSocketServer.getSessionPools();
        Set<String> set = map.keySet();
        Iterator<String> it = set.iterator();
        Set<String> nameset = new HashSet<String>();
        while (it.hasNext()) {
            String entry = it.next();
            if (!entry.equals(currentuser))
                nameset.add(entry);
        }
        return nameset;
    }
}
