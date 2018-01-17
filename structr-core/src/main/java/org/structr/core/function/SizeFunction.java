/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.structr.common.error.FrameworkException;
import static org.structr.core.function.Functions.NULL_STRING;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SIZE = "Usage: ${size(collection)}. Example: ${size(this.children)}";

	@Override
	public String getName() {
		return "size()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();
		for (final Object source : sources) {

			if (source != null) {

				if (source instanceof Collection) {

					// filter null objects
					for (Object obj : (Collection)source) {
						if (obj != null && !NULL_STRING.equals(obj)) {

							list.add(obj);
						}
					}

				} else if (source.getClass().isArray()) {

					list.addAll(Arrays.asList((Object[])source));

				} else if (source != null && !NULL_STRING.equals(source)) {

					list.add(source);
				}

				return list.size();
			}
		}

		return 0;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SIZE;
	}

	@Override
	public String shortDescription() {
		return "Returns the size of the given collection";
	}

}
