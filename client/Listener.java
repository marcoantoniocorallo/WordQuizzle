/**
 * Thread WQ in ascolto di sfide ricevute
 * @author Marco Antonio Corallo, 531466
 */

package client;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener implements Runnable {
	private int port;
	private DatagramSocket sock;
	private Thread mainthread;
	protected AtomicBoolean busy;
	
	public Listener(Thread mainthread){
		Random r = new Random();
		this.mainthread=mainthread;
		this.busy = new AtomicBoolean();
		this.busy.set(false);
		
		// porta appartenente [50K, 65K]
		while (true) {
			port = r.nextInt(15000) + 50000;
			try{
				 sock = new DatagramSocket(port);
			}
			catch (BindException e) {continue;}
			catch (Exception e) {e.printStackTrace();}
			break;
		}
	}
	
	public int getPort(){
		return port;
	}
	
	@Override
	public void run() {
		
		// Si mette in ascolto sulla porta
		byte[] bytes = new byte[1];
		DatagramPacket dp = new DatagramPacket(bytes, 1);
		try {
			sock.setSoTimeout(1000);
		}catch (SocketException e) { e.printStackTrace(); }
		
		while (true){
			try {
				sock.receive(dp);
				
				// Se riceve una richiesta mentre l'utente gioca -> la ignora
				if (this.busy.get()==true)
					continue;
				
				mainthread.interrupt();
			}
			catch (SocketTimeoutException timeout) {
				
				// Se è stato interrotto -> è ora di chiudere
				if (Thread.currentThread().interrupted())
					return;
			}
			catch (IOException ioe ) { ioe.printStackTrace();}
		}
	}
}
