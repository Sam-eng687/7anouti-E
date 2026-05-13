package projet.hanouti.produit_fournisseur.utils;

import projet.hanouti.common.utils.MyBD;

import java.sql.Connection;

public final class MyConnection {
    private static final MyConnection INSTANCE = new MyConnection();

    private MyConnection() {
    }

    public static MyConnection getInstance() {
        return INSTANCE;
    }

    public Connection getCnx() {
        return MyBD.getInstance().getConnection();
    }
}
