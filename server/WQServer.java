/**
 * WordQuizzle Implementazione del Server
 * @author Marco Antonio Corallo, 531466
 */

package server;

import myExceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import users.ConfrontaPunteggi;
import users.Utente;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WQServer implements Server {
	private final File	 database;						 // DB utenti registrati
	private final int 	registryPort;					 // Porta usata per il registry
	private final int 	welcomingPort;					 // Porta in ascolto di nuove connessioni
	private Lock		fileLock;						 // Lock per l'accesso al DB
	private ConcurrentHashMap<String, Utente> utenti;    // utenti registrati
	private ExecutorService games;						 // Thread pool in cui risiedono le sfide avviate
	
	public WQServer() {
		// se il file non esiste -> viene creato
		// altrimenti utilizza il file esistente
		// Note: "single operation that is atomic..."
		//TODO: Probabilmente dovrò modificare il path, eliminando la parte di IntelliJ
		database = new File("./users/DB.json");
		fileLock = new ReentrantLock();
		registryPort  = 50000;
		welcomingPort = 50050;
		try {
			database.createNewFile();
		}
		catch (IOException ioe){ ioe.printStackTrace(); }
		
		games = Executors.newCachedThreadPool();
		
		// Ricarica utenti registrati in sessioni passate
		utenti = new ConcurrentHashMap<>();
		reload();
	}
	
	@Override
	public void avviaRegistrazione() {
		
		try {
			// Crea istanza dell'oggetto remoto
			ServizioRegistrazione register = new ServizioRegistrazione(utenti, database, fileLock);
			
			// Esportazione oggetto
			Registrazione stub = (Registrazione) UnicastRemoteObject.exportObject(register, 0);
			
			// Crea e riferisce il registry per la creazione
			LocateRegistry.createRegistry(registryPort);
			Registry r = LocateRegistry.getRegistry(registryPort);
			
			// Pubblicazione dello stub nel Registry
			r.rebind("Servizio-Registrazione", stub);
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	@Override
	public int login(String nick, String password)
			throws NullPointerException, UserNotFoundException, AlreadyLoggedException, WrongPasswordException
	{
		// Controlla nullability
		if (nick == null || password == null)
			throw new NullPointerException("login dell'utente: "+ nick+ " " + password);
		
		// Controlla esistenza dell'utente
		if (!utenti.containsKey(nick))
			throw new UserNotFoundException("Utente "+nick+" non trovato.");
		
		// Controlla che l'utente non sia già loggato
		if (utenti.get(nick).isOnline())
			throw new AlreadyLoggedException("Utente "+nick+" già loggato.");
		
		// Controlla password
		if (!utenti.get(nick).samePassword(password))
			throw new WrongPasswordException("Password errata. Utente: "+ nick);
		
		if (utenti.get(nick).setOnline())
			return 0;
		else
			return -1;
	}
	
	@Override
	public int logout(String nick)
			throws NullPointerException,UserNotFoundException, AlreadySloggedException {
		
		// Controlla nullability
		if (nick==null)
			throw new NullPointerException("Logout: username nullo");
		
		// Controlla che l'utente esista
		if (!utenti.containsKey(nick))
			throw new UserNotFoundException("Utente "+nick+" non trovato.");
		
		// Controlla che l'utente non sia già stato sloggato
		if (!utenti.get(nick).isOnline())
			throw new AlreadySloggedException("Utente era già sloggato.");
		
		// Slogga
		utenti.get(nick).setOffline();
		System.out.println(nick +": utente sloggato.");
		
		return 0;
	}
	
	@Override
	public int aggiungiAmico(String nickUtente, String nickAmico)
	throws AlreadyFriendsException, UserNotFoundException {
	
		// Controlla nullability
		if (nickAmico==null || nickUtente==null)
			throw new NullPointerException("elemento null nell'amicizia: <"+nickUtente+","+nickAmico+">");
		
		// Controlla che gli utenti esistano
		if (!utenti.containsKey(nickUtente) || !utenti.containsKey(nickAmico))
			throw new UserNotFoundException("Utente non trovato!");
		
		// Controlla che nickUtente sia loggato
		if (utenti.get(nickUtente).isOnline()==false)
			throw new IllegalArgumentException("Utente "+nickUtente+" non loggato");
		
		// Controlla che non siano già amici
		if (utenti.get(nickUtente).isFriend(nickAmico))
			throw new AlreadyFriendsException("Questi utenti sono già amici.");
		
		// Aggiunge l'uno nella lista dell'altro
		utenti.get(nickUtente).addFriend(nickAmico);
		utenti.get(nickAmico).addFriend(nickUtente);
		
		// Aggiorna DB
		updateDB();
		
		System.out.println(nickUtente + " e " + nickAmico + " sono ora amici.");
		return 0;
	}
	
	@Override
	public JSONObject listaAmici(String utente)
			throws NullPointerException, IllegalArgumentException, UserNotFoundException{
	
		// Controlla nullability, appartenenza agli utenti e che l'utente sia online
		if (utente==null) throw new NullPointerException("Utente nullo!");
		if (!utenti.containsKey(utente))
			throw new UserNotFoundException("Utente "+utente+" non trovato");
		if (!utenti.get(utente).isOnline())
			throw new IllegalArgumentException("L'utente "+ utente
					+ " è offline: chi ha mandato la richiesta?");
		
		// Forgia un oggetto JSON con gli amici dell'utente
		JSONObject obj = new JSONObject();
		JSONArray arr = new JSONArray();
		Iterator<String> it = utenti.get(utente).iterator();
		while (it.hasNext()){
			arr.add(it.next());
		}
		obj.put("amici",arr);
		
		return obj;
	}
	
	@Override
	public int punteggio(String utente)
			throws NullPointerException, UserNotFoundException, IllegalArgumentException{
		
		// Controlli sull'input
		if (utente==null)
			throw new NullPointerException("utente nullo!");
		if (!utenti.containsKey(utente))
			throw new UserNotFoundException("Utente non trovato!");
		if (!utenti.get(utente).isOnline())
			throw new IllegalArgumentException("L'utente "+ utente
					+ " è offline: chi ha mandato la richiesta?");
		
		// Restituisci punteggio
		return utenti.get(utente).getPunteggio();
		
	}
	
	@Override
	public JSONObject classifica(String utente){
		
		// Riordina utenti per punteggio
		Vector<Utente> users = new Vector<>(utenti.values());
		users.sort(new ConfrontaPunteggi().reversed());
		
		// Incapsula in oggetti json
		JSONArray arr = new JSONArray();
		JSONObject obj = new JSONObject();
		
		for (Utente u:
				users) {
			
			// Se l'utente u non è nella lista amici di utente -> passa al prossimo
			if (utenti.get(utente).isFriend(u.getName()) || u.getName().equals(utente)) {
				
				JSONObject o = new JSONObject();
				o.put(u.getName(), u.getPunteggio());
				arr.add(o);
			}
		}
		
		obj.put("classifica",arr);
		return obj;
	}
	
	public void sfida(SelectionKey k1, SelectionKey k2, Selector sel){
		
		// Controlla nullability
		if (k1==null)
			throw new NullPointerException("Prima chiave nulla!");
		if (k2==null)
			throw new NullPointerException("Seconda chiave nulla!");
		if (sel==null)
			throw new NullPointerException("Selettore nullo!");
			
		Sfida sfida = new Sfida(k1,k2, this, sel);
		games.submit(sfida);
	}
	
	// Metodi ausiliari
	// Deserializza gli utenti registrati nelle sessioni passate e li carica nella mappa utenti
	// Nota: Accede al file condiviso!
	private void reload() {
		
		// Acquisisce la lock sul file per poterlo leggere!
		fileLock.lock();
		
		//try-with-resources: la risorsa viene chiusa sistematicamente
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(database.getPath()), "UTF-8"))) {
			
			// Legge tutto il file una linea alla volta
			JSONParser parser = new JSONParser();
			String line;
			while ((line = in.readLine()) != null) {
				
				// Parsa la stringa in un JSObject e ricrea l'utente serializzato
				JSONObject jsobj = (JSONObject) parser.parse(line);
				Utente u = new Utente((String) jsobj.get("nick"),
									  (String) jsobj.get("password"));
				u.incrementPunteggio( ((Long) jsobj.get("punteggio")).intValue() );
				// Recupera la lista amici serializzata
				JSONArray arr = (JSONArray) jsobj.get("amici");
				Iterator<String> iterator = arr.iterator();
				while (iterator.hasNext()){
					u.addFriend(iterator.next());
				}
				
				// Inserisci nella struttura dati locale
				utenti.put((String) jsobj.get("nick"), u);
			}
			
		}
		catch(Exception e){ e.printStackTrace(); }
		
		// Rilascia lock!
		fileLock.unlock();
		
	}
	
	// Controlla che user e friend possono giocare insieme
	protected boolean canWePlay(String user, String friend){
		if (!utenti.get(user).isFriend(friend))
			return false;
		if (!utenti.get(user).isOnline()
				|| !utenti.get(friend).isOnline() )
			return false;
		return true;
	}
	
	// Aggiorna DB inserendo un utente nella lista amici dell'altro
	// Nota: Accede al file condiviso!
	private void updateDB(){
		
		// Acquisisce lock sul file!
		fileLock.lock();
		
		try {
			database.delete();
			database.createNewFile();
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		
		try (BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(database.getPath(),true))) {
			
			Collection<Utente> users = utenti.values();
			for (Utente u :
					users) {
				
				// JsonObject rappresentante l'utente da inserire
				JSONObject jsobj = new JSONObject();
				jsobj.put("nick", u.getName());
				jsobj.put("password", u.getPass());
				jsobj.put("punteggio", u.getPunteggio());
				Iterator it = u.iterator();
				JSONArray arr = new JSONArray();
				while (it.hasNext()) {
					arr.add(it.next());
				}
				jsobj.put("amici", arr);
				
				// Scrivi a file
				String tmp = database.length() > 0 ? "\n" : "";
				String jsstr = tmp + jsobj.toJSONString();
				int size = jsstr.length();
				out.write(jsstr.getBytes(), 0, size);
				out.flush();
			}
			
		}catch ( IOException ioe) { ioe.printStackTrace(); }
		
		// Rilascia lock!
		fileLock.unlock();
		
	}
	
	protected int getWelcomingPort(){
		return welcomingPort;
	}
	
	protected void aggiornaPunteggioDi(String nick1, int n1, String nick2, int n2){
		utenti.get(nick1).incrementPunteggio(n1);
		utenti.get(nick2).incrementPunteggio(n2);
		updateDB();
	}
}
