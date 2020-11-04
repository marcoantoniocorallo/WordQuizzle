package myExceptions;

public class AlreadySloggedException extends Exception{
	public AlreadySloggedException(){
		super();
	}
	public AlreadySloggedException(String s){
		super(s);
	}
}
