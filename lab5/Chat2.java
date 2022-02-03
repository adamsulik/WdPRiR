package lab5;

public class Chat2 {
    static public void main(String[] args) throws Exception {
        new Thread(new Receive()).start();
        new Thread(new Send("Chat2")).start();
    }
}
