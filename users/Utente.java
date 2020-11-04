/**
 * Utente del servizio WQ
 * @author Marco Antonio Corallo, 531466
 */

package users;

import java.util.Iterator;
import java.util.Vector;

import myExceptions.AlreadyFriendsException;

/**
 * Utente del servizio Word Quizzle
 * @author Marco Antonio Corallo, 531466
 */

public class Utente {
	private String 	nick;			// identifica univocamente un utente
	private String 	password;
	private boolean online;
	private int		punteggio;
	private Vector<String> amici;	// nickname degli utenti amici		Nota: Struttura synchronized

	public Utente(String nick, String password){
		this.nick=nick;
		this.password=password;
		this.amici=new Vector<>();
		this.online=false;
		this.punteggio=0;
	}
	
	public void addFriend(String friend) throws AlreadyFriendsException, NullPointerException {
		
		// Controlla nullability
		if (friend==null)
			throw new NullPointerException("Amico di "+ nick +" nullo");
		
		// Controlla che non siano già amici
		if (amici.contains(friend)){
			throw new AlreadyFriendsException(nick+" e "+friend+" sono già amici");}
		
		amici.add(friend);
	}
	
	public boolean isFriend(String friend){
		return amici.contains(friend);
	}
	
	public boolean isOnline(){
		return online;
	}
	
	public boolean samePassword(String password){
		return this.password.equals(password);
	}
	
	public boolean setOnline(){
		return online=true;
	}
	
	public boolean setOffline(){
		return online=false;
	}
	
	public Iterator<String> iterator(){
		return amici.iterator();
	}
	
	public String getName(){
		return nick;
	}
	
	public String getPass(){
		return password;
	}
	
	public int getPunteggio(){
		return punteggio;
	}
	
	public void incrementPunteggio(int punteggio){
		this.punteggio+=punteggio;
	}
}
