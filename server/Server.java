/**
 * WordQuizzle Server Word Quizzle
 * @author Marco Antonio Corallo, 531466
 */

package server;
import myExceptions.*;
import org.json.simple.JSONObject;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface Server{
	
	/**
	 * Istanzia ed esporta un oggetto di tipo Registrazione,
	 * pubblicandone uno stub nel Registry.
	 * Un thread della JVM resta in ascolto di nuove registrazioni
	 */
	public void avviaRegistrazione();
	
	/**
	 * Effettua il login dell'utente nick se le credenziali sono corrette.
	 * @param nick 		String != null, rappresenta il nickname dell'utente
	 * @param password  String != null, identifica la password dell'utente nick
	 * @return 0 se l'operazione di login viene effettuata correttamente.
	 * @throws NullPointerException se nick==null || password==null
	 * @throws UserNotFoundException se nick non rappresenta alcun utente registrato.
	 * @throws AlreadyLoggedException se l'utente nick è già loggato
	 * @throws WrongPasswordException se nick.password != password
	 */
	public int login(String nick, String password)
		throws NullPointerException, UserNotFoundException, AlreadyLoggedException, WrongPasswordException;
	
	/**
	 * Sconnette l'utente nick dal servizio
	 * @param nick String != null rappresenta l'utente che vuole sconnettersi
	 * @return 0 se l'operazione va a buon fine
	 * @throws NullPointerException se nick==null
	 * @throws UserNotFoundException se nick non rappresenta alcun utente registrato.
	 * @throws AlreadySloggedException se l'utente nick è già sloggato
	 */
	public int logout(String nick)
			throws NullPointerException, UserNotFoundException, AlreadySloggedException;
	
	/**
	 * crea un arco non orientato tra i due nodi utente
	 * @param nickUtente nodo sorgente
	 * @param nickAmico	 nodo destinazione
	 * @return 0 se l'operazione va a buon fine
	 * @throws NullPointerException se nickUtente==null || nickAmico==null
	 * @throws UserNotFoundException se nickUtente non esiste || nickAmico non esiste
	 * @throws AlreadyFriendsException se nickUtente è già amico di nickAmico
	 */
	public int aggiungiAmico(String nickUtente, String nickAmico)
		throws NullPointerException, UserNotFoundException, AlreadyFriendsException;
	
	/**
	 * Restituisce la lista amici di Utente
	 * @param utente String != null Identifica l'utente di cui ricercare la lista amici.
	 * @return Oggetto JSON che denoti gli amici di Utente
	 * @throws NullPointerException se utente==null
	 * @throws UserNotFoundException se l'utente non è registrato
	 * @throws IllegalArgumentException se l'utente è sloggato
	 */
	public JSONObject listaAmici(String utente)
		throws NullPointerException, IllegalArgumentException, UserNotFoundException;
	
	/**
	 * Restituisce il punteggio di utente
	 * @param utente String != null identifica l'utente di cui si vuole conoscere il punteggio
	 * @return	punteggio di utente
	 * @throws NullPointerException se utente==null
	 * @throws UserNotFoundException se l'utente non è registrato
	 * @throws IllegalArgumentException se l'utente non è loggato
	 */
	public int punteggio(String utente)
		throws NullPointerException, UserNotFoundException, IllegalArgumentException;
	
	/**
	 * @return 	JSONObject != null contenente un solo attributo:
	 * 			la classifica, ordinata per punteggio, degli amici di utente
	 * { "classifica": [ { "name1":"punteggio1" },...,{ "nameN":"punteggioN" } ] }
	 */
	public JSONObject classifica(String utente);
	
	/**
	 * Si occupa della sfida tra i clients con canali in k1 e k2
	 * @param k1 SelectionKey!=null chiave registrata per giocatore sfidante
	 * @param k2 SelectionKey!=null chiave registrata per giocatore sfidato
	 * @param sel    Selector!=null selettore in cui registrare le chiavi al termine della sfida
	 */
	public void sfida(SelectionKey k1, SelectionKey k2, Selector sel);
}
