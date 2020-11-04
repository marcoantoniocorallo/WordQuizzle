/**
 * Servizio di registrazione remota al sistema WordQuizzle
 * @author Marco Antonio Corallo, 531466
 */

package server;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import myExceptions.AlreadyRegisteredException;

public interface Registrazione extends Remote {
	
	/**
	 * Registra utente nel DB
	 * @param nickUtente Utente da registrare  	 != null
	 * @param password	 password per nickUtente != null
	 * @return 	0 se la registrazione ha avuto successo
	 * @throws AlreadyRegisteredException        se l'utente nickUtente è già registrato
	 * @throws  NullPointerException	 se password == null || nickUtente == null
	 * @throws  IllegalArgumentException se password == ""
	 * @throws  IOException				 se si verificano problemi durante l'interazione col file
	 * @throws 	RemoteException			 se si verificano problemi durante la comunicazione
	 */
	int registra_utente(String nickUtente, String password)
		throws AlreadyRegisteredException, IllegalArgumentException,
		RemoteException, NullPointerException, IOException;
	
}
