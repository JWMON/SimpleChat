//

import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
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
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
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
			while((line = br.readLine()) != null){
				if(line.equals("/quit"))
					break;
				else if(line.contains("meanword") || line.contains("cuss1") || line.contains("cuss2") || line.contains("cuss3") || line.contains("cuss4")) {
					PrintWriter p = (PrintWriter)hm.get(id);
					p.println("WARNING : Profanity prohibited");
					p.flush();
				}
				//if input string contains certain string, warn the user without calling other methods
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
				pw.println(id + " whisphered. : " + msg2);
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
				pw.println(msg);
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
				pw.println(key);
				pw.flush();
			}
			pw.println("Total " + count + " users connected");
			pw.flush();
		}//send userlist
	//get current chatthread id's printwriter
	//for every key value, print the key and increment variable count
	//print total variable count
	}
}
