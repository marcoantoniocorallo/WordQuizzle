/**
 * Thread WQ per la sfida tra client
 * @author Marco Antonio Corallo, 531466
 */

package server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class Sfida implements Runnable {
	private SocketChannel giocatore1;
	private String			utente1;
	private int				punteggio1;
	private int				port1;
	private SocketChannel giocatore2;
	private String			utente2;
	private int				punteggio2;
	private int				port2;
	
	private String[]		wordList;
	private String[]		translatedList;
	private int 			numParole;
	private Server			server;
	private Selector		selector;
	
	private final int		rispostacorretta = 3;
	private final int		rispostasbagliata = -2;
	
	private File			logpartita;
	
	public Sfida(SelectionKey g1, SelectionKey g2, Server server, Selector selector){
		this.giocatore1 = (SocketChannel) g1.channel();
		this.giocatore2 = (SocketChannel) g2.channel();
		this.utente1	= (((MyPair<String, Integer> )g1.attachment() ).getKey());
		this.utente2	= (((MyPair<String, Integer> )g2.attachment() ).getKey());
		this.port1	= (((MyPair<String, Integer> )g1.attachment() ).getValue());
		this.port2	= (((MyPair<String, Integer> )g2.attachment() ).getValue());
		this.punteggio1 = 0;
		this.punteggio2 = 0;
		this.server = server;
		this.selector = selector;
		
		Random r = new Random();
		numParole = 10;
		
		// Individua file
		RandomAccessFile reader=null;
		try {
			File file = new File("./dizionario.txt");
			reader = new RandomAccessFile(file,"r");
			
			logpartita = new File(utente1+"AND"+utente2+".txt");
			logpartita.createNewFile();
			
			// Scegli parole
			wordList=new String[numParole];
			for (int i = 0; i < numParole; i++) {
				
				// Genera un numero di linea casuale all'interno del file
				int k = r.nextInt(1067);	// numero di caratteri nel file, tolta l'ultima riga
				reader.seek(k);
				while ( (char)reader.readByte() != '\n') ;
				
				wordList[i]=reader.readLine();
			}
			
			// Ottieni parole tradotte
			this.translatedList = ottieniTraduzioni();
			
		}
		catch (FileNotFoundException f) { f.printStackTrace(); }
		catch (IOException ioe ) { ioe.printStackTrace(); }
	}
	
	// Traduce la wordlist tramite API REST
	private String[] ottieniTraduzioni(){
		
		String[] translatedList = new String[wordList.length];
		int i = 0;
		
		try {
			
			for (String word :
					wordList) {
				
				// Specifica URL e apri connessione
				URL url = new URL("https://api.mymemory.translated.net/get?q="
						+ URLEncoder.encode(word, "UTF8") + "&" + "langpair=it|en");
				URLConnection uc = url.openConnection();
				uc.connect();
				
				// Ricevi dati
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					
					// Risposta (inputline) in formato JSON:
					// {"responseData":{"translatedText":"as","match":0.99},... <Garbage>
					JSONParser parser = new JSONParser();
					JSONObject obj = (JSONObject) parser.parse(inputLine);
					obj = (JSONObject) obj.get("responseData");
					translatedList[i++] = ((String) obj.get("translatedText")).toLowerCase();
				}
				in.close();
			}
		}
		catch (UnsupportedEncodingException e){ e.printStackTrace();}
		catch (IOException ioe) { ioe.printStackTrace();}
		catch (ParseException p) { p.printStackTrace();}
		
		return translatedList;
	}
	
	@Override
	public void run() {
		
		System.out.println("Sta per iniziare la sfida tra " + utente1 + " e " + utente2);
		
		// Selettore locale in cui vengono registrati i messaggi inviati dagli utenti
		Selector newselector = null;
		
		try {
			newselector = Selector.open();
			giocatore1.register(newselector, SelectionKey.OP_READ);
			giocatore2.register(newselector, SelectionKey.OP_READ);
		}catch (IOException ioe) { ioe.printStackTrace(); }
		
		// Buffer in cui ricevere i messaggi
		ByteBuffer buf = ByteBuffer.allocate(24);
		
		// Avvia timer ed invia prima parola ad entrambi
		long time1 = System.currentTimeMillis();
		int i = 0; int j = 0;
		
		BufferedOutputStream writer = null;
		
		try {
			
			// Scrive a file i riscontri dei giocatori
			writer = new BufferedOutputStream(
					new FileOutputStream(logpartita.getPath(),true));
			
			// Invia a g1
			buf.putInt(wordList[i].length());
			buf.put(wordList[i].getBytes(),0,wordList[0].length());
			buf.flip();
			giocatore1.write(buf);
			buf.clear();
			
			// Invia a g2
			buf.putInt(wordList[i].length());
			buf.put(wordList[i].getBytes(),0,wordList[0].length());
			buf.flip();
			giocatore2.write(buf);
			buf.clear();
		}
		catch (IOException e) { e.printStackTrace(); }
		
		while (true) {
			try {
				
				// Controlla se il gioco è finito!
				long time2 = System.currentTimeMillis();
				if ((i==numParole && j==numParole) || (time2-time1 > 30000))
					break;
				
				// Si blocca per un totale di 30 secondi, a partire dalla prima parola data
				newselector.select(30000-(time2-time1));
			}
			catch (IOException ioe ) { ioe.printStackTrace(); }
			
			// SelectionKeys canali pronti
			Set<SelectionKey> readyKeys = newselector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
			
			while (iterator.hasNext()) {
				
				// seleziona evento e lo rimuove dagli eventi pronti (non dagli eventi registrati!)
				SelectionKey key = iterator.next();
				iterator.remove();
				
				// Se evento == dati in arrivo -> Legge operazione da effettuare e la gestisce
				if (key.isReadable()) {
					
					SocketChannel client=null;
					int n=-1;
					try {
						client = (SocketChannel) key.channel();
						client.read(buf);
						buf.flip();
					}
					catch (IOException ioe) { ioe.printStackTrace(); }
					
					// Controlla che ci siano dati nel buffer
					if (buf.hasRemaining()) {
						
						// Prepara buffer a ricevere
						n = buf.getInt();
						byte[] bytes = new byte[n];
						buf.get(bytes,0,n);
						buf.clear();
						
						try {
							
							// Se g1 -> Controlla correttezza, aggiorna punteggio e manda prossimo
							if (key.channel().equals(giocatore1)) {
								String parola = new String(bytes);
								
								// Verifica correttezza risposta e assegna punti
								if (parola.equals(translatedList[i]))
									punteggio1+=rispostacorretta;
								else
									punteggio1+=rispostasbagliata;
								
								// Scrivi a file
								String toWrite = (utente1+","+wordList[i]+","+parola+","+translatedList[i]+","+punteggio1+"\n");
								writer.write(toWrite.getBytes());
								i++;
								
								// Se parole finite -> invia avviso e cancella chiave
								if (i>=numParole) {
									buf.putInt(-3);
									
									// Cancella chiave
									key.cancel();
								}
								else {
									buf.putInt(wordList[i].length());
									buf.put(wordList[i].getBytes(), 0, wordList[i].length());;
								}
								buf.flip();
								giocatore1.write(buf);
							}
							
							// Se g2 -> Controlla correttezza, aggiorna punteggio e manda prossimo
							if (key.channel().equals(giocatore2)) {
								String parola = new String(bytes);
								
								// Verifica correttezza risposta e assegna punti
								if (parola.equals(translatedList[j]))
									punteggio2+=rispostacorretta;
								else
									punteggio2+=rispostasbagliata;
								
								// Scrivi a file
								String toWrite = (utente2+","+wordList[j]+","+parola+","+translatedList[j]+","+punteggio2+"\n");
								writer.write(toWrite.getBytes());
								j++;
								
								// Se parole finite -> invia avviso e cancella chiave
								if (j>=numParole) {
									buf.putInt(-3);
									
									// Cancella chiave
									key.cancel();
								}
								else {
									buf.putInt(wordList[j].length());
									buf.put(wordList[j].getBytes(), 0, wordList[j].length());
								}
								buf.flip();
								giocatore2.write(buf);
								
							}
							buf.clear();
						}
						catch (IOException ioe ){ ioe.printStackTrace(); }
						
					}
					
				}
			
			} // end iteration
			
		} // end event loop
		
		// ri-registra le chiavi nel selettore principale e chiude il selettore secondario
		try {
			newselector.close();
			giocatore1.register(selector, SelectionKey.OP_READ, new MyPair<String, Integer>(utente1, port1));
			giocatore2.register(selector, SelectionKey.OP_READ, new MyPair<String, Integer>(utente2, port2));
		}
		catch (ClosedChannelException cl) { cl.printStackTrace(); }
		catch (IOException ioe ) {ioe.printStackTrace();}
		
		// Comunica punteggi e vincitore.
		System.out.println("Gioco terminato.");
		
		// Invia -42 al vincitore, -2 al perdente (seguiti dai punteggi), -21 se pareggiano
		try {
			if (punteggio1 > punteggio2) {
				buf.putInt(-42);
				buf.putInt(punteggio1);
				buf.flip();
				giocatore1.write(buf);
				punteggio1+=5;
				
				buf.clear();
				buf.putInt(-2);
				buf.putInt(punteggio2);
				buf.flip();
				giocatore2.write(buf);
			} else if (punteggio2 > punteggio1){
				buf.putInt(-42);
				buf.putInt(punteggio2);;
				buf.flip();
				giocatore2.write(buf);
				punteggio2+=5;
				
				buf.clear();
				buf.putInt(-2);
				buf.putInt(punteggio1);
				buf.flip();
				giocatore1.write(buf);
			}
			else {
				buf.putInt(-21);
				buf.putInt(punteggio2);;
				buf.flip();
				giocatore2.write(buf);
				
				buf.clear();
				buf.putInt(-21);
				buf.putInt(punteggio1);
				buf.flip();
				giocatore1.write(buf);
			}
			// Chiude lo scrittore
			writer.close();
		}
		catch (IOException ioe ) { ioe.printStackTrace(); }
		
		// Aggiorna punteggi
		((WQServer)server).aggiornaPunteggioDi(utente1,punteggio1,utente2,punteggio2);
		return;
	}
}
