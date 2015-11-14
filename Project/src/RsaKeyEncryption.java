import java.math.BigInteger;
import java.io.*;

class RsaKeyEncryption {
    private BigInteger n, e;
    public static void main(String[] a) {
//        if (a.length<3) {
//            System.out.println("Usage:");
//            System.out.println("java RsaKeyEncryption key input output");
//            return;
//        }
//        String keyFile = a[0];
//        String input = a[1];
//        String output = a[2];
//
        RsaKeyEncryption encryptor6 = new RsaKeyEncryption(6); //create an encryptor which uses publick key6
        encryptor6.initencrypt("input.txt", "interput.txt"); //the first encryption uses the "initencrypt" method
        RsaKeyEncryption encryptor1 = new RsaKeyEncryption(1);
        encryptor1.interencrypt("interput.txt");  //the intermediate encryptions use the "interencrypt" method
        RsaKeyEncryption encryptor2 = new RsaKeyEncryption(2);
        encryptor2.interencrypt("interput.txt");
        RsaKeyEncryption encryptor3 = new RsaKeyEncryption(3);
        encryptor3.interencrypt("interput.txt");
        RsaKeyEncryption encryptor4 = new RsaKeyEncryption(4);
        encryptor4.interencrypt("interput.txt");
        RsaKeyEncryption encryptor5 = new RsaKeyEncryption(5);
        encryptor5.interencrypt("interput.txt");
        RsaKeyEncryption encryptor0 = new RsaKeyEncryption(0);
        encryptor0.finencrypt("interput.txt", "output.txt"); //the final encryption uses the "finercrypt" method
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
    public void initencrypt(String intput, String output) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(intput);
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(output),"utf-8");
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = fis.read(clearTextBlock);
            boolean isPadded = false;

//       Reading input message
//            System.out.println("cipherText");
            while (dataSize>0) {
                blocks++;
                if (dataSize<clearTextSize) {
                    padBytesBlock(clearTextBlock,dataSize);
                    isPadded = true;
                }

                BigInteger clearText = new BigInteger(1,clearTextBlock);
                BigInteger cipherText = clearText.modPow(e,n);
                System.out.println(cipherText.toString());
                fos.write(cipherText.toString()+"\n");

//                byte[] cipherTextData = cipherText.toByteArray();
//                putBytesBlock(cipherTextBlock,cipherTextData);
//                fos.write(cipherTextBlock);

                dataSize = fis.read(clearTextBlock);
            }

//       Adding a full padding block, if needed
            if (!isPadded) {
                blocks++;
                padBytesBlock(clearTextBlock,0);
                BigInteger clearText = new BigInteger(1,clearTextBlock);
                BigInteger cipherText = clearText.modPow(e,n);
                System.out.println(cipherText.toString());
                fos.write(cipherText.toString()+"\n");
//                byte[] cipherTextData = cipherText.toByteArray();
//                putBytesBlock(cipherTextBlock,cipherTextData);
//                fos.write(cipherTextBlock);
            }

            fis.close();
            fos.close();
            System.out.println("Encryption block count: "+blocks);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    public void interencrypt(String file){
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            while (line!=null&&line.length()!=0) {
                BigInteger clearText = new BigInteger(line);
                BigInteger cipherText = clearText.modPow(e, n);
                output = output + cipherText+"\n";
                line = in.readLine();
            }
//            System.out.println("cipherText\n"+output);
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(file),"utf-8");
            fos.write(output+"\n");
            fos.close();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void finencrypt(String interput, String outputfile){
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("final Cleartext block size: "+clearTextSize);
        System.out.println("final Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(interput));
            FileOutputStream fos = new FileOutputStream(outputfile);
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            String line = in.readLine();
            while (line!=null&&line.length()!=0) {
                BigInteger clearText = new BigInteger(line);
                BigInteger cipherText = clearText.modPow(e, n);
                output = output + cipherText+"\n";
                byte[] cipherTextData = cipherText.toByteArray();
                putBytesBlock(cipherTextBlock,cipherTextData);
                fos.write(cipherTextBlock);
                line = in.readLine();
            }
//            System.out.println("cipherText\n"+output);
            fos.close();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}