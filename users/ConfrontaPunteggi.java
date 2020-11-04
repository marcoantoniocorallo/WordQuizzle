/**
 * Classe ausiliare per il confronto tra utenti (tramite punteggio)
 * @author Marco Antonio Corallo, 531466
 */

package users;

import java.util.Comparator;

public class ConfrontaPunteggi implements Comparator {
	
	@Override
	public int compare(Object o1, Object o2) {
		Utente u1 = (Utente) o1;
		Utente u2 = (Utente) o2;
		
		if (u1.getPunteggio()>u2.getPunteggio())
			return 1;
		else if (u2.getPunteggio()>u1.getPunteggio())
			return -1;
		return 0;
	}
}
