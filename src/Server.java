import javafx.util.Pair;

import javax.sound.midi.Soundbank;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class Server {

    private ServerSocket server;
    protected List<Pair<File, String>> underUploadedFiles = new ArrayList<>();
    private ThreadPool pool;
    private List<Integer> conectionNubmers;
    private int poolCapacity;

    public Server(int port, int poolCapacity) {
        try {
            this.poolCapacity = poolCapacity;
            server = new ServerSocket(port);
            pool = new ThreadPool(poolCapacity);
            conectionNubmers = new ArrayList<>(poolCapacity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        if (server != null) {
            System.out.println("Server is up");
            while (true) {
                try {
                    Integer connection = pool.startConnection(this, server.accept());
                    if(connection == null){
                        System.out.println("Pool is full");
                    }
                    else {
                        conectionNubmers.add(connection);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected Pair<File, String> findUnderUploadedFileBy(String name) {
        for (Pair<File, String> underUploadedFile : underUploadedFiles) {
            if (name.toUpperCase().equals(underUploadedFile.getKey().getName().toUpperCase())) {
                return underUploadedFile;
            }
        }
        return null;
    }


}