import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Aliaksei_Piskun1 on 12/29/2016.
 */
public class ThreadPool {
    private ArrayList<PoolThread> threads;
    private int capacity;

    public ThreadPool(int poolCapacity){
        capacity = poolCapacity;
        threads = new ArrayList<>(poolCapacity);
        for(int i = 0; i < poolCapacity; i ++){
            threads.add(new PoolThread());
        }
    }

    public Integer startConnection(Server server, Socket connection) throws IOException {

        Integer result = null;
        for(int i = 0 ; i < capacity; i++){
            PoolThread thread = threads.get(i);
            if(thread.getState().equals(Thread.State.NEW) || thread.getState().equals(Thread.State.TERMINATED)){
                result = i;
                if(thread.getState() == Thread.State.NEW){
                    thread.setSC(server, connection);
                    thread.start();
                }
                else {
                    thread = new PoolThread();
                    thread.setSC(server, connection);
                    thread.start();
                }
                break;
            }
        }
        return result;
    }
}
