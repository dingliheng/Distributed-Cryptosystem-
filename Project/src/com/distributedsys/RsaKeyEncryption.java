package com.distributedsys;

import java.math.BigInteger;
import java.io.*;

class RsaKeyEncryption {
    private BigInteger n, e;
    public static void main(String[] a) {
//        if (a.length<3) {
//            System.out.println("Usage:");
//            System.out.println("java com.distributedsys.RsaKeyEncryption key input output");
//            return;
//        }
//        String keyFile = a[0];
//        String input = a[1];
//        String output = a[2];
//
        String interput;
        RsaKeyEncryption encryptor6 = new RsaKeyEncryption(0); //create an encryptor which uses publick key6
        interput = encryptor6.initencrypt("input.txt"); //the first encryption uses the "initencrypt" method
        RsaKeyEncryption encryptor1 = new RsaKeyEncryption(5);
        interput =encryptor1.interencrypt(interput);  //the intermediate encryptions use the "interencrypt" method
        RsaKeyEncryption encryptor2 = new RsaKeyEncryption(6);
        interput =encryptor2.interencrypt(interput);
        RsaKeyEncryption encryptor3 = new RsaKeyEncryption(1);
        interput =encryptor3.interencrypt(interput);
        RsaKeyEncryption encryptor4 = new RsaKeyEncryption(2);
        interput =encryptor4.interencrypt(interput);
        RsaKeyEncryption encryptor5 = new RsaKeyEncryption(3);
        interput =encryptor5.interencrypt(interput);
        RsaKeyEncryption encryptor0 = new RsaKeyEncryption(4);
//        encryptor0.finencrypt(interput, "output.txt"); //the final encryption uses the "finercrypt" method
        interput =encryptor0.interencrypt(interput);

        RsaKeyDecryption decryptor3 = new RsaKeyDecryption(3);
        interput = decryptor3.interdecrypt(interput);  // "initdencrypt" method
        RsaKeyDecryption decryptor5 = new RsaKeyDecryption(5);
        interput = decryptor5.interdecrypt(interput);    // "interdencrypt" method
//        System.out.println("intervalue: "+ interput);
        RsaKeyDecryption decryptor4 = new RsaKeyDecryption(4);
        interput = decryptor4.interdecrypt(interput);
        RsaKeyDecryption decryptor6 = new RsaKeyDecryption(6);
        interput = decryptor6.interdecrypt(interput);
        RsaKeyDecryption decryptor1 = new RsaKeyDecryption(1);
        interput = decryptor1.interdecrypt(interput);
        RsaKeyDecryption decryptor2 = new RsaKeyDecryption(2);
        interput =  decryptor2.interdecrypt(interput);
        RsaKeyDecryption decryptor = new RsaKeyDecryption(0);
//        decryptor.findecrypt(interput, "decrypt.txt");    // "findencrypt" method
        interput =  decryptor.interdecrypt(interput);

        RsaKeyEncryption encryptor11 = new RsaKeyEncryption(0);
        encryptor11.finalwritetofile(interput,"output.txt");




//        try {
//            BufferedReader br=new BufferedReader(new FileReader("initBiginter.txt"));
//            String line="";
//            StringBuffer  buffer = new StringBuffer();
//            while((line=br.readLine())!=null){
//                buffer.append(line+"\n");
//            }
//            String fileContent = buffer.toString();
//            interput = fileContent;
//        }catch (Exception ex) {
//            ex.printStackTrace();
//        }
        System.out.println("the result of verification :" + encryptor6.verifyencrypt("input.txt", interput));
    }

    // Reading in RSA public key
    RsaKeyEncryption(int keyNum) {
        try {
            BufferedReader in = new BufferedReader(new FileReader("RSAkeys.txt"));
            String line = in.readLine();
            while (line!=null) {
                if (line.indexOf("Modulus: ")>=0) {
                    n = new BigInteger(line.substring(9));
                }
                if (line.indexOf("Public key"+keyNum+": ")>=0) {
                    e = new BigInteger(line.substring(13));
                }
                line = in.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("--- Reading public key ---");
        System.out.println("Modulus: "+n);
        System.out.println("Key size: " + n.bitLength());
        System.out.println("Public key" + keyNum + ": " + e);
    }

    // Encrypting original message
    public String initencrypt(String intput) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
//        System.out.println("Cleartext block size: "+clearTextSize);
//        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(intput);
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream("initBiginter.txt"),"utf-8");
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = fis.read(clearTextBlock);
            boolean isPadded = false;

//       Reading input message
//            System.out.println("cipherText");
            StringWriter outstring = new StringWriter();
            while (dataSize>0) {
                blocks++;
                if (dataSize<clearTextSize) {
                    padBytesBlock(clearTextBlock,dataSize);
                    isPadded = true;
                }

                BigInteger clearText = new BigInteger(1,clearTextBlock);
                fos.write(clearText.toString() + "\n");
                BigInteger cipherText = clearText.modPow(e, n);
                System.out.println(cipherText.toString());
                outstring.write(cipherText.toString()+"\n");
                dataSize = fis.read(clearTextBlock);
            }

//       Adding a full padding block, if needed
            if (!isPadded) {
                blocks++;
                padBytesBlock(clearTextBlock,0);
                BigInteger clearText = new BigInteger(1,clearTextBlock);
                fos.write(clearText.toString() + "\n");
                BigInteger cipherText = clearText.modPow(e,n);
                System.out.println(cipherText.toString());
                outstring.write(cipherText.toString()+"\n");
            }

            fis.close();
            fos.close();
            outstring.close();
            System.out.println("Encryption block count: "+blocks);
            System.out.println("init encrypted value: "+outstring.toString());
            return outstring.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    // Putting bytes data into a block
    public static void putBytesBlock(byte[] block, byte[] data) {
        int bSize = block.length;
        int dSize = data.length;
        int i = 0;
        while (i<dSize && i<bSize) {
            block[bSize-i-1] = data[dSize-i-1];
            i++;
        }
        while (i<bSize) {
            block[bSize-i-1] = (byte)0x00;
            i++;
        }
    }

    // Padding input message block
    public static void padBytesBlock(byte[] block, int dataSize) {
        int bSize = block.length;
        int padSize = bSize - dataSize;
        int padValue = padSize%bSize;
        for (int i=0; i<padSize; i++) {
            block[bSize-i-1] = (byte) padValue;
        }
    }

    public String interencrypt(String string){
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(string));
            String line = in.readLine();
            StringWriter outstring = new StringWriter();
            while (line!=null&&line.length()!=0) {
                BigInteger clearText = new BigInteger(line);
                BigInteger cipherText = clearText.modPow(e, n);
                output = output + cipherText+"\n";
                line = in.readLine();
            }
//            System.out.println("cipherText\n"+output);
//            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(file),"utf-8");
//            output=output.substring(0,output.length()-1);
            outstring.write(output);
            outstring.close();
            System.out.println("intevalue: "+ outstring.toString());
            return outstring.toString();
        }catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void finalwritetofile(String interput, String outputfile){
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("final Cleartext block size: "+clearTextSize);
        System.out.println("final Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(interput));
            File file = new File(outputfile);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
//            StringWriter foss = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            String line = in.readLine();
            while (line!=null&&line.length()!=0) {
                BigInteger clearText = new BigInteger(line);
//                BigInteger cipherText = clearText.modPow(e, n);
                BigInteger cipherText = clearText;
                output = output + cipherText+"\n";
                byte[] cipherTextData = cipherText.toByteArray();
                putBytesBlock(cipherTextBlock,cipherTextData);
                fos.write(cipherTextBlock);
//                foss.write(cipherTextBlock);

                line = in.readLine();
            }
//            System.out.println("cipherText\n"+output);
            fos.close();
            in.close();
        }catch (Exception ex) {
            System.err.println(ex.getStackTrace());
        }
    }


    public String finencrypt(String interput){
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("final Cleartext block size: "+clearTextSize);
        System.out.println("final Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(interput));
            ByteArrayOutputStream fos = new ByteArrayOutputStream();
//            StringWriter outString = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            String line = in.readLine();
            while (line!=null&&line.length()!=0) {
                BigInteger clearText = new BigInteger(line);
                BigInteger cipherText = clearText.modPow(e, n);
                output = output + cipherText+"\n";
                byte[] cipherTextData = cipherText.toByteArray();
                putBytesBlock(cipherTextBlock, cipherTextData);
                fos.write(cipherTextBlock);
                line = in.readLine();
            }
//            System.out.println("cipherText\n"+output);
            fos.close();
            return fos.toString();
        }catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean verifyencrypt (String inputfile,String interput) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize - 1) / 8, 256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        String cleartxt = "";
        try {
            FileInputStream fis = new FileInputStream(inputfile);
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = fis.read(clearTextBlock);
            boolean isPadded = false;

            StringWriter outstring = new StringWriter();
            while (dataSize>0) {
                blocks++;
                if (dataSize<clearTextSize) {
                    padBytesBlock(clearTextBlock,dataSize);
                    isPadded = true;
                }

                BigInteger clearText = new BigInteger(1,clearTextBlock);
                cleartxt = cleartxt + clearText.toString()+"\n";
                dataSize = fis.read(clearTextBlock);
            }

//       Adding a full padding block, if needed
            if (!isPadded) {
                blocks++;
                padBytesBlock(clearTextBlock, 0);
                BigInteger clearText = new BigInteger(1,clearTextBlock);
                cleartxt = cleartxt + clearText.toString()+"\n";
            }
            fis.close();
            System.out.println("The Biginter of original message: "+cleartxt);
            if (cleartxt.equals(interput)){
                return true;
            }
            else return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}