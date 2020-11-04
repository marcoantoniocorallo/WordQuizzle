/**
 * Classe ausiliare che accoppia due oggetti, utilizzata per <nomeutente,portaListener>
 * @author Marco Antonio Corallo, 531466
 */

package server;

public class MyPair<K,V> {
	private K key;
	private V value;
	
	public MyPair(K k, V v){
		this.key=k;
		this.value=v;
	}
	
	public K getKey(){ return key;}
	public V getValue() {return value;}
}
