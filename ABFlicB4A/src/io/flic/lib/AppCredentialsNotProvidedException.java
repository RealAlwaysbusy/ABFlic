package io.flic.lib;

import anywheresoftware.b4a.BA.Hide;

/**
 * Exception thrown when app credentials were not provided.
 */
@Hide
public class AppCredentialsNotProvidedException extends RuntimeException {
	AppCredentialsNotProvidedException(String s) {
		super(s);
	}
}
