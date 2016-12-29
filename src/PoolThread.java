import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class PoolThread extends Thread implements BasicConnector{
    private Server server;
    private Socket connection;
    private InputStream input;
    private OutputStream output;

    @Override
    public void run() {
        try {
            send("Thread connected to client: " + connection.getLocalSocketAddress());
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String line = inputReader.readLine();
                if (line == null) break;
                System.out.println(connection.getRemoteSocketAddress() + " " +  line);
                process(line);
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
        finally
        {
            if (!connection.isClosed()) try {
                connection.close();
            } catch (IOException ignored) {
            }
            disconnect();
        }
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setConnection(Socket connection) {
        this.connection = connection;
    }

    public void setSC(Server server, Socket connection) throws IOException {
        setServer(server);
        setConnection(connection);
        this.server = server;
        this.connection = connection;
        input = connection.getInputStream();
        output = connection.getOutputStream();
        System.out.println(connection.getRemoteSocketAddress() + " connected");
    }

    private void process(String line) throws IOException {
        line = line.trim();
        int delimiterIndex = line.indexOf(" ");
        String command = delimiterIndex == -1 ? line.toLowerCase()
                : line.toLowerCase().substring(0, delimiterIndex).trim();
        String argument = delimiterIndex == -1 ? "" : line.substring(delimiterIndex).trim();
        switch (command) {
            case "echo":
                send(argument);
                break;
            case "time":
                time();
                break;
            case "close":
                connection.close();
                break;
            case "upload":
                preupload(argument);
                break;
            case "download":
                download(argument);
                break;
            default:
                send("Unknown command: " + command);
        }
    }

    private void time() throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        send(dateFormat.format(date));
    }

    private void preupload(String argument) throws IOException {
        String fileName = argument.substring(0, argument.indexOf("|"));
        long fileSize = Long.parseLong(argument.substring(argument.indexOf("|") + 1));
        File file = new File("uploaded_" + fileName);
        if (!file.exists()) {
            upload(file, 0, fileSize);
        } else {
            Pair<File, String> desiredFile = server.findUnderUploadedFileBy(file.getName());
            if (desiredFile != null) {
                String desiredFileIP = desiredFile.getValue().substring(0, desiredFile.getValue().indexOf(":"));
                String currentClientIP = connection.getRemoteSocketAddress().toString()
                        .substring(0, connection.getRemoteSocketAddress().toString().indexOf(":"));
                if (!desiredFileIP.equals(currentClientIP)) {
                    file.delete();
                    server.underUploadedFiles.remove(desiredFile);
                    upload(file, 0, fileSize);
                } else {
                    upload(file, file.length(), fileSize);
                }
            } else {
                sendQuiet("-1");
                send("File exists");
            }
        }
    }

    private void upload(File file, long offset, long fileSize) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            server.underUploadedFiles.add(new Pair<>(file, connection.getRemoteSocketAddress().toString()));
        }
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(file.getPath()), StandardOpenOption.WRITE);
        long startOffset = offset;
        long startTime = new Date().getTime();
        sendQuiet(String.valueOf(offset));
        while (true) {
            if (offset >= fileSize) {
                Pair<File, String> desiredFile = server.findUnderUploadedFileBy(file.getName());
                if (desiredFile != null) {
                    String desiredFileIP = desiredFile.getValue().substring(0, desiredFile.getValue().indexOf(":"));
                    String currentClientIP = connection.getRemoteSocketAddress().toString()
                            .substring(0, connection.getRemoteSocketAddress().toString().indexOf(":"));
                    if (desiredFileIP.equals(currentClientIP)) {
                        server.underUploadedFiles.remove(desiredFile);
                        send("File " + file.getName() + " was successfully uploaded");
                        fileChannel.close();
                        break;
                    }
                }
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int count = input.read(buffer);
            fileChannel.write(ByteBuffer.wrap(Arrays.copyOfRange(buffer, 0, count)), offset);
            offset += count;
            long time = (new Date().getTime() - startTime);
            double speed = Double.MAX_VALUE;
            if (time != 0) speed = (((offset - startOffset) / time) * 1000 * 8) / 1000000;
        }
    }

    private void download(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            send("No file");
            return;
        }
        send("File was found|" + file.length());
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        RandomAccessFile fileReader = new RandomAccessFile(file, "r");
        String lineFromClient = inputReader.readLine();
        if (lineFromClient == null) throw new IOException();
        long uploadedBytes = Long.parseLong(lineFromClient);
        long startOffset = uploadedBytes;
        long startTime = new Date().getTime();
        while (true) {
            fileReader.seek(uploadedBytes);
            byte[] bytes = new byte[BUFFER_SIZE];
            int countBytes = fileReader.read(bytes);
            if (countBytes <= 0) {
                break;
            }
            send(bytes, countBytes);
            uploadedBytes += countBytes;
            long time = (new Date().getTime() - startTime);
            double speed = Double.MAX_VALUE;
            if (time != 0) speed = (((uploadedBytes - startOffset) / time) * 1000 * 8) / 1000000;
        }
    }

    private void send(String data) throws IOException {
        data += "\r\n";
        output.write(data.getBytes());
        output.flush();
        System.out.print(connection.getRemoteSocketAddress() + " Server: " + data);
    }

    private void send(byte[] data, int count) throws IOException {
        output.write(data, 0, count);
        output.flush();
    }

    private void sendQuiet(String data) throws IOException {
        data += "\r\n";
        output.write(data.getBytes());
        output.flush();
    }

    private void disconnect() {
        System.out.println(connection.getRemoteSocketAddress() + " disconnected");
    }

}