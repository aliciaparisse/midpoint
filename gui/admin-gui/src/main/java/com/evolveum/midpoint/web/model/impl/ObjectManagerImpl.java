/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.dto.ObjectDto;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class ObjectManagerImpl<C extends ObjectType, T extends ObjectDto<C>> implements
		ObjectManager<T>, Serializable {

	private static final long serialVersionUID = -7853884441389039036L;
	private static final Trace LOGGER = TraceManager.getTrace(ObjectManagerImpl.class);
	@Autowired(required = true)
	private transient ModelService model;
	@Autowired(required = true)
	private transient TaskManager taskManager;

	protected ModelService getModel() {
		return model;
	}
	
	protected TaskManager getTaskManager() {
		return taskManager;
	}

	@Override
	public Collection<T> list() {
		return list(null);
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	protected <O extends ObjectType> O get(Class<O> objectClass, String oid, PropertyReferenceListType resolve)
			throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		Validate.notNull(resolve, "Property reference list must not be null.");
		Validate.notNull(objectClass, "Object class must not be null.");

		LOGGER.debug("Get object with oid {}.", new Object[] { oid });
		OperationResult result = new OperationResult(GET);

		O objectType = null;
		try {
			objectType = getModel().getObject(objectClass, oid, resolve, result);
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (SystemException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object {} from model", ex, oid);
			result.recordFatalError(ex);
			throw new SystemException("Couldn't get object with oid '" + oid + "'.", ex);
		} finally {
			result.computeStatus();
		}

		ControllerUtil.printResults(LOGGER, result, null);

		return objectType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(String oid, PropertyReferenceListType resolve) {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		LOGGER.debug("Get object with oid {}.", new Object[] { oid });
		OperationResult result = new OperationResult(GET);

		T object = null;
		try {
			ObjectType objectType = get(getSupportedObjectClass(), oid, resolve);
			isObjectTypeSupported(objectType);
			object = createObject((C) objectType);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object {} from model", ex, oid);
			result.recordFatalError(ex);
		} finally {
			result.computeStatus();
		}

		ControllerUtil.printResults(LOGGER, result, null);

		return object;
	}

	@Override
	public void delete(String oid) {
		Validate.notEmpty(oid, "Object oid must not be null or empty.");
		LOGGER.debug("Deleting object '" + oid + "'.");

		Task task = taskManager.createTaskInstance(DELETE);
		OperationResult result = task.getResult();
		try {
			getModel().deleteObject(getSupportedObjectClass(), oid, task, result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object {} from model", ex, oid);
			result.recordFatalError(ex);
		} finally {
			result.computeStatus();
		}

		ControllerUtil.printResults(LOGGER, result, null);
	}

	@Override
	public String add(T object) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(object.getXmlObject(), "Xml object type in object must not be null.");
		LOGGER.debug("Adding object '" + object.getName() + "'.");

		OperationResult result = new OperationResult(ADD);
		Task task = taskManager.createTaskInstance();
		// TODO: task initialization
		String oid = null;
		try {
			oid = getModel().addObject(object.getXmlObject(), task, result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to model", ex, object.getName());
			result.recordFatalError(ex);
		} finally {
			result.computeStatus();
		}

		ControllerUtil.printResults(LOGGER, result, null);

		return oid;
	}

	protected <O extends ObjectType> Collection<O> list(PagingType paging, Class<O> type) {
		Validate.notNull(type, "Class object must not be null.");

		Collection<O> collection = new ArrayList<O>();
		OperationResult result = new OperationResult(LIST);
		try {
			List<O> objectList = getModel().listObjects(type, paging, result);
			LOGGER.debug("Found {} objects of type {}.", new Object[] { objectList.size(), type });
			for (O objectType : objectList) {
				collection.add(objectType);
			}
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list {} objects from model", ex, type);
			result.recordFatalError(ex);
		} finally {
			result.computeStatus();
		}

		ControllerUtil.printResults(LOGGER, result, null);
		return collection;
	}

	@SuppressWarnings("unchecked")
	protected Collection<T> list(PagingType paging, ObjectTypes type) {
		Validate.notNull(type, "Object type must not be null.");
		LOGGER.debug("Listing '" + type.getObjectTypeUri() + "' objects.");

		Collection<T> collection = new ArrayList<T>();
		Collection<ObjectType> objects = (Collection<ObjectType>) list(paging, type.getClassDefinition());
		for (ObjectType objectType : objects) {
			isObjectTypeSupported(objectType);

			collection.add(createObject((C) objectType));
		}

		return collection;
	}

	@Override
	public T create() {
		return createObject(null);
	}

//	protected void printResults(Trace logger, OperationResult result) {
//		if (result == null) {
//			return;
//		}
//		if (!result.isSuccess()) {
//			FacesUtils.addMessage(result);
//		}
//
//		if (logger.isDebugEnabled()) {
//			logger.debug(result.dump());
//		}
//	}

	private void isObjectTypeSupported(ObjectType object) {
		Class<? extends ObjectType> type = getSupportedObjectClass();
		Validate.notNull(type, "Supported object class must not be null.");

		if (!type.isAssignableFrom(object.getClass())) {
			throw new IllegalArgumentException("Object type '" + object.getClass()
					+ "' is not supported, supported class is '" + type + "'.");
		}
	}

	protected abstract Class<? extends ObjectType> getSupportedObjectClass();

	protected abstract T createObject(C objectType);
}
