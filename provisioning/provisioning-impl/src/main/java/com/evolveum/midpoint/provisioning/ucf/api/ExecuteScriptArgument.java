/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Collection;

public class ExecuteScriptArgument {

	
	private String argumentName;
	/**
	 * NOTE! This may contain both Object (for single-value arguments) and Collection<Object> (for multi-value arguments).
	 */
	private Object argumentValue;

	public ExecuteScriptArgument() {
		
	}
	
	public ExecuteScriptArgument(String name, Object value) {
		this.argumentName = name;
		this.argumentValue = value;
	}
	
	public String getArgumentName() {
		return argumentName;
	}

	public void setArgumentName(String argumentName) {
		this.argumentName = argumentName;
	}

	public Object getArgumentValue() {
		return argumentValue;
	}

	public void setArgumentValue(Object argumentValue) {
		this.argumentValue = argumentValue;
	}
}
