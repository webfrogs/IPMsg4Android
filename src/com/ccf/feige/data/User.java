package com.ccf.feige.data;

/**
 * 用户类，对应局域网中每个在线用户的信息
 * @author ccf
 * V1.0 2012/2/8 新建
 *
 */
public class User {
	private String userName;	// 用户名
	private String alias;		//别名（若为pc，则是登录名）
	private String groupName;	//组名
	private String ip;			//ip地址
	private String hostName;	//主机名
	private String mac;			//MAC地址
	private int msgCount;		//未接收消息数
	
	
	public User(){
		msgCount = 0;	//初始化为零
	}
	
	public User(String userName, String alias, String groupName, String ip,
			String hostName, String mac) {
		super();
		this.userName = userName;
		this.alias = alias;
		this.groupName = groupName;
		this.ip = ip;
		this.hostName = hostName;
		this.mac = mac;
		msgCount = 0;	//初始化为零
	}


	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getMac() {
		return mac;
	}
	public void setMac(String mac) {
		this.mac = mac;
	}

	public int getMsgCount() {
		return msgCount;
	}

	public void setMsgCount(int msgCount) {
		this.msgCount = msgCount;
	}
	
	

}
