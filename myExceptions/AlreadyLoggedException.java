package myExceptions;

public class AlreadyLoggedException extends Exception {
	public AlreadyLoggedException(){
		super();
	}
	public AlreadyLoggedException(String s){
		super(s);
	}
}
