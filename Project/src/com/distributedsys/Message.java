package com.distributedsys;

import javax.imageio.IIOException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jpan on 11/9/15.
 */
public class Message {
    public static final int nodeNum = 7;
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
        return obj;
    }

    public static void socketObjSend(Serializable obj, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(obj);
    }

    public static class KickMessage implements Serializable{
        private final ProcType proctype;
        private final String filename;
        private final int destnode;

        public KickMessage(ProcType proctype, String filename, int destnode) {
            this.proctype = proctype;
            this.filename = filename;
            this.destnode = destnode;
        }

        @Override
        public String toString() {
            return "KickMessage("+proctype.name()+",data: "+filename+", Node: "+destnode+")\n";
        }
    }

    public static class MessageClosure<T extends Serializable> implements Serializable{
        private final T object;
        private final Class<T> type;

        public Class<T> getMyType() {
            return this.type;
        }

        MessageClosure(T object){
            this.object = object;
            this.type = (Class<T>) object.getClass();
        }

        public T getObject() {
            return object;
        }
    }

    public enum ProcType{
        ENC,
        DEC
    }

    public static List<Integer> getInitPort (List<Integer> portlist, File nodeMap) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(nodeMap));
        List<Integer> initport = new ArrayList<>();
        String line;
        Set<Integer> portdup = new HashSet<>();
        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            int firstport = portlist.get(Integer.parseInt(tokens[0])-1);
            if (!portdup.contains(firstport)){
                portdup.add(firstport);
                initport.add(firstport);
            }
        }
        br.close();
        return initport;
    }
}
