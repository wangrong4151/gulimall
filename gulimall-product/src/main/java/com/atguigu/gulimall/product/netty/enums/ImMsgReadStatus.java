package com.atguigu.gulimall.product.netty.enums;

/**
 * @Description:消息读状态    1、已读    ，  2、未读
 * @author wr
 * @since 2022-11-16
 */
public enum ImMsgReadStatus {
	
	/**
	 * 1、已读
	 */
	STATUS_1(1,"已读"),
	
	/**
	 * 2、未读
	 */
	STATUS_2(2,"未读");
	
	private Integer code;
    private String msg;
	
	private ImMsgReadStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
	
    public static String getMsgName(Integer code) {
    	if(code==null) {
    		return null;
    	}
        for (ImMsgReadStatus o : ImMsgReadStatus.values()) {
	        if (o.getCode().equals(code)) {
	            return o.msg;
	        }
        }
        return null;
    }

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
