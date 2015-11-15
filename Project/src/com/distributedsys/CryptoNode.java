package com.distributedsys;

import java.io.File;
import java.util.List;

import static com.distributedsys.Message.*;

/**
 * Created by jpan on 11/14/15.
 */
public class CryptoNode {

    public static void main(String[] a) {
        try{
            if (a.length<1) {
                System.out.println("Usage:");
                System.out.println("input node id");
                return;
            }

            File portFile = new File("port.txt");
            List<Integer> portList = ReadPortFile(portFile);

            int nodeIdx = Integer.parseInt(a[0]);


        } catch (Exception e){
            System.err.println(e.getStackTrace());
        }

    }
}
