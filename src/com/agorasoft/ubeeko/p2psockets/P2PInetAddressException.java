package com.agorasoft.ubeeko.p2psockets;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.UnknownHostException;

public class P2PInetAddressException extends UnknownHostException {	
	private Throwable t;

	public P2PInetAddressException(Throwable t) {
		this.t = t;
	}

	public Throwable getCause() {
		return t.getCause();
	}
	public String getLocalizedMessage() {
		return t.getLocalizedMessage();
	}

	public String getMessage() {
		return t.getMessage();
	}

	public StackTraceElement[] getStackTrace() {
		return t.getStackTrace();
	}

	public Throwable initCause(Throwable cause) {
		return t.initCause(cause);
	}

	public void printStackTrace() {
		t.printStackTrace();
	}

	public void printStackTrace(PrintStream s) {
		t.printStackTrace(s);
	}

	public void printStackTrace(PrintWriter s) {
		t.printStackTrace(s);
	}

	public void setStackTrace(StackTraceElement[] stackTrace) {
		t.setStackTrace(stackTrace);
	}

	public String toString() {
		return t.toString();
	}
}
