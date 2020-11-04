/**
 * WordQuizzle Word Quizzle server main
 * @author Marco Antonio Corallo, 531466
 */

package server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import myExceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MainClass {
	
	public static void main(String args[]){
		System.out.println("------------Server avviato------------");
		Server server = new WQServer();
		ServerSocketChannel serverSocketChannel = null;
		Selector selector = null;
		
		// Abilita processo di registrazione remota, gestito da un thread della JVM
		server.avviaRegistrazione();
		
		try {
			
			// Apre welcoming socket in attesa di connessioni
			serverSocketChannel = ServerSocketChannel.open();
			
			// collega la socket associata al canale all'indirizzo locale,
			// configurandola in ascolto su una porta nota al server
			serverSocketChannel.socket().bind(
					new InetSocketAddress(
							((WQServer) server).getWelcomingPort()));
			
			// Rende la welcoming socket non-bloccante!!!
			serverSocketChannel.configureBlocking(false);
			
		}
		catch (IOException ioe) {
			System.err.println("Errore durante l'apertura della connessione");
			ioe.printStackTrace();
		}
		
		// Apre il selettore e vi registra il canale in attesa di una connessione
		try {
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch (ClosedChannelException cce) {
			System.err.println("Errore durante la registrazione nel selettore (ACCEPT)");
			cce.printStackTrace();
		}
		catch (IOException ioe) {
			System.err.println("Errore durante l'apertura del selettore.");
			ioe.printStackTrace();
		}
		
		// Event-loop del Server
		while (true){
		
			// Si blocca in attesa di un evento
			try {
				selector.select(500);
			}
			catch (IOException ioe) {
				System.err.println("Errore durante la selezione dell'evento.");
				ioe.printStackTrace();
				break;
			}
			
			// SelectionKeys canali pronti
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
			
			// Itera sugli eventi "pronti"
			while (iterator.hasNext()){
				
				// seleziona evento e lo rimuove dagli eventi pronti (non dagli eventi registrati!)
				SelectionKey key = iterator.next();
				iterator.remove();
				
				// Se evento == Connessione -> Accetta, effettua login e registra possibili read
				if (key.isAcceptable()) {
					SocketChannel client = null;
					
					try {
						// Accetta connessione: Il socketChannel col client è bloccante di default!
						client = ((ServerSocketChannel) (key.channel())).accept();
						System.out.println("Connessione accettata da " + client.getRemoteAddress());
							
						// Riceve credenziali login
						ByteBuffer buf = ByteBuffer.allocate(25);
						int n = client.read(buf);
						
						if (n>-1) {	// Se si raggiunge l'EOS, chiude la connessione
							
							// Acquisisci numero di porta del listener
							buf.clear();
							int port = buf.getInt();
							
							// Separa le credenziali (utilizza la virgola come separatore)
							String nickandpass = new String(buf.array(), 4, n-4);
							int indexOfComma = nickandpass.indexOf(',');
							String nick = nickandpass.substring(0, indexOfComma);
							String pass = nickandpass.substring(indexOfComma + 1);
							
							// Effettua login
							int result = 0;
							try {
								if (server.login(nick, pass) == 0)
									System.out.println("Ha effettuato l'accesso: " + nick);
								else System.err.println("Problemi durante l'accesso di: " + nick);
							} catch (UserNotFoundException unf) {
								result = 1;
							} catch (AlreadyLoggedException al) {
								result = 2;
							} catch (WrongPasswordException wp) {
								result = 3;
							} catch (NullPointerException npe) {
								npe.printStackTrace();
								System.err.println(npe.getMessage());
							}
							
							// restituisci esito login
							buf.clear();
							buf.putInt(result);
							buf.flip();
							client.write(buf);
							
							// Registra l'interesse a leggere da questo canale
							// allega una coppia "nick, porta"
							client.configureBlocking(false);
							SelectionKey readKey = client.register(
									selector, SelectionKey.OP_READ, new MyPair<String,Integer>(nick,port));
						}
						else client.close();
					}
					catch (ClosedChannelException cce) {
						System.err.println("Errore durante la registrazione nel selettore (READ)"
						+ " canale con " + client);
						cce.printStackTrace();
					}
					catch (IOException ioe) {
						System.err.println("Errore nell'accettazione della connessione da parte di "
						+ client);
						ioe.printStackTrace();
					}
				}
				
				// Se evento == dati in arrivo -> Legge operazione da effettuare e la gestisce
				if (key.isReadable()){
					SocketChannel client=null;
					int op = 0;
					
					// Legge operazione da effettuare
					try {
						client = (SocketChannel) key.channel();
						
						//4 cod.op.+4 par.length+10 par.length massima
						ByteBuffer buf = ByteBuffer.allocate(18);
						client.read(buf);
						buf.flip();
						
						// Controlla che ci sia qualcosa nel buffer
						if (buf.hasRemaining()) {
							op = buf.getInt();
							int result = 0;
							switch (op){
								
								// logout -> Sconnetti, informa il client e chiudi la connessione
								case -1:
									
									try {
										server.logout((String)((MyPair<String,Integer>) key.attachment()).getKey());
									}
									catch (UserNotFoundException unfe) { result = 1; }
									catch (AlreadySloggedException ase){ result = 2; }
									catch (NullPointerException npe) {
										npe.printStackTrace();
										System.err.println(npe.getMessage());
									}
									finally {
										buf.clear();
										buf.putInt(result);
										buf.flip();
										client.write(buf);
										client.close();
										break;
									}
									
									
								// add-Friend -> Aggiungi amico in entrambe le direzioni,
								// aggiorna il DB ed informa il client
								case -2:
									
									int n = buf.getInt();		// lunghezza nome
									byte[] bytes = new byte[n];
									buf.get(bytes,0,n);
									String friend = new String(bytes);
									
									try {
										server.aggiungiAmico(
												(String)((MyPair<String,Integer>) key.attachment()).getKey(), friend);
									}
									catch (UserNotFoundException unf)	{ result = 1; }
									catch (IllegalArgumentException il) { result = 2; }
									catch (AlreadyFriendsException all) { result = 3; }
									catch (NullPointerException npe){
										npe.printStackTrace();
										System.err.println(npe.getMessage());
									}
									finally{
										buf.clear();
										buf.putInt(result);
										buf.flip();
										client.write(buf);
										break;
									}
								
								// lista amici -> restituisci
								case -3:
									JSONObject obj=null;
									try {
										obj = server.listaAmici((String)((MyPair<String,Integer>) key.attachment()).getKey());
									}
									catch (UserNotFoundException unfe)	{
										buf.clear();
										buf.putInt(-1);
										buf.flip();
										client.write(buf);
										break;
									}
									catch (Exception e){	//npe e IllegalArg
										e.printStackTrace();
										System.err.println(e.getMessage());
										break;
									}
									
									// Se lista vuota -> segnala
									if (obj.get("amici").equals(new JSONArray())){
										buf.clear();
										buf.putInt(-2);
										buf.flip();
										client.write(buf);
										break;
									}
									
									// Invia chunk di stringa:
									// Primo chunk: n.chunk + 14 bytes
									// Successivi: lunghezza chunk + 14 bytes al massimo
									String s = obj.toJSONString();
									int len = s.length();
									int nChunk = len/14;
									if (len%14!=0) nChunk++;
									
									buf.clear();
									buf.putInt(nChunk);
									while (nChunk>0) {
										buf.put(s.getBytes(), 0, Integer.min(14, len));
										buf.flip();
										client.write(buf);;
										len -= 14;
										nChunk--;
										buf.clear();
										buf.putInt(Integer.min(14, len));
										// Scala caratteri della stringa
										if (s.length()>14)	s=s.substring(14);
									}
									break;
									
									
								// mostra_punteggio -> restituisce il punteggio
								case -4:
									try {
										result = server.punteggio((String)((MyPair<String,Integer>) key.attachment()).getKey());
									}
									catch (UserNotFoundException unfe) { result = -1; }
									catch (IllegalArgumentException il){ result = -2; }
									finally{
										buf.clear();
										buf.putInt(result);
										buf.flip();
										client.write(buf);
										break;
									}
									
								// Classifica -> calcola e invia in formato JSON
								case -5:
									JSONObject jobj = server.classifica((String)((MyPair<String,Integer>) key.attachment()).getKey());
									String str = jobj.toJSONString();
									
									// Invia chunk di stringa:
									// Primo chunk: n.chunk + 14 bytes
									// Successivi: lunghezza chunk + 14 bytes al massimo
									int ln = str.length();
									int nChunks = ln/14;
									if (ln%14!=0) nChunks++;
									
									buf.clear();
									buf.putInt(nChunks);
									while (nChunks>0) {
										buf.put(str.getBytes(), 0, Integer.min(14, ln));
										buf.flip();
										client.write(buf);
										ln -= 14;
										nChunks--;
										buf.clear();
										buf.putInt(Integer.min(14, ln));
										// Scala caratteri della stringa
										if (str.length()>14)	str=str.substring(14);
									}
									break;
									
								// Sfida!
								case 42:
									int leng = buf.getInt();		// lunghezza nome
									byte[] bytess = new byte[leng];
									buf.get(bytess,0,leng);
									String frnd = new String(bytess);
									
									// Controlla che i due possano giocare insieme
									if (!((WQServer) server).canWePlay(
											(String)((MyPair<String,Integer>) key.attachment()).getKey(),frnd )){
										buf.clear();
										buf.putInt(-7);
										buf.flip();
										client.write(buf);
										continue;
									}
									
									// Cerca porta dell'utente frnd
									int p=-1;
									Iterator<SelectionKey> it = selector.keys().iterator();
									SelectionKey selected=null;	// punterà alla chiave del client amico (player2)
									while (it.hasNext()){
										selected = it.next();
										
										// Ignora la chiave di accettazione richieste
										if (selected.interestOps()==16)
											continue;
										
										// Se utente==frnd -> memorizza porta in p
										p = ((MyPair<String,Integer>)selected.attachment()).getKey().equals(frnd)?
												((MyPair<String,Integer>)selected.attachment()).getValue() : p;
										
										// Esce dal ciclo, in modo da preservare il puntatore al socketchannel trovato
										if (p!=-1) break;
									}
																		
									// invia sfida al giocatore sfidato (invia un intero)
									int ans = -1;
									if (p!=-1){

										// Giocatore trovato (o libero da altre sfide)
										DatagramSocket ds = new DatagramSocket();
										byte[] b = new byte[1];
										b[0] = new Integer(42).byteValue();
										DatagramPacket dp = new DatagramPacket(
												b,1, InetAddress.getByName("localhost"), p);
										ds.send(dp);
									
										// Attendi risposta dal giocatore sfidato
										SocketChannel client2 = (SocketChannel) selected.channel();
										long time1 = System.currentTimeMillis();
										long time2 = time1;
									
										// Avvia timeout
										while (time2-time1 < 10000) {
											ByteBuffer tmp = ByteBuffer.allocate(1);
											int howmany = client2.read(tmp);
											tmp.flip();
										
											// Valuta byte letti SSE sono stati ricevuti
											if (howmany!=0) {
												ans = new Byte(tmp.get()).intValue();
												break;
											}
											time2 = System.currentTimeMillis();
										}
									}
									
									// invia esito al giocatore sfidante
									buf.clear();
									buf.putInt(ans);
									buf.flip();
									client.write(buf);
									if (ans==-1)
										System.out.println("Timeout scaduto: sfida rifiutata");
									
									// Altrimenti -> lancia un thread per la sfida
									else {
										
										// Invoca un nuovo thread che si occupi della sfida
										server.sfida(key,selected, selector);
										
										// De-registra le chiavi dei giocatori dal selettore
										// Nota: Vengono ri-registrate prima di terminare l'esecuzione del task!
										key.cancel();
										selected.cancel();
									}
									
							} // end switch
							
						}
					}
					catch (IOException ioe) {
						System.err.println("Errore nell'accettazione di dati da parte di "
								+ client);
						ioe.printStackTrace();
					}
				
				}
			
			} // end iteration
			
		} // end event-loop
		
	} // end main
	
}
