package ma.telecomatlas.abo.outils;

/**
 * Erreur metier remontee aux boutiques (le message est affiche tel quel en caisse).
 */
public class AboException extends RuntimeException {

    private static final long serialVersionUID = 4471L;

    private String code;

    public AboException(String msg) {
        super(msg);
        this.code = "KO";
    }

    public AboException(String code, String msg) {
        super(msg);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
