JC = javac
LIBS = -cp .:json-simple-1.1.1.jar
JE = java

.PHONY: server client

default:
		$(JC) $(LIBS) ./myExceptions/*.java ./users/*.java ./client/*.java ./server/*.java

server:
		$(JE) $(LIBS) server.MainClass

client:
		$(JE) $(LIBS) client.MainClass

clean:
		-rm -f client/*.class myException/*.class server/*.class users/*.class *AND*
