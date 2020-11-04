/**
 * WordQuizzle Word Quizzle client main
 * @author Marco Antonio Corallo, 531466
 */

package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import myExceptions.*;

public class MainClass {
	
	public static void printUsage(){
		System.out.println("usage : COMMAND [ ARGS ...]\n" +
				"Commands: \n" +
				"registra <nickUtente> <password>	: registra l'utente (MAX 10 caratteri) \n" +
				"login    <nickUtente> <password> 	: effettua il login	\n" +
				"logout 							\t: effettua il logout \n" +
				"aggiungi <nickAmico>				: crea relazione di amicizia con nickAmico \n" +
				"amici								: mostra la lista dei propri amici \n" +
				"sfida	 <nickAmico> 				: richiesta di una sfida a nickAmico \n" +
				"punteggio 							: mostra il punteggio dell’utente \n" +
				"classifica							: mostra una classifica degli amici dell’utente (incluso l’utente stesso)");
	}
	
	public static void main(String args[]){
		
		Client client = new WQClient();
		SocketChannel server = null;
		
		// Utilizzo un bufferedReader in modo da poter controllare quando è pronto!
		// Grazie a questo posso dar priorità alle sfide arrivate
		BufferedReader scanner = new BufferedReader(
				new InputStreamReader(System.in));
		
		String  input;
		String[] comando=null;
		String nick = null;
		String password = null;
		
		boolean goon = true;
		
		// Stampa USAGE
		if ( args.length > 0 && args[0].equals("--help")) {
			printUsage();
			return;
		}
		
		// Attendi primo comando
		try {
			System.out.println("Benvenuto su Word Quizzle!\n"
					+ "Desideri registrarti o effettuare il login?"
					+ "[registra/login] <nickUtente> <password>");
			input = scanner.readLine();
			comando = input.split(" ");
		} catch (IOException ioe){ioe.printStackTrace();}
		
		
		// Controlla che il comando sia corretto
		while (	! (comando[0].equals("login") || comando[0].equals("registra"))
				||(comando.length!=3)){
			if ( !( comando.equals("login") || comando.equals("registra") ))
				 System.err.println("Comando non valido. \n[registra/login] <nickUtente> <password>");
			else System.err.println("n. argomenti errato.\n[registra/login] <nickUtente> <password>");
			
			try {
				input = scanner.readLine();
				comando = input.split(" ");
			} catch (IOException ioe){ioe.printStackTrace();}
		}
		
		nick=comando[1]; password=comando[2];
		
		// Effettua eventuale registrazione
		if (comando[0].equals("registra"))
			try {
				client.registra(nick, password);
			}
			catch (Exception e){
				System.err.println(e.getMessage());
				return;
			}
			
		// Apri la connessione per effettuare il login
		try {
			server = SocketChannel.open();
			server.connect(new InetSocketAddress(((WQClient)client).getServerPort()));
			((WQClient)client).setServerChannel(server);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
		
		// Avvia thread in ascolto di sfide
		Listener listener = new Listener(Thread.currentThread());
		Thread threadListener = new Thread(listener);
		threadListener.start();
		
		// Effettua il login
		try {
			client.login(nick, password, listener.getPort());
		}
		catch (Exception e) {
			nick=null;
			System.err.println(e.getMessage());
			goon=false;	// non entra nell'event-loop -> chiude thread e risorse ed esce.
		}
		
		// Rileva richieste utente (event-loop)
		while (goon){
			System.out.println("\nCosa vuoi fare? [help] per vedere la lista comandi.");
			
			// Testa eventuali interruzioni da parte del listener
			try {
				while (!scanner.ready())
					if (Thread.currentThread().interrupted()) {
						
						System.out.println("\nRichiesta arrivata.\n Accettare? [s/n]");
						long startTimeout = System.currentTimeMillis();
						input = scanner.readLine();
						if (input.equals("s")) {
							
							// Se timeout scaduto -> non inviare
							long endTimeout = System.currentTimeMillis();
							if (endTimeout-startTimeout >= 9990)
								System.out.println("Tempo scaduto, sfida rifiutata.");
							
							// Altrimenti -> accetta e gioca
							else {
								((WQClient) client).accettaSfida();
								listener.busy.set(true);
								client.sfida();
								listener.busy.set(false);
							}
						}
						
						// Altrimenti: non vuole giocare -> ignora la sfida
						else
							((WQClient) client).rifiutaSfida();
						break;
					}
					
			}catch (IOException ioe) {ioe.printStackTrace();}
			
			// Arrivo fin qui per due motivi:
			// 1. lo scanner è ready
			// 2. ho accettato/rifiutato una sfida.
			try {
				if (!scanner.ready())
					continue;
				
				input = scanner.readLine();
				comando = input.split(" ");
			}
			catch (IOException ioe) {ioe.printStackTrace();}
			
			// Gestisci richiesta d'aiuto con l'usage
			if (comando[0].equals("help")) {
				printUsage();
				continue;
			}
			
			// Aggiungi amico
			if (comando[0].equals("aggiungi")){
				if (comando.length<2) {
					System.err.println("Specificare l'utente da aggiungere.");
					continue;
				}
				try {
					client.addFriend(comando[1]);
				}
				catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			
			// Lista amici
			if (comando[0].equals("amici")){
				
				try {
					Collection<String> coll = client.lista_amici();
					if (coll.isEmpty()){
						System.out.println("L'utente non ha ancora amici");
						continue;
					}
					
					System.out.println("Lista amici:");
					for (String s : coll)
						System.out.println(s);
					System.out.println("-------------------");
				}
				catch (Exception e) {
					e.printStackTrace();
					System.err.println(e.getMessage());
				}
			}
			
			// Mostra punteggio
			if (comando[0].equals("punteggio")){
				try {
					System.out.println("Punteggio: "+client.punteggio());
				}
				catch (Exception e){
					System.err.println(e.getMessage());
				}
			}
		
			// Mostra classifica
			if (comando[0].equals("classifica")){
				try {
					System.out.println("Classifica: ");
					JSONObject j = client.classifica();
					JSONArray arr = (JSONArray) j.get("classifica");
					Iterator<JSONObject> it = arr.iterator();
					while (it.hasNext())
						System.out.println(it.next());
					System.out.println("-------------------");
				}
				catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			
			// Sfida !
			if (comando[0].equals("sfida")){
				if (comando.length<2){
					System.err.println("Specificare l'utente da aggiungere.");
					continue;
				}
				
				try {
					client.sfida(comando[1]);
				}
				catch (IllegalArgumentException il) {
					System.err.println(il.getMessage());
				}
				catch (Exception e){
					System.err.println(e.getMessage());
					break;
				}
				
			}

			// logout
			if (comando[0].equals("logout"))
				break;	
			
			
		} // End event-loop
		
		// Chiude risorse
		System.out.println("\nChiusura in corso...");
		try {
			if (nick!=null)
				client.logout(nick);
			if (server.isOpen())
				server.close();
			scanner.close();
			threadListener.interrupt();
			threadListener.join();
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		catch (InterruptedException ie) { ie.printStackTrace(); }
		catch (AlreadySloggedException a) { a.printStackTrace();}
		catch (Exception e){ e.printStackTrace(); }
	}

}
