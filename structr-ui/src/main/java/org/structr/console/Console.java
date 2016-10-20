/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.console.command.ConsoleCommand;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.script.StructrScriptable;
import org.structr.schema.action.ActionContext;
import org.structr.util.Writable;

/**
 *
 */
public class Console {

	public enum ConsoleMode {
		cypher, javascript, structrscript, adminshell
	};

	private ConsoleMode mode             = ConsoleMode.javascript;
	private StructrScriptable scriptable = null;
	private ActionContext actionContext  = null;
	private ScriptableObject scope       = null;

	public Console(final SecurityContext securityContext, final Map<String, Object> parameters) {
		this.actionContext = new ActionContext(securityContext, parameters);
	}

	public String runForTest(final String line) throws FrameworkException {

		final PrintWritable writable = new PrintWritable();

		// run
		try { run(line, writable); } catch (IOException ioex) {}

		return writable.getBuffer();
	}

	public void run(final String line, final Writable output) throws FrameworkException, IOException {

		if (line.startsWith("Console.setMode('javascript')") || line.startsWith("Console.setMode(\"javascript\")")) {

			mode = ConsoleMode.javascript;
			output.println("Mode set to 'JavaScript'.");

		} else if (line.startsWith("Console.setMode('cypher')") || line.startsWith("Console.setMode(\"cypher\")")) {

			mode = ConsoleMode.cypher;
			output.println("Mode set to 'Cypher'.");

		} else if (line.startsWith("Console.setMode('structr')") || line.startsWith("Console.setMode(\"structr\")")) {

			mode = ConsoleMode.structrscript;
			output.println("Mode set to 'StructrScript'.");

		} else if (line.startsWith("Console.setMode('shell')") || line.startsWith("Console.setMode(\"shell\")")) {

			mode = ConsoleMode.adminshell;
			output.println("Mode set to 'AdminShell'. Type 'help' to get a list of commands.");

		} else {

			switch (mode) {

				case cypher:
					runCypher(line, output);
					break;

				case javascript:
					runJavascript(line, output);
					break;

				case structrscript:
					runStructrScript(line, output);
					break;

				case adminshell:
					runAdminShell(line, output);
					break;
			}
		}
	}

	public SecurityContext getSecurityContext() {
		return actionContext.getSecurityContext();
	}

	// ----- private methods -----
	private void runCypher(final String line, final Writable writable) throws FrameworkException, IOException {

		final App app                  = StructrApp.getInstance(actionContext.getSecurityContext());
		final long t0                  = System.currentTimeMillis();
		final List<GraphObject> result = app.cypher(line, Collections.emptyMap());
		final long t1                  = System.currentTimeMillis();
		final int size                 = result.size();

		writable.print("Query returned ", size, " objects in ", (t1-t0), " ms.");
		writable.println();
		writable.println();

		if (size <= 10) {

			writable.print(Functions.get("to_json").apply(actionContext, null, new Object[] { result } ));

		} else {

			writable.print("Too many results (> 10), please use LIMIT to reduce the result count of your Cypher query.");
		}

		writable.println();
	}

	private void runStructrScript(final String line, final Writable writable) throws FrameworkException, IOException {

		final Object result = Functions.evaluate(actionContext, null, line);
		if (result != null) {

			writable.println(result.toString());
		}
	}

	private void runJavascript(final String line, final Writable writable) throws FrameworkException {

		final Context scriptingContext = Context.enter();

		init(scriptingContext);

		try {
			Object extractedValue = scriptingContext.evaluateString(scope, line, "interactive script, line ", 1, null);

			if (scriptable.hasException()) {
				throw scriptable.getException();
			}

			// prioritize written output over result returned from method
			final String output = actionContext.getOutput();
			if (output != null && !output.isEmpty()) {
				extractedValue = output;
			}

			if (extractedValue != null) {
				writable.println(extractedValue.toString());
			}

		} catch (final FrameworkException fex) {

			// just throw the FrameworkException so we dont lose the information contained
			throw fex;

		} catch (final Throwable t) {

			throw new FrameworkException(422, t.getMessage());

		} finally {

			Context.exit();
		}
	}

	private void runAdminShell(final String line, final Writable writable) throws FrameworkException, IOException {

		final List<String> parts = splitAnClean(line);
		if (!parts.isEmpty()) {

			final ConsoleCommand cmd = ConsoleCommand.getCommand(parts.get(0));
			if (cmd != null) {

				cmd.run(actionContext.getSecurityContext(), parts, writable);

			} else {

				writable.println("Unknown command '" + line + "'.");
			}

		} else {

			writable.println("Syntax error.");
		}
	}

	private void init(final Context scriptingContext) {

		// Set version to JavaScript1.2 so that we get object-literal style
		// printing instead of "[object Object]"
		scriptingContext.setLanguageVersion(Context.VERSION_1_2);

		// Initialize the standard objects (Object, Function, etc.)
		// This must be done before scripts can be executed.
		if (this.scope == null) {
			this.scope = scriptingContext.initStandardObjects();
		}

		// set optimization level to interpreter mode to avoid
		// class loading / PermGen space bug in Rhino
		//scriptingContext.setOptimizationLevel(-1);

		if (this.scriptable == null) {

			this.scriptable = new StructrScriptable(actionContext, null, scriptingContext);
			this.scriptable.setParentScope(scope);

			// register Structr scriptable
			scope.put("Structr", scope, scriptable);
		}

		// clear output buffer
		actionContext.clear();
	}

	private List<String> splitAnClean(final String src) {

		final List<String> parts = new ArrayList<>();

		for (final String part : src.split("[ ]+")) {

			final String trimmed = part.trim();

			if (StringUtils.isNotBlank(trimmed)) {

				parts.add(trimmed);
			}
		}

		return parts;
	}

	// ----- nested classes -----
	private static class PrintWritable implements Writable {

		final StringBuilder buf = new StringBuilder();

		@Override
		public void print(final Object... text) throws IOException {
			for (final Object o : text) {
				buf.append(o);
			}
		}

		@Override
		public void println(final Object... text) throws IOException {
			for (final Object o : text) {
				buf.append(o);
			}
			println();
		}

		@Override
		public void println() throws IOException {
			buf.append("\r\n");
		}

		@Override
		public void flush() throws IOException {
		}

		public String getBuffer() {
			return buf.toString();
		}
	}
}