package com.ccf.feige.activity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import com.ccf.feige.net.NetTcpFileReceiveThread;
import com.ccf.feige.net.NetThreadHelper;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * 本抽象类实现了使用Handle方法来异步处理消息
 * 只要继承本类，并重写void processMessage(Message msg)方法即可
 * @author ccf
 *
 */
public abstract class MyFeiGeBaseActivity extends Activity {
	private static int notification_id = 9786970;
	private NotificationManager mNotManager;
	private Notification mNotification;
	
	protected static LinkedList<MyFeiGeBaseActivity> queue = new LinkedList<MyFeiGeBaseActivity>();
	private static MediaPlayer player;
	protected static NetThreadHelper netThreadHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		netThreadHelper = NetThreadHelper.newInstance();
		
		//建立notification
		mNotManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotification = new Notification(android.R.drawable.stat_sys_download, "飞鸽接收文件", System.currentTimeMillis());
		mNotification.contentView = new RemoteViews(getPackageName(), R.layout.file_download_notification);
		mNotification.contentView.setProgressBar(R.id.pd_download, 100, 0, false);
		Intent notificationIntent = new Intent(this,MyFeiGeBaseActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNotification.contentIntent = contentIntent;
		
		
		if(!queue.contains(this))
			queue.add(this);
		if(player == null){
			player = MediaPlayer.create(this, R.raw.msg);
			try {
				player.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static MyFeiGeBaseActivity getActivity(int index){
		if (index < 0 || index >= queue.size())
			throw new IllegalArgumentException("out of queue");
		return queue.get(index);
	}
	
	public static MyFeiGeBaseActivity getCurrentActivity(){
		return queue.getLast();
	}
	
	public void makeTextShort(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void makeTextLong(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}
	
	public abstract void processMessage(Message msg);
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		queue.removeLast();
	}

	public static void sendMessage(int cmd, String text) {
		Message msg = new Message();
		msg.obj = text;
		msg.what = cmd;
		sendMessage(msg);
	}

	public static void sendMessage(Message msg) {
		handler.sendMessage(msg);
	}

	public static void sendEmptyMessage(int what) {
		handler.sendEmptyMessage(what);
	}

	private static Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT:{
				//收到发送文件请求
				
				final String[] extraMsg = (String[]) msg.obj;	//得到附加文件信息,字符串数组，分别放了  IP，附加文件信息,发送者名称，包ID
				Log.d("receive file....", "receive file from :" + extraMsg[2] + "(" + extraMsg[0] +")");
				Log.d("receive file....", "receive file info:" + extraMsg[1]);
				byte[] bt = {0x07};		//用于分隔多个发送文件的字符
				String splitStr = new String(bt);
				final String[] fileInfos = extraMsg[1].split(splitStr);	//使用分隔字符进行分割
				
				Log.d("feige", "收到文件传输请求,共有" + fileInfos.length + "个文件");
				
				String infoStr = "发送者IP:\t" + extraMsg[0] + "\n" + 
								 "发送者名称:\t" + extraMsg[2] + "\n" +
								 "文件总数:\t" + fileInfos.length +"个";
				
				new AlertDialog.Builder(queue.getLast())
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle("收到文件传输请求")
					.setMessage(infoStr)
					.setPositiveButton("接收", 
							new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Thread fileReceiveThread = new Thread(new NetTcpFileReceiveThread(extraMsg[3], extraMsg[0],fileInfos));	//新建一个接受文件线程
									fileReceiveThread.start();	//启动线程
									
									Toast.makeText(getCurrentActivity(), "开始接收文件", Toast.LENGTH_SHORT).show();
									
									queue.getLast().showNotification();	//显示notification
								}
							})
					 .setNegativeButton("取消", 
							 new DialogInterface.OnClickListener() {
						 		public void onClick(DialogInterface dialog, int which) {
						 			//发送拒绝报文
						 			//构造拒绝报文
									IpMessageProtocol ipmsgSend = new IpMessageProtocol();
									ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//拒绝命令字
									ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RELEASEFILES);
									ipmsgSend.setSenderName("android飞鸽");
									ipmsgSend.setSenderHost("android");
									ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//附加信息里是确认收到的包的编号
						 			
									InetAddress sendAddress = null;
									try {
										sendAddress = InetAddress.getByName(extraMsg[0]);
									} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									netThreadHelper.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);
									
						 		}
					 }).show();
					 
			}
				break;
				
			case UsedConst.FILERECEIVEINFO:{	//更新接收文件进度条
				int[] sendedPer = (int[]) msg.obj;	//得到信息
				queue.getLast().mNotification.contentView.setProgressBar(R.id.pd_download, 100, sendedPer[1], false);
				queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "文件"+ (sendedPer[0] + 1) +"接收中:" + sendedPer[1] + "%");
				
				queue.getLast().showNotification();	//显示notification
			}
				break;
				
			case UsedConst.FILERECEIVESUCCESS:{	//文件接收成功
				int[] successNum = (int[]) msg.obj;
				
				queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "第"+ successNum[0] +"个文件接收成功");
				queue.getLast().makeTextShort("第"+ successNum[0] +"个文件接收成功");
				if(successNum[0] == successNum[1]){
					queue.getLast().mNotification.contentView.setTextViewText(R.id.fileRec_info, "所有文件接收成功");
//					queue.getLast().mNotManager.cancel(notification_id);
					
					queue.getLast().makeTextShort("所有文件接收成功");
				}
				queue.getLast().showNotification();
			}
				break;
			default:
				if(queue.size() > 0)
					queue.getLast().processMessage(msg);
				break;
			}
		}

	};
	
	public void exit() {
		while (queue.size() > 0)
			queue.getLast().finish();
	}
	
	public static void playMsg(){
		player.start();
	}
	
	protected void showNotification(){
		mNotManager.notify(notification_id, mNotification);
	}
	

}
