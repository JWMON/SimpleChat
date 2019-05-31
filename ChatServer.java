//https://github.com/JWMON/SimpleChat

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ChatServer {

	public static void main(String[] args) {
		try{
			Date date1 = new Date();
			SimpleDateFormat d1 = new SimpleDateFormat("hh:mm:ss");
			String time1 = "[" + d1.format(date1) + "] ";
			
			ServerSocket server = new ServerSocket(10001);
			System.out.println(time1 + "Waiting connection...");
			HashMap hm = new HashMap();
			ArrayList<String> profanity = new ArrayList<String>();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm, profanity);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	private ArrayList<String> profanity;
	
	Date date = new Date();
	SimpleDateFormat d = new SimpleDateFormat("hh:mm:ss");
	String time = "[" + d.format(date) + "] ";
	
	public ChatThread(Socket sock, HashMap hm, ArrayList<String> profanity){
		this.sock = sock;
		this.hm = hm;
		this.profanity = profanity;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			System.out.println(time + "[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			String str = null;
			while((line = br.readLine()) != null){
				if(line.equals("/quit"))
					break;
				if((str = checkword(line))!= null){
					warning(str);
				}
				else if(line.contentEquals("/spamlist"))
					spamlist();
				else if(line.indexOf("/addspam ") == 0){
					addspam(line);
				}
				else if(line.indexOf("/to ") == 0){
					sendmsg(line);
				}
				else if(line.equals("/userlist")) 
					send_userlist();
				
				else
					broadcast(id + " : " + line);
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(time + id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg
	public void broadcast(String msg){

		synchronized(hm){
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			while(iter.hasNext()){
				Object cpw = iter.next();
				if(cpw == hm.get(id)) continue;
				PrintWriter pw = (PrintWriter)cpw;
				pw.println(time + msg);
				pw.flush();
			}
		}
	} // broadcast
	//if current printwriter matches the iterating printwriter, continue
	//hasNext() method should be used only once for one rep!!!!!!!
	public void send_userlist() {
		synchronized(hm) {
			Set set = hm.keySet();
			Iterator iter = set.iterator();
			PrintWriter pw = (PrintWriter)hm.get(id);
			int count = 0;
			while(iter.hasNext()) {
				String key = (String)iter.next();
				count++;
				pw.println(time + key);
				//pw.flush();
			}
			pw.println(time + "Total " + count + " users connected");
			pw.flush();
		}//send userlist
	//get current chatthread id's printwriter
	//for every key value, print the key and increment variable count
	//print total variable count
	}
	
	public String checkword(String msg){
		int b = 1;
		synchronized(profanity) {
			for(int i=0;i<profanity.size();i++){
				if(msg.contains(profanity.get(i)))
					return profanity.get(i);
			}
		}
		
		return null;
	}
	public void warning(String msg){
		synchronized(hm) {
			Object obj = hm.get(id);
		}
		if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(time + "Word filterd: "+ msg);
				pw.flush();
		}
	}
	
	public void spamlist() {
		PrintWriter pw = (PrintWriter)hm.get(id);
		String list = "";
		synchronized(profanity) {
			if(profanity.size() == 0) {
				pw.println(time + "No words on ban list");
				pw.flush();
				return;
			}
			for(int i = 0; i < profanity.size(); i++) {
				list += profanity.get(i) + " ";
			}
		}
		pw.println(time + "Words banned: " + list);
		pw.flush();
	}
	
	public void addspam(String msg) {
		int start = msg.indexOf(" <");
		int end = msg.indexOf(">");
		PrintWriter pw = (PrintWriter)hm.get(id);
		if(start == -1 || end == -1) {
			pw.println(time + "Format: /addspam <spamword>");
			pw.flush();
			return;
		}
		String spam = msg.substring(start+2, end);
		synchronized(profanity) {
			profanity.add(spam);
		}
		pw.println(time + spam + " added to spamlist");
		pw.flush();
		
	}
	// /addspam <word> adds "word" on spamlist, not "<word>"
}
