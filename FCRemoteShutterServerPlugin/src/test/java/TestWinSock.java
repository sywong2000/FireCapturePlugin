import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestWinSock {
    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader in;
    public static void main(String[] args) throws Exception
    {
        clientSocket = new Socket("localhost",8088);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        System.out.println(sendMessage("6"));
        Thread.sleep(1000);
        System.out.println(sendMessage("1"));
        Thread.sleep(10000);
        System.out.println(sendMessage("6"));
        out.close();
        in.close();
        clientSocket.close();
    }

    static public String sendMessage(String msg) throws Exception
    {
        out.println(msg);
        String resp = in.readLine();
        return resp;
    }

    static public void stopConnection() throws Exception
    {
        in.close();
        out.close();
        clientSocket.close();
    }
}
