package com.ccf.feige.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

import com.ccf.feige.activity.MyFeiGeBaseActivity;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

/**
 * Tcp发送文件线程
 * @author ccf
 * 
 * 2012/2/28
 */
public class NetTcpFileSendThread implements Runnable{
	private final static String TAG = "NetTcpFileSendThread";
	private String[] filePathArray;	//保存发送文件路径的数组
	
	public static ServerSocket server;	
	private Socket socket;	
	private byte[] readBuffer = new byte[1024];
	
	public NetTcpFileSendThread(String[] filePathArray){
		this.filePathArray = filePathArray;
		
		try {
			server = new ServerSocket(IpMessageConst.PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "监听tcp端口失败");
		}
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for(int i = 0; i < filePathArray.length; i ++){
			try {
				socket = server.accept();
				Log.i(TAG, "与IP为" + socket.getInetAddress().getHostAddress() + "的用户建立TCP连接");
				BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
				BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
//				DataInputStream dis = new DataInputStream(bis);
//				String ipmsgStr = dis.readUTF();
				int mlen = bis.read(readBuffer);
				String ipmsgStr = new String(readBuffer,0,mlen,"gbk");
				
				
				Log.d(TAG, "收到的TCP数据信息内容是：" + ipmsgStr);
				
				IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);
				String fileNoStr = ipmsgPro.getAdditionalSection();
				String[] fileNoArray = fileNoStr.split(":");
				int sendFileNo = Integer.valueOf(fileNoArray[1]);
				
				Log.d(TAG, "本次发送的文件具体路径为" + filePathArray[sendFileNo]);
				File sendFile = new File(filePathArray[sendFileNo]);	//要发送的文件
				BufferedInputStream fbis = new BufferedInputStream(new FileInputStream(sendFile));
				
				int rlen = 0;
				while((rlen = fbis.read(readBuffer)) != -1){
					bos.write(readBuffer, 0, rlen);
				}
				bos.flush();
				Log.i(TAG, "文件发送成功");
				
				if(bis != null){
					bis.close();
					bis = null;
				}
				
				if(fbis != null){
					fbis.close();
					fbis = null;
				}
				
				if(bos != null){
					bos.close();
					bos = null;
				}
				
				if(i == (filePathArray.length -1)){
					MyFeiGeBaseActivity.sendEmptyMessage(UsedConst.FILESENDSUCCESS);	//文件发送成功
				}
				
			}catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "接收数据时，系统不支持GBK编码");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "发生IO错误");
				break;
			} finally{
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
			}
			
		}
		
		if(server != null){
			try {
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			server = null;
		}
		
		
	}

}
