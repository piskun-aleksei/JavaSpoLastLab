import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class Application {

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        loop:
        while (true) {
            System.out.println("1. Client");
            System.out.println("2. Server");
            System.out.println("3. Exit");
            try {
                switch (Integer.parseInt(reader.readLine())) {
                    case 1:
                        new Client().up();
                        break;
                    case 2:
                        new Server(6790, 10).listen();
                        break;
                    case 3:
                        break loop;
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }
}
