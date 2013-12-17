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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.model.delta.ContainerValuePanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDetailedDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mederly
 */
public class PageWorkItem extends PageAdminWorkItems {

    public static final String PARAM_TASK_ID = "taskId";
    private static final String DOT_CLASS = PageWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEM = DOT_CLASS + "loadWorkItem";
    private static final String OPERATION_LOAD_PROCESS_INSTANCE = DOT_CLASS + "loadProcessInstance";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";

    private static final String ID_ACCORDION = "accordion";
    private static final String ID_ADDITIONAL_INFO = "additionalInfo";
    private static final String ID_DELTA_PANEL = "deltaPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);
    private static final String ID_DELTA_ACCORDION = "deltaAccordion";
    private static final String ID_DELTA_INFO = "deltaInfo";

    private static final String ID_REQUESTER_ACCORDION = "requesterAccordion";
    private static final String ID_REQUESTER_ACCORDION_INFO = "requesterAccordionInfo";
    private static final String ID_REQUESTER_PANEL = "requesterPanel";
    private static final String ID_OBJECT_OLD_ACCORDION = "objectOldAccordion";
    private static final String ID_OBJECT_OLD_ACCORDION_INFO = "objectOldAccordionInfo";
    private static final String ID_OBJECT_OLD_PANEL = "objectOldPanel";
    private static final String ID_OBJECT_NEW_ACCORDION = "objectNewAccordion";
    private static final String ID_OBJECT_NEW_ACCORDION_INFO = "objectNewAccordionInfo";
    private static final String ID_OBJECT_NEW_PANEL = "objectNewPanel";
    private static final String ID_ADDITIONAL_DATA_ACCORDION = "additionalDataAccordion";
    private static final String ID_ADDITIONAL_DATA_ACCORDION_INFO = "additionalDataAccordionInfo";
    private static final String ID_ADDITIONAL_DATA_PANEL = "additionalDataPanel";
    private static final String ID_PROCESS_INSTANCE_ACCORDION = "processInstanceAccordion";
    private static final String ID_PROCESS_INSTANCE_ACCORDION_INFO = "processInstanceAccordionInfo";
    private static final String ID_PROCESS_INSTANCE_PANEL = "processInstancePanel";
    private static final String ID_SHOW_TECHNICAL_INFORMATION = "showTechnicalInformation";

    private PageParameters parameters;

    private IModel<WorkItemDetailedDto> workItemDtoModel;

    private IModel<ObjectWrapper> requesterModel;
    private IModel<ObjectWrapper> objectOldModel;
    private IModel<ObjectWrapper> objectNewModel;
    private IModel<ObjectWrapper> requestSpecificModel;
    private IModel<ObjectWrapper> additionalDataModel;
    private IModel<ObjectWrapper> trackingDataModel;
    private LoadableModel<ProcessInstanceDto> processInstanceDtoModel;
    private IModel<DeltaDto> deltaModel;

    private IModel<Boolean> showTechnicalInformationModel = new Model<Boolean>();
    private Accordion additionalInfoAccordion;
    private AccordionItem additionalInfoAccordionItem;

    public PageWorkItem() {
        this(new PageParameters(), null);
    }

    public PageWorkItem(PageParameters parameters, PageBase previousPage) {
        this(parameters, previousPage, false);
    }

    public PageWorkItem(PageParameters parameters, PageBase previousPage, boolean reinitializePreviousPage) {

        this.parameters = parameters;
        setPreviousPage(previousPage);
        setReinitializePreviousPages(reinitializePreviousPage);

        requesterModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getRequesterWrapper();
            }
        };
        objectOldModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getObjectOldWrapper();
            }
        };
        objectNewModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getObjectNewWrapper();
            }
        };
        requestSpecificModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getRequestSpecificWrapper();
            }
        };
        additionalDataModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getAdditionalDataWrapper();
            }
        };
        trackingDataModel = new LoadableModel<ObjectWrapper>(false) {
            @Override
            protected ObjectWrapper load() {
                loadWorkItemDetailedDtoIfNecessary();
                return getTrackingDataWrapper();
            }
        };
        workItemDtoModel = new LoadableModel<WorkItemDetailedDto>(false) {
            @Override
            protected WorkItemDetailedDto load() {
                return loadWorkItemDetailedDtoIfNecessary();
            }
        };
        processInstanceDtoModel = new LoadableModel<ProcessInstanceDto>(false) {
            @Override
            protected ProcessInstanceDto load() {
                return loadProcessInstanceDto();
            }
        };
        deltaModel = new PropertyModel<DeltaDto>(workItemDtoModel, WorkItemDetailedDto.F_DELTA);

        initLayout();
    }

    private ObjectWrapper getRequesterWrapper() {
        PrismObject<UserType> prism = workItemDtoModel.getObject().getWorkItem().getRequester().asPrismObject();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.requester.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(false);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getObjectOldWrapper() {
        ObjectType objectOld = workItemDtoModel.getObject().getWorkItem().getObjectOld();

        PrismObject<? extends ObjectType> prism;
        if (objectOld != null) {
            prism = objectOld.asPrismObject();
        } else {
            prism = createEmptyUserObject();
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.objectOld.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getObjectNewWrapper() {
        ObjectType objectNew = workItemDtoModel.getObject().getWorkItem().getObjectNew();

        PrismObject<? extends ObjectType> prism;
        if (objectNew != null) {
            prism = objectNew.asPrismObject();
        } else {
            prism = createEmptyUserObject();
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.objectNew.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setShowAssignments(true);
        wrapper.setReadonly(true);

        return wrapper;
    }


    private PrismObject<? extends ObjectType> createEmptyUserObject() {
        PrismObject<? extends ObjectType> p = new PrismObject<UserType>(UserType.COMPLEX_TYPE, UserType.class);
        try {
            getWorkflowManager().getPrismContext().adopt(p);
        } catch (SchemaException e) {   // safe to convert; this should not occur
            throw new SystemException("Got schema exception when creating empty user object.", e);
        }
        return p;
    }

    private ObjectWrapper getRequestSpecificWrapper() {
        PrismObject<?> prism = workItemDtoModel.getObject().getWorkItem().getRequestSpecificData().asPrismObject();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(true);
        wrapper.setMinimalized(false);
        wrapper.setShowInheritedObjectAttributes(false);

        return wrapper;
    }


    private ObjectWrapper getAdditionalDataWrapper() {
        ObjectType relatedObject = workItemDtoModel.getObject().getWorkItem().getRelatedObject();
        PrismObject<? extends ObjectType> prism;
        if (relatedObject != null) {
            prism = relatedObject.asPrismObject();
        } else {
            prism = createEmptyUserObject();            // not quite correct, but ... ok
        }

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(
                createStringResource("pageWorkItem.additionalData.description").getString(),     // name (large font)
                PolyString.getOrig(prism.asObjectable().getName()),                         // description (smaller font)
                prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private ObjectWrapper getTrackingDataWrapper() {
        PrismObject<? extends ObjectType> prism = workItemDtoModel.getObject().getWorkItem().getTrackingData().asPrismObject();

        ContainerStatus status = ContainerStatus.MODIFYING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, prism, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        wrapper.setMinimalized(true);
        wrapper.setReadonly(true);

        return wrapper;
    }

    private WorkItemDetailedDto loadWorkItemDetailedDtoIfNecessary() {

        if (((LoadableModel) workItemDtoModel).isLoaded()) {
            return workItemDtoModel.getObject();
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_WORK_ITEM);
        WorkItemDetailedDto workItemDetailedDto = null;
        WorkItemType workItem = null;
        try {
            WorkflowManager wfm = getWorkflowManager();
            workItem = wfm.getWorkItemDetailsById(parameters.get(PARAM_TASK_ID).toString(), result);
            workItemDetailedDto = new WorkItemDetailedDto(workItem, getPrismContext());
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get work item.", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
            throw getRestartResponseException(PageWorkItems.class);
        }

        return workItemDetailedDto;
    }

    private ProcessInstanceDto loadProcessInstanceDto() {
        OperationResult result = new OperationResult(OPERATION_LOAD_PROCESS_INSTANCE);
        WfProcessInstanceType processInstance;
        try {
            String taskId = parameters.get(PARAM_TASK_ID).toString();
            LOGGER.trace("Loading process instance for task {}", taskId);
            WorkflowManager wfm = getWorkflowManager();
            processInstance = wfm.getProcessInstanceByWorkItemId(taskId, result);
            LOGGER.trace("Found process instance {}", processInstance);
            result.recordSuccess();
            return new ProcessInstanceDto(processInstance);
        } catch (ObjectNotFoundException ex) {
            result.recordWarning("Work item seems to be already closed.");
            LoggingUtils.logException(LOGGER, "Couldn't get process instance for work item; it might be already closed.", ex);
            showResultInSession(result);
            throw getRestartResponseException(PageWorkItems.class);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get process instance for work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't get process instance for work item.", ex);
            showResultInSession(result);
            throw getRestartResponseException(PageWorkItems.class);
        }
    }


    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        Label title = new Label("title", new PropertyModel(workItemDtoModel, "name"));
        mainForm.add(title);

        Label requestedBy = new Label("requestedBy", new PropertyModel(requesterModel, "object.asObjectable.name"));
        mainForm.add(requestedBy);

        Label requestedByFullName = new Label("requestedByFullName", new PropertyModel(requesterModel, "object.asObjectable.fullName"));
        mainForm.add(requestedByFullName);

        Label requestedOn = new Label("requestedOn", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ProcessInstanceDto dto = processInstanceDtoModel.getObject();
                if (dto.getProcessInstance().getStartTimestamp() == null) {
                    return "";
                }
                return WebMiscUtil.formatDate(XmlTypeConverter.toDate(dto.getProcessInstance().getStartTimestamp()));
            }

        });
        mainForm.add(requestedOn);

        Label workItemCreatedOn = new Label("workItemCreatedOn", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                WorkItemDetailedDto dto = workItemDtoModel.getObject();
                if (dto.getWorkItem().getMetadata() == null || dto.getWorkItem().getMetadata().getCreateTimestamp() == null) {
                    return "";
                }
                return WebMiscUtil.formatDate(XmlTypeConverter.toDate(dto.getWorkItem().getMetadata().getCreateTimestamp()));
            }

        });
        mainForm.add(workItemCreatedOn);

        PrismObjectPanel requestSpecificForm = new PrismObjectPanel("requestSpecificForm", requestSpecificModel,
                new PackageResourceReference(ImgResources.class, ImgResources.DECISION_PRISM), mainForm) {

            @Override
            protected IModel<String> createDisplayName(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.requestSpecific.description");
            }

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return new Model<String>("");
            }
        };
        mainForm.add(requestSpecificForm);

        additionalInfoAccordion = new Accordion(ID_ACCORDION);
        additionalInfoAccordion.setOutputMarkupId(true);
        additionalInfoAccordion.setMultipleSelect(true);
        additionalInfoAccordion.setExpanded(true);
        mainForm.add(additionalInfoAccordion);

        additionalInfoAccordionItem = new AccordionItem(ID_ADDITIONAL_INFO, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString("pageWorkItem.additionalInfo");
            }
        });
        additionalInfoAccordionItem.setOutputMarkupId(true);
        additionalInfoAccordion.getBodyContainer().add(additionalInfoAccordionItem);

        PrismObjectPanel requesterForm = new PrismObjectPanel("requesterForm", requesterModel,
                new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        requesterForm.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return requesterModel != null && !requesterModel.getObject().getObject().isEmpty();
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(requesterForm);

        PrismObjectPanel objectOldForm = new PrismObjectPanel("objectOldForm", objectOldModel,
                new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        objectOldForm.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getObjectOld() != null;
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(objectOldForm);

        PrismObjectPanel objectNewForm = new PrismObjectPanel("objectNewForm", objectNewModel,
                new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm);
        objectNewForm.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getObjectNew() != null;
            }
        });
        additionalInfoAccordionItem.getBodyContainer().add(objectNewForm);

        PrismObjectPanel additionalDataForm = new PrismObjectPanel("additionalDataForm", additionalDataModel,
                new PackageResourceReference(ImgResources.class, ImgResources.ROLE_PRISM), mainForm);
        additionalInfoAccordionItem.getBodyContainer().add(additionalDataForm);

        PrismObjectPanel trackingDataForm = new PrismObjectPanel("trackingDataForm", trackingDataModel,
                new PackageResourceReference(ImgResources.class, ImgResources.TRACKING_PRISM), mainForm) {

            @Override
            protected IModel<String> createDisplayName(IModel<ObjectWrapper> model) {
                return createStringResource("pageWorkItem.trackingData.description");
            }

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return new Model("");
            }
        };
        additionalInfoAccordionItem.getBodyContainer().add(trackingDataForm);

        Accordion deltaAccordion = new Accordion(ID_DELTA_ACCORDION);
        deltaAccordion.setOutputMarkupId(true);
        deltaAccordion.setMultipleSelect(true);
        deltaAccordion.setExpanded(false);
        additionalInfoAccordionItem.getBodyContainer().add(deltaAccordion);

        AccordionItem deltaInfo = new AccordionItem(ID_DELTA_INFO, new ResourceModel("pageWorkItem.delta"));
        deltaInfo.setOutputMarkupId(true);
        deltaAccordion.getBodyContainer().add(deltaInfo);

        DeltaPanel deltaPanel = new DeltaPanel(ID_DELTA_PANEL, deltaModel);
        deltaInfo.getBodyContainer().add(deltaPanel);

        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_REQUESTER_ACCORDION, ID_REQUESTER_ACCORDION_INFO, ID_REQUESTER_PANEL, "pageWorkItem.accordionLabel.requester", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_REQUESTER), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_OBJECT_OLD_ACCORDION, ID_OBJECT_OLD_ACCORDION_INFO, ID_OBJECT_OLD_PANEL, "pageWorkItem.accordionLabel.objectOld", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_OBJECT_OLD), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_OBJECT_NEW_ACCORDION, ID_OBJECT_NEW_ACCORDION_INFO, ID_OBJECT_NEW_PANEL, "pageWorkItem.accordionLabel.objectNew", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_OBJECT_NEW), true));
        additionalInfoAccordionItem.getBodyContainer().add(createObjectAccordion(ID_ADDITIONAL_DATA_ACCORDION, ID_ADDITIONAL_DATA_ACCORDION_INFO, ID_ADDITIONAL_DATA_PANEL, "pageWorkItem.accordionLabel.additionalData", new PropertyModel(workItemDtoModel, WorkItemDetailedDto.F_RELATED_OBJECT), true));

        LOGGER.trace("processInstanceDtoModel = {}, loaded = {}", processInstanceDtoModel, processInstanceDtoModel.isLoaded());
        ProcessInstanceDto processInstanceDto = processInstanceDtoModel.getObject();
        WfProcessInstanceType processInstance = processInstanceDto.getProcessInstance();
        String detailsPageClassName = getWorkflowManager().getProcessInstanceDetailsPanelName(processInstance);

        additionalInfoAccordionItem.getBodyContainer().add(createAccordion(ID_PROCESS_INSTANCE_ACCORDION,
                ID_PROCESS_INSTANCE_ACCORDION_INFO,
                "pageWorkItem.accordionLabel.processInstance",
                new ProcessInstancePanel(ID_PROCESS_INSTANCE_PANEL, processInstanceDtoModel, detailsPageClassName), true));
        initButtons(mainForm);
    }

    private Component createAccordion(String idAccordion, String idAccordionInfo, String key, Panel panel, boolean isTechnical) {

        Accordion accordion = new Accordion(idAccordion);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(false);

        AccordionItem info = new AccordionItem(idAccordionInfo, new ResourceModel(key));
        info.setOutputMarkupId(true);
        accordion.getBodyContainer().add(info);

        info.getBodyContainer().add(panel);

        if (isTechnical) {
            accordion.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return Boolean.TRUE.equals(showTechnicalInformationModel.getObject());
                }
            });
        }

        return accordion;
    }

    private Component createObjectAccordion(String idAccordion, String idAccordionInfo, String idPanel, String key, IModel model, boolean isTechnical) {
        Accordion accordion = new Accordion(idAccordion);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(false);

        AccordionItem info = new AccordionItem(idAccordionInfo, new ResourceModel(key));
        info.setOutputMarkupId(true);
        accordion.getBodyContainer().add(info);

        ContainerValuePanel panel = new ContainerValuePanel(idPanel, model);
        info.getBodyContainer().add(panel);

        if (isTechnical) {
            accordion.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return Boolean.TRUE.equals(showTechnicalInformationModel.getObject());
                }
            });
        }

        return accordion;
    }


    private void initButtons(Form mainForm) {

        VisibleEnableBehaviour isAuthorizedToSubmit = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getWorkflowManager().isCurrentUserAuthorizedToSubmit(workItemDtoModel.getObject().getWorkItem());
            }
        };

        AjaxSubmitButton approve = new AjaxSubmitButton("approve", createStringResource("pageWorkItem.button.approve")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, true);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        approve.add(isAuthorizedToSubmit);
        mainForm.add(approve);

        AjaxSubmitButton reject = new AjaxSubmitButton("reject", createStringResource("pageWorkItem.button.reject")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, false);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        reject.add(isAuthorizedToSubmit);
        mainForm.add(reject);

//        AjaxSubmitLinkButton done = new AjaxSubmitLinkButton("done",
//                createStringResource("pageWorkItem.button.done")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                try {
//                    savePerformed(target);
//                } catch(RuntimeException e) {
//                    LoggingUtils.logException(LOGGER, "Exception in savePerformed", e);
//                    throw e;
//                }
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target, Form<?> form) {
//                target.add(getFeedbackPanel());
//            }
//        };
//        mainForm.add(done);

        AjaxButton cancel = new AjaxButton("cancel", createStringResource("pageWorkItem.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);

        CheckBox showTechnicalInformationBox = new CheckBox(ID_SHOW_TECHNICAL_INFORMATION, showTechnicalInformationModel);
        showTechnicalInformationBox.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(additionalInfoAccordion);
            }
        });
        mainForm.add(showTechnicalInformationBox);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        goBack(PageWorkItems.class);
    }

    private void savePerformed(AjaxRequestTarget target, boolean decision) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        ObjectWrapper rsWrapper = requestSpecificModel.getObject();
        try {
            PrismObject object = rsWrapper.getObject();
            ObjectDelta delta = rsWrapper.getObjectDelta();
            delta.applyTo(object);

            getWorkflowManager().approveOrRejectWorkItemWithDetails(workItemDtoModel.getObject().getWorkItem().getWorkItemId(), object, decision, result);
            setReinitializePreviousPages(true);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't save work item", ex);
        }

        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);
            goBack(PageWorkItems.class);
        }
    }

    @Override
    public PageBase reinitialize() {
        return new PageWorkItem(parameters, getPreviousPage(), true);
    }
}
