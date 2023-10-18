package hbm.factory;
import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

public class HibernateSessionFactory {
	static Logger logger = Logger.getLogger( HibernateSessionFactory.class.getName());
	private static String CONFIG_FILE_LOCATION = "hibernate.cfg.xml";
	private static ThreadLocal<Session> threadLocal = new ThreadLocal<Session>();
	private static Configuration configuration = new Configuration();
	private static SessionFactory sessionFactory = null;
	private static boolean encryptored = false;
	
	private static boolean encryptoredPassword() {
		if(encryptored)return encryptored;
		String sEncryptorPassword = configuration.getProperty("hibernate.connection.encryptor_password");
		if(sEncryptorPassword!=null && (sEncryptorPassword.equals("1") || sEncryptorPassword.equalsIgnoreCase("des"))){
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword("MYTAB");
			configuration.setProperty("hibernate.connection.password",stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password")));
			encryptored = true;
		}else if(sEncryptorPassword!=null && (sEncryptorPassword.equals("2") || sEncryptorPassword.equalsIgnoreCase("aes"))){
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword("MYTAB");
			stringEncryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
			stringEncryptor.setIvGenerator(new RandomIvGenerator()); // for PBE-AES-based algorithms, the IV generator is MANDATORY
			configuration.setProperty("hibernate.connection.password",stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password")));
			encryptored = true;
		}else if(sEncryptorPassword!=null && (sEncryptorPassword.equals("3") || sEncryptorPassword.equalsIgnoreCase("3des"))){
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword("MYTAB");
			stringEncryptor.setAlgorithm("PBEWithMD5AndTripleDES"); 
			configuration.setProperty("hibernate.connection.password",stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password")));
			encryptored = true;
		}
		return encryptored;
	}

	static {
		synchronized (configuration){
			try {
				java.io.File f = new java.io.File(System.getProperty("tab.path") + File.separator + CONFIG_FILE_LOCATION);
				if(f.exists()){
					configuration.configure(f);
				}else{
					configuration.configure(File.separator + CONFIG_FILE_LOCATION);
				}
				encryptoredPassword();
			}catch (Exception e) {
				logger.error("ERROR:",e);
			}catch(Throwable e){
				logger.error("ERROR:",e);
			}
			rebuildSessionFactory();
		}
	}

	private HibernateSessionFactory() {
	}
	
	public static Session getThreadSession() throws HibernateException {
		synchronized (configuration){
			Session session = threadLocal.get();
			if (session == null || !session.isOpen()) {
				rebuildSessionFactory();
				session = (sessionFactory != null) ? sessionFactory.openSession() : null;
				threadLocal.set(session);
			}
			return session;
		}
	}
	

	private static void rebuildSessionFactory() {
		if(sessionFactory==null){
			try {
				java.io.File f = new java.io.File(System.getProperty("tab.path") + File.separator + CONFIG_FILE_LOCATION);
				StandardServiceRegistryBuilder ssrb = null;
				if(f.exists()){
					ssrb = new StandardServiceRegistryBuilder().configure(f);
				}else{
					ssrb = new StandardServiceRegistryBuilder().configure(CONFIG_FILE_LOCATION);
				}
				encryptoredPassword();
				String sPassword = configuration.getProperty("hibernate.connection.password");
				ssrb.applySetting("hibernate.connection.password",sPassword);
				sessionFactory = configuration.buildSessionFactory(ssrb.build());
			}catch (Exception e) {
				logger.error("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
		}
	}

	public static void closeThreadSession() throws HibernateException {
		synchronized (configuration){
			Session session = (Session) threadLocal.get();
			threadLocal.set(null);
			if (session != null) {
				session.close();
			}
		}
	}
	public static Configuration getConfiguration() {
		return configuration;
	}	
	public static String getDriverClassName() throws ClassNotFoundException {
		return configuration.getProperty("hibernate.connection.driver_class");
	}
	public static String getConnectionUrl() {
		String sUrl = configuration.getProperty("hibernate.connection.url");
		String sUsername = configuration.getProperty("hibernate.connection.username");
		encryptoredPassword();
		String sPassword = configuration.getProperty("hibernate.connection.password");
		if(sUrl.indexOf("jdbc:mysql://")>=0 || sUrl.indexOf("jdbc:postgresql://")>=0){
			if(sUrl.indexOf("?")>=0) {
				sUrl +=  "&user=" + sUsername;
			}else {
				sUrl += "?user=" + sUsername;
			}
			sUrl +=  "&password=" + sPassword;
		}else if(sUrl.indexOf("jdbc:oracle:thin:")>=0) {
			String sNewUrl = "jdbc:oracle:thin:" + sUsername + "/" + sPassword;
			sUrl = sUrl.replaceFirst("jdbc:oracle:thin:", sNewUrl);
		}else {//sUrl.indexOf("jdbc:sqlserver://")>=0
			sUrl += ";user=" + sUsername + ";password=" + sPassword;
		}
		logger.info("ConnectionUrl: "+sUrl);
		return sUrl;
	}
}