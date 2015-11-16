package com.distributedsys;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

import static com.distributedsys.Message.*;

/**
 * Created by jpan on 11/14/15.
 */
public class CryptoNode {

    private static LinkedBlockingQueue<RequestPackage> kickmessageQ;
    private static LinkedBlockingQueue<RequestPackage> nodemessageQ;
    private static List<Integer> portList;
    private static List<Integer> keyFrgList;
    private static int nodeIdx;

    private static class RequestPackage {
        public Socket socket;
        public MessageClosure messageClosure;

        public RequestPackage(Socket socket, MessageClosure messageClosure) {
            this.socket = socket;
            this.messageClosure = messageClosure;
        }
    }


    public static void main(String[] a) {
        try{
            if (a.length<1) {
                System.out.println("Usage:");
                System.out.println("input node id");
                return;
            }

            File portFile = new File("port.txt");
            portList = ReadPortFile(portFile);
            kickmessageQ = new LinkedBlockingQueue<RequestPackage>();
            nodemessageQ = new LinkedBlockingQueue<RequestPackage>();
            nodeIdx = Integer.parseInt(a[0]);

            ServerSocket srvr = new ServerSocket(portList.get(nodeIdx));
            System.out.println("Node " + nodeIdx + " Port " + portList.get(nodeIdx) + " socket opened");
            keyFrgList = getKeyfrgList(nodeIdx,new File("keyfrg_map.txt"));


            Runnable kickRequestHandler = new KickRequestHandler();
            new Thread(kickRequestHandler).start();
            Runnable nodeRequestHandler = new NodeRequestHandler();
            new Thread(nodeRequestHandler).start();

            while(true){
                Socket clientSocket = srvr.accept();
                System.out.print("\nServer has connected!\n");
                InputStream inputStream = clientSocket.getInputStream();
                MessageClosure receivedmessage = (MessageClosure) socketObjReceive(inputStream);
                RequestPackage requestPackage = new RequestPackage(clientSocket, receivedmessage);
                if (receivedmessage.getMyType() == KickMessage.class){
                    kickmessageQ.put(requestPackage);
                    System.out.println("Kick Request enter Queue"+requestPackage.messageClosure.getObject().toString());
                } else {
                    nodemessageQ.put(requestPackage);
                    System.out.println("Node Request enter Queue: "+requestPackage.messageClosure.getObject().toString());
                }
            }




        } catch (Exception e){
            System.err.println(e.getStackTrace());
        }

    }



    private static class KickRequestHandler implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    RequestPackage requestPackage = kickmessageQ.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
//                    OutputStream out = socket.getOutputStream();
//                    InputStream in = socket.getInputStream();
                    if (messageClosure.getMyType() != KickMessage.class){
                        System.err.println("Expecting kick message from the closure");
                        throw new Exception();
                    }
                    KickMessage kickMessage = (KickMessage) messageClosure.getObject();
                    socket.close();

                    String intervalue = null;

                    switch (kickMessage.getProctype()){
                        case ENC:
                            RsaKeyEncryption encryptor = new RsaKeyEncryption(keyFrgList.get(0));
                            intervalue = encryptor.initencrypt(kickMessage.getFilename());
                            System.out.println("Init Encryting with key "+keyFrgList.get(0));
                            break;
                        case DEC:
                            RsaKeyDecryption decryptor = new RsaKeyDecryption(keyFrgList.get(0));
                            intervalue = decryptor.initdecrypt(kickMessage.getFilename());
                            System.out.println("Init Decryting with key "+keyFrgList.get(0));
                            break;
                        default:
                            System.err.println("Expecting ENC/DEC");
                            throw new Exception();
                    }
                    List<Integer> newkeyFrgList = new ArrayList<>(keyFrgList);
                    newkeyFrgList.remove(0);
                    for (int keyfrg : newkeyFrgList){
                        switch (kickMessage.getProctype()){
                            case ENC:
                                RsaKeyEncryption encryptor = new RsaKeyEncryption(keyfrg);
                                intervalue = encryptor.interencrypt(intervalue);
                                System.out.println("Encryting with key "+keyfrg);
                                break;
                            case DEC:
                                RsaKeyDecryption decryptor = new RsaKeyDecryption(keyfrg);
                                intervalue = decryptor.interdecrypt(intervalue);
                                System.out.println("Decryting with key "+keyfrg);
                                break;
                            default:
                                System.err.println("Expecting ENC/DEC");
                                throw new Exception();
                        }
                    }
                    List<Integer> nextPortList = getNextNodePortList(nodeIdx, new File("node_map.txt"), portList, makeSequence(0,nodeNum));
                    PathMessage pathMessage = new PathMessage(kickMessage.getProctype(),
                                                                kickMessage.getFilename(),
                                                                kickMessage.getDestnode(),
                                                                intervalue,
                                                                new ArrayList<Integer>(Collections.singletonList(nodeIdx))
                                                                );
                    System.out.println("next ports are: "+nextPortList);
                    NextProcInvoke(nextPortList,pathMessage);

//                    out.flush();
//                    in.close();
//                    out.close();

                } catch (Exception e) {
                    System.err.println(e.getStackTrace());
                }
            }

        }
    }

    private static void NextProcInvoke(List<Integer> nextPortList, PathMessage pathMessage) throws Exception{
        List<PathNode> pathNodeList = new ArrayList<>();
        MessageClosure<PathMessage> pathMessageClosure = new MessageClosure<>(pathMessage);
        for (int port : nextPortList){
            pathNodeList.add(new PathNode(port, pathMessageClosure));
        }
        if (pathNodeList.size() >0) {
            ExecutorService taskRecoverExecutor = Executors.newFixedThreadPool(pathNodeList.size());
            List<Future<Boolean>> results = taskRecoverExecutor.invokeAll(pathNodeList, 5, TimeUnit.SECONDS);
            System.out.println("next proc sent");
            taskRecoverExecutor.shutdown();
            Boolean atLeastOneNode = false;
            for (Future<Boolean> result : results) {
                try {
                    if(result.get()){
                        atLeastOneNode = true;
                    }
                } catch (CancellationException e) {
                    System.err.println("Nect proc cancelled: " + e.getClass().getName() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Next proc unknown exception: " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
            if (!atLeastOneNode){
                System.err.println("All nodes after me are dead now");
            }else {
                System.out.println("At least one node after me receives message");
            }
        }
    }

    private static class PathNode implements Callable<Boolean> {
        private final int port;
        private final MessageClosure<PathMessage> message ;

        public PathNode(int port, MessageClosure<PathMessage> message) {
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

    private static class NodeRequestHandler implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    RequestPackage requestPackage = nodemessageQ.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
                    if (messageClosure.getMyType() != PathMessage.class){
                        System.err.println("Expecting path message from the closure");
                        throw new Exception();
                    }

                    PathMessage pathMessage = (PathMessage) messageClosure.getObject();
                    ProcType type = pathMessage.getProctype();
                    System.out.println("Request is type: "+ type.name());
                    socket.close();
                    List<Integer> path = pathMessage.getPath();
                    String intervalue = pathMessage.getIntervalue();
                    List<Integer> enList = getEncfrg(new File("keyfrg_map.txt"), path, keyFrgList);
                    System.out.println("these keys needed to be proc: "+ enList);

                    List<Integer> matchedPath = matchPath(new File("node_map.txt"), path);
                    path.add(nodeIdx);
                    List<Integer> nextPortList = getNextNodePortList(nodeIdx, new File("node_map.txt"), portList, matchedPath);
                    System.out.println("Request is type: "+ type.name());
                    for (int keyfrg : enList){
                            switch (type){
                                case ENC:
                                    RsaKeyEncryption encryptor = new RsaKeyEncryption(keyfrg);
                                    intervalue = encryptor.interencrypt(intervalue);
                                    System.out.println("Encryting with key "+keyfrg);
                                    break;
                                case DEC:
                                    RsaKeyDecryption decryptor = new RsaKeyDecryption(keyfrg);
                                    intervalue = decryptor.interdecrypt(intervalue);
                                    System.out.println("Decryting with key "+keyfrg);
                                    break;
                                default:
                                    System.err.println("Expecting ENC/DEC");
                                    throw new Exception();
                            }

                    }



                    pathMessage = new PathMessage(pathMessage.getProctype(),
                            pathMessage.getFilename(),
                            pathMessage.getDestnode(),
                            intervalue,
                            path
                    );
                    System.out.println("next ports are: "+nextPortList);

                    if (nextPortList.size() >0 ){
                        NextProcInvoke(nextPortList,pathMessage);
                    } else{
                        System.out.println("Final value is: "+intervalue);
                    }

                } catch (Exception e) {
                    System.err.println(e.getStackTrace());
                }
            }
        }
    }
}
