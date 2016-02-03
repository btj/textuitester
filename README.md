Provides two classes: `textuitester.TextUITester` and `textuitester.TextUITestScriptRunner`.

Class **TextUITester** helps to send text lines to a process's standard input and to expect text lines from its standard output.

For example:

```java
	@Test
	public void test() {
		TextUITester tester = new TextUITester("java -cp examplebin textuitesterexample.Echo");
		tester.sendLine("Hello World!");
		tester.expectLine("Hello World!");
		tester.expectExit(0);
	}
```

Class **TextUITestScriptRunner** runs test scripts that look like this:

```
Ejava -cp examplebin textuitesterexample.Echo
<Hello World!
>Hello World!
X0
Ejava -cp examplebin textuitesterexample.Echo
<Hello again!
>Hello again!
<Bye World!
>Bye World!
K
```

The first character of each line in the script is the _opcode_, the rest of the line is the _argument_.

| Opcode | Meaning |
|:-------|:--------|
| E      | Start a new process under test |
| <      | Send the argument to the process under test |
| >      | Expect the argument from the process under test |
| X      | Expect the process under test to terminate with the exit code given by the argument |
| K      | Kill the process under test |
| T      | Set the timeout (in milliseconds) |

The timeout determines how long the TextUITester waits for the process's expected behavior before it signals a failure. All failures are reported as TextUITesterException instances.

Suppose the above script is stored as testScript1.txt in the same directory as your JUnit test suite. You can run it as follows:

```java
class Test1 {

	@Test
	public void scriptTest() throws IOException {
		TextUITestScriptRunner.runTestScript(Test1.class, "testScript1.txt");
	}

}
```

See also the example test suite and the Javadoc documentation included in the distribution, and the source code on this website.
