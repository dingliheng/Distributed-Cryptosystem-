package com.distsys.jun;


import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jpan on 9/16/15.
 */
public class Common {
    public static final int ticketNumber = 100;
    public static ArrayList<Integer> ReadPortFile(File fin) throws IOException {
        // Construct BufferedReader from FileReader
        ArrayList<Integer> portArray = new ArrayList<Integer>();
        BufferedReader br = new BufferedReader(new FileReader(fin));
        String line;
        while ((line = br.readLine()) != null) {
            portArray.add(Integer.parseInt(line.trim()));
//            System.out.println(line);
        }
        br.close();
        return portArray;
    }

    public static Object socketObjReceive(InputStream inputStream) throws IOException, ClassNotFoundException {
        Object obj;
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        while ((obj = ois.readObject()) == null){}
//        ois.close();
//        TicketServer.MessageClosure message = (TicketServer.MessageClosure) obj;
//        if (message.getMyType() == TicketServer.RequestCSMessage.class){
//            System.out.print("Yes\n");
//        }
        return obj;
    }

    public static void socketObjSend(Serializable obj, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(obj);
//        oos.close();
    }


    public static class MessageClosure<T extends Serializable> implements Serializable{
        private TicketServer.LamportClock timestamp;
        private final T object;
        private final Class<T> type;

        public Class<T> getMyType() {
            return this.type;
        }

        MessageClosure(T object){
            this.timestamp = new TicketServer.LamportClock();
            this.object = object;
            this.type = (Class<T>) object.getClass();
        }

        MessageClosure(TicketServer.LamportClock lamportClock, T object){
            this.timestamp = lamportClock;
            this.object = object;
            this.type = (Class<T>) object.getClass();
        }

        @Override
        public int hashCode() {
            return timestamp.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            MessageClosure mc = (MessageClosure)obj;
            return timestamp == mc.timestamp;
        }

        @Override
        public String toString(){
            String outstr = "Clock: "+timestamp.getClockValue()
                                +",from server: "+timestamp.getServerId()+", "+object.toString();
            return outstr;
        }

        public TicketServer.LamportClock getTimestamp() {
            return timestamp;
        }

        public T getObject() {
            return object;
        }
    }
}
