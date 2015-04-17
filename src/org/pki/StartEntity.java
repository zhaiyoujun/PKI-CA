package org.pki;

import org.pki.entities.CertificateOfAuthority;
import org.pki.entities.Server;
import org.pki.util.Certificate;
import org.pki.util.Key;
import org.pki.util.Keygen;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Principal;
import java.util.HashMap;

public class StartEntity {
    public static void main(String [] args) throws Exception{
        if(args.length > 4){
            System.out.println("Error: Invalid Arguments!");
        }else{
            String role = args[0];
            if(role.equals("SERVER")){
                startServer(args[1], args[2], args[3]);
            }else if(role.equals("CLIENT")){
                startClient();
            }else if(role.equals("CA")){
                startCertificateOfAuthority(args[1], args[2], args[3]);
            }else {
                System.out.println("Error: Invalid Role!");
            }
        }
    }

    private static void startServer(String trustedCertsDir, String certificatePath, String keyPath) throws Exception{
        if(certificatePath.equals("D")){
            System.out.println("Using default certificate path.");
            certificatePath = Server.CertificateFile_Default;
        }
        if(keyPath.equals("D")) {
            System.out.println("Using default key path.");
            keyPath = Server.KeyFile_Default;
        }
        if(trustedCertsDir.equals("D")) {
            System.out.println("Using default trusted certificate path.");
            trustedCertsDir = Server.TrustedCertsDir_Default;
        }
        File cp = new File(certificatePath);
        File kp = new File(keyPath);
        if((!cp.exists() && !kp.exists()) || Server.OverwriteKeys){
            System.out.println("Generating Certs and Keys...");
            Keygen kg = new Keygen(Server.getX500Name());
            kg.getCertificate().outputCertificateToFile(cp);
            kg.getKey().outputKeyToFile(kp);
        }

        // Load certs/keys
        Certificate serverCertificate = new Certificate(cp);
        Key serverKey = new Key(kp, Key.ALGORITHM_RSA);
        HashMap<Principal, Certificate> certificateStore = getCertificateStore(trustedCertsDir);

        ServerSocket serverSocket = new ServerSocket(Server.Port);
        while (true){
            Socket socket = serverSocket.accept();
            new Thread(new Server(socket, certificateStore, serverCertificate, serverKey)).start();
        }
    }

    private static void startCertificateOfAuthority(String trustedCertsDir, String certificatePath, String keyPath)throws Exception{
        if(certificatePath.equals("D")){
            System.out.println("Using default certificate path.");
            certificatePath = CertificateOfAuthority.CertificateFile_Default;
        }
        if(keyPath.equals("D")) {
            System.out.println("Using default key path.");
            keyPath = CertificateOfAuthority.KeyFile_Default;
        }
        if(trustedCertsDir.equals("D")) {
            System.out.println("Using default trusted certificate path.");
            trustedCertsDir = CertificateOfAuthority.TrustedCertsDir_Default;
        }
        File cp = new File(certificatePath);
        File kp = new File(keyPath);
        if((!cp.exists() && !kp.exists()) || CertificateOfAuthority.OverwriteKeys){
            System.out.println("Generating Certs and Keys...");
            Keygen kg = new Keygen(Server.getX500Name());
            kg.getCertificate().outputCertificateToFile(cp);
            kg.getKey().outputKeyToFile(kp);
        }

        // Load certs/keys
        Certificate caCertificate = new Certificate(cp);
        HashMap<Principal, Certificate> certificateStore = getCertificateStore(trustedCertsDir);
        Key key = new Key(kp, Key.ALGORITHM_RSA);

        ServerSocket serverSocket = new ServerSocket(CertificateOfAuthority.Port);
        while (true){
            Socket socket = serverSocket.accept();
            new Thread(new CertificateOfAuthority(socket, certificateStore, caCertificate, key)).start();
        }
    }

    private static void startClient(){
        String trustedCertsDir = "certificatestore/ca/trustedcerts";

    }

    private static HashMap<Principal, Certificate> getCertificateStore(String trustedCertsDir) throws Exception{
        HashMap<Principal, Certificate> certificateStore = new HashMap<Principal, Certificate>();

        for(File file : new File(trustedCertsDir).listFiles()){
            if(file == null){
                break;
            }
            Certificate certificate = new Certificate(file);
            certificateStore.put(certificate.getSubject(), certificate);
        }
        return certificateStore;
    }
}
