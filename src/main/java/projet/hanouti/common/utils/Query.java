package projet.hanouti.common.utils;

public class Query {
    //user
    public static String ADD_USER_QUERY =
            "INSERT INTO users (nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status, face_id_enabled, face_image_path)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static String SHOW_USERS_QUERY =
            "SELECT * FROM users";

    public static String GET_USER_BY_ID_QUERY =
            "SELECT * FROM users WHERE id = ?";

    public static String GET_USER_BY_EMAIL_QUERY =
            "SELECT * FROM users WHERE e_mail = ?";

    public static String GET_USER_BY_NAME_QUERY =
            "SELECT * FROM users WHERE nom LIKE ? OR prenom LIKE ?";

    public static String SIGN_IN_QUERY =
            "SELECT * FROM users WHERE e_mail = ? AND mot_de_pass = ?";

    public static String UPDATE_USER_QUERY =
            "UPDATE users SET nom = ?, prenom = ?, date_naiss = ?, e_mail = ?, num_tel = ?, role = ?, status = ? " +
                    "WHERE id = ?";

    public static String UPDATE_IMAGE_QUERY =
            "UPDATE users SET image = ? WHERE id = ?";

    public static String UPDATE_PASSWORD_QUERY =
            "UPDATE users SET mot_de_pass = ? WHERE id = ?";

    public static String MARK_EMAIL_VERIFIED_QUERY =
            "UPDATE users SET email_verified = TRUE WHERE id = ?";

    public static String UPDATE_USER_STATUS_QUERY =
            "UPDATE users SET status = ? WHERE id = ?";

    public static String DELETE_USER_QUERY =
            "DELETE FROM users WHERE id = ?";

    public static String CHECK_EMAIL_EXISTS_QUERY =
            "SELECT COUNT(*) FROM users WHERE e_mail = ?";

    public static String GET_USERS_BY_ROLE_QUERY =
            "SELECT * FROM users WHERE role = ?";

    public static String GET_USERS_BY_STATUS_QUERY =
            "SELECT * FROM users WHERE status = ?";

    public static String GET_TOTAL_USER_COUNT_QUERY =
            "SELECT COUNT(*) FROM users";

}