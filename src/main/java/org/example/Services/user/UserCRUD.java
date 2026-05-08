package org.example.Services.user;

import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Utils.MyBD;
import org.example.Utils.Query;
import org.example.Entites.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserCRUD implements CRUDuser<User> {

    private Connection connection;

    public UserCRUD() {
        this.connection = MyBD.getInstance().getConnection();
    }

    /**
     * Hash un mot de passe avec SHA-256
     * @param password Le mot de passe en clair
     * @return Le mot de passe hashé en hexadécimal
     */
    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createUser(User user) throws SQLException {

        String hashedPassword = hashPassword(user.getMot_de_pass());

        String req = "INSERT INTO users " +
                "(nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status, adresse, face_id_enabled, face_image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getDate_naiss());
            pstmt.setString(4, user.getE_mail());
            pstmt.setString(5, user.getNum_tel());
            pstmt.setString(6, hashedPassword);
            pstmt.setString(7, user.getImage());
            pstmt.setString(8, user.getRole().name());
            pstmt.setString(9, user.getStatus().name());

            pstmt.setString(10, user.getAdresse());

            pstmt.setBoolean(11, user.isFaceIdEnabled());
            pstmt.setString(12, user.getFaceImagePath());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
            }
        }
    }

    @Override
    public void updateUser(User user) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.UPDATE_USER_QUERY)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getDate_naiss());
            pstmt.setString(4, user.getE_mail());
            pstmt.setString(5, user.getNum_tel());
            pstmt.setString(6, user.getRole().name());
            pstmt.setString(7, user.getStatus().name());
            pstmt.setInt(8, user.getId());

            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateImageUser(User user) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.UPDATE_IMAGE_QUERY)) {
            pstmt.setString(1, user.getImage());
            pstmt.setInt(2, user.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updatePassword(User user) throws SQLException {
        // Hasher le nouveau mot de passe
        String hashedPassword = hashPassword(user.getMot_de_pass());

        try (PreparedStatement pstmt = connection.prepareStatement(Query.UPDATE_PASSWORD_QUERY)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, user.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteUser(User user) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.DELETE_USER_QUERY)) {
            pstmt.setInt(1, user.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<User> ShowUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(Query.SHOW_USERS_QUERY)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    @Override
    public List<User> getUserByName(String name) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(Query.GET_USER_BY_NAME_QUERY)) {
            pstmt.setString(1, "%" + name + "%");
            pstmt.setString(2, "%" + name + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public User getUserById(int id) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.GET_USER_BY_ID_QUERY)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User getUserByEmail(String email) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.GET_USER_BY_EMAIL_QUERY)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User signIn(User user) throws SQLException {
        // D'abord, récupérer l'utilisateur par email
        User dbUser = getUserByEmail(user.getE_mail());

        if (dbUser == null) {
            return null; // Email n'existe pas
        }

        // Vérifier le mot de passe hashé
        String hashedInputPassword = hashPassword(user.getMot_de_pass());

        if (!hashedInputPassword.equals(dbUser.getMot_de_pass())) {
            return null; // Mot de passe incorrect
        }

        // Vérifier si le compte n'est pas banni
        if (dbUser.getStatus() == Status.Banned) {
            throw new SQLException("Ce compte est banni. Veuillez contacter l'administrateur.");
        }

        return dbUser;
    }

    @Override
    public void markEmailAsVerified(int userId) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.MARK_EMAIL_VERIFIED_QUERY)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setDate_naiss(rs.getString("date_naiss"));
        user.setE_mail(rs.getString("e_mail"));
        user.setNum_tel(rs.getString("num_tel"));
        user.setMot_de_pass(rs.getString("mot_de_pass"));
        user.setImage(rs.getString("image"));
        user.setFaceIdEnabled(rs.getBoolean("face_id_enabled"));
        user.setFaceImagePath(rs.getString("face_image_path"));
        user.setAdresse(rs.getString("adresse"));

        String roleStr = rs.getString("role");
        if (roleStr != null) {
            user.setRole(Role.valueOf(roleStr.toLowerCase()));
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            user.setStatus(Status.valueOf(statusStr));
        }

        return user;
    }

    public boolean emailExists(String email) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.CHECK_EMAIL_EXISTS_QUERY)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<User> getUsersByRole(Role role) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(Query.GET_USERS_BY_ROLE_QUERY)) {
            pstmt.setString(1, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        return users;
    }

    public List<User> getUsersByStatus(Status status) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(Query.GET_USERS_BY_STATUS_QUERY)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        return users;
    }


    public void updateUserStatus(int userId, Status status) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(Query.UPDATE_USER_STATUS_QUERY)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    public boolean updatePasswordByEmail(String email, String newPassword) throws SQLException {

        String hashedPassword = hashPassword(newPassword);

        String sql = "UPDATE user SET mot_de_pass = ? WHERE e_mail = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, hashedPassword);
            pst.setString(2, email);

            return pst.executeUpdate() > 0;
        }
    }

    public int getTotalUserCount() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(Query.GET_TOTAL_USER_COUNT_QUERY)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    public void updateUserAdminFields(User user) throws SQLException {

        String sql =
                "UPDATE users SET role = ?, status = ? WHERE id = ?";

        PreparedStatement pst =
                connection.prepareStatement(sql);

        pst.setString(1, user.getRole().name());
        pst.setString(2, user.getStatus().name());
        pst.setInt(3, user.getId());

        pst.executeUpdate();
    }
}