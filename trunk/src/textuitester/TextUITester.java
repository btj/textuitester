package textuitester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class TextUITester {
	
	private static class TimerThread extends Thread {
		
		Object monitor = new Object();
		boolean done;
		Runnable task;
		
		@Override
		public void run() {
			while (!done) {
				Runnable currentTask;
				synchronized (monitor) {
					while (task == null) {
						try {
							monitor.wait();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					currentTask = task;
					task = null;
				}
				currentTask.run();
			}
		}
		
		/** Execute the runnable. Cancels the preceding runnable if not yet started. */
		public void execute(Runnable run) {
			synchronized (monitor) {
				task = run;
				monitor.notify();
			}
		}
		
		public void kill() {
			execute(new Runnable() {

				@Override
				public void run() {
					done = true;
				}
				
			});
		}
		
	}
	
	private Process process;
	private PrintWriter writer;
	private BufferedReader reader;
	private int timeoutMillis = 500;
	private TimerThread timerThread = new TimerThread();
	
	{ timerThread.start(); }
	
	private final class ErrorStreamPumper extends Thread {
		public void run() {
			BufferedReader errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			for (;;) {
				String line;
				try {
					line = errorStream.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				if (line == null)
					break;
				System.err.println("[TextUITester process error stream] "+line);
			}
		}
	}
	
	public TextUITester(String command) {
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e1) {
			throw new TextUITesterException("Error while starting process: "+e1, e1);
		}
		writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		new ErrorStreamPumper().start();
	}
	
	public void setTimeout(int millis) {
		timeoutMillis = millis;
	}
	
	private void withTimeout(Runnable runnable) {
		class Task implements Runnable {
			
			boolean startedSleeping;
			boolean doneSleeping;
			boolean payloadFinished;
			
			@Override
			public void run() {
				synchronized (this) {
					if (payloadFinished)
						return;
					startedSleeping = true;
				}
				try {
					Thread.sleep(timeoutMillis);
				} catch (InterruptedException e) {
				}
				synchronized (this) {
					Thread.interrupted(); // Clear interrupted flag
					doneSleeping = true;
					if (!payloadFinished) {
						kill();
					}
				}
			}
			
		}
		Task task = new Task();
		timerThread.execute(task);
		TextUITesterException ex;
		try {
			runnable.run();
			ex = null;
		} catch (TextUITesterException e) {
			ex = e;
		}
		synchronized (task) {
			if (task.doneSleeping)
				throw new TextUITesterException("Timeout");
			else {
				task.payloadFinished = true;
				if (task.startedSleeping)
					timerThread.interrupt();
			}
		}
		if (ex != null) {
			kill();
			throw ex;
		}
	}
	
	public void sendLine(final String line) {
		withTimeout(new Runnable() {

			@Override
			public void run() {
				writer.println(line);
				if (writer.checkError())
					throw new TextUITesterException("Error while writing");
			}
			
		});
	}
	
	public void expectLine(final String expectedLine) {
		withTimeout(new Runnable() {

			@Override
			public void run() {
				String actualLine;
				try {
					actualLine = reader.readLine();
				} catch (IOException e) {
					throw new TextUITesterException("I/O erorr while reading: "+e, e);
				}
				if (actualLine == null)
					throw new TextUITesterException("Process ended while reading");
			    if (!expectedLine.equals(actualLine))
			    	throw new TextUITesterException("Unexpected line: \""+actualLine+"\"");
			}
			
		});
	}
	
	public void expectExit(final int expectedExitCode) {
		withTimeout(new Runnable() {

			@Override
			public void run() {
				int actualExitCode;
				try {
					writer.close();
					actualExitCode = process.waitFor();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (expectedExitCode != actualExitCode)
					throw new TextUITesterException("Unexpected exit code: "+actualExitCode);
			}
			
		});
		timerThread.kill();
	}
	
	public void kill() {
		process.destroy();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		timerThread.kill();
	}
	
}
