package com.distsys.jun;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.distsys.jun.Common.*;

public class TicketServer {

    private static int serverIdx;
    private static LamportClock serverClock;
    private static Seat seat;
    private static LinkedBlockingQueue<RequestPackage> clientMessageQueue;
    private static LinkedBlockingQueue<RequestPackage> serverMessageQueue;
    private static PriorityBlockingQueue<MessageClosure> readRequestQueue;
    private static PriorityBlockingQueue<MessageClosure> writeRequestQueue;
    private static ArrayList<Integer> portList;

    public static Comparator<MessageClosure> lamportClockComparator = new Comparator<MessageClosure>(){
        @Override
        public int compare(MessageClosure o1, MessageClosure o2) {
            if (o1.getTimestamp().getClockValue() != o2.getTimestamp().getClockValue()){
                return o1.getTimestamp().getClockValue() - o2.getTimestamp().getClockValue();
            } else {
                return o1.getTimestamp().getServerId() - o2.getTimestamp().getServerId();
            }

        }
    };

    public static void main(String[] args) {
	// write your code here
        seat = new Seat(ticketNumber);
        try {
            File portFile = new File("port.txt");
            portList = ReadPortFile(portFile);
            if (args.length < 1) {
                System.out.print("Need at least 1 argument");
                System.exit(-1);
            }
            TicketServer.serverIdx = Integer.parseInt(args[0]);
            ServerSocket srvr = new ServerSocket(portList.get(serverIdx));
            System.out.println("Server "+serverIdx+" Port "+ portList.get(serverIdx) +" socket opened");
            serverClock = new LamportClock(serverIdx);
            clientMessageQueue = new LinkedBlockingQueue<RequestPackage>();
            serverMessageQueue = new LinkedBlockingQueue<RequestPackage>();

            readRequestQueue = new PriorityBlockingQueue<MessageClosure>(portList.size()*5, lamportClockComparator);
            writeRequestQueue = new PriorityBlockingQueue<MessageClosure>(portList.size()*5, lamportClockComparator);

            Runnable clientRequestHandler = new ClientRequestHandler();
            new Thread(clientRequestHandler).start();
            Runnable serverRequestHandler = new ServerRequestHandler();
            new Thread(serverRequestHandler).start();

            while (true) {
                Socket clientSocket = srvr.accept();
                System.out.print("\nServer has connected!\n");
//                OutputStream outputStream = clientSocket.getOutputStream();
                InputStream inputStream = clientSocket.getInputStream();
                MessageClosure receivedmessage = (MessageClosure) socketObjReceive(inputStream);
                RequestPackage requestPackage = new RequestPackage(clientSocket, receivedmessage);
                if (receivedmessage.getMyType() == String.class){
                    clientMessageQueue.put(requestPackage);
                } else {
                    serverMessageQueue.put(requestPackage);
                }
//                inputStream.close();
//                outputStream.close();
//                clientSocket.close();
            }

        }
        catch(Exception e) {
            System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
            e.printStackTrace(System.out);
        }
    }

    public static class LamportClock implements Serializable{
        private int clockValue;
        private final int serverId;

        @Override
        public String toString() {
            return "server "+serverId+" clock "+clockValue;
        }

        LamportClock(){
            clockValue = 0;
            serverId = -1;
        }

        LamportClock(int serverId){
            this.serverId = serverId;
        }

        public synchronized void clockInc(){
            clockValue += 1;
        }
        public synchronized void clockInc(LamportClock lamportClock){
            this.clockValue = Math.max(this.clockValue, lamportClock.clockValue)+1;
        }

        public int getClockValue() {
            return clockValue;
        }

        public int getServerId() {
            return serverId;
        }

        @Override
        public boolean equals(Object obj) {
            return (clockValue == ((LamportClock)obj).clockValue) && (serverId == ((LamportClock)obj).serverId);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(clockValue)
                    .append(serverId)
                    .toHashCode();
        }
    }

    public enum RequestType{
        READ,
        WRITE
    }

    public static class RequestCSMessage implements Serializable{
        private RequestType type;

        public RequestCSMessage(RequestType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "RequestCSMessage("+type.name()+")\n";
        }
    }

    public static class AckMessage implements Serializable{
        public AckMessage(){
        }

        @Override
        public String toString() {
            return "Ack\n";
        }
    }

    public static class ReleaseMesage implements Serializable{
        private final LamportClock entry;
        private final MessageClosure<RequestCSMessage> request;
        public ReleaseMesage(MessageClosure<RequestCSMessage> request) {
            this.entry = request.getTimestamp();
            this.request = request;
        }

        public ReleaseMesage(LamportClock uniqueEntry) {
            this.entry = uniqueEntry;
            this.request = null;
        }

        public String toString() {
            return "Release "+entry.toString()+ " request is " + ObjectUtils.toString(request.getObject(), "no info")+"\n";
        }
    }


    private static class ServerCSRequester implements Callable<Boolean>{
        private final int port;
        private final MessageClosure<RequestCSMessage> message;
        public ServerCSRequester(int port, MessageClosure<RequestCSMessage> message){
            this.port = port;
            this.message = message;
        }

        @Override
        public Boolean call() {
            try {
                Socket skt = new Socket("localhost", port);
                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
//                RequestCSMessage thisRequestCSMessage = new RequestCSMessage(type);
//                MessageClosure<RequestCSMessage> requestMessageClosure = new MessageClosure<>(serverClock, thisRequestCSMessage);
                socketObjSend(message, out);
                out.flush();
                MessageClosure messageClosure = (MessageClosure) socketObjReceive(in);
                if (messageClosure.getMyType() != AckMessage.class){
                    System.out.println("ServerCSRequester: expect to receive ack");
                }
                out.close();
                in.close();
                skt.close();
                return true;
            }
            catch(Exception e) {
                System.out.println("exception when requesting from port "+port+": "+ e.getClass().getName()+": "+e.getMessage());
                return false;
            }
        }
    }

    private static class ServerRelease implements Runnable{
        private final int port;
        private final MessageClosure<ReleaseMesage> message;
        public ServerRelease(int port, MessageClosure<ReleaseMesage> message){
            this.port = port;
            this.message = message;
        }
        @Override
        public void run() {
            try{
                Socket skt = new Socket("localhost", port);
//                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
                socketObjSend(message, out);
                out.flush();
                out.close();
//                in.close();
                skt.close();
            }catch (Exception e){
                System.out.println("exception when release to port "+port+": "+ e.getClass().getName()+": "+e.getMessage());
            }

        }
    }

    private static class ServerUpdate implements Runnable{
        private final int port;
        private final MessageClosure<HashMap> message;
        public ServerUpdate(int port, MessageClosure<HashMap> message) {
            this.port = port;
            this.message = message;
        }
        @Override
        public void run() {
            try{
                Socket skt = new Socket("localhost", port);
//                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
                socketObjSend(message, out);
                out.flush();
                out.close();
//                in.close();
                skt.close();
            }catch (Exception e){
                System.out.println("exception when send map update to port "+port+": "+ e.getClass().getName()+": "+e.getMessage());
            }
        }
    }


    private static class ServerRequestHandler implements Runnable{
        @Override
        public void run() {
            try {
                while(true){
                    RequestPackage requestPackage = serverMessageQueue.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    System.out.print("Receive from server: " + messageClosure.toString());

                    if (messageClosure.getMyType() == AckMessage.class){
                        System.out.println("ServerRequestHandler: can't receive ack");
                    } else if (messageClosure.getMyType() == RequestCSMessage.class){
                        LamportClock clock = messageClosure.getTimestamp();
                        RequestCSMessage requestCSMessage = (RequestCSMessage) messageClosure.getObject();
                        switch  (requestCSMessage.type){
                            case READ:
                                serverClock.clockInc(clock);
                                readRequestQueue.offer(messageClosure);
                                break;
                            case WRITE:
                                serverClock.clockInc(clock);
                                writeRequestQueue.offer(messageClosure);
                                break;
                            default:
                                System.out.println("ServerRequestHandler: unknown CS type request received");
                        }
                        MessageClosure<AckMessage> ackMessageClosure = new MessageClosure<>(serverClock,new AckMessage());
                        socketObjSend(ackMessageClosure,out);
                        out.flush();

                    } else if (messageClosure.getMyType() == ReleaseMesage.class) {
                        ReleaseMesage releaseMesage = (ReleaseMesage)messageClosure.getObject();
                        RequestCSMessage messageToBeDel = releaseMesage.request.getObject();
                        switch (messageToBeDel.type){
                            case READ:
                                if(!readRequestQueue.remove(releaseMesage.request)){
                                    System.out.println("ServerRequestHandler: read write request is not in the queue");
                                }
                                break;
                            case WRITE:
                                MessageClosure removedRequest = writeRequestQueue.poll();
                                if (removedRequest == null) {
                                    System.out.println("ServerRequestHandler: before release write queue is empty? ");
                                }
                                if (!removedRequest.equals(releaseMesage.request)){
                                    System.out.println("ServerRequestHandler: released write request is not the smallest!");

                                }
                                break;
                            default:
                                System.out.println("ServerRequestHandler: unknown CS type request to be deleted");
                        }
                        System.out.println(releaseMesage.toString());

                    } else if (messageClosure.getMyType() == HashMap.class){
                        seat.num_name = (HashMap)messageClosure.getObject();
                    } else {
                        System.out.println("ServerRequestHandler: can't receive unknown package");
                    }


                    out.close();
                    in.close();
                    socket.close();

                    System.out.println("\nComplete Server request: " + requestPackage.toString());

                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
                e.printStackTrace(System.out);
            }

        }
    }
    private static class ClientRequestHandler implements Runnable{
        private Callable<Void> toCallable(final Runnable runnable) {
            return new Callable<Void>() {
                @Override
                public Void call() {
                    runnable.run();
                    return null;
                }
            };
        }
        @Override
        public void run() {
            try {
                while(true){
                    RequestPackage requestPackage = clientMessageQueue.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    TicketProcessor ticketProcessor = new TicketProcessor((String)messageClosure.getObject());

                    List<ServerCSRequester> requestList = new ArrayList<ServerCSRequester>();
                    RequestCSMessage thisRequestCSMessage = new RequestCSMessage(ticketProcessor.type);
                    MessageClosure<RequestCSMessage> requestMessageClosure = new MessageClosure<>(serverClock, thisRequestCSMessage);
                    for (int port : portList){
                        if (port != portList.get(serverIdx)) {
                            requestList.add(new ServerCSRequester(port, requestMessageClosure));
                        }
                    }
                    if (requestList.size() >0) {
                        ExecutorService taskRequestCSExecutor = Executors.newFixedThreadPool(requestList.size());
                        List<Future<Boolean>> results = taskRequestCSExecutor.invokeAll(requestList, 5, TimeUnit.SECONDS);
                        taskRequestCSExecutor.shutdown();
                        for (Future<Boolean> result : results) {
                            try {
                                if (result.get() != true) {
                                    System.out.println("CS request meet exception when requesting from port");
                                }
                            } catch (CancellationException e) {
                                System.out.println("CS request cancelled: " + e.getClass().getName() + ": " + e.getMessage());
                            } catch (Exception e) {
                                System.out.println("CS request unknown exception: " + e.getClass().getName() + ": " + e.getMessage());
                            }

                        }
                    }
                    if (ticketProcessor.type == RequestType.READ){
                        readRequestQueue.offer(requestMessageClosure);
                    } else if (ticketProcessor.type == RequestType.WRITE){
                        writeRequestQueue.offer(requestMessageClosure);
                    }
                    serverClock.clockInc();
                    TicketProcessor.MessageTOclient messageTOclient = new TicketProcessor.MessageTOclient();
                    if (ticketProcessor.type == RequestType.READ){
                        if (writeRequestQueue.size()>0) {
                            while(requestMessageClosure.isGreater(writeRequestQueue.peek()));
                        }
                        messageTOclient = ticketProcessor.execute(seat);
                    } else if (ticketProcessor.type == RequestType.WRITE){
                        if (readRequestQueue.size() >0) {
                            while(requestMessageClosure.isGreater(writeRequestQueue.peek()) || requestMessageClosure.isGreater(readRequestQueue.peek()));
                        }
                        messageTOclient = ticketProcessor.execute(seat);
                    }

                    if (messageTOclient.change == true) {
                        MessageClosure<HashMap> updateMesageClosure = new MessageClosure<HashMap>(serverClock, (HashMap) seat.num_name);
                        List<Callable<Void>> updateList = new ArrayList<>();
                        for (int port : portList) {
                            if (port != portList.get(serverIdx)) {
                                updateList.add(toCallable(new ServerUpdate(port, updateMesageClosure)));
                            }
                        }
                        ExecutorService taskUpdateExecutor = Executors.newFixedThreadPool(updateList.size());
                        taskUpdateExecutor.invokeAll(updateList, 5, TimeUnit.SECONDS);
                        taskUpdateExecutor.shutdown();
                    }


                    MessageClosure<ReleaseMesage> releaseMesageClosure = new MessageClosure<ReleaseMesage>(new ReleaseMesage(requestMessageClosure));
                    List<Callable<Void>> releaseList = new ArrayList<>();
                    for (int port: portList){
                        if (port != portList.get(serverIdx)) {
                            releaseList.add(toCallable(new ServerRelease(port, releaseMesageClosure)));
                        }
                    }
                    ExecutorService taskReleaseExecutor = Executors.newFixedThreadPool(releaseList.size());
                    taskReleaseExecutor.invokeAll(releaseList, 5, TimeUnit.SECONDS);
                    taskReleaseExecutor.shutdown();
                    if (ticketProcessor.type == RequestType.READ){
                        if (!requestMessageClosure.equals(readRequestQueue.poll())){
                            System.out.println("Self read queue release is not smallest");
                        }
                    } else if (ticketProcessor.type == RequestType.WRITE){
                        if (!requestMessageClosure.equals(writeRequestQueue.poll())){
                            System.out.println("Self write queue release is not smallest");
                        }
                    }
                    socketObjSend(new MessageClosure<String>(messageTOclient.message), out);

                    out.flush();
                    out.close();
                    in.close();
                    socket.close();
                    System.out.println("Complete Client request: " + requestPackage.toString());
//                    System.out.print(messageClosure.toString());
                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
                e.printStackTrace(System.out);
            }

        }
    }

    private static class RequestPackage {
        public Socket socket;
        public MessageClosure messageClosure;

        public RequestPackage(Socket socket, MessageClosure messageClosure) {
            this.socket = socket;
            this.messageClosure = messageClosure;
        }
    }




}
