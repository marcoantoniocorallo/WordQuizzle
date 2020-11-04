package myExceptions;

public class AlreadyRegisteredException extends Exception {
	public AlreadyRegisteredException(){ super(); }
	public AlreadyRegisteredException(String s){ super(s); }
}
