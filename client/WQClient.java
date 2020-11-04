/**
 * WQ Client implementation
 * @author Marco Antonio Corallo, 531466
 */

package client;

import myExceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.Registrazione;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

public class WQClient implements  Client{
	private final int registryPort;	// Porta su cui localizzare il servizio di registrazione
	private final int serverPort;	// Porta alla quale connettersi con il server
	private String currentUser;		// Utente loggato in questo client
	private SocketChannel server;
	
	public WQClient(){
		registryPort = 50000;
		serverPort   = 50050;
		currentUser = null;
	}
	
	public void registra(String nickUtente, String password)
			throws AlreadyRegisteredException, IllegalArgumentException{
		try {
			
			// Individua stub nel registry, castandolo (Remote -> Registrazione)
			Registry r = LocateRegistry.getRegistry(registryPort);
			Registrazione remoteObj = (Registrazione) r.lookup("Servizio-Registrazione");
			remoteObj.registra_utente(nickUtente, password);
			System.out.println(nickUtente +": Registrazione avvenuta con successo.");
		}
		// catch exceptions
		catch (AlreadyRegisteredException arex) {
			throw new AlreadyRegisteredException(arex.getMessage()+": utente già registrato");
		}
		catch (IllegalArgumentException il) {
			throw new IllegalArgumentException(il.getMessage());
		}
		//NPE, Remote, IOE: Eccezioni non gestibili dall'utente
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Si consiglia di inviare una segnalazione al server");}
	}
	
	public void login(String nick, String password, int port)
			throws NullPointerException, UserNotFoundException,
			AlreadyLoggedException, WrongPasswordException, IllegalArgumentException{
		
		// Controlla nullability
		if (nick==null || password==null)
			throw new NullPointerException("login dell'utente: "+ nick+ " " + password);
		
		// Controlla la lunghezza dei parametri
		if (nick.length() > 10 || password.length() > 10)
			throw new IllegalArgumentException(
					nick+", "+password+": User o Password troppo lunghi. MAX 10 caratteri.");
		
		// Controlla non ci sia già un utente loggato
		if (this.currentUser!=null)
			throw new AlreadyLoggedException("Devi prima sloggarti dall'account: "+nick);
		
		int res=0;
		try {
			
			// Concatena il num. di porta del listener
			// e le credenziali, separate da virgola
			ByteBuffer buf = ByteBuffer.allocate(25);
			String s = nick + "," + password;
			
			// Codifica all'interno del buffer ed invia
			buf.putInt(port);
			buf.put(s.getBytes("UTF-8"), 0, s.length());
			buf.flip();
			server.write(buf);
			
			// Attende esito login
			buf.clear();
			server.read(buf);
			buf.flip();
			res = buf.getInt();
		}
		catch (IOException ioe){ ioe.printStackTrace(); }
		
		// Valuta esito
		switch (res){
			case 0:
				currentUser=nick;
				System.out.println(nick+": login effettuato con successo.");
				break;
			case 1:
				throw new UserNotFoundException("Utente "+nick+" non trovato.");
			case 2:
				throw new AlreadyLoggedException("Utente "+nick+" già loggato.");
			case 3:
				throw new WrongPasswordException("Password errata. Utente: "+ nick);
		}
	}
	
	public void logout(String nick)
			throws NullPointerException, AlreadySloggedException, UserNotFoundException {
			
		// Controlla che l'utente sia loggato nel client
		if (this.currentUser==null || !this.currentUser.equals(nick))
			throw new AlreadySloggedException("Devi essere loggato per eseguire questa operazione.");
		
		// Invia n. operazione al server (-1)
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(4);
			buf.putInt(-1);
			buf.flip();
			server.write(buf);
			
			// Attende esito logout
			buf.clear();
			server.read(buf);
			buf.flip();
			res = buf.getInt();
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		
		// Valuta esito e slogga dal client
		switch (res){
			case 0:
				System.out.println(nick+" sloggato correttamente.");
				System.out.flush();
				this.currentUser=null;
				break;
			case 1:
				throw new UserNotFoundException("Utente "+nick+" non trovato.");
			case 2:
				throw new AlreadySloggedException("Utente "+nick+" già sloggato.");
		}
		
	}
	
	public void addFriend(String friend)
		throws UserNotFoundException, AlreadyFriendsException, IllegalArgumentException{
		
		// Controlla nullability
		if (friend==null)
			throw new NullPointerException("Utente da aggiungere nullo");
		
		// Controlla di essere loggato
		if (this.currentUser==null)
			throw new NullPointerException("Devi prima effettuare il login.");
		
		// Controlla che currentUser!=friend
		if (this.currentUser.equals(friend))
			throw new IllegalArgumentException("Hai qualche problema con te stesso? Dovresti già essere tuo amico.");
		
		// Dimensione parametro
		if (friend.length()>10)
			throw new IllegalArgumentException("Parametro "+friend+" troppo lungo. MAX 10 caratteri.");
		
		// Invia cod. op (-2) e parametro al server
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(8+friend.length()); //=2*sizeof(int)+friend.length
			buf.putInt(-2);
			buf.putInt(friend.length());
			buf.put(friend.getBytes());
			buf.flip();
			server.write(buf);
			
			// Attende esito operazione
			buf.clear();
			server.read(buf);
			buf.flip();
			res = buf.getInt();
		}
		catch (IOException ioe){ ioe.printStackTrace(); }
		
		// Gestisce esito
		switch (res){
			case 0:
				System.out.println(this.currentUser+" e "+friend+" sono ora amici.");
				break;
			case 1:
				throw new UserNotFoundException("Utente "+ friend+" non trovato.");
			case 2:
				throw new NullPointerException("Devi prima effettuare il login.");
			case 3:
				throw new AlreadyFriendsException("I due utenti sono già amici.");
		}
		
	}
	
	public Collection<String> lista_amici()
		throws UserNotFoundException{
		
		String s="";
		Collection<String> amici= new Vector<>();
		
		// Invia cod. op (-3) e parametro al server
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(18);
			buf.putInt(-3);
			buf.flip();
			server.write(buf);
			
			// Attende lista amici (o esito negativo)
			buf.clear();
			int n = server.read(buf);
			buf.flip();
			
			// Legge chunk di stringa JSON:
			// Primo chunk: n. Chunks + 14 bytes
			// Successivi: lunghezza del chunk + 14 bytes massimo di chunk!
			
			int size = buf.getInt();
			
			// Gestisci possibile eccezione segnalata dal server
			if (size==-1)
				throw new UserNotFoundException("Utente non trovato!");
			if (size==-2)
				return amici;
			
			int len = 14;
			for (int i = 0; i < size; i++) {
				
				byte[] bytes = new byte[14];
				buf.get(bytes, 0, len);
				s += new String(bytes, 0, len);
				buf.clear();;
				
				if (i==size-1) break;
				server.read(buf);
				buf.flip();
				len = buf.getInt();
			}
			
			// Parsa la stringa in un JSONObject
			JSONParser p = new JSONParser();
			JSONObject obj = (JSONObject) p.parse(s);
			JSONArray arr = (JSONArray) obj.get("amici");
			Iterator<String> it = arr.iterator();
			
			// Inserisci elementi in una collection che restituirà al chiamante
			while (it.hasNext()){
				((Vector<String>) amici).add(it.next());
			}
		
		}
		catch (IOException ioe){ ioe.printStackTrace(); }
		catch (ParseException p) { p.printStackTrace(); }
		
		return amici;
		
	}
	
	public int punteggio()
		throws UserNotFoundException, IllegalArgumentException{
		
		// Invia n. operazione al server (-4)
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(4);
			buf.putInt(-4);
			buf.flip();
			server.write(buf);
			
			// Attende punteggio
			buf.clear();
			server.read(buf);
			buf.flip();
			res = buf.getInt();
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		
		// Gestisci eventuali eccezioni
		if (res==-1)
			throw new UserNotFoundException("Utente non trovato!");
		if (res==-2)
			throw new IllegalArgumentException("Devi prima effettuare il login! ");
		
		return res;
	}
	
	public JSONObject classifica(){
		
		String s="";
		JSONObject obj=null;
		
		// Invia cod. op (-5)
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(18);
			buf.putInt(-5);
			buf.flip();
			server.write(buf);
			
			// Attende classifica
			buf.clear();
			server.read(buf);
			buf.flip();
			
			// Legge chunk di stringa JSON:
			// Primo chunk: n. Chunks + 14 bytes
			// Successivi: lunghezza del chunk + 14 bytes massimo di chunk!
			
			int n = buf.getInt();
			int len = 14;
			for (int i = 0; i < n; i++) {
				
				byte[] bytes = new byte[14];
				buf.get(bytes, 0, len);
				s += new String(bytes, 0, len);
				buf.clear();
				
				if (i==n-1) break;
				server.read(buf);
				buf.flip();
				len = buf.getInt();
			}
			
			
			// Parsa la stringa in un JSONObject
			JSONParser p = new JSONParser();
			obj = (JSONObject) p.parse(s);
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		catch (ParseException pe){ pe.printStackTrace(); }
		
		return obj;
		
	}
	
	public void sfida(String friend){
		
		// Dimensione parametro
		if (friend.length()>10)
			throw new IllegalArgumentException("Parametro "+friend+" troppo lungo. MAX 10 caratteri.");
		
		if (friend==null)
			throw new NullPointerException("Amico nullo!");
		
		// Invia cod. op. (42), lunghezza parametro e parametro friend
		int res=0;
		try {
			ByteBuffer buf = ByteBuffer.allocate(8 + friend.length()); //=2*sizeof(int)+friend.length
			buf.putInt(42);
			buf.putInt(friend.length());
			buf.put(friend.getBytes());
			buf.flip();
			server.write(buf);
			System.out.println("\nSfida lanciata! In attesa della risposta da "+friend);
			System.out.flush();
			
			// Attendi esito
			buf.clear();
			server.read(buf);
			buf.flip();
			res = buf.getInt();
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		
		if (res==-7)
			throw new IllegalArgumentException("\nQuesto utente non è tuo amico, oppure è offline.");
		else if (res==-1)
			System.out.println("\nIl tuo amico ha rifiutato la sfida.");
		else // G A M E !
		{
			sfida();
		}
		
		try {
			Thread.currentThread().sleep(5);
		}
		catch (InterruptedException ie) { ie.printStackTrace(); }
	}
	
	// Overload del metodo sfida richiamato dal giocatore sfidato
	public void sfida(){
		
		System.out.println("La sfida sta per iniziare...");
		System.out.println("Avete 30 secondi, per rispondere a 10 domande!");
		
		// Alloca buffer parole
		ByteBuffer buf = ByteBuffer.allocate(24);
		int n = -1;
		
		try {
			
			while (true) {
				
				// Attendi parola
				buf.clear();
				server.read(buf);
				
				buf.flip();
				n = buf.getInt();
				
				if (n<0)
					break;
				
				byte[] bytes = new byte[n];
				buf.get(bytes,0,n);
				System.out.println("Parola: "+new String(bytes));
				
				// Acquisisci traduzione utente
				Scanner scan = new Scanner(System.in);
				String parola = scan.nextLine();
				
				// Libera buffer e manda traduzione
				buf.clear();
				buf.putInt(parola.length());
				buf.put(parola.getBytes(),0,parola.length());
				buf.flip();
				server.write(buf);
				
			}
		}
		catch (IOException ioe ) { ioe.printStackTrace(); }
		
		try {
			
			// Errore di comunicazione
			if (n == -1)
				System.err.println("È avvenuto un errore di comunicazione con il server.");
			
			// Finite le parole
			if (n == -3){
				System.out.println("Complimenti: hai terminato le parole!\n Attendi che il gioco finisca...");
				buf.clear();
				server.read(buf);
				buf.flip();
				n = buf.getInt();
			}
			
			// Sconfitta
			if (n == -2)
				System.out.println("Purtroppo hai perso. Punteggio: " + buf.getInt());
			
			// Vittoria
			if (n == -42) {
				int p = buf.getInt();
				System.out.println("Complimenti, hai vinto! Punteggio: " + p
						+ "\n" + "Acquisti 5 punti bonus per aver vinto la sfida, per un totale di: " + (p+5));
			}
			
			// pareggio
			if (n == -21)
				System.out.println("Colpo di scena: pareggio! Siete entrambi vincitori, con il punteggio di "
				+ buf.getInt() +", ma senza bonus!");
			
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		
	}
	
	// Metodi ausiliari
	
	protected void accettaSfida(){
		try {
			
			// Invia '1' al server con il significato di "accetto la sfida"
			ByteBuffer buf = ByteBuffer.allocate(1);
			buf.put((new Integer(1)).byteValue());
			buf.flip();
			server.write(buf);
		}catch (IOException ioe) { ioe.printStackTrace(); }
	}

	protected void rifiutaSfida(){
		try {
	
			// Invia '-1' al server con il significato di "rifiuto la sfida"
			ByteBuffer buf = ByteBuffer.allocate(1);
			buf.put((new Integer(-1)).byteValue());
			buf.flip();
			server.write(buf);
		}catch (IOException ioe) { ioe.printStackTrace(); }
	}
	
	protected int getServerPort(){
		return serverPort;
	}
	
	protected void setServerChannel(SocketChannel server){
		this.server=server;
	}
	
	protected String getCurrentUser(){
		return new String(currentUser);
	}
	
}
