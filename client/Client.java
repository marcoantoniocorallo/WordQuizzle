/**
 * WordQuizzle Client interface
 * @author Marco Antonio Corallo, 531466
 */

package client;

import myExceptions.*;
import org.json.simple.JSONObject;
import java.util.Collection;

public interface Client {
	
	/**
	 * Registra nickUtente al servizio WordQuizzle
	 * @param nickUtente Utente da registrare  	 != null
	 * @param password   password per nickUtente != null
	 * @throws IllegalArgumentException se nickUtente.length() > 10 || password.length()>10 || password.equals("")
	 * @throws AlreadyRegisteredException se l'utente nickUtente è già registrato
	 */
	public void registra(String nickUtente, String password) throws IllegalArgumentException, AlreadyRegisteredException;
	
	/**
	 * Effettua il login dell'utente nick
	 * @param nick utente da loggare
	 * @param password password di nick
	 * @throws NullPointerException se nick==null || password == null
	 * @throws UserNotFoundException se l'utente nick non è registrato al sistema
	 * @throws AlreadyLoggedException se l'utente è già loggato nel sistema
	 * @throws WrongPasswordException se nick.password!=password
	 * @throws IllegalArgumentException se nick.length()>10 || password.length()>10
	 */
	public void login(String nick, String password, int port)
			throws NullPointerException, UserNotFoundException,
			AlreadyLoggedException, WrongPasswordException, IllegalArgumentException;
	
	/**
	 * Slogga nick dal servizio
	 * @param nick String != null rappresentante l'utente da sloggare
	 * @throws NullPointerException se nick==null
	 * @throws AlreadySloggedException se utente nick non ha effettuato l'accesso
	 */
	public void logout(String nick)
			throws NullPointerException, AlreadySloggedException, UserNotFoundException;
	
	/**
	 * Aggiunge friend alla lista amici di this
	 * @param friend String != null
	 * @throws UserNotFoundException Se l'utente friend non viene trovato
	 * @throws AlreadyFriendsException se gli utenti this e friend sono già amici
	 * @throws IllegalArgumentException	se friend.equals(this)
	 */
	public void addFriend(String friend)
			throws UserNotFoundException, AlreadyFriendsException, IllegalArgumentException;
	
	/**
	 * @return lista amici di this (eventualmente vuota)
	 * @throws UserNotFoundException se l'utente this non viene trovato nel DB
	 */
	public Collection<String> lista_amici()
			throws UserNotFoundException;
	
	/**
	 * @return punteggio dell'utente this
	 * @throws UserNotFoundException se l'utente non viene trovato nel DB
	 * @throws IllegalArgumentException se l'utente esiste ma non è loggato
	 */
	public int punteggio()
			throws UserNotFoundException, IllegalArgumentException;
	
	/**
	 * @return classifica degli amici dell'utente this, in ordine di punteggio, in formato JSON
	 */
	public JSONObject classifica();
	
	/**
	 * Richiamata dal giocatore sfidante, che vuole sfidare friend
	 * @param friend String != null che indica l'utente con cui vuole giocare
	 * @throws NullPointerException se friend==null
	 */
	public void sfida(String friend);
	
	/**
	 * @Overload del metodo sfida, richiamato dal giocatore che accetta di giocare
	 */
	public void sfida();
	
}
