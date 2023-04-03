package com.atguigu.gulimall.product.netty.enums;

/**
 * @Description:消息类型   1 表示文本消息, 2 表示图片， 3 表示语音， 4 表示视频
 * @author wr
 * @since 2022-11-16
 */
public enum ImMsgType {
	
	/**
	 * 1 表示文本消息
	 */
	TYPE_1("1","文本消息"),
	
	/**
	 * 2 表示图片
	 */
	TYPE_2("2","表示图片"),
	
	/**
	 * 3 表示语音
	 */
	TYPE_3("3","表示语音"),
	
	/**
	 * 4 表示视频
	 */
	TYPE_4("4","表示视频");
	
	private String code;
    private String msg;
	
	private ImMsgType(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
	
    public static String getChatVersionMsg(String code) {
    	if(code==null) {
    		return null;
    	}
        for (ImMsgType o : ImMsgType.values()) {
	        if (o.getCode().equals(code)) {
	            return o.msg;
	        }
        }
        return null;
    }

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
