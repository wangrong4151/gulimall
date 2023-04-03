package com.atguigu.gulimall.product.netty.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.product.netty.bean.ImChatMsgLogs;

import java.util.List;

/**
 * <p>
 * im_chat聊天记录 服务类
 * </p>
 *
 * @author wr
 * @since 2022-11-16
 */
public interface ImChatMsgLogsService extends IService<ImChatMsgLogs> {
	
	/**
	 * @Description:添加消息
	 * @param logs
	 * @return
	 * Integer
	 * @exception:
	 * @author wr
	 * @since 2022-11-16
	 */
	String saveWebMsgLogs(ImChatMsgLogs logs);
	
	/**
	 * @Description:修改休息为离线
	 * @param msgId
	 * @return
	 * Integer
	 * @exception:
	 * @author wr
	 * @since 2022-11-16
	 */
	String updateOfflineStatusTwo(String msgId);
	
	/**
	 * @Description:消息读取
	 * @param msgIdList
	 * void
	 * @exception:
	 * @author wr
	 * @since 2022-11-16
	 */
	void updateMsgReadStatusOne(List<String> msgIdList);
}
