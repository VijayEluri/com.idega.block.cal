package com.idega.block.cal.business;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;
import javax.servlet.ServletContext;

import org.apache.commons.collections.MapUtils;
import org.apache.myfaces.custom.schedule.model.ScheduleModel;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.cal.bean.CalendarManagerBean;
import com.idega.block.cal.business.events.EventsProvider;
import com.idega.block.cal.data.CalendarEntry;
import com.idega.block.cal.data.CalendarEntryType;
import com.idega.block.cal.data.CalendarLedger;
import com.idega.block.cal.presentation.CalendarEntryInfoBlock;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogic;
import com.idega.business.IBOLookup;
import com.idega.business.chooser.helper.CalendarsChooserHelper;
import com.idega.cal.bean.CalendarPropertiesBean;
import com.idega.core.builder.business.ICBuilderConstants;
import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.GroupService;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.WebUtil;
import com.idega.webface.WFUtil;

public class CalServiceBean implements CalService {

	private CalBusiness calBusiness = null;
	private GroupService groupService = null;
	private GroupBusiness groupBusiness = null;

	private String calendarCacheName = "calendarViewersUniqueIdsCache";
	private String eventsCacheName = "eventsForCalendarViewersUniqueIdsCache";
	private String ledgersCacheName = "ledgersForCalendarViewersUniqueIdsCache";

	/**
	 * Checks if can use DWR on remote server
	 */
	@Override
	public boolean canUseRemoteServer(String server) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return false;
		}

		GroupService groupService = getGroupService(iwc);
		if (groupService == null) {
			return false;
		}

		List<String> scripts = new ArrayList<String>();
		scripts.add(CoreConstants.DWR_ENGINE_SCRIPT);
		scripts.add(CalendarConstants.CALENDAR_SERVICE_DWR_INTERFACE_SCRIPT);

		return groupService.canMakeCallToServerAndScript(server, scripts);
	}

	private CalendarManagerBean getBean() {
		Object o = WFUtil.getBeanInstance(CalendarConstants.CALENDAR_MANAGER_BEAN_ID);
		if (!(o instanceof CalendarManagerBean)) {
			return null;
		}
		return (CalendarManagerBean) o;
	}

	@Override
	public CalendarPropertiesBean getCalendarProperties(String instanceId) {
		if (instanceId == null) {
			return null;
		}
		CalendarManagerBean bean = getBean();
		if (bean == null) {
			return null;
		}

		return bean.getCalendarProperties(instanceId);
	}

	@Override
	public List<AdvancedProperty> getAvailableCalendarEventTypes() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		CalBusiness calBusiness = getCalBusiness(iwc);
		if (calBusiness == null) {
			return null;
		}

		List eventsTypes = null;
		try {
			eventsTypes = calBusiness.getAllEntryTypes();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		if (eventsTypes == null) {
			return null;
		}

		List<AdvancedProperty> types = new ArrayList<AdvancedProperty>();
		Object o = null;
		CalendarEntryType calendarEntryType = null;
		for (int i = 0; i < eventsTypes.size(); i++) {
			o = eventsTypes.get(i);
			if (o instanceof CalendarEntryType) {
				calendarEntryType = (CalendarEntryType) o;
				types.add(new AdvancedProperty(calendarEntryType.getId(), calendarEntryType.getName()));
			}
		}

		return types;
	}

	@Override
	public List<AdvancedProperty> getAvailableCalendarEventTypesWithLogin(String login, String password) {
		if (login == null || password == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		GroupService groupService = getGroupService(iwc);
		if (groupService == null) {
			return null;
		}
		if (!groupService.isLoggedUser(iwc, login)) {
			if (!groupService.logInUser(iwc, login, password)) {
				return null;
			}
		}

		return getAvailableCalendarEventTypes();
	}

	@Override
	public List<AdvancedProperty> getAvailableLedgers() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		CalBusiness calBusiness = getCalBusiness(iwc);
		if (calBusiness == null) {
			return null;
		}

		List<CalendarLedger> userLedgers = calBusiness.getUserLedgers(iwc.getCurrentUser(), iwc);
		if (userLedgers == null) {
			return null;
		}

		List<AdvancedProperty> ledgers = new ArrayList<AdvancedProperty>();

		CalendarLedger ledger = null;
		for (int i = 0; i < userLedgers.size(); i++) {
			ledger = userLedgers.get(i);
			ledgers.add(new AdvancedProperty(String.valueOf(ledger.getLedgerID()), ledger.getName()));
		}

		return ledgers;
	}

	@Override
	public List<AdvancedProperty> getAvailableLedgersWithLogin(String login, String password) {
		if (login == null || password == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		GroupService groupService = getGroupService(iwc);
		if (groupService == null) {
			return null;
		}
		if (!groupService.isLoggedUser(iwc, login)) {
			if (!groupService.logInUser(iwc, login, password)) {
				return null;
			}
		}

		return getAvailableLedgers();
	}

	private CalBusiness getCalBusiness(IWApplicationContext iwac) {
		if (calBusiness == null) {
			try {
				calBusiness = IBOLookup.getServiceInstance(iwac, CalBusiness.class);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return calBusiness;
	}

	private GroupBusiness getGroupBusiness(IWApplicationContext iwac) {
		if (groupBusiness == null) {
			try {
				groupBusiness = IBOLookup.getServiceInstance(iwac, GroupBusiness.class);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return groupBusiness;
	}

	private GroupService getGroupService(IWApplicationContext iwac) {
		if (groupService == null) {
			try {
				groupService = IBOLookup.getServiceInstance(iwac, GroupService.class);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return groupService;
	}

	private GroupService getGroupService() {
		if (groupService == null) {
			return getGroupService(CoreUtil.getIWContext());
		}
		return groupService;
	}

	@Override
	public List<String> getCalendarInformation() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		IWBundle bundle = null;
		try {
			bundle = iwc.getIWMainApplication().getBundle(CalendarConstants.IW_BUNDLE_IDENTIFIER);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		if (iwrb == null) {
			return null;
		}

		List<String> info = new ArrayList<String>();

		info.add(ICBuilderConstants.CALENDAR_EVENTS_ADVANCED_PROPERTY_KEY);								//	0
		info.add(ICBuilderConstants.CALENDAR_LEDGERS_ADVANCED_PROPERTY_KEY);							//	1

		info.add(iwrb.getLocalizedString("no_events_exist", "Sorry, there are no events created."));	//	2
		info.add(iwrb.getLocalizedString("no_ledgers_exist", "Sorry, there are no ledgers created."));	//	3

		info.add(calendarCacheName);																	//	4
		info.add(eventsCacheName);																		//	5
		info.add(ledgersCacheName);																		//	6

		info.add(String.valueOf(ScheduleModel.MONTH));													//	7
		info.add(String.valueOf(ScheduleModel.DAY));													//	8
		info.add(String.valueOf(ScheduleModel.WEEK));													//	9
		info.add(String.valueOf(ScheduleModel.WORKWEEK));												//	10

		info.add(iwrb.getLocalizedString("name", "Name"));												//	11
		info.add(iwrb.getLocalizedString("endDate", "End date"));										//	12
		info.add(iwrb.getLocalizedString("type", "Type"));												//	13
		info.add(iwrb.getLocalizedString("time", "Time"));												//	14
		info.add(iwrb.getLocalizedString("date", "Date"));												//	15
		info.add(iwrb.getLocalizedString("noEntriesToDisplay", "There are no entries to display"));		//	16
		info.add(iwrb.getLocalizedString("cantConnectTo", "can not connect to:"));						//	17
		info.add(iwrb.getLocalizedString("loadingMsg", "Loading..."));									//	18

		info.add(iwrb.getLocalizedString("previousLabel", "Previous"));									//	19
		info.add(iwrb.getLocalizedString("nextLabel", "Next"));											//	20
		info.add(iwrb.getLocalizedString("dayLabel", "Day"));											//	21
		info.add(iwrb.getLocalizedString("weekLabel", "Week"));											//	22
		info.add(iwrb.getLocalizedString("workweekLabel", "Work week"));								//	23
		info.add(iwrb.getLocalizedString("monthLabel", "Month"));										//	24

		info.add(CalendarConstants.SCHEDULE_ENTRY_STYLE_CLASS);											//	25

		BuilderLogic builder = BuilderLogic.getInstance();
		info.add(builder.getUriToObject(CalendarEntryInfoBlock.class));									//	26

		info.add(iwrb.getLocalizedString("calendar_entry_info", "Calendar entry information"));			//	27

		return info;
	}

	@Override
	public boolean addUniqueIdsForCalendarGroups(String instanceId, List<String> ids) {
		GroupService groupService = getGroupService();
		if (groupService == null) {
			return Boolean.FALSE;
		}

		return groupService.addUniqueIds(calendarCacheName, instanceId, ids);
	}

	@Override
	public boolean addUniqueIdsForCalendarLedgers(String instanceId, List<String> ids) {
		if (ids == null || ids.size() == 0) {
			return Boolean.TRUE;
		}

		GroupService groupService = getGroupService();
		if (groupService == null) {
			return Boolean.FALSE;
		}

		return groupService.addUniqueIds(ledgersCacheName, instanceId, ids);
	}

	@Override
	public boolean addUniqueIdsForCalendarEvents(String instanceId, List<String> ids) {
		if (ids == null || ids.size() == 0) {
			return Boolean.TRUE;
		}

		GroupService groupService = getGroupService();
		if (groupService == null) {
			return Boolean.FALSE;
		}

		return groupService.addUniqueIds(eventsCacheName, instanceId, ids);
	}

	@Override
	public CalendarPropertiesBean reloadProperties(String instanceId) {
		if (instanceId == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		IWMainApplication iwma = iwc.getIWMainApplication();
		BuilderLogic builder = BuilderLogic.getInstance();
		String pageKey = builder.getCurrentIBPage(iwc);

		CalendarPropertiesBean properties = null;
		UIComponent calendar = builder.findComponentInPage(iwc, pageKey, instanceId);
		if (calendar == null) {
			String propertyName = new StringBuffer(":method:1:implied:void:setCalendarProperties:").append(CalendarPropertiesBean.class.getName()).append(":").toString();
			String values[] = builder.getPropertyValues(iwma, pageKey, instanceId, propertyName, null, true);
			if (values == null) {
				return null;
			}
			if (values.length == 0) {
				return null;
			}

			CalendarsChooserHelper helper = new CalendarsChooserHelper();
			properties = helper.getExtractedPropertiesFromString(values[0]);
			properties.setInstanceId(instanceId);
		}
		else {
			builder.getRenderedComponent(calendar, iwc, false);
			properties = getCalendarProperties(instanceId);
		}

		if (properties == null) {
			return null;
		}
		Object[] parameters = new Object[2];
		parameters[0] = instanceId;
		parameters[1] = properties;

		Class<?>[] classes = new Class[2];
		classes[0] = String.class;
		classes[1] = CalendarPropertiesBean.class;

		WFUtil.invoke(CalendarConstants.CALENDAR_MANAGER_BEAN_ID, "addCalendarProperties", parameters, classes);
		return properties;
	}

	private List<CalScheduleEntry> getMyExternalEvents(User user, Timestamp from, Timestamp to) {
		try {
			ServletContext context = IWMainApplication.getDefaultIWMainApplication().getServletContext();
			WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(context);
			Map<String, EventsProvider> eventsProviders = webApplicationContext.getBeansOfType(EventsProvider.class);
			if (MapUtils.isEmpty(eventsProviders))
				return Collections.emptyList();

			Locale locale = CoreUtil.getCurrentLocale();
			Map<String, CalScheduleEntry> allEvents = new HashMap<String, CalScheduleEntry>();
			for (EventsProvider provider: eventsProviders.values()) {
				List<CalendarEntry> events = provider.getEvents(user, from, to);
				if (ListUtil.isEmpty(events))
					return Collections.emptyList();

				List<CalScheduleEntry> entries = getConvertedEntries(events, locale);
				if (ListUtil.isEmpty(entries))
					continue;

				for (CalScheduleEntry entry: entries)
					allEvents.put(entry.getId(), entry);
			}
			return new ArrayList<CalScheduleEntry>(allEvents.values());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<CalScheduleEntry> getCalendarEntries(String login, String password, String instanceId, Integer cacheTime, boolean remoteMode) {
		if (instanceId == null) {
			return null;
		}

		if (remoteMode && (login == null || password == null)) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		GroupService groupService = getGroupService(iwc);
		if (groupService == null) {
			return null;
		}

		if (remoteMode && !groupService.isUserLoggedOn(iwc, login, password)) {
			return null;
		}

		CalendarPropertiesBean properties = getCalendarProperties(instanceId);
		if (properties != null && properties.isShowMyBedeworkEvents()) {
			IWTimestamp from = WebUtil.getFirstDay(true);	//	TODO
			IWTimestamp to = WebUtil.getLastDay(true);
			return getMyExternalEvents(iwc.isLoggedOn() ? iwc.getCurrentUser() : null, from.getTimestamp(), to.getTimestamp());
		}

		List<String> groupsUniqueIds = null;
		try {
			groupsUniqueIds = groupService.getUniqueIds(calendarCacheName).get(instanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}

		List<String> ledgersIds = null;
		try {
			ledgersIds = groupService.getUniqueIds(ledgersCacheName).get(instanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}

		List<String> eventsIds = null;
		try {
			eventsIds = groupService.getUniqueIds(eventsCacheName).get(instanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}

		CalBusiness calBusiness = getCalBusiness(iwc);
		if (calBusiness == null) {
			return null;
		}
		GroupBusiness groupBusiness = getGroupBusiness(iwc);
		if (groupBusiness == null) {
			return null;
		}

		List<String> groupsIds = null;
		if (groupsUniqueIds != null) {
			//	Getting ids for groups from unique ids
			groupsIds = new ArrayList<String>();
			Group group = null;
			for (int i = 0; i < groupsUniqueIds.size(); i++) {
				group = null;

				try {
					group = groupBusiness.getGroupByUniqueId(groupsUniqueIds.get(i));
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (FinderException e) {
					e.printStackTrace();
				}

				if (group != null) {
					groupsIds.add(group.getId());
				}
			}
		}

		List<CalendarEntry> entriesByEvents = new ArrayList<CalendarEntry>();
		List<CalendarEntry> entriesByLedgers = new ArrayList<CalendarEntry>();
		List<CalendarEntry> entries = null;
		if (groupsIds == null || groupsIds.size() == 0) {
			if (ledgersIds == null || ledgersIds.size() == 0) {
				//	We don't want to get calendar entries only by events
				return null;
			}

			entries = calBusiness.getEntriesByLedgers(ledgersIds);
			if (entries == null) {
				return null;
			}
			entriesByLedgers.addAll(entries);
		}
		else {
			//	Events by type(s) and group(s)
			if (eventsIds != null && eventsIds.size() > 0) {
				entries = null;
				try {
					entries = calBusiness.getEntriesByEventsIdsAndGroupsIds(eventsIds, groupsIds);
				} catch(Exception e) {
					e.printStackTrace();
				}
				if (entries != null) {
					entriesByEvents.addAll(entries);
				}
			}

			//	Events by ledger(s) and group(s)
			if (ledgersIds != null && ledgersIds.size() > 0) {
				entries = null;
				try {
					entries = calBusiness.getEntriesByLedgersIdsAndGroupsIds(ledgersIds, groupsIds);
				} catch(Exception e) {
					e.printStackTrace();
				}

				if (entries != null) {
					entriesByLedgers.addAll(entries);
				}
			}
		}

		List<CalendarEntry> allEntries = new ArrayList<CalendarEntry>();
		allEntries.addAll(entriesByEvents);
		allEntries = getFilteredEntries(entriesByLedgers, allEntries);

		return getConvertedEntries(allEntries, iwc.getCurrentLocale());
	}

	private Map<String, List<CalScheduleEntry>> getCalendarCache(IWContext iwc) {
		if (iwc == null) {
			return null;
		}

		IWCacheManager2 cacheManager = IWCacheManager2.getInstance(iwc.getIWMainApplication());
		if (cacheManager == null) {
			return null;
		}

		Map<String, List<CalScheduleEntry>> cache = null;
		try {
			cache = cacheManager.getCache("cacheForCalendarViewerCalScheduleEntries");
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}

		return cache;
	}

	@Override
	public boolean removeCelandarEntriesFromCache(String instanceId) {
		if (instanceId == null) {
			return false;
		}

		try {
			Map<String, List<CalScheduleEntry>> cache = getCalendarCache(CoreUtil.getIWContext());
			if (cache == null) {
				return true;
			}
			cache.remove(instanceId);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private List<CalendarEntry> getFilteredEntries(List<CalendarEntry> source, List<CalendarEntry> destination) {
		if (source != null) {
			for (int i = 0; i < source.size(); i++) {
				if (!(destination.contains(source.get(i)))) {
					destination.add(source.get(i));
				}
			}
		}

		return destination;
	}

	private List<CalScheduleEntry> getConvertedEntries(List<CalendarEntry> entries, Locale locale) {
		if (entries == null)
			return null;

		if (locale == null)
			locale = CoreUtil.getCurrentLocale();

		CalendarEntry entry = null;
		List<CalScheduleEntry> convertedEntries = new ArrayList<CalScheduleEntry>();
		for (int i = 0; i < entries.size(); i++) {
			entry = entries.get(i);
			CalScheduleEntry calEntry = new CalScheduleEntry();

			IWTimestamp date = new IWTimestamp(entry.getDate());
			IWTimestamp endDate = new IWTimestamp(entry.getEndDate());

			calEntry.setId(String.valueOf(entry.getEntryID()));
			calEntry.setEntryName(entry.getName());

			calEntry.setEntryDate(date.getDateString(CalendarConstants.DATE_PATTERN));
			calEntry.setEntryEndDate(endDate.getDateString(CalendarConstants.DATE_PATTERN));

			calEntry.setEntryTime(date.getLocaleTime(locale));
			calEntry.setEntryEndTime(endDate.getLocaleTime(locale));

			calEntry.setEntryTypeName(entry.getEntryTypeName());
			calEntry.setRepeat(entry.getRepeat());
			calEntry.setEntryDescription(entry.getDescription());

			convertedEntries.add(calEntry);
		}

		return convertedEntries;
	}
}
