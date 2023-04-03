package com.atguigu.gulimall.product.netty.bean;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天记录
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("im_chat_msg_logs")
public class ImChatMsgLogs implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.UUID)
    private String id;




    /**
     * 用户id  消息属于谁的
     */
    @TableField("user_id")
    @ApiModelProperty(name = "userId",value = "用户id" ,dataType = "String")
    private String userId;



    /**
     * 群信息id  当消息类型为 2时
     */
    @TableField("group_info_id")
    @ApiModelProperty(name = "groupInfoId",value = "群信息id  当消息类型为 2时" ,dataType = "String")
    private Integer groupInfoId;

    /**
     * 发送类型：1、点对点个人消息  2、群消息
     */
    @TableField("to_type")
    @ApiModelProperty(name = "toType",value = "发送类型：1、点对点个人消息  2、群消息" ,dataType = "String")
    private Integer toType;

    /**
     * 发送者id
     */
    @TableField("send_id")
    @ApiModelProperty(name = "sendId",value = "发送者id" ,dataType = "String")
    private String sendId;

    /**
     * 发送时间
     */
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @TableField("send_time")
    @ApiModelProperty(name = "sendTime",value = "发送时间" ,dataType = "LocalDateTime")
    private LocalDateTime sendTime;

    /**
     * 接受者id
     */
    @TableField("receive_id")
    @ApiModelProperty(name = "receiveId",value = "接受者id" ,dataType = "String")
    private String receiveId;

    /**
     * 读取时间
     */
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @TableField("receive_time")
    @ApiModelProperty(name = "receiveTime",value = "读取时间" ,dataType = "LocalDateTime")
    private LocalDateTime receiveTime;

    /**
     * 消息类型   1 表示文本消息, 2 表示图片， 3 表示语音， 4 表示视频，
     */
    @TableField("msg_type")
    @ApiModelProperty(name = "msgType",value = "消息类型   1 表示文本消息, 2 表示图片， 3 表示语音" ,dataType = "String")
    private String msgType;

    /**
     * 消息内容
     */
    @TableField("msg_content")
    @ApiModelProperty(name = "msgContent",value = "消息内容" ,dataType = "String")
    private String msgContent;

    /**
     * 消息读状态    1、已读    ，  2、未读
     */
    @TableField("msg_read_status")
    @ApiModelProperty(name = "msgReadStatus",value = "消息读状态 1、已读 ，2、未读" ,dataType = "Integer")
    private Integer msgReadStatus;

    /**
     * 离线消息    1、在线  ， 2、离线
     */
    @TableField("msg_offline_status")
    @ApiModelProperty(name = "msgOfflineStatus",value = "消息读状态 1、已读 ，2、未读" ,dataType = "Integer")
    private Integer msgOfflineStatus;

    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    @ApiModelProperty(name = "createTime",value = "创建时间" ,dataType = "LocalDateTime")
    private LocalDateTime createTime;




    /**
     * 发送者ip地址
     */
    @TableField("ip_send_location")
    @ApiModelProperty(name = "ipSendLocation",value = "发送者ip地址" ,dataType = "String")
    private String ipSendLocation;

    /**
     * 接受者ip地址
     */
    @TableField("ip_receive_location")
    @ApiModelProperty(name = "ipReceiveLocation",value = "接受者ip地址" ,dataType = "String")
    private String ipReceiveLocation;


    /**
     * 是否删除   0、正常     1、删除
     */
    @TableLogic
    @TableField("isDel")
    @ApiModelProperty(name = "is_del",value = "是否删除   0、正常     1、删除" ,dataType = "String")
    private Integer isDel;


}
