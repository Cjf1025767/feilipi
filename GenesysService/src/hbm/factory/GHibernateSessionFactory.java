package hbm.factory;
import java.io.File;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class GHibernateSessionFactory {
	static Logger logger = Logger.getLogger( GHibernateSessionFactory.class.getName());
	private static String CONFIG_FILE_LOCATION = "ghibernate.cfg.xml";
	private static ThreadLocal<Session> threadLocal = new ThreadLocal<Session>();
	private static Configuration configuration = new Configuration();
	private static SessionFactory sessionFactory = null;
	public static String databaseType = "";//读取的数据库类型
	public String name="";
	

	static {
		synchronized (configuration){
			try {
				java.io.File f = new java.io.File(System.getProperty("tab.path") + File.separator + CONFIG_FILE_LOCATION);
				if(f.exists()){
					configuration.configure(f);
				}else{
					configuration.configure(File.separator + CONFIG_FILE_LOCATION);
				}
				String driverclass=configuration.getProperty("hibernate.connection.driver_class");
				if(driverclass.contains("SQLServerDriver")) {
					databaseType="SQLServer";
				}
				String sEncryptorPassword = configuration.getProperty("hibernate.connection.encryptor_password");
				if(sEncryptorPassword!=null && sEncryptorPassword.equals("1")){
					StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
					stringEncryptor.setPassword("MYTAB");
					configuration.setProperty("hibernate.connection.password",stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password")));
				}	
			}catch(org.jasypt.exceptions.EncryptionOperationNotPossibleException e){
				StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
				stringEncryptor.setPassword("MYTAB");
				logger.error("encrypt password:"+stringEncryptor.encrypt(configuration.getProperty("hibernate.connection.password")));
			}catch (Exception e) {
				logger.error("ERROR:",e);
			}catch(Throwable e){
				logger.error("ERROR:",e);
			}
			rebuildSessionFactory();
		}
	}

	private GHibernateSessionFactory() {
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
				String sEncryptorPassword = configuration.getProperty("hibernate.connection.encryptor_password");
				if(sEncryptorPassword!=null && sEncryptorPassword.equals("1")){
					StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
					stringEncryptor.setPassword("MYTAB");
					ssrb.applySetting("hibernate.connection.password",stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password")));
				}
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
		if(sUrl.indexOf("?")>=0) {
			sUrl +=  "&user=" + configuration.getProperty("hibernate.connection.username");
		}else {
			sUrl += "?user=" + configuration.getProperty("hibernate.connection.username");
		}
		String sEncryptorPassword = configuration.getProperty("hibernate.connection.encryptor_password");
		if(sEncryptorPassword!=null && sEncryptorPassword.equals("1")){
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword("MYTAB");
			sUrl +=  "&password=" + stringEncryptor.decrypt(configuration.getProperty("hibernate.connection.password"));
		}else {
			sUrl +=  "&password=" + configuration.getProperty("hibernate.connection.password");
		}
		return sUrl;
	}
}