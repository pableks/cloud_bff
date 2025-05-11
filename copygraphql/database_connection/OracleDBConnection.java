package com.database_connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class OracleDBConnection {

    private static final Logger logger = Logger.getLogger(OracleDBConnection.class.getName());

    public static Connection getConnection() throws SQLException {
        String tnsName = System.getenv("ORACLE_TNS_NAME");
        String user = System.getenv("ORACLE_USER");
        String password = System.getenv("ORACLE_PASSWORD");
        String walletPath = System.getenv("ORACLE_WALLET_PATH");

        logger.info("Conectando con Oracle...");
        logger.info("TNS: " + tnsName);
        logger.info("Usuario: " + user);
        logger.info("Ruta Wallet: " + walletPath);
        System.out.println("Test");

        String url = "jdbc:oracle:thin:@" + tnsName + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.ssl_version", "1.2");
        props.setProperty("oracle.net.wallet_location",
                "(SOURCE=(METHOD=file)(METHOD_DATA=(DIRECTORY=" + walletPath + ")))");
        
        logger.info("Conectando con Oracle...");
        logger.info("TNS: " + tnsName);
        logger.info("Usuario: " + user);
        logger.info("Ruta Wallet: " + walletPath);
        
        try {
            Connection conn = DriverManager.getConnection(url, props);
            logger.info(("Conectado a Oracle!"));
            return conn;
        } catch (Exception e) {
            logger.severe("No se pudo conectar a Oracle!: " + e.getMessage());
            throw e;
        }
    }
    
}
