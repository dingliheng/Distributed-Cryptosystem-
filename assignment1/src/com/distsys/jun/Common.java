package com.distsys.jun;


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
        ois.close();
//        if (obj instanceof TicketServer.MessageCreator){
//            System.out.print("Yes\n");
//        }
        return obj;
    }

    public static void socketObjSend(Serializable obj, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(obj);
//        oos.close();
    }



}
