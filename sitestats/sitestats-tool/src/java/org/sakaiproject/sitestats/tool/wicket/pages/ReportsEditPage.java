/**
 * $URL$
 * $Id$
 *
 * Copyright (c) 2006-2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.sitestats.tool.wicket.pages;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.IntegerConverter;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.sitestats.api.PrefsData;
import org.sakaiproject.sitestats.api.StatsManager;
import org.sakaiproject.sitestats.api.event.EventInfo;
import org.sakaiproject.sitestats.api.event.ToolInfo;
import org.sakaiproject.sitestats.api.report.ReportDef;
import org.sakaiproject.sitestats.api.report.ReportManager;
import org.sakaiproject.sitestats.api.report.ReportParams;
import org.sakaiproject.sitestats.tool.facade.Locator;
import org.sakaiproject.sitestats.tool.wicket.components.CSSFeedbackPanel;
import org.sakaiproject.sitestats.tool.wicket.components.FileSelectorPanel;
import org.sakaiproject.sitestats.tool.wicket.components.IStylableOptionRenderer;
import org.sakaiproject.sitestats.tool.wicket.components.LastJobRun;
import org.sakaiproject.sitestats.tool.wicket.components.Menus;
import org.sakaiproject.sitestats.tool.wicket.components.StylableSelectOptions;
import org.sakaiproject.sitestats.tool.wicket.components.StylableSelectOptionsGroup;
import org.sakaiproject.sitestats.tool.wicket.models.EventModel;
import org.sakaiproject.sitestats.tool.wicket.models.ReportDefModel;
import org.sakaiproject.sitestats.tool.wicket.models.ToolModel;
import org.sakaiproject.sitestats.tool.wicket.util.Comparators;
import org.sakaiproject.wicket.component.SakaiDateTimeField;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Nuno Fernandes
 */
@Slf4j
public class ReportsEditPage extends BasePage {
	private static final long		serialVersionUID	= 1L;
	private static final String		REPORT_THISSITE		= "this";
	private static final String		REPORT_ALLSITES		= "all";

	private String					realSiteId;
	private String					siteId;
	private boolean					predefined		= false;
	private String					reportSiteOpt	= REPORT_THISSITE;
	private boolean					visitsEnabled	= true;
	private FeedbackPanel			feedback		= null;
	
	/** Options visiblity */
	private boolean 				visitsVisible	= true;
	private boolean 				activityVisible	= true;
	private boolean 				resourcesVisible = true;
	private boolean 				presencesVisible = true;
	
	/** Report related */
	private ReportDefModel			reportDefModel;
	private PrefsData				prefsdata		= null;
	private WebPage					returnPage;

	/** Ajax update lock */
	private final ReentrantLock		ajaxUpdateLock	= new ReentrantLock();
	private boolean					usersLoaded		= false;

	private ZonedDateTime startDate, endDate;

	// namespace for sakai icons see _icons.scss
	public static final String ICON_SAKAI = "si si-";

	public ReportsEditPage(ReportDefModel reportDef, PageParameters pageParameters, final WebPage returnPage) {
		realSiteId = Locator.getFacade().getToolManager().getCurrentPlacement().getContext();
		if(pageParameters != null) {
			siteId = pageParameters.get("siteId").toString();
			predefined = pageParameters.get("predefined").toBoolean(false);
		}
		if(siteId == null) {
			siteId = realSiteId;
		}
		if(reportDef != null) {
			this.reportDefModel = reportDef;
		}else{
			if(predefined) {
				this.reportDefModel = new ReportDefModel(null, null);
			}else{
				this.reportDefModel = new ReportDefModel(siteId, siteId);
			}
		}
		if(returnPage == null) {
			this.returnPage = new ReportsPage(pageParameters);			
		}else{
			this.returnPage = returnPage;
		}
		boolean allowed = Locator.getFacade().getStatsAuthz().isUserAbleToViewSiteStats(siteId);
		if(allowed) {
			// options visibility
			visitsVisible = Locator.getFacade().getStatsManager().getEnableSiteVisits() && Locator.getFacade().getStatsManager().getVisitsInfoAvailable();
			activityVisible = Locator.getFacade().getStatsManager().isEnableSiteActivity();
			resourcesVisible = false;
			try{
				resourcesVisible = Locator.getFacade().getStatsManager().isEnableResourceStats() &&
									(Locator.getFacade().getSiteService().getSite(siteId).getToolForCommonId(StatsManager.RESOURCES_TOOLID) != null);
			}catch(Exception e) {
				resourcesVisible = false;
			}
			presencesVisible = Locator.getFacade().getStatsManager().getEnableSitePresences();
			// render body
			renderBody();
		}else{
			setResponsePage(NotAuthorizedPage.class);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forUrl(JQUERYSCRIPT));
		response.render(JavaScriptHeaderItem.forUrl(StatsManager.SITESTATS_WEBAPP + "/script/reports.js"));
		StringBuilder onDomReady = new StringBuilder();
		onDomReady.append("checkWhatSelection();");
		onDomReady.append("checkWhenSelection();");
        onDomReady.append("checkWhoSelection();");
        onDomReady.append("checkHowSelection();");
        onDomReady.append("checkReportDetails();");
        onDomReady.append("checkHowChartSelection();");
		response.render(OnDomReadyHeaderItem.forScript(onDomReady.toString()));
	}
	
	private void renderBody() {
		StatsManager statsManager = Locator.getFacade().getStatsManager();
		
		// menu
		add(new Menus("menu", siteId));
		
		// reportAction
		String action = null;
		if(getReportDef().isTitleLocalized()) {
			if(reportDefModel.isNew()) {
				action = (String) new ResourceModel("report_adding").getObject();
			}else{
				action = (String) new ResourceModel("report_editing").getObject();
			}
			action = action.replaceAll("\\$\\{title\\}", (String) new ResourceModel(getReportDef().getTitleBundleKey()).getObject());
		}else{
			if(reportDefModel.isNew()) {
				action = new StringResourceModel("report_adding", this, reportDefModel).getString();
			}else{
				action = new StringResourceModel("report_editing", this, reportDefModel).getString();
			}
		}
		add(new Label("reportAction", action));
		
		// model
		visitsEnabled = statsManager.getEnableSiteVisits();
		if(!visitsEnabled) {
			getReportParams().setWhat(ReportManager.WHAT_EVENTS_BYTOOL);
		}
		setDefaultModel(new CompoundPropertyModel(this));
		
		// last job run
		add(new LastJobRun("lastJobRun", siteId));
		
		// form
		Form form = new Form("reportsForm");
		form.setOutputMarkupId(true);
		add(form);

		// feedback panel (messages)
		feedback = new CSSFeedbackPanel("messages");
		feedback.setOutputMarkupId(true);
		form.add(feedback);
		
		// report details, what, when & who
		renderReportDetailsUI(form);
		renderWhatUI(form);
		renderWhenUI(form);
		renderWhoUI(form);
		renderHowUI(form);
		
		// buttons
		final Button generateReport = new Button("generateReport") {
			@Override
			public void onSubmit() {
				setISODates();
				if(validReportParameters()) {
					if(predefined) {
						getReportParams().setSiteId(siteId);
					}
					setResponsePage(new ReportDataPage(reportDefModel, new PageParameters().set("siteId", siteId), ReportsEditPage.this));
				}
				super.onSubmit();
			}
		};
		form.add(generateReport);
		final Button saveReport = new Button("saveReport") {
			@Override
			public void onSubmit() {
				setISODates();
				if(validReportParameters()) {
					if(getReportDef().getTitle() == null || getReportDef().getTitle().trim().isEmpty()) {
						error((String) new ResourceModel("report_reporttitle_req").getObject());
					}else{
						if(predefined) {
							getReportParams().setSiteId(null);
						}
						boolean saved = Locator.getFacade().getReportManager().saveReportDefinition(getReportDef());
						String titleStr = null;
						if(saved) {
							if(getReportDef().isTitleLocalized()) {
								titleStr = (String) new ResourceModel("report_save_success").getObject();
								titleStr = titleStr.replaceAll("\\$\\{title\\}", (String) new ResourceModel(getReportDef().getTitleBundleKey()).getObject());
							}else{
								titleStr = new StringResourceModel("report_save_success", getPage(), reportDefModel).getString();
							}							
							returnPage.info(titleStr);
							setResponsePage(returnPage);
						}else{
							if(getReportDef().isTitleLocalized()) {
								titleStr = (String) new ResourceModel("report_save_error").getObject();
								titleStr = titleStr.replaceAll("\\$\\{title\\}", (String) new ResourceModel(getReportDef().getTitleBundleKey()).getObject());
							}else{
								titleStr = new StringResourceModel("report_save_error", getPage(), reportDefModel).getString();
							}		
							error(titleStr);							
						}						
					}
				}
				super.onSubmit();
			}
		};
		saveReport.setVisible(!predefined || Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage() && realSiteId.equals(siteId));
		form.add(saveReport);
		final Button back = new Button("back") {
			@Override
			public void onSubmit() {
				reportDefModel.detach();
				setResponsePage(returnPage);
				super.onSubmit();
			}
		};
		back.setDefaultFormProcessing(false);
		form.add(back);
	}

	@SuppressWarnings("serial")
	private void renderReportDetailsUI(Form form) {
		// top
		WebMarkupContainer reportDetailsTop = new WebMarkupContainer("reportDetailsTop");
		WebMarkupContainer reportDetailsShow = new WebMarkupContainer("reportDetailsShow");
		reportDetailsTop.add(reportDetailsShow);
		form.add(reportDetailsTop);
		WebMarkupContainer fakeReportDetails = new WebMarkupContainer("fakeReportDetails");
		reportDetailsTop.add(fakeReportDetails);
		
		// details
		WebMarkupContainer reportDetails = new WebMarkupContainer("reportDetails");
		form.add(reportDetails);
		
		// details: title
		TextField title = new TextField("reportDef.title");
		reportDetails.add(title);
		final WebMarkupContainer titleLocalizedContainer = new WebMarkupContainer("titleLocalizedContainer");
		titleLocalizedContainer.setOutputMarkupId(true);
		titleLocalizedContainer.setOutputMarkupPlaceholderTag(true);
		titleLocalizedContainer.setVisible(getReportDef().isTitleLocalized());
		titleLocalizedContainer.add(new Label("titleLocalized"));
		reportDetails.add(titleLocalizedContainer);
		title.add(new AjaxFormComponentUpdatingBehavior("change") {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				titleLocalizedContainer.setVisible(getReportDef().isTitleLocalized());
				target.add(titleLocalizedContainer);
				target.appendJavaScript("setMainFrameHeightNoScroll(window.name);");
			}		
		});
		
		// details: description
		TextArea description = new TextArea("reportDef.description");
		reportDetails.add(description);
		final WebMarkupContainer descriptionLocalizedContainer = new WebMarkupContainer("descriptionLocalizedContainer");
		descriptionLocalizedContainer.setOutputMarkupId(true);
		descriptionLocalizedContainer.setOutputMarkupPlaceholderTag(true);
		descriptionLocalizedContainer.setVisible(getReportDef().isDescriptionLocalized());
		descriptionLocalizedContainer.add(new Label("descriptionLocalized"));
		reportDetails.add(descriptionLocalizedContainer);
		description.add(new AjaxFormComponentUpdatingBehavior("change") {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				descriptionLocalizedContainer.setVisible(getReportDef().isDescriptionLocalized());
				target.add(descriptionLocalizedContainer);
				target.appendJavaScript("setMainFrameHeightNoScroll(window.name);");
			}			
		});

		// set visibility
		if(predefined) {
			if(Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage() && realSiteId.equals(siteId)) {
				reportDetailsTop.setVisible(true);
				reportDetailsShow.setVisible(false);
				reportDetails.setVisible(true);
				fakeReportDetails.setVisible(false);
			}else{
				reportDetailsTop.setVisible(false);
				reportDetailsShow.setVisible(false);
				reportDetails.setVisible(false);
			}
		}else{
			reportDetailsTop.setVisible(true);
			reportDetailsShow.setVisible(false);
		}
	}

	@SuppressWarnings("serial")
	private void renderWhatUI(Form form) {
		// -------------------------------------------------------
		// left panel
		// -------------------------------------------------------
		// activity
		List<String> whatOptions = new ArrayList<String>();
		if(visitsVisible) { 	whatOptions.add(ReportManager.WHAT_VISITS); 	}
		if(activityVisible) { 	whatOptions.add(ReportManager.WHAT_EVENTS); 	}
		if(resourcesVisible) { 	whatOptions.add(ReportManager.WHAT_RESOURCES); 	}
		if(presencesVisible) { 	whatOptions.add(ReportManager.WHAT_PRESENCES); 	}
		IChoiceRenderer whatChoiceRenderer = new IChoiceRenderer() {
			@Override
			public Object getDisplayValue(Object object) {
				if(ReportManager.WHAT_VISITS.equals(object)) {
					return new ResourceModel("report_what_visits").getObject();
				}
				if(ReportManager.WHAT_EVENTS.equals(object)) {
					return new ResourceModel("report_what_events").getObject();
				}
				if(ReportManager.WHAT_RESOURCES.equals(object)) {
					return new ResourceModel("report_what_resources").getObject();
				}
				if(ReportManager.WHAT_PRESENCES.equals(object)) {
					return new ResourceModel("report_what_presences").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice what = new DropDownChoice("reportParams.what", whatOptions, whatChoiceRenderer);
		what.setMarkupId("what");
		what.setOutputMarkupId(true);
		form.add(what);
		
		// event selection type
		List<String> whatEventSelTypeOptions = Arrays.asList(ReportManager.WHAT_EVENTS_BYTOOL, ReportManager.WHAT_EVENTS_BYEVENTS);
		IChoiceRenderer whatEventSelTypeChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(ReportManager.WHAT_EVENTS_BYTOOL.equals(object)) {
					return new ResourceModel("report_what_events_bytool").getObject();
				}
				if(ReportManager.WHAT_EVENTS_BYEVENTS.equals(object)) {
					return new ResourceModel("report_what_events_byevent").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice whatEventSelType = new DropDownChoice("reportParams.whatEventSelType", whatEventSelTypeOptions, whatEventSelTypeChoiceRenderer);
		whatEventSelType.setEscapeModelStrings(false);
		whatEventSelType.setMarkupId("whatEventSelType");
		whatEventSelType.setOutputMarkupId(true);
		form.add(whatEventSelType);
		
		// tool selection
		Select whatToolIds = new Select("reportParams.whatToolIds");
		RepeatingView selectOptionsRV1 = new RepeatingView("selectOptionsRV1");
		whatToolIds.add(selectOptionsRV1);
		whatToolIds.add(new AttributeModifier("title", new ResourceModel("report_multiple_sel_instruction")));
		addTools(selectOptionsRV1);
		form.add(whatToolIds);
		
		// event selection
		Select whatEventIds = new Select("reportParams.whatEventIds");
		RepeatingView selectOptionsRV2 = new RepeatingView("selectOptionsRV2");
		whatEventIds.add(selectOptionsRV2);
		whatEventIds.add(new AttributeModifier("title", new ResourceModel("report_multiple_sel_instruction")));
		addEvents(selectOptionsRV2);
		form.add(whatEventIds);
		
		// resources selection
		boolean isSiteStatsAdminTool = Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage();
		boolean showDefaultBaseFoldersOnly = isSiteStatsAdminTool && predefined && realSiteId.equals(siteId);
		CheckBox whatLimitedAction = new CheckBox("reportParams.whatLimitedAction");
		whatLimitedAction.setMarkupId("whatLimitedAction");
		whatLimitedAction.setOutputMarkupId(true);
		form.add(whatLimitedAction);
		CheckBox whatLimitedResourceIds = new CheckBox("reportParams.whatLimitedResourceIds");
		whatLimitedResourceIds.setMarkupId("whatLimitedResourceIds");
		whatLimitedResourceIds.setOutputMarkupId(true);
		form.add(whatLimitedResourceIds);
		final FileSelectorPanel whatResourceIds = new FileSelectorPanel("reportParams.whatResourceIds", siteId, showDefaultBaseFoldersOnly);
		whatResourceIds.setMarkupId("whatResourceIds");
		whatResourceIds.setOutputMarkupId(true);
		form.add(whatResourceIds);
		whatResourceIds.setEnabled(true);
		
		// resource actions
		List<String> resourceActions = new ArrayList<String>();
		resourceActions.add(ReportManager.WHAT_RESOURCES_ACTION_NEW);
		resourceActions.add(ReportManager.WHAT_RESOURCES_ACTION_READ);
		resourceActions.add(ReportManager.WHAT_RESOURCES_ACTION_REVS);
		resourceActions.add(ReportManager.WHAT_RESOURCES_ACTION_DEL);
		resourceActions.add(ReportManager.WHAT_RESOURCES_ACTION_DOW);
		DropDownChoice whatResourceAction = new DropDownChoice("reportParams.whatResourceAction", resourceActions, new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(object == null){
					return "";
				}else{
					return (String) new ResourceModel("action_" + ((String) object)).getObject();
				}
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		}) {
			@Override
			protected CharSequence getDefaultChoice(String selected) {
				return "";
			}
			
		};
		whatResourceAction.setMarkupId("whatResourceAction");
		whatResourceAction.setOutputMarkupId(true);
		form.add(whatResourceAction);
	}

	@SuppressWarnings("serial")
	private void renderWhenUI(Form form) {
		List<String> whenOptions = Arrays.asList(
				ReportManager.WHEN_ALL, ReportManager.WHEN_LAST7DAYS,
				ReportManager.WHEN_LAST30DAYS, ReportManager.WHEN_LAST365DAYS,
				ReportManager.WHEN_CUSTOM
				);
		IChoiceRenderer whenChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(ReportManager.WHEN_ALL.equals(object)) {
					return new ResourceModel("report_when_all").getObject();
				}
				if(ReportManager.WHEN_LAST7DAYS.equals(object)) {
					return new ResourceModel("report_when_last7days").getObject();
				}
				if(ReportManager.WHEN_LAST30DAYS.equals(object)) {
					return new ResourceModel("report_when_last30days").getObject();
				}
				if(ReportManager.WHEN_LAST365DAYS.equals(object)) {
					return new ResourceModel("report_when_last365days").getObject();
				}
				if(ReportManager.WHEN_CUSTOM.equals(object)) {
					return new ResourceModel("report_when_custom").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}
		};
		DropDownChoice when = new DropDownChoice("reportParams.when", whenOptions, whenChoiceRenderer);
		when.setMarkupId("when");
		when.setOutputMarkupId(true);
		form.add(when);

		String localSakaiName = Locator.getFacade().getStatsManager().getLocalSakaiName();
		StringResourceModel model = new StringResourceModel("report_server_time_zone").setParameters(localSakaiName);
		form.add(new Label("reportParams.when.serverTimeZone", model));
		
		// custom dates
		// date range for reports uses the server time zone to match how the events are counted
		ZoneId sys = ZoneId.systemDefault();
		startDate = ZonedDateTime.ofInstant(getReportParams().getWhenFrom().toInstant(), sys);
		endDate = ZonedDateTime.ofInstant(getReportParams().getWhenTo().toInstant(), sys);
		SakaiDateTimeField startDateField = new SakaiDateTimeField("whenFrom", new PropertyModel<>(this, "startDate"), sys);
		startDateField.setUseTime(false).setAllowEmptyDate(false);
		form.add(startDateField);
		SakaiDateTimeField endDateField = new SakaiDateTimeField("whenTo", new PropertyModel<>(this, "endDate"), sys);
		endDateField.setUseTime(false).setAllowEmptyDate(false);
		form.add(endDateField);
	}
	
	
	@SuppressWarnings("serial")
	private void renderWhoUI(Form form) {		
		List<String> groups = getGroups();
		RepeatingView selectOptionsRV = new RepeatingView("selectOptionsRV");
		Select whoUserIds = new Select("reportParams.whoUserIds");
		
		// who
		List<String> whoOptions = new ArrayList<>(Arrays.asList(ReportManager.WHO_ALL, ReportManager.WHO_ROLE, ReportManager.WHO_CUSTOM, ReportManager.WHO_NONE));
		if(!groups.isEmpty()) {
			whoOptions.add(2, ReportManager.WHO_GROUPS);
		}
		IChoiceRenderer whoChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(ReportManager.WHO_ALL.equals(object)) {
					return new ResourceModel("report_who_all").getObject();
				}
				if(ReportManager.WHO_ROLE.equals(object)) {
					return new ResourceModel("report_who_role").getObject();
				}
				if(ReportManager.WHO_GROUPS.equals(object)) {
					return new ResourceModel("report_who_group").getObject();
				}
				if(ReportManager.WHO_CUSTOM.equals(object)) {
					return new ResourceModel("report_who_custom").getObject();
				}
				if(ReportManager.WHO_NONE.equals(object)) {
					return new ResourceModel("report_who_none").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice who = new DropDownChoice("reportParams.who", whoOptions, whoChoiceRenderer);
		who.setMarkupId("who");
		who.setOutputMarkupId(true);
		form.add(who);
		
		// users
		whoUserIds.add(selectOptionsRV);
		whoUserIds.add(new AttributeModifier("title", new ResourceModel("report_multiple_sel_instruction")));
		addUsers(selectOptionsRV);
		form.add(whoUserIds);
		
		// roles
		List<String> roles = getRoles();
		IChoiceRenderer rolesRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				return (String) object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}			
		};
		roles.sort(Comparators.getChoiceRendererComparator(rolesRenderer));
		DropDownChoice whoRoleId = new DropDownChoice("reportParams.whoRoleId", roles, rolesRenderer);
		whoRoleId.setEnabled(!roles.isEmpty());
		if(getReportParams().getWhoRoleId() == null) {
			if(!roles.isEmpty()) {
				getReportParams().setWhoRoleId(roles.get(0));
			}else{
				getReportParams().setWhoRoleId("");
			}
		}
		form.add(whoRoleId);
		
		// groups
		WebMarkupContainer whoGroupTr = new WebMarkupContainer("who-groups-tr");
		form.add(whoGroupTr);
		IChoiceRenderer groupsRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				try{
					return Locator.getFacade().getSiteService().getSite(siteId).getGroup((String) object).getTitle();
				}catch(IdUnusedException e){
					return (String) object;
				}
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		groups.sort(Comparators.getChoiceRendererComparator(groupsRenderer));
		DropDownChoice whoGroupId = new DropDownChoice("reportParams.whoGroupId", groups, groupsRenderer);
		if(groups.isEmpty()) {
			whoGroupTr.setVisible(false);
		}else{
			if(getReportParams().getWhoGroupId() == null) {
				if(!groups.isEmpty()) {
					getReportParams().setWhoGroupId(groups.get(0));
				}else{
					getReportParams().setWhoGroupId("");
				}
			}
		}
		whoGroupTr.add(whoGroupId);
	}

	@SuppressWarnings("serial")
	private void renderHowUI(Form form) {		
		boolean isSiteStatsAdminTool = Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage();
		boolean renderSiteSelectOption = Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage() && !predefined && realSiteId.equals(siteId);
		boolean renderSiteSortOption = isSiteStatsAdminTool && !predefined && realSiteId.equals(siteId);
		boolean renderSortAscendingOption = isSiteStatsAdminTool && predefined && realSiteId.equals(siteId);

		// common
		IChoiceRenderer allColumnsChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(object != null) {
					String id = (String) object;
					if(ReportManager.HOW_SORT_DEFAULT.equals(id)) {
						return (String) new ResourceModel("default").getObject();
					}
					if(StatsManager.T_NONE.equals(id)) {
						return (String) new ResourceModel("none").getObject();
					}
					if(StatsManager.T_SITE.equals(id)) {
						return (String) new ResourceModel("report_option_site").getObject();
					}
					if(StatsManager.T_USER.equals(id)) {
						return (String) new ResourceModel("report_option_user").getObject();
					}
					if(StatsManager.T_TOOL.equals(id)) {
						return (String) new ResourceModel("report_option_tool").getObject();
					}
					if(StatsManager.T_EVENT.equals(id)) {
						return (String) new ResourceModel("report_option_event").getObject();
					}
					if(StatsManager.T_RESOURCE.equals(id)) {
						return (String) new ResourceModel("report_option_resource").getObject();
					}
					if(StatsManager.T_RESOURCE_ACTION.equals(id)) {
						return (String) new ResourceModel("report_option_resourceaction").getObject();
					}
					if(StatsManager.T_DATE.equals(id)) {
						return (String) new ResourceModel("report_option_date").getObject();
					}
					if(StatsManager.T_TOTAL.equals(id)) {
						return (String) new ResourceModel("report_option_total").getObject();
					}
				}
				return (String) new ResourceModel("default").getObject();
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};

		// site to report
		WebMarkupContainer siteContainer = new WebMarkupContainer("siteContainer");		
		siteContainer.setVisible(renderSiteSelectOption);
		form.add(siteContainer);
		List<String> reportSiteOptions = Arrays.asList(REPORT_THISSITE, REPORT_ALLSITES);
		IChoiceRenderer reportSiteRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(REPORT_THISSITE.equals(object)) {
					return (String) new ResourceModel("report_reportsite_this").getObject();
				}
				if(REPORT_ALLSITES.equals(object)) {
					return (String) new ResourceModel("report_reportsite_all").getObject();
				}
				return (String) new ResourceModel("report_reportsite_this").getObject();
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice reportSite = new DropDownChoice("reportSite",new PropertyModel(this, "reportSite") , reportSiteOptions, reportSiteRenderer);
		reportSite.setMarkupId("reportSite");
		reportSite.setOutputMarkupId(true);
		siteContainer.add(reportSite);
		if(getReportParams().getSiteId() == null) {
			this.reportSiteOpt = REPORT_ALLSITES;
		}else {
			this.reportSiteOpt = REPORT_THISSITE;
		}
		
		// totals by
		Select howTotalsBy = new Select("reportParams.howTotalsBy");
		howTotalsBy.setRequired(true);
		howTotalsBy.setMarkupId("howTotalsBy");
		howTotalsBy.setOutputMarkupId(true);
		form.add(howTotalsBy);
		RepeatingView howTotalsByOptions = new RepeatingView("howTotalsByOptions");
		howTotalsBy.add(howTotalsByOptions);
		addGroupOptions(howTotalsByOptions);
		
		// sorting
		WebMarkupContainer trSortBy = new WebMarkupContainer("trSortBy");
		trSortBy.setVisible(renderSortAscendingOption);
		form.add(trSortBy);
		CheckBox howSortCheck = new CheckBox("reportParams.howSort");
		howSortCheck.setMarkupId("howSortCheck");
		howSortCheck.setOutputMarkupId(true);
		trSortBy.add(howSortCheck);
		// sort options
		List<String> sortOptions = null;
		if(renderSiteSortOption) {
			sortOptions = Arrays.asList(/*StatsManager.T_USER,*/ StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE, StatsManager.T_TOTAL, StatsManager.T_SITE);
		}else{
			sortOptions = Arrays.asList(/*StatsManager.T_USER,*/ StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE, StatsManager.T_TOTAL);
		}
		DropDownChoice howSortBy = new DropDownChoice("reportParams.howSortBy", sortOptions, allColumnsChoiceRenderer); 
		howSortBy.setMarkupId("howSortBy");
		howSortBy.setOutputMarkupId(true);
		trSortBy.add(howSortBy);
		CheckBox howSortAscending = new CheckBox("reportParams.howSortAscending");
		howSortAscending.setMarkupId("howSortAscending");
		howSortAscending.setOutputMarkupId(true);
		trSortBy.add(howSortAscending);
		
		// max results
		CheckBox howMaxResultsCheck = new CheckBox("reportParams.howLimitedMaxResults");
		howMaxResultsCheck.setMarkupId("howMaxResultsCheck");
		howMaxResultsCheck.setOutputMarkupId(true);
		form.add(howMaxResultsCheck);
		TextField howMaxResults = new TextField("reportParams.howMaxResults", Integer.class) {
			@Override
			public String getInput() {
				String[] input = getInputAsArray();
				if(input == null || input.length == 0){
					return "0";
				}else{
					return trim(input[0]);
				}
			}
			@Override
			public IConverter<Integer> getConverter(Class type) {
				return new IntegerConverter() {
					@Override
					public Integer convertToObject(String value, Locale locale) {
						if (value != null) {
							return super.convertToObject(value, locale);
						}

						return 0;
					}
				};
			}
		};
		howMaxResults.setMarkupId("howMaxResults");
		howMaxResults.setOutputMarkupId(true);
		form.add(howMaxResults);
		
		// presentation
		List<String> howPresentationOptions = Arrays.asList(ReportManager.HOW_PRESENTATION_TABLE, ReportManager.HOW_PRESENTATION_CHART, ReportManager.HOW_PRESENTATION_BOTH);
		IChoiceRenderer howPresentationChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(ReportManager.HOW_PRESENTATION_TABLE.equals(object)) {
					return new ResourceModel("report_howpresentation_table").getObject();
				}
				if(ReportManager.HOW_PRESENTATION_CHART.equals(object)) {
					return new ResourceModel("report_howpresentation_chart").getObject();
				}
				if(ReportManager.HOW_PRESENTATION_BOTH.equals(object)) {
					return new ResourceModel("report_howpresentation_both").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice howPresentation = new DropDownChoice("reportParams.howPresentationMode", howPresentationOptions, howPresentationChoiceRenderer);
		howPresentation.setMarkupId("howPresentation");
		howPresentation.setOutputMarkupId(true);
		form.add(howPresentation);

		// chart type
		List<String> howChartTypeOptions = Arrays.asList(
				StatsManager.CHARTTYPE_BAR, /*StatsManager.CHARTTYPE_LINE,*/ StatsManager.CHARTTYPE_PIE,
				StatsManager.CHARTTYPE_TIMESERIES, StatsManager.CHARTTYPE_TIMESERIESBAR);
		IChoiceRenderer howChartTypeChoiceRenderer = new IChoiceRenderer() {

			@Override
			public Object getDisplayValue(Object object) {
				if(StatsManager.CHARTTYPE_BAR.equals(object)) {
					return new ResourceModel("report_howchart_bar").getObject();
				}
				if(StatsManager.CHARTTYPE_LINE.equals(object)) {
					return new ResourceModel("report_howchart_line").getObject();
				}
				if(StatsManager.CHARTTYPE_PIE.equals(object)) {
					return new ResourceModel("report_howchart_pie").getObject();
				}
				if(StatsManager.CHARTTYPE_TIMESERIES.equals(object)) {
					return new ResourceModel("report_howchart_timeseries").getObject();
				}
				if(StatsManager.CHARTTYPE_TIMESERIESBAR.equals(object)) {
					return new ResourceModel("report_howchart_timeseries_bar").getObject();
				}
				return object;
			}

			@Override
			public String getIdValue(Object object, int index) {
				return (String) object;
			}		
		};
		DropDownChoice howChartType = new DropDownChoice("reportParams.howChartType", howChartTypeOptions, howChartTypeChoiceRenderer);
		howChartType.setMarkupId("howChartType");
		howChartType.setOutputMarkupId(true);
		form.add(howChartType);
		
		// chart source, chart series
		List<String> howChartSourceOptions = null;
		List<String> howChartCategorySourceOptions = null;
		List<String> howChartSeriesSourceOptions = null;
		if(renderSiteSortOption) {
			howChartSourceOptions = Arrays.asList(StatsManager.T_SITE, StatsManager.T_USER, StatsManager.T_TOOL, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE);
			howChartCategorySourceOptions = Arrays.asList(StatsManager.T_NONE, StatsManager.T_SITE, StatsManager.T_USER, StatsManager.T_TOOL, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE);
			howChartSeriesSourceOptions = Arrays.asList(StatsManager.T_SITE, StatsManager.T_USER, StatsManager.T_TOOL, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_TOTAL);
		}else{
			howChartSourceOptions = Arrays.asList(StatsManager.T_USER, StatsManager.T_TOOL, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE);
			howChartCategorySourceOptions = Arrays.asList(StatsManager.T_NONE, StatsManager.T_TOOL, StatsManager.T_USER, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_DATE);
			howChartSeriesSourceOptions = Arrays.asList(StatsManager.T_USER, StatsManager.T_TOOL, StatsManager.T_EVENT, StatsManager.T_RESOURCE, StatsManager.T_RESOURCE_ACTION, StatsManager.T_TOTAL);
		}
		DropDownChoice howChartSource = new DropDownChoice("reportParams.howChartSource", howChartSourceOptions, allColumnsChoiceRenderer);
		howChartSource.setMarkupId("howChartSource");
		howChartSource.setOutputMarkupId(true);
		form.add(howChartSource);
		DropDownChoice howChartCategorySource = new DropDownChoice("reportParams.howChartCategorySource", howChartCategorySourceOptions, allColumnsChoiceRenderer);
		howChartCategorySource.setMarkupId("howChartCategorySource");
		howChartCategorySource.setOutputMarkupId(true);
		form.add(howChartCategorySource);
		DropDownChoice howChartSeriesSource = new DropDownChoice("reportParams.howChartSeriesSource", howChartSeriesSourceOptions, allColumnsChoiceRenderer);
		howChartSeriesSource.setMarkupId("howChartSeriesSource");
		howChartSeriesSource.setOutputMarkupId(true);
		form.add(howChartSeriesSource);

	}
	
	
	@SuppressWarnings("serial")
	private void addTools(final RepeatingView rv) {
		List<SelectOption> tools = new ArrayList<SelectOption>();
		List<ToolInfo> siteTools = Locator.getFacade().getEventRegistryService().getEventRegistry(siteId, getPrefsdata().isListToolEventsOnlyAvailableInSite());
        // add tools
        for (ToolInfo toolInfo : siteTools) {
            if (isToolSuported(toolInfo)) {
                tools.add(new SelectOption("option", new ToolModel(toolInfo)));
            }
        }
		WebMarkupContainer optgroupItem = new WebMarkupContainer(rv.newChildId());
		optgroupItem.setRenderBodyOnly(true);
		rv.add(optgroupItem);
		IStylableOptionRenderer optionRenderer = new IStylableOptionRenderer() {

			@Override
			public String getDisplayValue(Object object) {
				SelectOption opt = (SelectOption) object;
				return " " + ((ToolModel) opt.getDefaultModel()).getToolName();
			}

			@Override
			public IModel getModel(Object value) {
				SelectOption opt = (SelectOption) value;
				return new Model(((ToolModel) opt.getDefaultModel()).getToolId());
			}

			@Override
			public String getStyle(Object object) {
				SelectOption opt = (SelectOption) object;
				ToolModel toolModel = (ToolModel) opt.getDefaultModel();
				String toolId = toolModel.getToolId();
                return "display:block;";
			}

			@Override
			public String getIconClass(Object object) {
				SelectOption opt = (SelectOption) object;
				ToolModel toolModel = (ToolModel) opt.getDefaultModel();
				String toolId = toolModel.getToolId();
                return ICON_SAKAI + toolId.replace('.', '-');
			}
		};
		Collections.sort(tools, Comparators.getOptionRendererComparator(optionRenderer));
		// "all" tools (insert in position 0
		tools.add(0, new SelectOption("option", new ToolModel(ReportManager.WHAT_EVENTS_ALLTOOLS, ReportManager.WHAT_EVENTS_ALLTOOLS)));
		StylableSelectOptions selectOptions = new StylableSelectOptions("selectOptions", tools, optionRenderer);
		selectOptions.setRenderBodyOnly(true);
		optgroupItem.add(selectOptions);
	}
	
	@SuppressWarnings("serial")
	private void addEvents(final RepeatingView rv) {
		List<ToolInfo> siteTools = Locator.getFacade().getEventRegistryService().getEventRegistry(siteId, getPrefsdata().isListToolEventsOnlyAvailableInSite());
		siteTools.sort(Comparators.getToolInfoComparator());
		// add events
        for (ToolInfo toolInfo : siteTools) {
            if (isToolSuported(toolInfo)) {
                List<EventInfo> eventInfos = toolInfo.getEvents();
                List<SelectOption> events = new ArrayList<SelectOption>();
                for (EventInfo e : eventInfos) {
                    SelectOption opt = new SelectOption("option", new EventModel(e));
                    events.add(opt);
                }
                WebMarkupContainer optgroupItem = new WebMarkupContainer(rv.newChildId());
                optgroupItem.setRenderBodyOnly(true);
                rv.add(optgroupItem);
                String style = "display:block;";
                String toolId = toolInfo.getToolId();
                String toolName = Locator.getFacade().getEventRegistryService().getToolName(toolId);
                String hclass = ICON_SAKAI + toolId.replace('.', '-');
                StylableSelectOptionsGroup group = new StylableSelectOptionsGroup("group", new Model(toolName), new Model(style), new Model(hclass));
                optgroupItem.add(group);
                SelectOptions selectOptions = new SelectOptions("selectOptions", events, new IOptionRenderer() {

					@Override
                    public String getDisplayValue(Object object) {
                        SelectOption opt = (SelectOption) object;
                        return ((EventModel) opt.getDefaultModel()).getEventName();
                    }

					@Override
                    public IModel getModel(Object value) {
                        SelectOption opt = (SelectOption) value;
                        return new Model(((EventModel) opt.getDefaultModel()).getEventId());
                    }
                });
                selectOptions.setRenderBodyOnly(true);
                group.add(selectOptions);
            }
        }
	}
	
	@SuppressWarnings("serial")
	private void addUsers(final RepeatingView rv) {
		if(usersLoaded) {
			return;
		}
		ajaxUpdateLock.lock();
		try{
			List<SelectOption> users = new ArrayList<SelectOption>();
			// anonymous access
			if(Locator.getFacade().getStatsManager().isShowAnonymousAccessEvents()) {
				SelectOption anon = new SelectOption("option", new Model(EventTrackingService.UNKNOWN_USER));
				users.add(anon);
			}
			// site users
			Set<String> siteUsers = null;
			try{
				siteUsers = Locator.getFacade().getSiteService().getSite(siteId).getUsers();
			}catch(IdUnusedException e){
				log.warn("Site does not exist: " + siteId);
				siteUsers = new HashSet<>();
			}
            for (String userId : siteUsers) {
                if (userId != null) {
                    SelectOption opt = new SelectOption("option", new Model(userId));
                    opt.setEscapeModelStrings(true);
                    users.add(opt);
                }
            }
			WebMarkupContainer optgroupItem = new WebMarkupContainer(rv.newChildId());
			optgroupItem.setRenderBodyOnly(true);
			rv.add(optgroupItem);
			IOptionRenderer optionRenderer = new IOptionRenderer() {

				@Override
				public String getDisplayValue(Object object) {
					SelectOption opt = (SelectOption) object;
					String userId = (String) opt.getDefaultModel().getObject();
					if(EventTrackingService.UNKNOWN_USER.equals(userId)) {
						return (String) new ResourceModel("user_anonymous_access").getObject();
					}else{
						return Locator.getFacade().getStatsManager().getUserInfoForDisplay(userId, siteId);
					}
				}

				@Override
				public IModel getModel(Object value) {
					SelectOption opt = (SelectOption) value;
					return new Model( (String) opt.getDefaultModel().getObject() );
				}			
			};
			users.sort(Comparators.getOptionRendererComparator(optionRenderer));
			SelectOptions selectOptions = new SelectOptions("selectOptions", users, optionRenderer);
			selectOptions.setRenderBodyOnly(true);
			optgroupItem.add(selectOptions);
			usersLoaded = true;
		}finally{
			ajaxUpdateLock.unlock();
		}
	}
	
	@SuppressWarnings("serial")
	private void addGroupOptions(final RepeatingView rv) {
		boolean isSiteStatsAdminTool = Locator.getFacade().getStatsAuthz().isSiteStatsAdminPage();
		boolean renderAdminOptions = isSiteStatsAdminTool && !predefined && realSiteId.equals(siteId);

		List<String> totalsOptions = new ArrayList<>(Arrays.asList(
				StatsManager.T_USER,
				StatsManager.T_TOOL,
				StatsManager.T_EVENT,
				StatsManager.T_RESOURCE,
				StatsManager.T_RESOURCE_ACTION,
				StatsManager.T_DATE
		));
		if(renderAdminOptions) {
			totalsOptions.add(StatsManager.T_SITE);
		}
		
		// add grouping options
		List<SelectOption> selectOptionList = new ArrayList<>();
        for (String totalOpt : totalsOptions) {
            SelectOption so = new SelectOption("option", new Model(totalOpt));
            so.setEscapeModelStrings(false);
            selectOptionList.add(so);
        }
		
		WebMarkupContainer optgroupItem = new WebMarkupContainer(rv.newChildId());
		optgroupItem.setRenderBodyOnly(true);
		rv.add(optgroupItem);
		final IOptionRenderer optionRenderer = new IOptionRenderer() {

			@Override
			public String getDisplayValue(Object o) {
				SelectOption opt = (SelectOption) o;
				Object object = opt.getDefaultModel().getObject();
				if(StatsManager.T_USER.equals(object)) {
					return (String) new ResourceModel("report_option_user").getObject();					
				}
				if(StatsManager.T_TOOL.equals(object)) {
					return (String) new ResourceModel("report_option_tool").getObject();
				}
				if(StatsManager.T_EVENT.equals(object)) {
					return (String) new ResourceModel("report_option_event").getObject();
				}
				if(StatsManager.T_RESOURCE.equals(object)) {
					return (String) new ResourceModel("report_option_resource").getObject();
				}
				if(StatsManager.T_RESOURCE_ACTION.equals(object)) {
					return (String) new ResourceModel("report_option_resourceaction").getObject();
				}
				if(StatsManager.T_DATE.equals(object)) {
					return (String) new ResourceModel("report_option_date").getObject();
				}
				if(StatsManager.T_LASTDATE.equals(object)) {
					return (String) new ResourceModel("report_option_lastdate").getObject();
				}
				if(StatsManager.T_SITE.equals(object)) {
					return (String) new ResourceModel("report_option_site").getObject();
				}
				return (String) object;			
			}

			@Override
			public IModel getModel(Object value) {
				SelectOption opt = (SelectOption) value;
				return opt.getDefaultModel();
			}
		};
		SelectOptions selectOptions = new SelectOptions("selectOptions", selectOptionList, optionRenderer);
		selectOptions.setRenderBodyOnly(true);
		selectOptions.setEscapeModelStrings(false);
		optgroupItem.add(selectOptions);
	}
	
	private List<String> getGroups() {
		List<String> groups = new ArrayList<String>();
		try{
			Collection<Group> groupCollection = Locator.getFacade().getSiteService().getSite(siteId).getGroups();
            for (Group g : groupCollection) {
                groups.add(g.getId());
            }
		}catch(IdUnusedException e){
			log.warn("getGroups site does not exist: {}", siteId);
			
		}
		return groups;
	}
	
	private List<String> getRoles() {
		Set<String> siteIdWithRoles = new HashSet<>(List.of("/site/" + siteId));

		if ("!admin".equals(siteId) || "~admin".equals(siteId)) {
			siteIdWithRoles.add("!site.template");
			siteIdWithRoles.add("!site.user");
			Locator.getFacade().getSiteService().getSiteTypes().stream().map(s -> "!site.template." + s).forEach(siteIdWithRoles::add);
		}

		Set<String> roles = new HashSet<String>();
		for (String s : siteIdWithRoles) {
			try {
				Locator.getFacade().getAuthzGroupService().getAuthzGroup(s).getRoles().forEach(r -> roles.add(r.getId()));
			} catch (GroupNotDefinedException e) {
				log.debug("AuthzGroup does not exist, skipping: {}", s);
			}
		}
		return new ArrayList<>(roles);
	}
	
	private boolean isToolSuported(final ToolInfo toolInfo) {
		if(Locator.getFacade().getStatsManager().isEventContextSupported()){
			return true;
		}else{
			List<ToolInfo> siteTools = Locator.getFacade().getEventRegistryService().getEventRegistry(siteId, getPrefsdata().isListToolEventsOnlyAvailableInSite());
            for (ToolInfo t : siteTools) {
                if (t.getToolId().equals(toolInfo.getToolId())) {
                    boolean match = t.getEventParserTips().stream()
                            .anyMatch(tip -> StatsManager.PARSERTIP_FOR_CONTEXTID.equals(tip.getFor()));
                    if (match) {
                        return true;
                    }
                }
            }
		}
		return false;
	}

	private PrefsData getPrefsdata() {
		if(prefsdata == null) {
			prefsdata = Locator.getFacade().getStatsManager().getPreferences(siteId, true);
		}
		return prefsdata;
	}

	private boolean validReportParameters() {
		Site site = null;
		try{
			site = Locator.getFacade().getSiteService().getSite(siteId);
		}catch(IdUnusedException e){
			log.error("No site found with id: {}", siteId);
		}
		
		// check WHAT
		if(getReportParams().getWhat().equals(ReportManager.WHAT_EVENTS)
				&& getReportParams().getWhatEventSelType().equals(ReportManager.WHAT_EVENTS_BYTOOL) 
				&& (getReportParams().getWhatToolIds() == null || getReportParams().getWhatToolIds().isEmpty())){
			error((String) new ResourceModel("report_err_notools").getObject());
		}
		if(getReportParams().getWhat().equals(ReportManager.WHAT_EVENTS) 
				&& getReportParams().getWhatEventSelType().equals(ReportManager.WHAT_EVENTS_BYEVENTS) 
				&& (getReportParams().getWhatEventIds() == null || getReportParams().getWhatEventIds().isEmpty())) {
			error((String) new ResourceModel("report_err_noevents").getObject());
		}
		if(getReportParams().getWhat().equals(ReportManager.WHAT_RESOURCES) 
				&& getReportParams().isWhatLimitedResourceIds() 
				&& (getReportParams().getWhatResourceIds() == null || getReportParams().getWhatResourceIds().isEmpty())){
			error((String) new ResourceModel("report_err_noresources").getObject());	
		}
		
		// check WHEN
		if(getReportParams().getWhen().equals(ReportManager.WHEN_CUSTOM)
				&& (getReportParams().getWhenFrom() == null || getReportParams().getWhenTo() == null)) {
			error((String) new ResourceModel("report_err_nocustomdates").getObject());
		}

		// check WHO
		if(getReportParams().getWho().equals(ReportManager.WHO_ROLE)){
			if(!siteId.equals("!admin") && !siteId.equals("~admin") && site.getUsersHasRole(getReportParams().getWhoRoleId()).isEmpty())
				error((String) new ResourceModel("report_err_emptyrole").getObject());	
		}else if(getReportParams().getWho().equals(ReportManager.WHO_GROUPS)){
			if(getReportParams().getWhoGroupId() == null || getReportParams().getWhoGroupId().isEmpty())
				error((String) new ResourceModel("report_err_nogroup").getObject());
			else if(site.getGroup(getReportParams().getWhoGroupId()).getUsers().isEmpty())
				error((String) new ResourceModel("report_err_emptygroup").getObject());	
		}else if(getReportParams().getWho().equals(ReportManager.WHO_CUSTOM) 
				&& (getReportParams().getWhoUserIds() == null || getReportParams().getWhoUserIds().isEmpty())){
			error((String) new ResourceModel("report_err_nousers").getObject());
		}

		// check HOW
		if(getReportParams().getHowTotalsBy() != null){
			if(getReportParams().getHowSortBy().isEmpty()) {
				error((String) new ResourceModel("report_err_totalsbynone").getObject());
			}
			if(getReportParams().getWhat().equals(ReportManager.WHAT_EVENTS)
					&& (getReportParams().getHowTotalsBy().contains(StatsManager.T_RESOURCE) || getReportParams().getHowTotalsBy().contains(StatsManager.T_RESOURCE_ACTION) )) {
				error((String) new ResourceModel("report_err_totalsbyevent").getObject());	
			}else if(getReportParams().getWhat().equals(ReportManager.WHAT_RESOURCES)
					&& getReportParams().getHowTotalsBy().contains(StatsManager.T_EVENT)) {
				error((String) new ResourceModel("report_err_totalsbyresource").getObject());	
			}

		}
		if(getReportParams().isHowSort() && getReportParams().getHowSortBy() != null && !getReportParams().getHowSortBy().equals(ReportManager.HOW_SORT_DEFAULT)){
			if(!StatsManager.T_TOTAL.equals(getReportParams().getHowSortBy()) 
					&& !getReportParams().getHowTotalsBy().contains(getReportParams().getHowSortBy())
					){
				getReportParams().setHowSort(false);
				getReportParams().setHowSortBy(null);
			}
		}
		
		
		return !hasErrorMessage();
	}
	
	public String getReportSite() {
		return reportSiteOpt;
	}
	
	public void setReportSite(String reportSiteOpt) {
		this.reportSiteOpt = reportSiteOpt;
		if(REPORT_THISSITE.equals(reportSiteOpt)) {
			getReportParams().setSiteId(siteId);
		}else if(REPORT_ALLSITES.equals(reportSiteOpt)) {
			getReportParams().setSiteId(null);
		}
	}
	
	public String getTitleLocalized() {
		return (String) new ResourceModel(getReportDef().getTitleBundleKey()).getObject();
	}
	
	public String getDescriptionLocalized() {
		return (String) new ResourceModel(getReportDef().getDescriptionBundleKey()).getObject();
	}

	public ReportDef getReportDef() {
		return (ReportDef) this.reportDefModel.getObject();
	}

	public void setReportParams(ReportParams reportParams) {
		getReportDef().setReportParams(reportParams);
	}

	public ReportParams getReportParams() {
		return getReportDef().getReportParams();
	}

	private void setISODates() {
		if (startDate != null) {
			getReportParams().setWhenFrom(Date.from(startDate.toInstant()));
		}
		if (endDate != null) {
			getReportParams().setWhenTo(Date.from(endDate.toInstant()));
		}
	}
}

