package org.openmrs.module.openconceptlab;

public class ImportException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public ImportException() {
		super();
	}
	
	public ImportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
	public ImportException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ImportException(String message) {
		super(message);
	}
	
	public ImportException(Throwable cause) {
		super(cause);
	}
	
}
