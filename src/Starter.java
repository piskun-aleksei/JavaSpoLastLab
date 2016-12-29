import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class Starter {

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println("1 for Client");
            System.out.println("2 for Server");
            try {
                switch (Integer.parseInt(reader.readLine())) {
                    case 1:
                        new Client().up();
                        break;
                    case 2:
                        new Server(6790, 10).listen();
                        break;
                    case 3:
                        break;
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }
}
