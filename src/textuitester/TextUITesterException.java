package textuitester;

@SuppressWarnings("serial")
public class TextUITesterException extends RuntimeException {

	public TextUITesterException() {
	}

	public TextUITesterException(String message) {
		super(message);
	}

	public TextUITesterException(Throwable cause) {
		super(cause);
	}

	public TextUITesterException(String message, Throwable cause) {
		super(message, cause);
	}

}
