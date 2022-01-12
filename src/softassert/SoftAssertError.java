package testpackage.softassert;

public class SoftAssertError {

    private Throwable assertionError;

    private String step;

    public SoftAssertError(Throwable assertionError, String step) {
        this.assertionError = assertionError;
        this.step = step;
    }

    public Throwable getAssertionError() {
        return assertionError;
    }

    public String getStep() {
        return step;
    }
}