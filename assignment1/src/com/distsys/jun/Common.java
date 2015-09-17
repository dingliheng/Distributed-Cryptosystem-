package com.distsys.jun;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by jpan on 9/16/15.
 */
public class Common {
    public static void ReadPortFile(File fin) throws IOException {
        // Construct BufferedReader from FileReader
        ArrayList portArray = new ArrayList<Integer>();
        BufferedReader br = new BufferedReader(new FileReader(fin));
        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();
    }
}
