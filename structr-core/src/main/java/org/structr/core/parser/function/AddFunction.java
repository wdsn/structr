/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.parser.function;

import java.util.logging.Level;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AddFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ADD = "Usage: ${add(values...)}. Example: ${add(1, 2, 3, this.children.size)}";

	@Override
	public String getName() {
		return "add()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		Double result = 0.0;

		if (sources != null) {

			for (Object i : sources) {

				if (i != null) {

					try {

						result += Double.parseDouble(i.toString());

					} catch (Throwable t) {

						logException(t, sources);

						return t.getMessage();

					}

				}

			}

		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ADD;
	}

	@Override
	public String shortDescription() {
		return "Returns the sum of the given arguments";
	}
}
