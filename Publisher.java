import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
public class Publisher {

    public static final String BROKER_URL = "ssl://smartcity-challenge.edu-al.unipmn.it:8883";

    public static final String TOPIC = "pissir/20035991/1";
    private static final String USERNAME="pissir";
    private static final String PASSWORD="pissir2020";
    private MqttClient client;
    String caFilePath = "smartcity-ca/ca_certificate.pem";
    String clientCrtFilePath = "certificates/client.crt";
    String clientKeyFilePath = "certificates/client.key";

    private Date date = new Date();


    public Publisher(String hosturl) {
	String hurl;
        if(hosturl != "") hurl = hosturl;
	else hurl = BROKER_URL;
	System.out.println("connecting to: "+hurl);

        //We have to generate a unique Client id.
        String clientId = Long.toString(date.getTime()) + "-pub";


        try {

            client = new MqttClient(hurl, clientId);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void start() {

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setWill(client.getTopic("pissir/all/cmd/LWT"), "I'm gone :(".getBytes(), 0, false);
	    
	     options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
	     SSLSocketFactory socketFactory = getSocketFactory(caFilePath, clientCrtFilePath, clientKeyFilePath, "");
			options.setSocketFactory(socketFactory);
            client.connect(options);
            int index=0;
            CmdAccepts accepted= createArray("AcceptedCommands.json");
            //Publish data forever
            while (true) {

                executeCmd(index,accepted.getArray());
		 index+=1;
		 if(index>accepted.getArray().size()-1) index=0;
                Thread.sleep(100000);


            }
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (Exception e) {
		e.printStackTrace();
	}
    }
    private CmdAccepts createArray (String namefile){
        String response="";
        String line;
        File file =new File(namefile);
        try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				response+=line;
				}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file ");

		} catch (IOException ex) {
			System.out.println("Error reading file");
		}
        Gson gson =new GsonBuilder().create();
        CmdAccepts found=gson.fromJson(response, CmdAccepts.class);

        return found;
    }
    private void executeCmd (int index, ArrayList<String> accepted) throws MqttException {
        
        String line;
		// saving the action requested
		String command = accepted.get(index);
		String response = "";
		System.out.println("accepted Commands :"+command);
		// Executing the command
		try {
			Process linuxProcess = Runtime.getRuntime().exec(command);
			// Getting the results
			linuxProcess.getOutputStream().close();
			BufferedReader stdout = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
			while ((line = stdout.readLine()) != null) {
				
				response += line;
				response += "\n";
				}
			stdout.close();
			
			BufferedReader stderr = new BufferedReader(new InputStreamReader(linuxProcess.getErrorStream()));
			while ((line = stderr.readLine()) != null) {
				
				response += line;
				response += "\n";
			}
			stderr.close();
			
			} catch (IOException ex) {
				System.out.println("errore lettura risultato comando");
			}
        
        Gson gson =new GsonBuilder().create();
        Results result=new Results(command,response);
        response=gson.toJson(result);
        
	final MqttTopic timeTopic = client.getTopic(TOPIC);
        timeTopic.publish(new MqttMessage(response.getBytes()));

        System.out.println("Published data. Topic: " + timeTopic.getName() + "  Message: " + response);
    }
public boolean isValidCommand(String command){
        ArrayList<String> commands=createArray("AcceptedCommands.json").getArray();
		return commands.contains(command);
    }
    public String executeRequest(String command)throws MqttException {
        String line;
		// saving the action requested
        String response = "";
		System.out.println("accepted Commands :"+command);

		// Executing the command
		try {
			Process linuxProcess = Runtime.getRuntime().exec(command);
			// Getting the results
			linuxProcess.getOutputStream().close();
			BufferedReader stdout = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
			while ((line = stdout.readLine()) != null) {
				
				response += line;
				response += "\n";
				}
			stdout.close();
			
			BufferedReader stderr = new BufferedReader(new InputStreamReader(linuxProcess.getErrorStream()));
			while ((line = stderr.readLine()) != null) {
				
				response += line;
				response += "\n";
			}
			stderr.close();
			
			} catch (IOException ex) {
				System.out.println("Errore esecuzione comando");
			}
        
        
	
        System.out.println("Published data. Message: " + response);
	return response;
    }
    private void startTest(String message){
	    try {
		    MqttConnectOptions options = new MqttConnectOptions();
		    options.setCleanSession(false);
		    options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
		    options.setWill(client.getTopic("pissir/all/cmd/LWT"), "I'm gone :(".getBytes(), 0, false);
		options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
			     SSLSocketFactory socketFactory = getSocketFactory(caFilePath, clientCrtFilePath, clientKeyFilePath, "");
			options.setSocketFactory(socketFactory);
		    client.connect(options);
		    if(isValidCommand(message)){
            try{
            String response=executeRequest(message);
             Gson gson =new GsonBuilder().create();
        Results result=new Results(message,response);
        response=gson.toJson(result);
            final MqttTopic timeTopic = client.getTopic("pissir/20034446/1");
            timeTopic.publish(new MqttMessage(response.getBytes()));
            client.disconnect();
            
            }catch(Exception ex){
            System.out.println("eccezione: "+ex.getMessage());
             }
        }
	    }catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        
    	}catch (Exception e) {
		e.printStackTrace();
	}
    }
    
    private static SSLSocketFactory getSocketFactory(final String caCrtFile,
			final String crtFile, final String keyFile, final String password)
			throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		// load CA certificate
		X509Certificate caCert = null;

		FileInputStream fis = new FileInputStream(caCrtFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		while (bis.available() > 0) {
			caCert = (X509Certificate) cf.generateCertificate(bis);
			// System.out.println(caCert.toString());
		}

		// load client certificate
		bis = new BufferedInputStream(new FileInputStream(crtFile));
		X509Certificate cert = null;
		while (bis.available() > 0) {
			cert = (X509Certificate) cf.generateCertificate(bis);
			// System.out.println(caCert.toString());
		}

		// load client private key
		PEMParser pemParser = new PEMParser(new FileReader(keyFile));
		Object object = pemParser.readObject();
		PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
				.build(password.toCharArray());
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
				.setProvider("BC");
		KeyPair key;
		if (object instanceof PEMEncryptedKeyPair) {
			System.out.println("Encrypted key - we will use provided password");
			key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
					.decryptKeyPair(decProv));
		} else {
			System.out.println("Unencrypted key - no password needed");
			key = converter.getKeyPair((PEMKeyPair) object);
		}
		pemParser.close();

		// CA certificate is used to authenticate server
		KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
		caKs.load(null, null);
		caKs.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		tmf.init(caKs);

		// client key and certificates are sent to server so it can authenticate
		// us
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("certificate", cert);
		ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
				new java.security.cert.Certificate[] { cert });
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());
		kmf.init(ks, password.toCharArray());

		// finally, create SSL socket factory
		SSLContext context = SSLContext.getInstance("TLSv1.2");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return context.getSocketFactory();
	}
    public static void main(String args[]) {
        final Publisher publisher;
	String hosturl = "";
	if(args.length >= 1) hosturl = args[0];
	String request="";
	if(args.length==2) request=args[1];
	publisher = new Publisher(hosturl);
	if(args.length==2){
	publisher.startTest(request);
	}
	else{
        publisher.start();
    	}
    	}
}
