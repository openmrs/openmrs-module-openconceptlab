package org.openmrs.module.openconceptlab.updater;

public class ImportException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public ImportException() {
		super();
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
