# WordQuizzle
###### Progetto universitario per il corso di Reti di calcolatori e laboratorio - Università di Pisa, a.a. 2019/2020

Il modulo di Laboratorio del corso di Reti di calcolatori prevede lo svolgimento di un progetto da sviluppare utilizzando il linguaggio Java in ambiente Linux.
Il progetto deve essere svolto singolarmente da un solo studente.
Il progetto dovrà essere sviluppato e documentato utilizzando gli strumenti, le tecniche e le convenzioni presentate durante il corso.

L’obiettivo del progetto è la realizzazione di un sistema client-server, che permette l’interazione fra coppie di clients al fine di sfidarsi in gare di traduzioni italiano-inglese del maggior numero di parole fornite dal server.
Il server si occupa della verifica delle risposte e dell'assegnazione di un punteggio per ogni risposta, nonchè del mantenimento del database utenti con relativi punteggi.
Il sistema consente inoltre la gestione di una rete sociale tra gli utenti iscritti.

La specifica di ciascun componente è descritto nel testo del progetto; i dettagli implementativi sono discussi all'interno della relazione.
Il sistema è stato testato su Ubuntu 16.04 e Windows 10, utilizza Java8 e la libreria esterna JSON-Simple (https://github.com/fangyidong/json-simple).

### Compilazione ed esecuzione
`make`: compila i file java

`make server`: esegue un'istanza del server (MAX 1)

`make client`: esegue un'istanza del client

`make clean` : rimuove file temporanei e file class

#### Nota: L'intero progetto è stato sviluppato e distribuito riservando particolare interesse al tempo di consegna.

