package Zad5;

public class Chat {
    static public void main(String[] args) throws Exception {
        new Thread(new Receive()).start();
        new Thread(new Send("Chat1")).start();
    }
}