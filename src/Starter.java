import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class Starter {

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        BasicConnector connector = null;
        while (true) {
            System.out.println("1 for Server");
            System.out.println("2 for Client");

            try {
                switch (Integer.parseInt(reader.readLine())) {
                    case 1:
                        connector = new Server();
                        break;
                    case 2:
                        connector = new Client();
                        break;
                    case 3:
                        break;
                }
                connector.connect();
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }
}
