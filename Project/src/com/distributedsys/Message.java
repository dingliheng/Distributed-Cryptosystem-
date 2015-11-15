package com.distributedsys;

import javax.imageio.IIOException;
import java.io.*;
import java.util.*;

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

    public static class PathMessage implements Serializable{
        private final ProcType proctype;
        private final String filename;
        private final int destnode;
        private final List<Integer> path;

        public PathMessage(ProcType proctype, String filename, int destnode, List<Integer> path) {
            this.proctype = proctype;
            this.filename = filename;
            this.destnode = destnode;
            this.path = path;
        }

        public ProcType getProctype() {
            return proctype;
        }

        public String getFilename() {
            return filename;
        }

        public int getDestnode() {
            return destnode;
        }

        public List<Integer> getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "PathMessage("+proctype.name()+",data: "+filename+", Node: "+destnode+", path:  "+path+")\n";
        }
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

        public ProcType getProctype() {
            return proctype;
        }

        public String getFilename() {
            return filename;
        }

        public int getDestnode() {
            return destnode;
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
            int firstport = portlist.get(Integer.parseInt(tokens[0]));
            if (!portdup.contains(firstport)){
                portdup.add(firstport);
                initport.add(firstport);
            }
        }
        br.close();
        return initport;
    }

    public static List<Integer> getKeyfrgList (int nodeId, File keyFrgMap) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(keyFrgMap));
        List<Integer> keyFrgList = new ArrayList<>();
        String line;
        int counter = 0;
        while ((line = br.readLine()) != null) {
            if (counter == nodeId){
                String[] tokens = line.trim().split("\\s+");
                for (String token: tokens){
                    keyFrgList.add(Integer.parseInt(token));
                }
                break;
            }
            counter++;
        }
        return keyFrgList;
    }

    public static List<Integer> getNextNodePortList (int nodeId, File nodeMap,List<Integer> portlist, List<Integer> match) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(nodeMap));
        List<Integer> nextNodeList = new ArrayList<>();
        String line;
        Set<Integer> portdup = new HashSet<>();
        int counter= 0;
        while ((line = br.readLine()) != null && match.contains(counter++)) {
            String[] tokens = line.trim().split("\\s+");
            boolean findFlag = false;
            for (String token: tokens){
                int node = Integer.parseInt(token);
                if (portdup.contains(node)) break;
                if (findFlag){
                    portdup.add(node);
                    nextNodeList.add(portlist.get(node));
                    break;
                }
                if ((node == nodeId)&&!portdup.contains(node)){
                    findFlag = true;
                }
            }
        }
        br.close();

        return nextNodeList;
    }

    public static List<Integer> matchPath (int nodeId, File nodeMap,List<Integer> path) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(nodeMap));
        List<Integer> matchList = new ArrayList<>();
        String line;
        Iterator<Integer> pathIter = path.iterator();
        if (pathIter.hasNext()){
            int currnode = pathIter.next();
            int counter = 0;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                for (String token: tokens){
                    int node = Integer.parseInt(token);
                    if (currnode != node){
                        pathIter = path.iterator();
                        currnode = pathIter.next();
                        break;
                    }
                    if (!pathIter.hasNext()){
                        matchList.add(counter);
                        pathIter = path.iterator();
                        currnode = pathIter.next();
                        break;
                    }else {
                        currnode = pathIter.next();
                    }
                }
                counter++;
            }
        }

        br.close();

        return matchList;
    }

    public static List<Integer> makeSequence(int begin, int end) {
        List<Integer> ret = new ArrayList(end - begin + 1);
        for(int i = begin; i <= end; i++){
            ret.add(i);
        }
        return ret;
    }
}
