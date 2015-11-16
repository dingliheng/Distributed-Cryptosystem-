package com.distributedsys;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.distributedsys.Message.*;

/**
 * Created by jpan on 11/14/15.
 */
public class UserConsole {
    public static void main(String[] a) {
        try {
            File portfile = new File("port.txt");
            List<Integer> portlist = ReadPortFile(portfile);
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("InputFilename:");
                String line=sin.readLine();
                String filename = new String(line);
                System.out.println("Mode: [E/D]:");
                line=sin.readLine();
                List<Integer> initlist = getInitPort(portlist, new File("node_map.txt"));
                ProcType procType;
                switch (line.trim().toLowerCase()){
                    case "e":
                        procType = ProcType.ENC;
                        break;
                    case "d":
                        procType = ProcType.DEC;
                        break;
                    default:
                        System.out.println("Enter E or D (Encryption or Decryption Mode)");
                        return;
                }
                System.out.println("init nodes: "+ initlist);
                startProcInvoke(initlist, procType,filename);
            }

        }
        catch(Exception ex)
        {
            System.err.println(ex.getStackTrace());
        }

    }

    private static void startProcInvoke(List<Integer> initList, ProcType procType, String data) throws Exception{
        List<Kicknode> kicknodeList = new ArrayList<>();
        for (int port : initList){
            MessageClosure<KickMessage> kickMessageClosure = new MessageClosure<>(new KickMessage(procType,data,port));
            kicknodeList.add(new Kicknode(port, kickMessageClosure));
        }
        if (kicknodeList.size() >0) {
            ExecutorService taskRecoverExecutor = Executors.newFixedThreadPool(kicknodeList.size());
            List<Future<Boolean>> results = taskRecoverExecutor.invokeAll(kicknodeList, 5, TimeUnit.SECONDS);
            System.out.println("Kick start sent");
            taskRecoverExecutor.shutdown();
            Boolean atLeastOneNode = false;
            for (Future<Boolean> result : results) {
                try {
                    if(result.get()){
                        atLeastOneNode = true;
                    }
                } catch (CancellationException e) {
                    System.err.println("Kick start cancelled: " + e.getClass().getName() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Kick start unknown exception: " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
            if (!atLeastOneNode){
                System.err.println("All start nodes are dead now");
            }else {
                System.out.println("At least one start node receives message");
            }
        }
    }

    private static class Kicknode implements Callable<Boolean> {
        private final int port;
        private final MessageClosure<KickMessage> message ;

        public Kicknode(int port, MessageClosure<KickMessage> message) {
            this.port = port;
            this.message = message ;
        }

        @Override
        public Boolean call() {
            try{
                Socket skt = new Socket("localhost", port);
//                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
                socketObjSend(message, out);
                out.flush();
//                in.close();
                out.close();
                skt.close();
                return true;
            }catch (Exception e){
                System.err.println("port "+port+" is dead");
                return false;
            }
        }
    }


}
