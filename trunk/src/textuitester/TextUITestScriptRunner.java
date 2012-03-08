package textuitester;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextUITestScriptRunner {
	
	public static void main(String[] args) throws IOException {
		try {
			runTestScript(args[0]);
		} catch (TextUITesterException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void runTestScript(String scriptFileName) throws IOException {
		runTestScript(new FileInputStream(scriptFileName));
	}
	
	public static void runTestScript(Class<?> testClass, String scriptResourceName) throws IOException {
		runTestScript(testClass.getResourceAsStream(scriptResourceName));
	}
	
	public static void runTestScript(InputStream stream) throws IOException {
		try {
			runTestScript(new BufferedReader(new InputStreamReader(stream)));
		} finally {
			stream.close();
		}
	}

	public static void runTestScript(BufferedReader script) throws IOException {
		int lineNumber = 0;
		TextUITester tester = null;
		try {
			for (;;) {
				lineNumber++;
				String scriptLine = script.readLine();
				if (scriptLine == null) {
					if (tester != null)
						throw new RuntimeException("Line "+lineNumber+": Script ends while process under test still running.");
					break;
				}
				try {
					char opcode = scriptLine.charAt(0);
					String argument = scriptLine.substring(1);
					switch (opcode) {
					case 'E':
						if (tester != null) {
							throw new RuntimeException("Line "+lineNumber+": Test script error: terminate current process under test first.");
						}
						tester = new TextUITester(argument);
						break;
					case 'T':
						tester.setTimeout(Integer.parseInt(argument));
						break;
					case '<':
						tester.sendLine(argument);
						break;
					case '>':
						tester.expectLine(argument);
						break;
					case 'X':
						tester.expectExit(Integer.parseInt(argument));
						tester = null;
						break;
					case 'K':
						tester.kill();
						tester = null;
						break;
					}
				} catch (TextUITesterException e) {
					throw new TextUITesterException("Line "+lineNumber+": "+e.getMessage(), e);
				}
			}
		} finally {
			script.close();
		}
	}
	
}
