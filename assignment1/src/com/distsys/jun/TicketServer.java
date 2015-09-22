package com.distsys.jun;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.PriorityBlockingQueue;

import static com.distsys.jun.Common.*;

public class TicketServer {

    private static int serverIdx;
    private static LamportClock serverClock;
    private static Seat seat;
    private static LinkedBlockingQueue<RequestPackage> clientMessageQueue;
    private static LinkedBlockingQueue<RequestPackage> serverMessageQueue;
    private static PriorityBlockingQueue<MessageClosure<RequestCSMessage>> readRequestQueue;
    private static PriorityBlockingQueue<MessageClosure<RequestCSMessage>> writeRequestQueue;
    private static LinkedBlockingQueue<RequestPackage> recoverMessageQueue;
    private static ArrayList<Integer> portList;
    private static AtomicBoolean cslock;


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
        clientMessageQueue = new LinkedBlockingQueue<RequestPackage>();
        serverMessageQueue = new LinkedBlockingQueue<RequestPackage>();
        recoverMessageQueue = new LinkedBlockingQueue<RequestPackage>();
        cslock = new AtomicBoolean(false);

        try {
            File portFile = new File("port.txt");
            portList = ReadPortFile(portFile);
//            if (args.length < 1) {
//                System.err.print("Need at least 1 argument");
//                System.exit(-1);
//            }
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please input server index");
            String line=sin.readLine();
            sin.close();
            TicketServer.serverIdx = Integer.parseInt(line.trim());
//            TicketServer.serverIdx = Integer.parseInt(args[0]);

            boolean needUpdate = false;
            MessageClosure<RecoverRequestMessage> recoverRequestMessageClosure = new MessageClosure<>(new RecoverRequestMessage());
            List<ServerRecoverRequester> serverRecoverRequesterList = new ArrayList<>();
            for (int port : portList){
                if (port != portList.get(serverIdx)) {
                    serverRecoverRequesterList.add(new ServerRecoverRequester(port, recoverRequestMessageClosure));
                }
            }
            RecoverMessage maxRecoverMessage = null;
            if (serverRecoverRequesterList.size() >0) {
                ExecutorService taskRecoverExecutor = Executors.newFixedThreadPool(serverRecoverRequesterList.size());
                List<Future<RecoverMessage>> results = taskRecoverExecutor.invokeAll(serverRecoverRequesterList, 5, TimeUnit.SECONDS);
                System.out.println("Recover request sent");
                taskRecoverExecutor.shutdown();
                for (Future<RecoverMessage> result : results) {
                    try {
                        RecoverMessage recoverMessage = result.get();
                        if (recoverMessage != null) {
                            needUpdate = true;
                            maxRecoverMessage = RecoverMessage.maxClock(maxRecoverMessage, recoverMessage);
                        }
                    } catch (CancellationException e) {
                        System.err.println("Recover request cancelled: " + e.getClass().getName() + ": " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Recover request unknown exception: " + e.getClass().getName() + ": " + e.getMessage());
                    }
                }
            }

            if (needUpdate){
                System.out.println("I am updating from Server: "+maxRecoverMessage.lamportClock.getServerId());
                serverClock = new LamportClock(serverIdx, maxRecoverMessage.lamportClock.getClockValue());
                seat.num_name = maxRecoverMessage.seatMap;
//                readRequestQueue = maxRecoverMessage.readRequestQueue;
//                writeRequestQueue = maxRecoverMessage.writeRequestQueue;
                readRequestQueue = new PriorityBlockingQueue<MessageClosure<RequestCSMessage>>(portList.size() * 10, lamportClockComparator);
                writeRequestQueue = new PriorityBlockingQueue<MessageClosure<RequestCSMessage>>(portList.size() * 10, lamportClockComparator);
            }else {
                System.out.println("I am the only server now");
                serverClock = new LamportClock(serverIdx);
                readRequestQueue = new PriorityBlockingQueue<MessageClosure<RequestCSMessage>>(portList.size() * 10, lamportClockComparator);
                writeRequestQueue = new PriorityBlockingQueue<MessageClosure<RequestCSMessage>>(portList.size() * 10, lamportClockComparator);
            }

            ServerSocket srvr = new ServerSocket(portList.get(serverIdx));
            System.out.println("Server " + serverIdx + " Port " + portList.get(serverIdx) + " socket opened");


            Runnable clientRequestHandler = new ClientRequestHandler();
            new Thread(clientRequestHandler).start();
            Runnable serverRequestHandler = new ServerRequestHandler();
            new Thread(serverRequestHandler).start();
            Runnable recoverRequestHandler = new RecoverRequestHandler();
            new Thread(recoverRequestHandler).start();

            while (true) {
                Socket clientSocket = srvr.accept();
                System.out.print("\nServer has connected!\n");
                System.out.println("read queue size: " + readRequestQueue.size());
                System.out.println("write queue size: " + writeRequestQueue.size());
//                OutputStream outputStream = clientSocket.getOutputStream();
                InputStream inputStream = clientSocket.getInputStream();
                MessageClosure receivedmessage = (MessageClosure) socketObjReceive(inputStream);
                RequestPackage requestPackage = new RequestPackage(clientSocket, receivedmessage);
                if (receivedmessage.getMyType() == String.class){
                    clientMessageQueue.put(requestPackage);
                    System.out.println("Client Request enter Queue");
                } else if (receivedmessage.getMyType() == RecoverRequestMessage.class){
                    recoverMessageQueue.put(requestPackage);
                    System.out.println("Recover Request enter Queue");
                } else {
                    serverMessageQueue.put(requestPackage);
                    System.out.println("Server Request enter Queue: "+requestPackage.messageClosure.getObject().toString());
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

        LamportClock(LamportClock clock){
            this.clockValue = clock.clockValue;
            this.serverId = clock.serverId;
        }

        LamportClock(int serverId){
            this.serverId = serverId;
        }

        LamportClock(int serverId, int clockValue){
            this.clockValue = clockValue;
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

    public static class AckUpdateMessage implements Serializable{
        public AckUpdateMessage(){
        }

        @Override
        public String toString() {
            return "Map update Ack\n";
        }
    }



    public static class ReleaseMessage implements Serializable{
        private final LamportClock entry;
        private final MessageClosure<RequestCSMessage> request;
        public ReleaseMessage(MessageClosure<RequestCSMessage> request) {
            this.entry = request.getTimestamp();
            this.request = request;
        }


        public String toString() {
            return "Release "+entry.toString()+ " request is " + ObjectUtils.toString(request.getObject(), "no info")+"\n";
        }
    }

    public static class RecoverRequestMessage implements Serializable{
        public RecoverRequestMessage() {
        }

        @Override
        public String toString() {
            return "Recover Request";
        }
    }

    public static class RecoverMessage implements Serializable{
        public final LamportClock lamportClock;
        public final HashMap<Integer, String> seatMap;
        public transient final PriorityBlockingQueue<MessageClosure<RequestCSMessage>> readRequestQueue;
        public transient final PriorityBlockingQueue<MessageClosure<RequestCSMessage>> writeRequestQueue;

        public RecoverMessage(LamportClock lamportClock, HashMap<Integer, String> seatMap, PriorityBlockingQueue<MessageClosure<RequestCSMessage>> readRequestQueue, PriorityBlockingQueue<MessageClosure<RequestCSMessage>> writeRequestQueue) {
            this.lamportClock = lamportClock;
            this.seatMap = seatMap;
            this.readRequestQueue = readRequestQueue;
            this.writeRequestQueue = writeRequestQueue;
        }

        public static RecoverMessage maxClock(RecoverMessage first, RecoverMessage second){
            if (first == null){
                return second;
            }
            if (second == null){
                return first;
            }
            if (first.lamportClock.getClockValue() > second.lamportClock.getClockValue()){
                return first;
            } else {
                return second;
            }
        }

        @Override
        public String toString() {
            return "Recover response from "+lamportClock.toString()+", RQ size: "+readRequestQueue.size()+", WQ size: "+writeRequestQueue.size();
        }
    }

    private static class ServerRecoverRequester implements Callable<RecoverMessage>{

        private final int port;
        private final MessageClosure<RecoverRequestMessage> message ;

        public ServerRecoverRequester(int port, MessageClosure<RecoverRequestMessage> message) {
            this.port = port;
            this.message = message ;
        }

        @Override
        public RecoverMessage call() {
            try{
                Socket skt = new Socket("localhost", port);
                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
                socketObjSend(message, out);
                out.flush();
                MessageClosure messageClosure = (MessageClosure) socketObjReceive(in);
                if (messageClosure.getMyType() != RecoverMessage.class){
                    System.err.println("ServerRecoverRequester: expect to receive recover message");
                }
                in.close();
                out.close();
                skt.close();
                return (RecoverMessage) messageClosure.getObject();

            }catch (Exception e){
                return null;
            }
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
                socketObjSend(message, out);
                out.flush();
                MessageClosure messageClosure = (MessageClosure) socketObjReceive(in);
                if (messageClosure.getMyType() != AckMessage.class){
                    System.err.println("ServerCSRequester: expect to receive ack");
                }
                out.close();
                in.close();
                skt.close();
                return true;
            }
            catch(Exception e) {
                System.err.println("exception when requesting from port " + port + ": " + e.getClass().getName() + ": " + e.getMessage());
                return false;
            }
        }
    }

    private static class ServerRelease implements Runnable{
        private final int port;
        private final MessageClosure<ReleaseMessage> message;
        public ServerRelease(int port, MessageClosure<ReleaseMessage> message){
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
                System.err.println("exception when release to port " + port + ": " + e.getClass().getName() + ": " + e.getMessage());
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
                System.err.println("exception when send map update to port " + port + ": " + e.getClass().getName() + ": " + e.getMessage());
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
                    System.out.print("ServerRequestHandler: Receive from server: " + messageClosure.toString());

                    if (messageClosure.getMyType() == AckMessage.class){
                        System.err.println("ServerRequestHandler: can't receive ack");
                    } else if (messageClosure.getMyType() == RequestCSMessage.class){
                        LamportClock clock = messageClosure.getTimestamp();
                        RequestCSMessage requestCSMessage = (RequestCSMessage) messageClosure.getObject();
                        while (!cslock.compareAndSet(false, true));
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
                                System.err.println("ServerRequestHandler: unknown CS type request received");
                        }
                        cslock.set(false);
                        MessageClosure<AckMessage> ackMessageClosure = new MessageClosure<>(new LamportClock(serverClock),new AckMessage());
                        socketObjSend(ackMessageClosure,out);
                        out.flush();

                    } else if (messageClosure.getMyType() == ReleaseMessage.class) {
                        ReleaseMessage releaseMessage = (ReleaseMessage)messageClosure.getObject();
                        RequestCSMessage messageToBeDel = releaseMessage.request.getObject();
                        switch (messageToBeDel.type){
                            case READ:
                                if(!readRequestQueue.remove(releaseMessage.request)){
                                    System.err.println("ServerRequestHandler: read write request is not in the queue");
                                }
                                break;
                            case WRITE:
                                MessageClosure removedRequest = writeRequestQueue.poll();
                                if (removedRequest == null) {
                                    System.err.println("ServerRequestHandler: before release write queue is empty? ");
                                }
                                if (!removedRequest.equals(releaseMessage.request)){
                                    System.err.println("ServerRequestHandler: released write request is not the smallest!" + "\n"
                                                    + "request: " + releaseMessage.request.getTimestamp().toString() + "\n"
                                                    + "inqueue: " + removedRequest.getTimestamp().toString()

                                    );

                                }
                                break;
                            default:
                                System.err.println("ServerRequestHandler: unknown CS type request to be deleted");
                        }
                        System.out.println(releaseMessage.toString());

                    } else if (messageClosure.getMyType() == HashMap.class){
                        seat.num_name = (HashMap)messageClosure.getObject();
                    } else {
                        System.err.println("ServerRequestHandler: can't receive unknown package");
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
                    MessageClosure<RequestCSMessage> requestMessageClosure = new MessageClosure<>(new LamportClock(serverClock), thisRequestCSMessage);
                    System.out.println("request key: "+requestMessageClosure.getTimestamp().toString());
                    for (int port : portList){
                        if (port != portList.get(serverIdx)) {
                            requestList.add(new ServerCSRequester(port, requestMessageClosure));
                        }
                    }
                    if (requestList.size() >0) {
                        ExecutorService taskRequestCSExecutor = Executors.newFixedThreadPool(requestList.size());
                        List<Future<Boolean>> results = taskRequestCSExecutor.invokeAll(requestList, 5, TimeUnit.SECONDS);
                        System.out.println("request CS send done");
                        taskRequestCSExecutor.shutdown();
                        for (Future<Boolean> result : results) {
                            try {
                                if (result.get() != true) {
                                    System.err.println("CS request meet exception when requesting from port");
                                }
                            } catch (CancellationException e) {
                                System.err.println("CS request cancelled: " + e.getClass().getName() + ": " + e.getMessage());
                            } catch (Exception e) {
                                System.err.println("CS request unknown exception: " + e.getClass().getName() + ": " + e.getMessage());
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

                    while (!cslock.compareAndSet(false, true));
                    if (messageTOclient.change == true) {
                        MessageClosure<HashMap> updateMesageClosure = new MessageClosure<HashMap>(new LamportClock(serverClock), (HashMap) seat.num_name);
                        List<Callable<Void>> updateList = new ArrayList<>();
                        for (int port : portList) {
                            if (port != portList.get(serverIdx)) {
                                updateList.add(toCallable(new ServerUpdate(port, updateMesageClosure)));
                            }
                        }
                        ExecutorService taskUpdateExecutor = Executors.newFixedThreadPool(updateList.size());
                        taskUpdateExecutor.invokeAll(updateList, 5, TimeUnit.SECONDS);
                        System.out.println("update send done");
                        taskUpdateExecutor.shutdown();
                    }

                    System.out.println("release key: "+requestMessageClosure.getTimestamp().toString());
                    MessageClosure<ReleaseMessage> releaseMesageClosure = new MessageClosure<ReleaseMessage>(new ReleaseMessage(requestMessageClosure));
                    List<Callable<Void>> releaseList = new ArrayList<>();
                    for (int port: portList){
                        if (port != portList.get(serverIdx)) {
                            releaseList.add(toCallable(new ServerRelease(port, releaseMesageClosure)));
                        }
                    }
                    ExecutorService taskReleaseExecutor = Executors.newFixedThreadPool(releaseList.size());
                    taskReleaseExecutor.invokeAll(releaseList, 5, TimeUnit.SECONDS);
                    System.out.println("release send done");
                    taskReleaseExecutor.shutdown();
                    MessageClosure<RequestCSMessage> popedrequest;
                    if (ticketProcessor.type == RequestType.READ){
                        if (!requestMessageClosure.equals(popedrequest = readRequestQueue.poll())){
                            System.err.println("Self read queue release is not smallest:\n request: "
                                            + requestMessageClosure.getTimestamp().toString() + "\n"
                                            + popedrequest.getTimestamp().toString()
                            );
                        }
                    } else if (ticketProcessor.type == RequestType.WRITE){
                        if (!requestMessageClosure.equals(popedrequest = writeRequestQueue.poll())){
                            System.err.println("Self write queue release is not smallest:\n request: "
                                            + requestMessageClosure.getTimestamp().toString() + "\n"
                                            + "inqueue" + popedrequest.getTimestamp().toString()
                            );
                        }
                    }

                    cslock.set(false);
                    socketObjSend(new MessageClosure<String>(messageTOclient.message), out);

                    out.flush();
                    out.close();
                    in.close();
                    socket.close();
                    System.out.println("Complete Client request: " + requestPackage.toString());
                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
                e.printStackTrace(System.out);
            }

        }
    }

    private static class RecoverRequestHandler implements Runnable{
        @Override
        public void run() {
            while (true) {
                try{
                    RequestPackage requestPackage = recoverMessageQueue.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    System.out.print("ServerRequestHandler: Receive from server: " + messageClosure.toString());
                    if (messageClosure.getMyType() != RecoverRequestMessage.class){
                        System.err.println("RecoverRequestHandler: Expect to receive Recover package");
                    }
                    while (!cslock.compareAndSet(false, true));
                    RecoverMessage recoverMessage = new RecoverMessage(serverClock, (HashMap)seat.num_name, readRequestQueue, writeRequestQueue);
                    MessageClosure<RecoverMessage> recoverMessageClosure = new MessageClosure<>(recoverMessage);
                    socketObjSend(recoverMessageClosure, out);
                    out.flush();
                    cslock.set(false);
                    in.close();
                    out.close();
                    socket.close();
                    System.out.println("Complete Recover request: " + requestPackage.toString());
                } catch (Exception e){
                    System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
                    e.printStackTrace(System.out);
                }
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
