package com.ccf.feige.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Message;
import android.util.Log;

import com.ccf.feige.activity.MyFeiGeActivity;
import com.ccf.feige.activity.MyFeiGeBaseActivity;
import com.ccf.feige.data.ChatMessage;
import com.ccf.feige.data.User;
import com.ccf.feige.interfaces.ReceiveMsgListener;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;

/**
 * 飞鸽的网络通信辅助类
 * 实现UDP通信以及UDP端口监听
 * 端口监听采用多线程方式
 * 
 * 单例模式
 * @author ccf
 * 
 * V1.0 2012/2/14，寂寞的情人节版本，嘿嘿
 *
 */

public class NetThreadHelper implements Runnable{
	public static final String TAG = "NetThreadHelper";
	
	private static NetThreadHelper instance;
	
	private static final int BUFFERLENGTH = 1024; //缓冲大小
	private boolean onWork = false;	//线程工作标识
	private String selfName;
	private String selfGroup;
	
	private Thread udpThread = null;	//接收UDP数据线程
	private DatagramSocket udpSocket = null;	//用于接收和发送udp数据的socket
	private DatagramPacket udpSendPacket = null;	//用于发送的udp数据包
	private DatagramPacket udpResPacket = null;	//用于接收的udp数据包
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//接收数据的缓存
	private byte[] sendBuffer = null;
	
	private Map<String,User> users;	//当前所有用户的集合，以IP为KEY
	private int userCount = 0; //用户个数。注意，此项值只有在调用getSimpleExpandableListAdapter()才会更新，目的是与adapter中用户个数保持一致
	
	private Queue<ChatMessage> receiveMsgQueue;	//消息队列,在没有聊天窗口时将接收的消息放到这个队列中
	private Vector<ReceiveMsgListener> listeners;	//ReceiveMsgListener容器，当一个聊天窗口打开时，将其加入。一定要记得适时将其移除
	
	private NetThreadHelper(){
		users = new HashMap<String, User>();
		receiveMsgQueue = new ConcurrentLinkedQueue<ChatMessage>();
		listeners = new Vector<ReceiveMsgListener>();
		
		selfName = "android飞鸽";
		selfGroup = "android";
	}
	
	public static NetThreadHelper newInstance(){
		if(instance == null)
			instance = new NetThreadHelper();
		return instance;
	}
	
	public Map<String, User> getUsers(){
		return users;
	}
	
	public int getUserCount(){
		return userCount;
	}
	
	public Queue<ChatMessage> getReceiveMsgQueue(){
		return receiveMsgQueue;
	}
	
	//添加listener到容器中
	public void addReceiveMsgListener(ReceiveMsgListener listener){
		if(!listeners.contains(listener)){
			listeners.add(listener);
		}
	}
	
	//从容器中移除相应listener
	public void removeReceiveMsgListener(ReceiveMsgListener listener){
		if(listeners.contains(listener)){
			listeners.remove(listener);
		}
	}
	
	/**
	 * 
	 * 此方法用来判断是否有处于前台的聊天窗口对应的activity来接收收到的数据。
	 */
	private boolean receiveMsg(ChatMessage msg){
		for(int i = 0; i < listeners.size(); i++){
			ReceiveMsgListener listener = listeners.get(i);
			if(listener.receive(msg)){
				return true;
			}
		}
		return false;
	}
	
	
	public void noticeOnline(){	// 发送上线广播
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(selfName);
		ipmsgSend.setSenderHost(selfGroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_BR_ENTRY);	//上线命令
		ipmsgSend.setAdditionalSection(selfName + "\0" );	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址
			sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....广播地址有误");
		}
		
	}
	
	public void noticeOffline(){	//发送下线广播
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(selfName);
		ipmsgSend.setSenderHost(selfGroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_BR_EXIT);	//下线命令
		ipmsgSend.setAdditionalSection(selfName + "\0" + selfGroup);	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址
			sendUdpData(ipmsgSend.getProtocolString() + "\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....广播地址有误");
		}

	}
	
	public void refreshUsers(){	//刷新在线用户
		users.clear();	//清空在线用户列表
		noticeOnline(); //发送上线通知
		MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_ENTRY);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(onWork){
			
			try {
				udpSocket.receive(udpResPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				onWork = false;
				
				if(udpResPacket != null){
					udpResPacket = null;
				}
				
				if(udpSocket != null){
					udpSocket.close();
					udpSocket = null;
				}
				
				udpThread = null;
				Log.e(TAG, "UDP数据包接收失败！线程停止");
				break;
			} 
			
			if(udpResPacket.getLength() == 0){
				Log.i(TAG, "无法接收UDP数据或者接收到的UDP数据为空");
				continue;
			}
			String ipmsgStr = "";
			try {
				ipmsgStr = new String(resBuffer, 0, udpResPacket.getLength(),"gbk");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "接收数据时，系统不支持GBK编码");
			}//截取收到的数据
			Log.i(TAG, "接收到的UDP数据内容为:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//获取命令字
			switch(commandNo2){
			case IpMessageConst.IPMSG_BR_ENTRY:	{	//收到上线数据包，添加用户，并回送IPMSG_ANSENTRY应答。
				addUser(ipmsgPro);	//添加用户
				
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_ENTRY);
				
				//下面构造回送报文内容
				IpMessageProtocol ipmsgSend = new IpMessageProtocol();
				ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
				ipmsgSend.setSenderName(selfName);
				ipmsgSend.setSenderHost(selfGroup);
				ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//回送报文命令
				ipmsgSend.setAdditionalSection(selfName + "\0" );	//附加信息里加入用户名和分组信息
				
				sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
			}	
				break;
			
			case IpMessageConst.IPMSG_ANSENTRY:	{	//收到上线应答，更新在线用户列表
				addUser(ipmsgPro);
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
			}	
				break;
			
			case IpMessageConst.IPMSG_BR_EXIT:{	//收到下线广播，删除users中对应的值
				String userIp = udpResPacket.getAddress().getHostAddress();
				users.remove(userIp);
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_BR_EXIT);
				
				Log.i(TAG, "根据下线报文成功删除ip为" + userIp + "的用户");
			}	
				break;
			
			case IpMessageConst.IPMSG_SENDMSG:{ //收到消息，处理
				String senderIp = udpResPacket.getAddress().getHostAddress();	//得到发送者IP
				String senderName = ipmsgPro.getSenderName();	//得到发送者的名称
				String additionStr = ipmsgPro.getAdditionalSection();	//得到附加信息
				Date time = new Date();	//收到信息的时间
				String msgTemp;		//直接收到的消息，根据加密选项判断是否是加密消息
				String msgStr;		//解密后的消息内容
				
				//以下是命令的附加字段的判断
				
				//若有命令字传送验证选项，则需回送收到消息报文
				if( (commandNo & IpMessageConst.IPMSG_SENDCHECKOPT) == IpMessageConst.IPMSG_SENDCHECKOPT){
					//构造通报收到消息报文
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//通报收到消息命令字
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RECVMSG);
					ipmsgSend.setSenderName(selfName);
					ipmsgSend.setSenderHost(selfGroup);
					ipmsgSend.setAdditionalSection(ipmsgPro.getPacketNo() + "\0");	//附加信息里是确认收到的包的编号
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
				}
				
				String[] splitStr = additionStr.split("\0"); //使用"\0"分割，若有附加文件信息，则会分割出来
				msgTemp = splitStr[0]; //将消息部分取出
				
				//是否有发送文件的选项.若有，则附加信息里截取出附带的文件信息
				if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
					//下面进行发送文件相关处理
					
					Message msg = new Message();
					msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
					//字符串数组，分别放了  IP，附加文件信息,发送者名称，包ID
					String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
					msg.obj = extraMsg;	//附加文件信息部分
					MyFeiGeBaseActivity.sendMessage(msg);
					
					break;
				}
				
				
				//是否有加密选项，暂缺
				msgStr = msgTemp;
				
				// 若只是发送消息，处理消息
				ChatMessage msg = new ChatMessage(senderIp, senderName, msgStr, time);
				if(!receiveMsg(msg)){	//没有聊天窗口对应的activity
					receiveMsgQueue.add(msg);	// 添加到信息队列
					MyFeiGeBaseActivity.playMsg();
					//之后可以做些UI提示的处理，用sendMessage()来进行，暂缺
					MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_SENDMSG);	//更新主界面UI
				}
				
				
			}
				break;
				
			case IpMessageConst.IPMSG_RELEASEFILES:{ //拒绝接受文件
				MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
			}
				break;
			
				
			}	//end of switch
			
			if(udpResPacket != null){	//每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
				udpResPacket.setLength(BUFFERLENGTH);
			}
			
		}
		
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
		}
		
		udpThread = null;
		
	}
	
	public boolean connectSocket(){	//监听端口，接收UDP数据
		boolean result = false;
		
		try {
			if(udpSocket == null){
				udpSocket = new DatagramSocket(IpMessageConst.PORT);	//绑定端口
				Log.i(TAG, "connectSocket()....绑定UDP端口" + IpMessageConst.PORT + "成功");
			}
			if(udpResPacket == null)
				udpResPacket = new DatagramPacket(resBuffer, BUFFERLENGTH);
			onWork = true;  //设置标识为线程工作
			startThread();	//启动线程接收udp数据
			result = true;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnectSocket();
			Log.e(TAG, "connectSocket()....绑定UDP端口" + IpMessageConst.PORT + "失败");
		}
		
		return result;
	}
	
	public void disconnectSocket(){	// 停止监听UDP数据
		onWork = false;	// 设置线程运行标识为不运行
		
		stopThread();
	}
	

	private void stopThread() {	//停止线程
		// TODO Auto-generated method stub
		if(udpThread != null){
			udpThread.interrupt();	//若线程堵塞，则中断
		}
		Log.i(TAG, "停止监听UDP数据");
	}

	private void startThread() {	//启动线程
		// TODO Auto-generated method stub
		if(udpThread == null){
			udpThread = new Thread(this);
			udpThread.start();
			Log.i(TAG, "正在监听UDP数据");
		}
	}
	
	public synchronized void sendUdpData(String sendStr, InetAddress sendto, int sendPort){	//发送UDP数据包的方法
		try {
			sendBuffer = sendStr.getBytes("gbk");
			// 构造发送的UDP数据包
			udpSendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sendto, sendPort);
			udpSocket.send(udpSendPacket);	//发送udp数据包
			Log.i(TAG, "成功向IP为" + sendto.getHostAddress() + "发送UDP数据：" + sendStr);
			udpSendPacket = null;
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "sendUdpData(String sendStr, int port)....系统不支持GBK编码");
		} catch (IOException e) {	//发送UDP数据包出错
			// TODO Auto-generated catch block
			e.printStackTrace();
			udpSendPacket = null;
			Log.e(TAG, "sendUdpData(String sendStr, int port)....发送UDP数据包失败");
		}
	}
	
	private synchronized void addUser(IpMessageProtocol ipmsgPro){ //添加用户到Users的Map中
		String userIp = udpResPacket.getAddress().getHostAddress();
		User user = new User();
//		user.setUserName(ipmsgPro.getSenderName());
		user.setAlias(ipmsgPro.getSenderName());	//别名暂定发送者名称
		
		String extraInfo = ipmsgPro.getAdditionalSection();
		String[] userInfo = extraInfo.split("\0");	//对附加信息进行分割,得到用户名和分组名
		if(userInfo.length < 1){
			user.setUserName(ipmsgPro.getSenderName());
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("自己");
			else
				user.setGroupName("对方未分组好友");
		}else if (userInfo.length == 1){
			user.setUserName(userInfo[0]);
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("自己");
			else
				user.setGroupName("对方未分组好友");
		}else{
			user.setUserName(userInfo[0]);
			if(userIp.equals(MyFeiGeActivity.hostIp))
				user.setGroupName("自己");
			else
				user.setGroupName(userInfo[1]);
		}
		
		user.setIp(userIp);
		user.setHostName(ipmsgPro.getSenderHost());
		user.setMac("");	//暂时没用这个字段
		users.put(userIp, user);
		Log.i(TAG, "成功添加ip为" + userIp + "的用户");
	}
	
}
