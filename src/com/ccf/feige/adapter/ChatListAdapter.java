package com.ccf.feige.adapter;

import java.util.List;

import com.ccf.feige.activity.R;
import com.ccf.feige.data.ChatMessage;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * 聊天activity中的ListView的adapter，实现发送者名称对应TextView字体的颜色变化
 * @author ccf
 * 
 * 2012/2/21
 *
 */
public class ChatListAdapter extends BaseAdapter {
	protected LayoutInflater mInflater;
	protected List<ChatMessage> msgList;
	protected Resources res;
	
	public ChatListAdapter(Context c, List<ChatMessage> list){
		super();
		this.mInflater = LayoutInflater.from(c);
		this.msgList = list;
		res = c.getResources();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return msgList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return msgList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View view = null;
		if(convertView == null){
			view = mInflater.inflate(R.layout.chat_item, null);
		}else{
			view = convertView;
		}
		ChatMessage msg = msgList.get(position);
		
		TextView show_name = (TextView) view.findViewById(R.id.show_name);
		show_name.setText(msg.getSenderName());
		if(msg.isSelfMsg()){	//根据是否是自己的消息更改颜色
			show_name.setTextColor(res.getColor(R.color.chat_myself));
		}else{
			show_name.setTextColor(res.getColor(R.color.chat_other));
		}
		
		TextView show_time = (TextView) view.findViewById(R.id.show_time);
		show_time.setText(msg.getTimeStr());
		
		TextView message = (TextView) view.findViewById(R.id.message);
		message.setText(msg.getMsg());
		
		return view;
	}

}
