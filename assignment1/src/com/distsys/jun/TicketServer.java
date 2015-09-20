package com.distsys.jun;

import com.sun.xml.internal.ws.handler.ServerMessageHandlerTube;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.naming.directory.SearchControls;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import static com.distsys.jun.Common.*;

public class TicketServer {

    private static int serverIdx;
    private static LamportClock serverClock;
    private static Seat seat;
    private static LinkedBlockingQueue<RequestPackage> clientMessageQueue;
    private static LinkedBlockingQueue<RequestPackage> serverMessageQueue;
    private static PriorityBlockingQueue<MessageClosure> readRequestQueue;
    private static PriorityBlockingQueue<MessageClosure> writeRequestQueue;

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
            ArrayList<Integer> portList = ReadPortFile(portFile);
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

//            RequestCSMessage requestCSMessage = new RequestCSMessage(RequestType.READ);
//            MessageClosure message = new MessageClosure(serverClock, requestCSMessage);
//            Runnable serverSender = new ServerSender(serverIdx,portList);
//            new Thread(serverSender).start();
//
//            while (true) {
//                Socket clientSocket = srvr.accept();
////                System.out.print("Server has connected!\n");
//                OutputStream outputStream = clientSocket.getOutputStream();
//                InputStream inputStream = clientSocket.getInputStream();
//
//                socketObjSend(message, outputStream);
//                MessageClosure reconst = (MessageClosure) socketObjReceive(inputStream);
//                System.out.print(reconst.toString());
//                Thread.sleep(2000);
////                outputStream.close();
////                inputStream.close();
////                srvr.close();
//
//                serverSender = new ServerSender(serverIdx,portList);
//                new Thread(serverSender).start();
//            }
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
                //Runnable requestHandler = new RequestHandler(clientSocket);
                //new Thread(requestHandler).start();
                //srvr.close();
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
            return clockValue == ((LamportClock)obj).clockValue && serverId == ((LamportClock)obj).serverId;
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


    private static class ServerSender implements  Runnable{

        private int serverIdx;
        private ArrayList<Integer> portList;

        public ServerSender(int serverIdx,ArrayList<Integer> portList){
            this.serverIdx = serverIdx;
            this.portList = portList;
        }
        @Override
        public void run() {
            try {
                for (int port : portList) {
                    if (port!=portList.get(serverIdx)) {
                        Socket skt = new Socket("localhost", port); //random port
                        InputStream in = skt.getInputStream();
                        OutputStream out = skt.getOutputStream();
                        RequestCSMessage requestCSMessage = new RequestCSMessage(RequestType.WRITE);
//                        ReleaseMesage releaseMessage = new ReleaseMesage(requestCSMessage);
//                        MessageClosure message = new MessageClosure(serverClock, releaseMessage);
//                        socketObjSend(message, out);
//                        serverClock.clockInc();
                        MessageClosure reconst = (MessageClosure) socketObjReceive(in);
                        System.out.print(reconst.toString());
                        in.close();
                        out.close();
                        skt.close();
                    }
                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+": "+e.getMessage()+"\n");
//                e.printStackTrace(System.out);
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
                    System.out.print("Receive from server: "+messageClosure.toString());

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
                                if (!removedRequest.equals(releaseMesage.request)){
                                    System.out.println("ServerRequestHandler: released write request is not the smallest!");
                                }
                                break;
                            default:
                                System.out.println("ServerRequestHandler: unknown CS type request to be deleted");
                        }

                    } else if (messageClosure.getMyType() == HashMap.class){
                        seat.num_name = (HashMap)messageClosure.getObject();
                    } else {
                        System.out.println("ServerRequestHandler: can't receive unknown package");
                    }


                    out.flush();
                    out.close();
                    in.close();
                    socket.close();



                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
                e.printStackTrace(System.out);
            }

        }
    }
    private static class ClientRequestHandler implements Runnable{
        @Override
        public void run() {
            try {
                while(true){
                    RequestPackage requestPackage = clientMessageQueue.take();
                    Socket socket = requestPackage.socket;
                    MessageClosure messageClosure = requestPackage.messageClosure;
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    socketObjSend(messageClosure, out);
                    out.flush();
                    out.close();
                    in.close();
                    socket.close();
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
