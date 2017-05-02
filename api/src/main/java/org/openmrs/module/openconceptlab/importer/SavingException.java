package org.openmrs.module.openconceptlab.importer;


public class SavingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SavingException() {
        super();
    }

    public SavingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SavingException(String message) {
        super(message);
    }

    public SavingException(Throwable cause) {
        super(cause);
    }

}
