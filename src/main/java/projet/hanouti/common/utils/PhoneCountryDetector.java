package projet.hanouti.common.utils;

public class PhoneCountryDetector {

    public static class PhoneInfo {
        public final boolean valid;
        public final String flag;
        public final String country;
        public final String message;

        public PhoneInfo(boolean valid, String flag, String country, String message) {
            this.valid = valid;
            this.flag = flag;
            this.country = country;
            this.message = message;
        }
    }

    public static PhoneInfo detect(String phone) {
        if (phone == null || phone.isBlank()) {
            return new PhoneInfo(false, "", "", "");
        }

        String clean = phone.replaceAll("[\\s\\-()]", "");

        if (clean.startsWith("00216")) clean = "+216" + clean.substring(5);
        if (clean.startsWith("0033")) clean = "+33" + clean.substring(4);
        if (clean.startsWith("00213")) clean = "+213" + clean.substring(5);
        if (clean.startsWith("00212")) clean = "+212" + clean.substring(5);
        if (clean.startsWith("0039")) clean = "+39" + clean.substring(4);
        if (clean.startsWith("0049")) clean = "+49" + clean.substring(4);
        if (clean.startsWith("001")) clean = "+1" + clean.substring(3);

        if (clean.startsWith("+216")) {
            String n = clean.substring(4);
            boolean ok = n.matches("[24579][0-9]{7}");
            return new PhoneInfo(ok, "🇹🇳", "Tunisie", ok ? "Numéro tunisien valide" : "Numéro tunisien invalide : 8 chiffres après +216");
        }

        if (clean.startsWith("+33")) {
            String n = clean.substring(3);
            boolean ok = n.matches("[1-9][0-9]{8}");
            return new PhoneInfo(ok, "🇫🇷", "France", ok ? "Numéro français valide" : "Numéro français invalide : 9 chiffres après +33");
        }

        if (clean.startsWith("+213")) {
            String n = clean.substring(4);
            boolean ok = n.matches("[567][0-9]{8}");
            return new PhoneInfo(ok, "🇩🇿", "Algérie", ok ? "Numéro algérien valide" : "Numéro algérien invalide");
        }

        if (clean.startsWith("+212")) {
            String n = clean.substring(4);
            boolean ok = n.matches("[567][0-9]{8}");
            return new PhoneInfo(ok, "🇲🇦", "Maroc", ok ? "Numéro marocain valide" : "Numéro marocain invalide");
        }

        if (clean.startsWith("+39")) {
            String n = clean.substring(3);
            boolean ok = n.matches("[0-9]{6,11}");
            return new PhoneInfo(ok, "🇮🇹", "Italie", ok ? "Numéro italien valide" : "Numéro italien invalide");
        }

        if (clean.startsWith("+49")) {
            String n = clean.substring(3);
            boolean ok = n.matches("[0-9]{7,12}");
            return new PhoneInfo(ok, "🇩🇪", "Allemagne", ok ? "Numéro allemand valide" : "Numéro allemand invalide");
        }

        if (clean.startsWith("+1")) {
            String n = clean.substring(2);
            boolean ok = n.matches("[2-9][0-9]{9}");
            return new PhoneInfo(ok, "🇺🇸", "USA/Canada",
                    ok ? "Numéro USA/Canada valide" : "Numéro USA/Canada invalide");
        }

        /* numéro tunisien sans +216 */
        if (clean.matches("[24579][0-9]{7}")) {
            return new PhoneInfo(true, "🇹🇳", "Tunisie", "Numéro tunisien valide");
        }


        return new PhoneInfo(false, "🌍", "Inconnu", "Indicatif pays non reconnu. Exemple: +216 XX XXX XXX");
    }
}