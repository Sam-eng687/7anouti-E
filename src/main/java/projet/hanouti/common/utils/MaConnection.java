package projet.hanouti.common.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MaConnection {
    private String url="jdbc:mysql://localhost:3306/7anouti_db";
    private String login="root";
    private String pwd="";
    private static MaConnection instance;

    private Connection cnx;

    public static MaConnection getInstance() {
        if (instance ==null)
            instance = new MaConnection();
        return instance;
    }

    private MaConnection(){
        try {
            cnx = DriverManager.getConnection(url,login,pwd);
            System.out.println("Connexion établie!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Connection getCnx() {
        return cnx;
    }
}

