/**
 * WordQuizzle server.Server
 * @author Marco Antonio Corallo, 531466
 */

package server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.json.simple.*;
import myExceptions.AlreadyRegisteredException;
import users.Utente;

public class ServizioRegistrazione extends RemoteServer implements Registrazione {
	private final File file;	// file json contenente gli utenti registrati
	private Map utenti;			// utenti registrati al servizio
	private Lock lock;			// lock per il file
	
	public ServizioRegistrazione(Map map, File file, Lock lock){
		utenti=map;
		this.file = file;
		this.lock=lock;
	}
	
	@Override
	public int registra_utente(String nickUtente, String password)
			throws IllegalArgumentException, AlreadyRegisteredException,
			RemoteException, NullPointerException, IOException{
		
		// nickUtente != null && password != null && password != ""
		if ( (nickUtente==null) || (password==null) )
			throw new NullPointerException("Registrazione di: " + nickUtente + ", " + password);
		
		if ( password.equals("") )
			throw new IllegalArgumentException("Password vuota, registrazione di "+ nickUtente);
		
		if ( nickUtente.equals("") )
			throw new IllegalArgumentException("Nick vuoto, registrazione di "+ nickUtente);
		
		// nickUtente.length() <= 10 && password.length <= 10
		if ( nickUtente.length() > 10 || password.length() > 10)
			throw new IllegalArgumentException("Nickname o password troppo lungo. Max 10 caratteri.");
		
		// se l'utente è già registrato -> solleva eccezione
		// altrimenti, inserisci e aggiorna DB Json
		if (utenti.containsKey(nickUtente))
			throw new AlreadyRegisteredException("Registrazione di: " + nickUtente);
		else
			insert(nickUtente, password);
		
		System.out.println("Utente " + nickUtente +" registrato.");
		return 0;
	}
	
	/**
	 * Inserisce nick nel DB degli utenti registrati
	 * @param nick String non-null che rappresenta l'utente da inserire
	 * @param password String non-null che indica la pass dell'utente nick
	 * @throws IOException se vi sono problemi durante l'apertura o la scrittura
	 *
	 * Nota: Accede al file condiviso!
	 */
	private void insert(String nick, String password) throws IOException {
		
		// Inserisce nella struttura dati
		utenti.put(nick, new Utente(nick,password));
		
		// Acquisisce lock su file
		lock.lock();
		
		// Aggiorna file contenente utenti registrati
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file.getPath(),true));
		
		// JsonObject rappresentante l'utente da inserire
		JSONObject jsobj = new JSONObject();
		jsobj.put("nick", nick);
		jsobj.put("password", password);
		jsobj.put("punteggio", 0);
		jsobj.put("amici", new JSONArray());

		// Converte in JS-String e scrive su file
		// Se il file non è vuoto, scrive su una nuova linea
		String tmp = file.length() > 0 ? "\n" : "";
		String jsstr = tmp+jsobj.toJSONString();
		int size = jsstr.length();
		out.write(jsstr.getBytes(),0,size);
		out.flush();
		out.close();
		
		// Rilascia lock
		lock.unlock();
	}
	
}
