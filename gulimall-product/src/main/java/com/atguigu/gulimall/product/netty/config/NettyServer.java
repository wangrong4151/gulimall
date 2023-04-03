package com.atguigu.gulimall.product.netty.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Description:netty 服务
 * @author wr
 * @since 2022-11-16
 */
@Component
public class NettyServer {
	
	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
	
	/**
	 *  netty端口
	 */
	private static int port = 9021;
	
	private static class SingletionWSServer {
		static final NettyServer instance = new NettyServer();
	}
	
	public static NettyServer getInstance() {
		return SingletionWSServer.instance;
	}
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workGroup;
	private ServerBootstrap server;
	private ChannelFuture future;
	
	public NettyServer() {
		bossGroup = new NioEventLoopGroup();
		workGroup = new NioEventLoopGroup();
		server = new ServerBootstrap();
		server.group(bossGroup, workGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new NettyChannelInitializer());
	}
	
	public void start() {
		this.future = server.bind(port);
		log.info("netty server server 启动完毕... port = "+port);
	}
	
}
