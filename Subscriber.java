
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
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
import java.util.Date;

public class Subscriber {
//"tcp://smartcity-challenge.edu-al.unipmn.it:1883";
    public static final String BROKER_URL = "ssl://smartcity-challenge.edu-al.unipmn.it:8883";
    private Date date = new Date();
    private static final String USERNAME="pissir";
    private static final String PASSWORD="pissir2020";
    public static final String TOPICALL = "pissir/all/cmd";
    public static final String TOPIC = "pissir/20035991/1/cmd";
    String caFilePath = "smartcity-ca/ca_certificate.pem";
		String clientCrtFilePath = "certificates/client.crt";
		String clientKeyFilePath = "certificates/client.key";
    //We have to generate a unique Client id.
    String clientId = Long.toString(date.getTime()) + "-sub";
    private MqttClient mqttClient;

    public Subscriber(String hosturl) {
	String hurl = BROKER_URL;
	if(hosturl != "") hurl = hosturl;
	System.out.println("Connecting to: "+hurl);

        try {
            mqttClient = new MqttClient(hurl, clientId);


        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        try {

            mqttClient.setCallback(new SubscribeCallback());
            MqttConnectOptions connOpt=new MqttConnectOptions();
            connOpt.setCleanSession(false);
            connOpt.setUserName(USERNAME);
            connOpt.setPassword(PASSWORD.toCharArray());
            connOpt.setConnectionTimeout(60);
	     connOpt.setKeepAliveInterval(60);
	     connOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
	     SSLSocketFactory socketFactory = getSocketFactory(caFilePath, clientCrtFilePath, clientKeyFilePath, "");
			connOpt.setSocketFactory(socketFactory);
            mqttClient.connect(connOpt);

            //Subscribe to all subtopics of home
            final String topicAll = TOPICALL;
            final String topic = TOPIC;
            mqttClient.subscribe(topicAll);
            mqttClient.subscribe(topic);
            System.out.println("Subscriber is now listening to "+topic+ " and "+topicAll);

        } catch (MqttException e) {
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
        final Subscriber subscriber;
	String hosturl = "";
	if(args.length >= 1) hosturl = "tcp://"+args[0]; 
	subscriber = new Subscriber(hosturl);
        subscriber.start();
	
	
    }

}
