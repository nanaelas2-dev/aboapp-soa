package ma.telecomatlas.abo.soap.types;

public class Reponse {

    private String code;
    private String message;
    private String reference;

    public Reponse() {
    }

    public Reponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Reponse ok(String ref) {
        Reponse r = new Reponse("OK", "traitement effectue");
        r.setReference(ref);
        return r;
    }

    public static Reponse ko(String msg) {
        return new Reponse("KO", msg);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

}
