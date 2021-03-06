package com.distributedsys;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.distributedsys.Message.*;

/**
 * Created by jpan on 11/14/15.
 */
public class UserConsole {

    public static String outputname = "defaultout.txt";
    public static void main(String[] a) {
        try {
            File portfile = new File("port.txt");
            List<Integer> portlist = ReadPortFile(portfile);
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            int id = 0;
            Runnable returnRequestHandler = new ReturnRequestHandler();
            new Thread(returnRequestHandler).start();

            while (true) {
                System.out.println("InputFilename:");
                String line=sin.readLine();
                String filename = new String(line);
                System.out.println("OutputFilename:");
                line=sin.readLine();
                outputname = new String(line);
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
                startProcInvoke(initlist, procType,filename, id++);
            }

        }
        catch(Exception ex)
        {
            System.err.println(ex.getStackTrace());
        }

    }

    private static void startProcInvoke(List<Integer> initList, ProcType procType, String data, int id) throws Exception{
        List<Kicknode> kicknodeList = new ArrayList<>();
        for (int port : initList){
            MessageClosure<KickMessage> kickMessageClosure = new MessageClosure<>(new KickMessage(procType,data,port,id));
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

    private static class ReturnRequestHandler implements Runnable{
        @Override
        public void run() {
            Set<Integer> idSet = new HashSet<>();
            ServerSocket srvr = null;
            try{
                srvr = new ServerSocket(userport);
            }catch (IOException e){
                System.err.println(e.getStackTrace());
            }
            while (true) {
               try{
                   Socket clientSocket = srvr.accept();
//                   System.out.print("\nReturn end has connected!\n");
                   InputStream inputStream = clientSocket.getInputStream();
                   MessageClosure receivedmessage = (MessageClosure) socketObjReceive(inputStream);
                   if (receivedmessage.getMyType() == FinalMessage.class){
                       FinalMessage finalMessage = (FinalMessage) receivedmessage.getObject();
                       if (!idSet.contains(finalMessage.getId())){
                           idSet.add(finalMessage.getId());
                           System.out.println(finalMessage);
                           ProcType procType = finalMessage.getProctype();
                           switch (procType){
                               case ENC:
                                   RsaKeyEncryption encryptor = new RsaKeyEncryption(0);
                                   encryptor.finalwritetofile(finalMessage.getData(), outputname);
                                   System.out.println("Enc Done, ID: "+ finalMessage.getId());
                                   break;
                               case DEC:
                                   RsaKeyDecryption decryptor = new RsaKeyDecryption(0);
                                   String finaldata = decryptor.finalwritetofile(finalMessage.getData(), outputname);
                                   System.out.println("Dec Done, ID: "+ finalMessage.getId());
                                   System.out.println("Data: "+ finaldata);
                                   break;
                           }
                       }else{
                           System.out.println("Throw away duplicated message");
                       }
                   } else {
                       System.err.println("Expect to receive final package");
                   }


               } catch (Exception e){
                   System.err.println(e.getStackTrace());
               }
            }
        }
    }


}
