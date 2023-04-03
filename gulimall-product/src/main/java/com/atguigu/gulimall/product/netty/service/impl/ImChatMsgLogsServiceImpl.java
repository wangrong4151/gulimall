package com.atguigu.gulimall.product.netty.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.product.netty.bean.ImChatMsgLogs;
import com.atguigu.gulimall.product.netty.enums.ImMsgReadStatus;
import com.atguigu.gulimall.product.netty.enums.ImMsgType;
import com.atguigu.gulimall.product.netty.mapper.ImChatMsgLogsMapper;
import com.atguigu.gulimall.product.netty.service.ImChatMsgLogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * im_chat聊天记录 服务实现类
 * </p>
 *
 * @author wr
 * @since 2022-11-16
 */
@Service
public class ImChatMsgLogsServiceImpl extends ServiceImpl<ImChatMsgLogsMapper, ImChatMsgLogs> implements ImChatMsgLogsService {
	
	@Autowired
	private ImChatMsgLogsMapper imChatMsgLogsMapper;
	
	@Override
	public String saveWebMsgLogs(ImChatMsgLogs logs) {
		LocalDateTime dateTime = LocalDateTime.now();
		logs.setCreateTime(dateTime);
		logs.setSendTime(dateTime);
		logs.setReceiveTime(dateTime);
		logs.setMsgType(ImMsgType.TYPE_1.getCode());
		logs.setMsgReadStatus(ImMsgReadStatus.STATUS_2.getCode());
		imChatMsgLogsMapper.insert(logs);
		return logs.getId();
	}

	@Override
	public String updateOfflineStatusTwo(String msgId) {
		ImChatMsgLogs logs = new ImChatMsgLogs();
		logs.setId(msgId);
		logs.setMsgOfflineStatus(2);
		imChatMsgLogsMapper.updateById(logs);
		return msgId;
	}

	@Override
	public void updateMsgReadStatusOne(List<String> msgIdList) {
		ArrayList<ImChatMsgLogs> batchList = new ArrayList<ImChatMsgLogs>();
		LocalDateTime localDateTime = LocalDateTime.now();
		for (String msg : msgIdList) {
			ImChatMsgLogs logs = new ImChatMsgLogs();
			logs.setId(msg);
			logs.setMsgReadStatus(ImMsgReadStatus.STATUS_1.getCode());
			logs.setReceiveTime(localDateTime);
			batchList.add(logs);
		}
		saveOrUpdateBatch(batchList);
	}
	
	
	
}
