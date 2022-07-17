package nghviet.hgw.security;

import nghviet.hgw.http.HttpHandler;

import nghviet.hgw.utility.FileIO;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class SecurityHandler {
    private static final String private_key_path = "client.key";
    private static final String public_key_path = "client.pub";
    private static final String ca_cert = "ca.crt";
    private static final String signed_cert = "client.crt";


    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;
    private KeyPairGenerator generator = null;
    private static SecurityHandler securityHandler = null;

    private X509Certificate CA = null;
    private X509Certificate crt = null;

    private CertificateFactory certificateFactory;


    public static synchronized SecurityHandler getInstance() {
        if(securityHandler == null) try {
            securityHandler = new SecurityHandler();
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return securityHandler;
    }

    public X509Certificate getCACertificate() {
        return CA;
    }

    public X509Certificate getSignedCert() {
        return crt;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getID() { return null; }

    private static class SignRequest {
        public String csr;
    }

    private SecurityHandler() throws Exception {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                init();
            } catch(Exception ex) {
                throw ex;
            }
        }
    }

    private void init() throws Exception {
        generator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = generator.generateKeyPair();

        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();

        PKCS10CertificationRequestBuilder pkcsBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Principal(""), publicKey);
        JcaContentSignerBuilder csbuilder = new JcaContentSignerBuilder("SHA256WithRSA");
        ContentSigner signer = csbuilder.build(privateKey);
        PKCS10CertificationRequest csr = pkcsBuilder.build(signer);

        try (OutputStream stream = new FileOutputStream("user.csr");
             Writer writer = new OutputStreamWriter(stream, "UTF-8");
             JcaPEMWriter out = new JcaPEMWriter(writer)) {
            out.writeObject(csr);
        }

        HashMap<String , String> body = new HashMap<>();
        body.put("csr", Base64.getEncoder().encodeToString(csr.getEncoded()));
        ArrayList<String> response = HttpHandler.getInstance().request("POST", "http://112.137.129.202:8080/API/signer/sign",body,true);
        if(response == null) throw new RuntimeException("Cannot sign CSR");

        crt = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(response.get(1))));
        FileIO.getInstance().writeTo(signed_cert, new ArrayList<String>(Arrays.asList(response.get(1))));
        response = HttpHandler.getInstance().request("GET", "http://112.137.129.202:8080/API/signer/CA",null,true);

        CA = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(response.get(1))));
        FileIO.getInstance().writeTo(ca_cert, new ArrayList<String>(Arrays.asList(response.get(1))));

        FileIO.getInstance().writeTo(private_key_path, new ArrayList<String>(Arrays.asList(Base64.getEncoder().encodeToString(privateKey.getEncoded()))));
    }

    private void load() throws Exception {
        String private_key_encoded = FileIO.getInstance().readFrom(private_key_path).get(0);
        if(private_key_encoded.equals("")) throw new IllegalAccessException("Invalid private key");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(private_key_encoded));
        privateKey = keyFactory.generatePrivate(keySpec);

        String CA_encoded = FileIO.getInstance().readFrom(ca_cert).get(0);
        if(CA_encoded.equals("")) throw new IllegalAccessException("Invalid CA cert input");
        CA = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(CA_encoded)));

        System.out.println(CA);

        String crt_encoded = FileIO.getInstance().readFrom(signed_cert).get(0);
        if(crt_encoded.equals("")) throw new IllegalAccessException("Invalid signed cert input");
        crt =(X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(crt_encoded)));

        System.out.println(crt);

    }
}
