package com.idega.block.cal.business;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.servlet.ServletException;

import com.idega.block.cal.data.CalendarEntry;
import com.idega.block.rss.business.RSSAbstractProducer;
import com.idega.block.rss.business.RSSBusiness;
import com.idega.block.rss.business.RSSProducer;
import com.idega.block.rss.data.RSSRequest;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * 
 * @author <a href="justinas@idega.com">Justinas Rakita</a>
 * 
 * Generates rss files for calendar entries. 1. For all entries of specified period, /rss/calendar/period/date_from-date_to for example
 * rss/calendar/period/20070501-20070528 2. For all entries of specified group, /rss/calendar/group/groups_id 2.1. For all entries of specified group
 * and specified period, /rss/calendar/group/groups_id/date_from-date_to 3. For all entries of specified ledger, /rss/calendar/ledger/ledger_id 3.1.
 * For all entries of specified ledger and specified period, /rss/calendar/ledger/ledger_id/date_from-date_to 4. For all entries of specified events
 * (or just one event), /rss/calendar/events/type1+type2+type3 or just /rss/calendar/events/type1 for single event 4.1 For all entries of specified
 * events and specified period, /rss/calendar/events/type1+type2+type3/20070219-20070319 or just /rss/calendar/events/type1/20070219-20070319 for
 * single event
 */

public class CalendarRSSProducer extends RSSAbstractProducer implements RSSProducer {

	private List rssFileURIsCacheList;

	private List rssFileURIsCacheListByPeriod = new ArrayList();
	private List rssFileURIsCacheListByGroup = new ArrayList();
	private List rssFileURIsCacheListByLedger = new ArrayList();
	private List rssFileURIsCacheListByEvents = new ArrayList();

	private static final int DATE_LENGTH = 8;
	private static final String FEED_DESCRIPTION = "Calendar feed generated by IdegaWeb ePlatform, Idega Software, http://www.idega.com";
	private static final String PATH_TO_FEED_PARENT_FOLDER = "/files/cms/calendar/rss/";

	private static final String NO_ENTRIES_FOUND_TITLE = "No entries found";
	private static final String NO_ENTRIES_FOUND_FILE = "no_entries.xml";
	private static final String INCORRECT_PERIOD_TITLE = "Incorrect period";
	private static final String INCORRECT_PERIOD_FILE = "incorect_period.xml";

	public void handleRSSRequest(RSSRequest rssRequest) throws IOException {
		String extraURI = rssRequest.getExtraUri();
		if (extraURI == null) {
			extraURI = "";
		}
		if ((!extraURI.endsWith("/")) && (extraURI.length() != 0)) {
			extraURI = extraURI.concat("/");
		}

		try {
			this.dispatch("/content" + getFeedFile(extraURI, rssRequest), rssRequest);
		}
		catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getFeedFile(String extraURI, RSSRequest rssRequest) {
		if (extraURI.startsWith("period")) {
			rssFileURIsCacheList = rssFileURIsCacheListByPeriod;
			return getFeedByPeriod(extraURI, rssRequest);
		}
		else if (extraURI.startsWith("group")) {
			rssFileURIsCacheList = rssFileURIsCacheListByGroup;
			return getFeedByGroup(extraURI, rssRequest);
		}
		else if (extraURI.startsWith("ledger")) {
			rssFileURIsCacheList = rssFileURIsCacheListByLedger;
			return getFeedByLedger(extraURI, rssRequest);
		}
		else if (extraURI.startsWith("events")) {
			rssFileURIsCacheList = rssFileURIsCacheListByEvents;
			return getFeedByEvents(extraURI, rssRequest);
		}
		else {
			return getFeed(NO_ENTRIES_FOUND_TITLE, NO_ENTRIES_FOUND_FILE, null, rssRequest, getIWContext(rssRequest));
		}
	}

	private String getFeedByPeriod(String extraURI, RSSRequest rssRequest) {
		IWContext iwc = getIWContext(rssRequest);
		String uri = extraURI.substring("period/".length(), extraURI.length());
		String feedFile = "period_" + getName(uri) + iwc.getLocale().getLanguage() + ".xml";

		if (rssFileURIsCacheList.contains(feedFile)) {
			return PATH_TO_FEED_PARENT_FOLDER + feedFile;
		}

		String period = extraURI.substring("period/".length());
		String fromStr = period.substring(0, DATE_LENGTH);
		String toStr = period.substring(DATE_LENGTH + 1, period.length() - 1);
		Timestamp fromTmst = getTimeStampFromString(fromStr);
		Timestamp toTmst = getTimeStampFromString(toStr);

		if (toTmst.before(fromTmst)) {
			return getFeed(INCORRECT_PERIOD_TITLE, INCORRECT_PERIOD_FILE, null, rssRequest, iwc);
		}
		CalBusiness calendar = new CalBusinessBean();

		Collection entries = calendar.getEntriesBetweenTimestamps(fromTmst, toTmst);
		if (entries.isEmpty()) {
			return getFeed(NO_ENTRIES_FOUND_TITLE, NO_ENTRIES_FOUND_FILE, null, rssRequest, iwc);
		}
		String title = fromStr + "-" + toStr;

		return getFeed(title, feedFile, entries, rssRequest, iwc);
	}

	private String getFeedByGroup(String extraURI, RSSRequest rssRequest) {
		IWContext iwc = getIWContext(rssRequest);
		String uri = extraURI.substring("group/".length(), extraURI.length());
		String feedFile = "group_" + getName(uri) + getPeriod(uri) + iwc.getLocale().getLanguage() + ".xml";

		if (rssFileURIsCacheList.contains(feedFile)) {
			return PATH_TO_FEED_PARENT_FOLDER + feedFile;
		}
		String group = extraURI.substring("group/".length());
		String groupID = group.substring(0, group.indexOf("/"));
		String groupPeriod = group.substring(groupID.length() + 1, group.length());
		Timestamp from = null;
		Timestamp to = null;
		CalBusiness calendar = new CalBusinessBean();
		int entryGroupID;
		List entries = null;
		try {
			entryGroupID = Integer.parseInt(groupID);
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		LedgerVariationsHandler ledgerVariationsHandler = new DefaultLedgerVariationsHandler();
		String title = "";
		try {
			title = ((DefaultLedgerVariationsHandler) ledgerVariationsHandler).getGroupBusiness(iwc).getGroupByGroupID(Integer.parseInt(groupID)).getName();
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FinderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (groupPeriod.length() != 0) {
			String fromStr = groupPeriod.substring(0, DATE_LENGTH);
			String toStr = groupPeriod.substring(DATE_LENGTH + 1, groupPeriod.length() - 1);
			from = getTimeStampFromString(fromStr);
			to = getTimeStampFromString(toStr);
			if (to.before(from)) {
				return getFeed(INCORRECT_PERIOD_TITLE, INCORRECT_PERIOD_FILE, null, rssRequest, iwc);
			}
			title = title + " " + from + "-" + to;
			Collection coll = calendar.getEntriesBetweenTimestamps(from, to);
			entries = new ArrayList();
			for (Iterator iter = coll.iterator(); iter.hasNext();) {
				CalendarEntry element = (CalendarEntry) iter.next();
				if (element.getGroupID() == entryGroupID) {
					entries.add(element);
				}
			}
		}
		else {
			entries = new ArrayList(calendar.getEntriesByICGroup(entryGroupID));
		}
		if (entries.isEmpty()) {
			return getFeed(NO_ENTRIES_FOUND_TITLE, NO_ENTRIES_FOUND_FILE, null, rssRequest, iwc);
		}
		else {
			return getFeed(title, feedFile, entries, rssRequest, iwc);
		}
	}

	private String getFeedByLedger(String extraURI, RSSRequest rssRequest) {
		IWContext iwc = getIWContext(rssRequest);
		String feedFile = "ledger_" + extraURI.substring("ledger/".length(), extraURI.length() - 1) + "_" + iwc.getLocale().getLanguage() + ".xml";
		if (rssFileURIsCacheList.contains(feedFile)) {
			return feedFile;
		}
		String ledger = extraURI.substring("ledger/".length());
		String ledgerID = ledger.substring(0, ledger.indexOf("/"));
		String ledgerPeriod = ledger.substring(ledgerID.length() + 1, ledger.length());
		Timestamp from = null;
		Timestamp to = null;
		CalBusiness calendar = new CalBusinessBean();
		int ledgerIdInt;
		List entries = null;
		try {
			ledgerIdInt = Integer.parseInt(ledgerID);
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		LedgerVariationsHandler ledgerVariationsHandler = new DefaultLedgerVariationsHandler();
		String title = ((DefaultLedgerVariationsHandler) ledgerVariationsHandler).getCalBusiness(iwc).getLedger(Integer.parseInt(ledgerID)).getName();

		if (ledgerPeriod.length() != 0) {
			String fromStr = ledgerPeriod.substring(0, DATE_LENGTH);
			String toStr = ledgerPeriod.substring(DATE_LENGTH + 1, ledgerPeriod.length() - 1);
			from = getTimeStampFromString(fromStr);
			to = getTimeStampFromString(toStr);
			if (to.before(from)) {
				return getFeed(INCORRECT_PERIOD_TITLE, INCORRECT_PERIOD_FILE, null, rssRequest, iwc);
			}
			title = title + " " + from + "-" + to;
			Collection coll = calendar.getEntriesBetweenTimestamps(from, to);
			entries = new ArrayList();
			for (Iterator iter = coll.iterator(); iter.hasNext();) {
				CalendarEntry element = (CalendarEntry) iter.next();
				if (element.getLedgerID() == ledgerIdInt) {
					entries.add(element);
				}
			}
		}
		else {
			entries = new ArrayList(calendar.getEntriesByLedgerID(ledgerIdInt));
		}
		if (entries.isEmpty()) {
			return getFeed(NO_ENTRIES_FOUND_TITLE, NO_ENTRIES_FOUND_FILE, null, rssRequest, iwc);
		}
		else {

			return getFeed(title, feedFile, entries, rssRequest, iwc);
		}
	}

	private String getFeedByEvents(String extraURI, RSSRequest rssRequest) {
		IWContext iwc = getIWContext(rssRequest);
		String uri = extraURI.substring("group/".length(), extraURI.length());
		String feedFile = "events_" + getTypesString(uri) + getPeriod(uri) + iwc.getLocale().getLanguage() + ".xml";

		if (rssFileURIsCacheList.contains(feedFile)) {
			return feedFile;
		}
		String events = extraURI.substring("events/".length());
		String eventsPeriod = null;
		List eventsList = new ArrayList();
		int index = -1;
		String title = "";
		if (events.indexOf("+") == -1) {
			eventsList.add(events.substring(0, events.indexOf("/")));
			events = events.substring(events.indexOf("/") + 1, events.length());
		}
		else {
			while (true) {
				index = events.indexOf("+");
				if (index == -1) {
					index = events.indexOf("/");
					eventsList.add(events.substring(0, index));
					events = events.substring(index + 1, events.length());
					title = title + events.substring(0, index);
					break;
				}
				else {
					title = title + events.substring(0, index) + ", ";
					eventsList.add(events.substring(0, index));
					events = events.substring(index + 1, events.length());
				}
			}
		}

		eventsPeriod = events;
		Timestamp from = null;
		Timestamp to = null;
		CalBusiness calendar = new CalBusinessBean();
		List entries = null;
		if (eventsPeriod.length() != 0) {
			String fromStr = eventsPeriod.substring(0, DATE_LENGTH);
			String toStr = eventsPeriod.substring(DATE_LENGTH + 1, eventsPeriod.length() - 1);
			from = getTimeStampFromString(fromStr);
			to = getTimeStampFromString(toStr);
			Collection coll = calendar.getEntriesBetweenTimestamps(from, to);
			entries = new ArrayList();
			title = title + fromStr + "-" + toStr;
			for (Iterator iter = coll.iterator(); iter.hasNext();) {
				CalendarEntry element = (CalendarEntry) iter.next();
				if (eventsList.contains(element.getEntryTypeName())) {
					entries.add(element);
				}
			}
		}
		else {
			entries = new ArrayList(calendar.getEntriesByEvents(eventsList));
		}

		if (entries.isEmpty()) {
			return getFeed(NO_ENTRIES_FOUND_TITLE, NO_ENTRIES_FOUND_FILE, null, rssRequest, iwc);
		}
		else {
			return getFeed(title, feedFile, entries, rssRequest, iwc);
		}
	}

	private String getFeed(String title, String feedFileName, Collection entries, RSSRequest rssRequest, IWContext iwc) {
		if (rssFileURIsCacheList.contains(feedFileName)) {
			return PATH_TO_FEED_PARENT_FOLDER + feedFileName;
		}
		Date now = new Date();
		RSSBusiness rss = null;
		try {
			rss = (RSSBusiness) IBOLookup.getServiceInstance(iwc, RSSBusiness.class);
		}
		catch (IBOLookupException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String serverName = iwc.getServerURL();
		serverName = serverName.substring(0, serverName.length() - 1);
		SyndFeed feed = null;

		feed = rss.createNewFeed(title, serverName, FEED_DESCRIPTION, "atom_1.0", iwc.getCurrentLocale().toString(), new Timestamp(now.getTime()));

		if (entries != null) {
			feed.setEntries(getFeedEntries(entries));
		}

		try {
			String feedContent = rss.convertFeedToAtomXMLString(feed);
			IWSlideService service = this.getIWSlideService(rssRequest);
			service.uploadFileAndCreateFoldersFromStringAsRoot(PATH_TO_FEED_PARENT_FOLDER, feedFileName, feedContent, RSS_CONTENT_TYPE, true);
			rssFileURIsCacheList.add(feedFileName);
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return PATH_TO_FEED_PARENT_FOLDER + feedFileName;
	}

	private List getFeedEntries(Collection entries) {
		List syndEntries = new ArrayList();
		try {
			List calendarEntries = new ArrayList(entries);
			CalendarEntry calEntry = null;
			for (int i = 0; i < entries.size(); i++) {
				SyndEntry sEntry = new SyndEntryImpl();
				calEntry = (CalendarEntry) calendarEntries.get(i);
				SyndContent scont = new SyndContentImpl();
				String content = "Name: " + calEntry.getName() + " Type: " + calEntry.getEntryTypeName() + " From: " + calEntry.getDate() + " To: " + calEntry.getEndDate();
				scont.setValue(content);
				sEntry.setTitle(calEntry.getName());
				sEntry.setDescription(scont);
				sEntry.setPublishedDate(calEntry.getDate());
				syndEntries.add(sEntry);
			}
		}
		catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return syndEntries;
	}

	protected List getrssFileURIsCacheList() {
		return rssFileURIsCacheList;
	}

	private Timestamp getTimeStampFromString(String dateString) {
		dateString = dateString.replaceAll("-", "");
		try {
			SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMdd");
			Date date = simpleDate.parse(dateString, new ParsePosition(0));
			return new Timestamp(date.getTime());
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String getPeriod(String uri) {
		if (uri.length() == 0) {
			return "";
		}
		String period = uri.substring(uri.indexOf("/") + 1);
		if (period.length() == 0) {
			return "";
		}
		if (period.startsWith("/")) {
			return period.substring(1, period.length() - 1) + "_";
		}
		else {
			return period.substring(0, period.length() - 1) + "_";
		}
	}

	private String getName(String extraURI) {
		return extraURI.substring(0, extraURI.indexOf("/")) + "_";
	}

	private String getTypesString(String extraURI) {
		String events = new String();
		try {
			List eventsList = getTypesList(extraURI);
			for (int i = 0; i < eventsList.size(); i++) {
				events = events.concat(eventsList.get(i) + "_");
			}
		}
		catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return events;
	}

	private List getTypesList(String events) {
		List eventsList = new ArrayList();

		int index;
		if (events.indexOf("+") == -1) {
			eventsList.add(events.substring(0, events.indexOf("/")));
			events = events.substring(events.indexOf("/") + 1, events.length());
		}
		else {
			while (true) {
				index = events.indexOf("+");
				if (index == -1) {
					index = events.indexOf("/");
					eventsList.add(events.substring(0, index));
					events = events.substring(index + 1, events.length());
					break;
				}
				else {
					eventsList.add(events.substring(0, index));
					events = events.substring(index + 1, events.length());
				}
			}
		}
		return eventsList;
	}

	public void clearRssCacheList(String[] parameters) {
		String entryDate = parameters[0];
		String entryEndDate = parameters[1];
		String entryLedger = parameters[2];
		String entryAttendees = parameters[3];
		String entryType = parameters[4];
		String entryRepeat = parameters[5];

		if (entryRepeat.equals("none")) {
			clearPeriodList(entryDate, entryDate);
		}
		else {
			clearPeriodList(entryDate, entryEndDate);
		}
		if (!entryLedger.equals("-1")) {
			rssFileURIsCacheListByLedger.clear();
		}
		if (entryAttendees.length() != 0) {
			rssFileURIsCacheListByGroup.clear();
		}
		if (entryType.length() != 0) {
			rssFileURIsCacheListByEvents.clear();
		}
	}

	public void clearPeriodList(String entryBeginDate, String entryEndDate) {
		String entry = null;
		Timestamp feedBeginDate = null;
		Timestamp feedEndDate = null;
		Timestamp entryBeginDateTs = getTimeStampFromString(entryBeginDate);
		Timestamp entryEndDateTs = getTimeStampFromString(entryEndDate);

		for (int i = 0; i < rssFileURIsCacheListByPeriod.size(); i++) {
			entry = (String) rssFileURIsCacheListByPeriod.get(i);
			if (entry.startsWith("period")) {
				feedBeginDate = getTimeStampFromString(entry.substring(7, 15));
				feedEndDate = getTimeStampFromString(entry.substring(16, 24));
			}
			if (!(entryBeginDateTs.after(feedEndDate) || entryEndDateTs.before(feedBeginDate))) {
				rssFileURIsCacheListByPeriod.remove(i);
			}
		}
	}

}
