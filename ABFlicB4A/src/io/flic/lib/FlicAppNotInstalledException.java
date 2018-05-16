package io.flic.lib;

/**
 * Exception thrown when the Flic App was not installed
 */
import anywheresoftware.b4a.BA.Hide;

@Hide
public class FlicAppNotInstalledException extends RuntimeException {
	FlicAppNotInstalledException(String s) {
		super(s);
	}
}
