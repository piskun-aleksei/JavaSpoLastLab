import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class Client implements BasicConnector {

    private Socket connection;
    private InputStream input;
    private OutputStream output;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
    private List<Pair<File, String>> underDownloadedFiles = new ArrayList<>();

    @Override
    public void connect() {
        try {
            System.out.println("Enter Server's IP: ");
            String ip = user.readLine();
            System.out.println("Enter Server's PORT: ");
            String portLine = user.readLine();
            if (portLine.equals("")) return;
            Integer port = Integer.parseInt(portLine);
            connectToServer(ip, port);
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    private void connectToServer(String ip, Integer port) {
        try {
            connection = new Socket(ip, port);
            input = connection.getInputStream();
            output = connection.getOutputStream();
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String lineFromServer = inputReader.readLine();
                if (lineFromServer == null) throw new IOException();
                System.out.println("Server: " + lineFromServer);
                while (true) {
                    String lineFromUser = user.readLine();
                    if (process(lineFromUser)) break;
                }
            }
        } catch (IOException ignored) {
        }
        disconnect();
    }

    private boolean process(String line) throws IOException {
        line = line.trim();
        int delimiterIndex = line.indexOf(" ");
        String command = delimiterIndex == -1 ? line.toLowerCase()
                : line.toLowerCase().substring(0, delimiterIndex).trim();
        String argument = delimiterIndex == -1 ? "" : line.substring(delimiterIndex).trim();
        switch (command) {
            case "upload":
                return upload(argument, line);
            case "download":
                predownload(argument, line);
                return false;
            default:
                send(line);
        }
        return true;
    }

    private boolean upload(String filename, String command) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No file");
            return false;
        }
        send(command + "|" + file.length());
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        RandomAccessFile fileReader = new RandomAccessFile(file, "r");
        String lineFromServer = inputReader.readLine();
        if (lineFromServer == null) throw new IOException();
        long uploadedBytes = Long.parseLong(lineFromServer);
        if (uploadedBytes == -1) return true;
        long startOffset = uploadedBytes;
        long startTime = new Date().getTime();
        while (true) {
            fileReader.seek(uploadedBytes);
            byte[] bytes = new byte[BUFFER_SIZE];
            int countBytes = fileReader.read(bytes);
            if (countBytes <= 0) break;
            send(bytes, countBytes);
            uploadedBytes += countBytes;
            long time = (new Date().getTime() - startTime);
            double speed = Double.MAX_VALUE;
            if (time != 0) speed = (((uploadedBytes - startOffset) / time) * 1000 * 8) / 1000000;
        }
        return true;
    }

    private void predownload(String filename, String command) throws IOException {
        File file = new File("downloaded_" + filename);
        if (file.exists()) {
            Pair<File, String> desiredFile = findUnderDownloadedFileBy(file.getName());
            if (desiredFile != null) {
                Long fileSize = checkFileOnRemote(command);
                if (fileSize == null) return;
                if (desiredFile.getValue().equals(connection.getRemoteSocketAddress().toString())) {
                    download(file, file.length(), fileSize);
                } else {
                    file.delete();
                    underDownloadedFiles.remove(desiredFile);
                    download(file, 0, fileSize);
                }
            } else {
                System.out.println("File already exists");
            }
        } else {
            Long fileSize = checkFileOnRemote(command);
            if (fileSize == null) return;
            download(file, 0, fileSize);
        }
    }

    private Long checkFileOnRemote(String command) throws IOException {
        send(command);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        String line = inputReader.readLine();
        if (line == null) throw new IOException();
        System.out.println("Server: " + line);
        if (line.trim().equals("No file")) {
            return null;
        } else {
            return Long.parseLong(line.trim().substring(line.indexOf("|") + 1));
        }
    }

    private void download(File file, long offset, long fileSize) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            underDownloadedFiles.add(new Pair<>(file, connection.getRemoteSocketAddress().toString()));
        }
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(file.getPath()), StandardOpenOption.WRITE);
        long startOffset = offset;
        long startTime = new Date().getTime();
        sendQuiet(String.valueOf(offset));
        double speed = 0;
        while (true) {
            if (offset >= fileSize) {
                Pair<File, String> desiredFile = findUnderDownloadedFileBy(file.getName());
                if (desiredFile != null) {
                    if (desiredFile.getValue().equals(connection.getRemoteSocketAddress().toString())) {
                        underDownloadedFiles.remove(desiredFile);
                        System.out.println("File was downloaded with a speed of: " + speed + " MBit/s");
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
            speed = Double.MAX_VALUE;
            if (time != 0) speed = (((offset - startOffset) / time) * 1000 * 8) / 1000000;
        }
    }

    private Pair<File, String> findUnderDownloadedFileBy(String name) {
        for (Pair<File, String> underDownloadedFile : underDownloadedFiles) {
            if (name.toUpperCase().equals(underDownloadedFile.getKey().getName().toUpperCase())) {
                return underDownloadedFile;
            }
        }
        return null;
    }

    private void sendQuiet(String data) throws IOException {
        data += "\r\n";
        output.write(data.getBytes());
        output.flush();
    }

    private void send(String data) throws IOException {
        data += "\r\n";
        output.write(data.getBytes());
        output.flush();
        System.out.print("Client: " + data);
    }

    private void send(byte[] data, int count) throws IOException {
        output.write(data, 0, count);
        output.flush();
    }

    private void disconnect() {
        System.out.println(connection.getRemoteSocketAddress() + " disconnected");
    }
}
