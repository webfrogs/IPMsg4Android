package com.ccf.feige.interfaces;

import com.ccf.feige.data.ChatMessage;

/**
 * 接收消息监听的listener接口
 * @author ccf
 *
 */
public interface ReceiveMsgListener {
	public boolean receive(ChatMessage msg);

}
