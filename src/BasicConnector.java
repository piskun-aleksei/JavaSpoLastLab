/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public interface BasicConnector {
    int BUFFER_SIZE = 50 * 1024;
    int PORT = 6790;
    int POOL_SIZE = 10;

    void connect();
}
