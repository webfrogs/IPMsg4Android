package com.ccf.feige.utils;

import java.util.Date;


/**
 * IPMSG协议抽象类
 * IPMSG协议格式：
 * Ver(1): PacketNo:SenderName:SenderHost:CommandNo:AdditionalSection
 * 每部分分别对应为：版本号（现在是1）:数据包编号:发送主机:命令:附加数据
 * 其中：
 * 数据包编号，一般是取毫秒数。利用这个数据，可以唯一的区别每个数据包；
 * SenderName指的是发送者的昵称(实际上是计算机登录名)
 * 发送主机，指的是发送主机的主机名；（主机名）
 * 命令，指的是飞鸽协议中定义的一系列命令，具体见下文；
 * 附加数据，指的是对应不同的具体命令，需要提供的数据。当为上线报文时，附加信息内容是用户名和分组名，中间用"\0"分隔
 * 
 * 例如：
 * 1:100:shirouzu:jupiter:32:Hello 
 * 表示 shirouzu用户发送了 Hello 这条消息（32对应为IPMSG_SEND_MSG这个命令，具体需要看源码中的宏定义）。
 * 
 * @author ccf
 * 
 * v1.0 2012/2/10
 */
public class IpMessageProtocol {
	private String version;	//版本号 目前都为1
	private String packetNo;//数据包编号
	private String senderName;	//发送者昵称（若是PC，则为登录名）
	private String senderHost;	//发送主机名
	private int commandNo;	//命令
	private String additionalSection;	//附加数据
	
	public IpMessageProtocol(){
		this.packetNo = getSeconds();
	}
	
	// 根据协议字符串初始化
	public IpMessageProtocol(String protocolString){
		String[] args = protocolString.split(":");	// 以:分割协议串
		version = args[0];
		packetNo = args[1];
		senderName = args[2];
		senderHost = args[3];
		commandNo = Integer.parseInt(args[4]);
		if(args.length >= 6){	//是否有附加数据
			additionalSection = args[5];
		}else{
			additionalSection = "";
		}
		for(int i = 6; i < args.length; i++){	//处理附加数据中有:的情况
			additionalSection += (":" + args[i]);
		}
		
	}
	
	public IpMessageProtocol(
			String senderName, String senderHost, int commandNo,
			String additionalSection) {
		super();
		this.version = "1";
		this.packetNo = getSeconds();
		this.senderName = senderName;
		this.senderHost = senderHost;
		this.commandNo = commandNo;
		this.additionalSection = additionalSection;
	}


	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPacketNo() {
		return packetNo;
	}
	public void setPacketNo(String packetNo) {
		this.packetNo = packetNo;
	}
	public String getSenderName() {
		return senderName;
	}
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	public String getSenderHost() {
		return senderHost;
	}
	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}
	public int getCommandNo() {
		return commandNo;
	}
	public void setCommandNo(int commandNo) {
		this.commandNo = commandNo;
	}
	public String getAdditionalSection() {
		return additionalSection;
	}
	public void setAdditionalSection(String additionalSection) {
		this.additionalSection = additionalSection;
	}
	
	//得到协议串
	public String getProtocolString(){
		StringBuffer sb = new StringBuffer();
		sb.append(version);
		sb.append(":");
		sb.append(packetNo);
		sb.append(":");
		sb.append(senderName);
		sb.append(":");
		sb.append(senderHost);
		sb.append(":");
		sb.append(commandNo);
		sb.append(":");
		sb.append(additionalSection);
		
		return sb.toString();
	}
	
	//得到数据包编号，毫秒数
	private String getSeconds(){
		Date nowDate = new Date();
		return Long.toString(nowDate.getTime());
	}
	
}
