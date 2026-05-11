package org.example.Utils;

public class WelcomeEmailTemplate {

    public static String buildLuxury(String nom, String prenom, String role, String avatarUrl, String loginUrl, String bannerUrl) {
        String fullName = safe(prenom) + " " + safe(nom);
        String roleText = roleLabel(role);
        String roleMsg = roleMessage(role);

        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = "https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+") + "&background=6366F1&color=fff&size=128";
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8">
        <style>
            .btn:hover {
                background: linear-gradient(135deg,#312E81,#4F46E5) !important;
                box-shadow: 0 14px 32px rgba(79,70,229,0.45) !important;
            }
        </style>
        </head>
        <body style="margin:0;padding:0;background:#EEF2FF;font-family:Arial,Helvetica,sans-serif;">
        <table width="100%%" cellpadding="0" cellspacing="0" style="background:#EEF2FF;padding:36px 0;">
        <tr><td align="center">

        <table width="660" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:22px;overflow:hidden;box-shadow:0 22px 55px rgba(67,56,202,0.22);">
            <tr>
                <td>
                    <img src="%s" style="width:100%%;display:block;border:0;" alt="Bienvenue sur 7anouti-E">
                </td>
            </tr>

            <tr>
                <td align="center" style="padding:0 40px;">
                    <img src="%s" width="86" height="86"
                         style="margin-top:-43px;border-radius:50%%;border:5px solid white;box-shadow:0 12px 28px rgba(17,24,39,0.18);object-fit:cover;"
                         alt="Avatar utilisateur">
                </td>
            </tr>

            <tr>
                <td style="padding:26px 42px 12px 42px;text-align:center;">
                    <h1 style="margin:0;color:#111827;font-size:28px;font-weight:900;">
                        Bienvenue, %s 👋
                    </h1>

                    <p style="color:#6B7280;font-size:15px;line-height:1.8;margin-top:16px;">
                        Votre compte <strong>%s</strong> a été créé avec succès sur <strong>7anouti-E</strong>.
                    </p>

                    <p style="color:#6B7280;font-size:15px;line-height:1.8;margin-top:8px;">
                        %s
                    </p>
                </td>
            </tr>

            <tr>
                <td style="padding:10px 42px 28px 42px;">
                    <div style="background:linear-gradient(135deg,#EEF2FF,#F5F3FF);border:1px solid #C7D2FE;border-radius:18px;padding:20px;">
                        <div style="color:#312E81;font-size:14px;font-weight:900;margin-bottom:12px;">
                            Informations du compte
                        </div>
                        <div style="color:#4338CA;font-size:14px;line-height:1.8;">
                            Nom : <strong>%s</strong><br>
                            Rôle : <strong>%s</strong>
                        </div>
                    </div>
                </td>
            </tr>

            <tr>
                <td align="center" style="padding:4px 42px 42px 42px;">
                    <a class="btn" href="%s"
                       style="background:linear-gradient(135deg,#4338CA,#6366F1);color:white;padding:15px 34px;border-radius:14px;text-decoration:none;font-weight:900;font-size:14px;display:inline-block;box-shadow:0 12px 28px rgba(79,70,229,0.38);">
                       Accéder à mon espace
                    </a>
                </td>
            </tr>

            <tr>
                <td style="background:#F9FAFB;text-align:center;padding:22px 30px;">
                    <p style="margin:0;color:#9CA3AF;font-size:12px;">
                        © 2026 7anouti-E — Tous droits réservés.
                    </p>
                </td>
            </tr>
        </table>

        </td></tr>
        </table>
        </body>
        </html>
        """.formatted(
                bannerUrl,
                avatarUrl,
                fullName.trim(),
                roleText,
                roleMsg,
                fullName.trim(),
                roleText,
                loginUrl
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String roleLabel(String role) {
        if (role == null) return "Utilisateur";

        return switch (role.toLowerCase()) {
            case "acheteur" -> "Acheteur";
            case "vendeur" -> "Vendeur";
            case "livreur" -> "Livreur";
            case "admin" -> "Administrateur";
            default -> "Utilisateur";
        };
    }

    private static String roleMessage(String role) {
        if (role == null) return "Votre espace personnel est prêt.";

        return switch (role.toLowerCase()) {
            case "acheteur" -> "Vous pouvez découvrir les produits, passer vos commandes et suivre vos achats facilement.";
            case "vendeur" -> "Vous pouvez préparer votre espace vendeur, gérer vos produits et développer votre activité.";
            case "livreur" -> "Vous pouvez accéder à votre espace livreur et préparer vos futures missions de livraison.";
            case "admin" -> "Vous avez accès à l’espace d’administration pour superviser la plateforme.";
            default -> "Votre espace personnel est prêt.";
        };
    }
}