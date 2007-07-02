package com.idega.block.cal.business;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.myfaces.custom.schedule.HtmlSchedule;
import org.apache.myfaces.custom.schedule.model.DefaultScheduleEntry;
import org.apache.myfaces.custom.schedule.model.ScheduleModel;
import org.apache.myfaces.custom.schedule.model.SimpleScheduleModel;
import org.jdom.Document;

import com.idega.business.IBOSessionBean;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.util.CoreUtil;

public class ScheduleSessionBean extends IBOSessionBean implements ScheduleSession{

//	private HtmlSchedule schedule = null;
	private Map<String, HtmlSchedule> htmlSchedules = new HashMap<String, HtmlSchedule>();
	private Map<String, Integer> dateMode = new HashMap<String, Integer>();
	private SimpleDateFormat simpleDate = null;
	private List<CalScheduleEntry> entriesInSchedule = null;
	
//	private int dateMode = -1;
	private static final int DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;
	private static final int WEEK_IN_MILLISECONDS = DAY_IN_MILLISECONDS * 7;
	private static final int MONTH_IN_MILLISECONDS = DAY_IN_MILLISECONDS * 30;	

	public Document changeModeToDay(String id){
		dateMode.put(id, ScheduleModel.DAY);
		htmlSchedules.get(id).getModel().setMode(ScheduleModel.DAY);
		htmlSchedules.get(id).getModel().refresh();
		return getScheduleDOM(null, id);
	}
	
	public List<CalScheduleEntry> changeModeToDayAndDisplayEntriesAsList(String id){
		dateMode.put(id, ScheduleModel.DAY);
		htmlSchedules.get(id).getModel().setMode(ScheduleModel.DAY);
		htmlSchedules.get(id).getModel().refresh();
		Calendar calendar = Calendar.getInstance();
		Calendar currentDate = Calendar.getInstance();
		currentDate.setTime(htmlSchedules.get(id).getModel().getSelectedDate());
		
		List<CalScheduleEntry> entriesOfSelectedDay = new ArrayList<CalScheduleEntry>();
		
		if (simpleDate == null){
			simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"); 
		}
		for (int i = 0; i < entriesInSchedule.size(); i++) {
			String entryDateString = entriesInSchedule.get(i).getEntryDate();
			Date entryDate = null;
			try {
				entryDate = simpleDate.parse(entryDateString);
				calendar.setTime(entryDate);
				if((calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)) && 
					(calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH)) && 
					(calendar.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH))){
						entriesOfSelectedDay.add(entriesInSchedule.get(i));
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return entriesOfSelectedDay;
	}
	
	public List<CalScheduleEntry> changeModeToMonthAndDisplayEntriesAsList(String id){
		dateMode.put(id, ScheduleModel.MONTH);
		htmlSchedules.get(id).getModel().setMode(ScheduleModel.MONTH);
		htmlSchedules.get(id).getModel().refresh();
		Calendar calendar = Calendar.getInstance();
		Calendar currentDate = Calendar.getInstance();
		currentDate.setTime(htmlSchedules.get(id).getModel().getSelectedDate());
		
		List<CalScheduleEntry> entriesOfSelectedMonth = new ArrayList<CalScheduleEntry>();
		
		if (simpleDate == null){
			simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"); 
		}
		for (int i = 0; i < entriesInSchedule.size(); i++) {
			String entryDateString = entriesInSchedule.get(i).getEntryDate();
			Date entryDate = null;
			try {
				entryDate = simpleDate.parse(entryDateString);
				calendar.setTime(entryDate);
				if((calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)) && 
					(calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH))){
					entriesOfSelectedMonth.add(entriesInSchedule.get(i));
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return entriesOfSelectedMonth;
	}	

	public Document changeModeToWorkweek(String id){
		dateMode.put(id, ScheduleModel.WORKWEEK);
		ScheduleModel scheduleModel = htmlSchedules.get(id).getModel();
		scheduleModel.setMode(ScheduleModel.WORKWEEK);
		scheduleModel.refresh();
		htmlSchedules.get(id).setModel(scheduleModel);
		htmlSchedules.get(id).getModel().refresh();
		return getScheduleDOM(null, id);
	}

	public List<CalScheduleEntry> changeModeToWeekAndDisplayEntriesAsList(String id){
		return getEntriesForCurrentWeek(id, false);
	}	
	
	public List<CalScheduleEntry> changeModeToWorkweekAndDisplayEntriesAsList(String id){
		return getEntriesForCurrentWeek(id, true);
//		dateMode.put(id, ScheduleModel.WORKWEEK);
//		Calendar calendar = Calendar.getInstance();
//		
//		Calendar currentWeekMonday = Calendar.getInstance();
//		currentWeekMonday.setTime(htmlSchedules.get(id).getModel().getSelectedDate());
//		currentWeekMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
//		currentWeekMonday.set(Calendar.HOUR_OF_DAY, 0);
//		currentWeekMonday.set(Calendar.MINUTE, 0);
//		currentWeekMonday.set(Calendar.SECOND, 0);
//		currentWeekMonday.set(Calendar.MILLISECOND, 0);
//		
//		Calendar currentWeekSaturday = Calendar.getInstance();
//		currentWeekSaturday.setTime(htmlSchedules.get(id).getModel().getSelectedDate());
//		currentWeekSaturday.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
//		currentWeekSaturday.set(Calendar.HOUR_OF_DAY, 0);
//		currentWeekSaturday.set(Calendar.MINUTE, 0);
//		currentWeekSaturday.set(Calendar.SECOND, 0);
//		currentWeekSaturday.set(Calendar.MILLISECOND, 0);			
//		
//		
//		List<CalScheduleEntry> entriesOfSelectedWorkweek = new ArrayList<CalScheduleEntry>();
//				
//		if (simpleDate == null){
//			simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"); 
//		}
//		for (int i = 0; i < entriesInSchedule.size(); i++) {
//			String entryDateString = entriesInSchedule.get(i).getEntryDate();
//			Date entryDate = null;
//			try {
//				entryDate = simpleDate.parse(entryDateString);
//				calendar.setTime(entryDate);
//				if(currentWeekMonday.before(calendar) && calendar.before(currentWeekSaturday)){
//					entriesOfSelectedWorkweek.add(entriesInSchedule.get(i));
//				}
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//				
//		return entriesOfSelectedWorkweek;
	}	
	
	public List<CalScheduleEntry> getEntriesForCurrentWeek(String id, boolean workdaysOnly){
		Calendar calendar = Calendar.getInstance();
		
		Calendar beginingOfTheWeek = Calendar.getInstance();
		beginingOfTheWeek.setTime(htmlSchedules.get(id).getModel().getSelectedDate());
		beginingOfTheWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		beginingOfTheWeek.set(Calendar.HOUR_OF_DAY, 0);
		beginingOfTheWeek.set(Calendar.MINUTE, 0);
		beginingOfTheWeek.set(Calendar.SECOND, 0);
		beginingOfTheWeek.set(Calendar.MILLISECOND, 0);
		
		Calendar endOfTheWeek = Calendar.getInstance();
		endOfTheWeek.setTime(htmlSchedules.get(id).getModel().getSelectedDate());

		if (workdaysOnly){
			dateMode.put(id, ScheduleModel.WORKWEEK);
			htmlSchedules.get(id).getModel().setMode(ScheduleModel.WORKWEEK);
			htmlSchedules.get(id).getModel().refresh();
			endOfTheWeek.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		}
		else{
			dateMode.put(id, ScheduleModel.WEEK);
			htmlSchedules.get(id).getModel().setMode(ScheduleModel.WEEK);
			htmlSchedules.get(id).getModel().refresh();
			endOfTheWeek.setTimeInMillis(endOfTheWeek.getTimeInMillis()+WEEK_IN_MILLISECONDS);
			endOfTheWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
//			date.setTime(date.getTime()+WEEK_IN_MILLISECONDS);
		}
		endOfTheWeek.set(Calendar.HOUR_OF_DAY, 0);
		endOfTheWeek.set(Calendar.MINUTE, 0);
		endOfTheWeek.set(Calendar.SECOND, 0);
		endOfTheWeek.set(Calendar.MILLISECOND, 0);			
		
		
		List<CalScheduleEntry> entriesOfSelectedWorkweek = new ArrayList<CalScheduleEntry>();
				
		if (simpleDate == null){
			simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S"); 
		}
		if(entriesInSchedule == null){
			entriesInSchedule = new ArrayList<CalScheduleEntry>();
		} 
		for (int i = 0; i < entriesInSchedule.size(); i++) {
			String entryDateString = entriesInSchedule.get(i).getEntryDate();
			Date entryDate = null;
			try {
				entryDate = simpleDate.parse(entryDateString);
				calendar.setTime(entryDate);
				if(beginingOfTheWeek.before(calendar) && calendar.before(endOfTheWeek)){
					entriesOfSelectedWorkweek.add(entriesInSchedule.get(i));
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		return entriesOfSelectedWorkweek;		
	}
	
	public Document changeModeToWeek(String id){
		dateMode.put(id, ScheduleModel.WEEK);
		htmlSchedules.get(id).getModel().setMode(ScheduleModel.WEEK);
		htmlSchedules.get(id).getModel().refresh();
//		return getSchedule(id, null, true);
		return getScheduleDOM(null, id);
	}

	public Document changeModeToMonth(String id){
		dateMode.put(id, ScheduleModel.MONTH);
		htmlSchedules.get(id).getModel().setMode(ScheduleModel.MONTH);
		htmlSchedules.get(id).getModel().refresh();
//		return getSchedule(id, null, true);
		return getScheduleDOM(null, id);
	}

	public Document getNext(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		switch (model.getMode()){
			case ScheduleModel.DAY: setSelectedDateToNextDay(id); break;
			case ScheduleModel.WORKWEEK: setSelectedDateToNextWeek(id); break;
			case ScheduleModel.WEEK: setSelectedDateToNextWeek(id); break;
			case ScheduleModel.MONTH: setSelectedDateToNextMonth(id); break;
		}
//		return getSchedule(id, null, true);
		return getScheduleDOM(null, id);
	}	

	public List<CalScheduleEntry> getNextAsList(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		switch (model.getMode()){
			case ScheduleModel.DAY: 
//System.out.println("day getNextAsList");				
				setSelectedDateToNextDay(id);
				return changeModeToDayAndDisplayEntriesAsList(id);
//				break;
			case ScheduleModel.WORKWEEK: 
//System.out.println("workweek getNextAsList");				
				setSelectedDateToNextWeek(id);
				return changeModeToWorkweekAndDisplayEntriesAsList(id);
//				break;
			case ScheduleModel.WEEK: 
//System.out.println("week getNextAsList");				
				setSelectedDateToNextWeek(id);
				return changeModeToWeekAndDisplayEntriesAsList(id);
//				break;
			case ScheduleModel.MONTH: 
//System.out.println("month getNextAsList");				
				setSelectedDateToNextMonth(id); 
				return changeModeToMonthAndDisplayEntriesAsList(id);
//				break;
		}
		return null;
	}	
	
	public Document getPrevious(String id){		
		ScheduleModel model = htmlSchedules.get(id).getModel();
		switch (model.getMode()){
			case ScheduleModel.DAY: setSelectedDateToPreviousDay(id); break;
			case ScheduleModel.WORKWEEK: setSelectedDateToPreviousWeek(id); break;
			case ScheduleModel.WEEK: setSelectedDateToPreviousWeek(id); break;
			case ScheduleModel.MONTH: setSelectedDateToPreviousMonth(id); break;
		}
//		return getSchedule(id, null, true);
		return getScheduleDOM(null, id);
	}

	public List<CalScheduleEntry> getPreviousAsList(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		switch (model.getMode()){
			case ScheduleModel.DAY: 
				setSelectedDateToPreviousDay(id);
				return changeModeToDayAndDisplayEntriesAsList(id);
			case ScheduleModel.WORKWEEK: 
				setSelectedDateToPreviousWeek(id);
				return changeModeToWorkweekAndDisplayEntriesAsList(id);
			case ScheduleModel.WEEK: 
				setSelectedDateToPreviousWeek(id);
				return changeModeToWeekAndDisplayEntriesAsList(id);
			case ScheduleModel.MONTH: 
				setSelectedDateToPreviousMonth(id); 
				return changeModeToMonthAndDisplayEntriesAsList(id);
		}
		return null;
	}		
	
	private void setSelectedDateToNextDay(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()+DAY_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}
	
	private void setSelectedDateToNextWeek(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()+WEEK_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}
	
	private void setSelectedDateToNextMonth(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()-MONTH_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}

	private void setSelectedDateToPreviousDay(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()-DAY_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}
	
	private void setSelectedDateToPreviousWeek(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()-WEEK_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}
	
	private void setSelectedDateToPreviousMonth(String id){
		ScheduleModel model = htmlSchedules.get(id).getModel();
		Date date = model.getSelectedDate();
		date.setTime(date.getTime()+MONTH_IN_MILLISECONDS);
		model.setSelectedDate(date);
		htmlSchedules.get(id).setModel(model);
	}
	public Document getSchedule(String id, List<CalScheduleEntry> result, boolean usePreviousEntries){
		IWContext iwc = CoreUtil.getIWContext();
		BuilderService service = null;
		try { 
			service = BuilderServiceFactory.getBuilderService(iwc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if (htmlSchedules.get(id) == null){
			htmlSchedules.put(id, new HtmlSchedule());
		}
		ScheduleModel scheduleModel = null;
		
		
		if(usePreviousEntries){
			scheduleModel = htmlSchedules.get(id).getModel();
		}
		else{ 
			scheduleModel = new SimpleScheduleModel();
			
			for (int i = 0; i < result.size(); i++) {
				CalScheduleEntry calEntry = result.get(i);
				DefaultScheduleEntry defaultScheduleEntry = new DefaultScheduleEntry();
				if (simpleDate == null){
					simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				}
				defaultScheduleEntry.setStartTime(simpleDate.parse(calEntry.getEntryDate(), new ParsePosition(0)));
				defaultScheduleEntry.setEndTime(simpleDate.parse(calEntry.getEntryEndDate(), new ParsePosition(0)));
				defaultScheduleEntry.setTitle(calEntry.getEntryName());
//				ScheduleEntry scheduleEntry = defaultScheduleEntry;
//				scheduleModel.addEntry(scheduleEntry);
				scheduleModel.addEntry(defaultScheduleEntry);
			}			
		}
		if(dateMode.get(id) == null){
			scheduleModel.setMode(ScheduleModel.MONTH);
			dateMode.put(id, ScheduleModel.MONTH);
			htmlSchedules.get(id).getModel().setMode(ScheduleModel.MONTH);
		}
		else{
			scheduleModel.setMode(dateMode.get(id));
		}
		scheduleModel.refresh();
		htmlSchedules.get(id).setModel(scheduleModel);
		Layer scheduleLayer = new Layer();
		scheduleLayer.add(htmlSchedules.get(id));
		return service.getRenderedComponent(iwc, scheduleLayer, false);
	}
	
	public Document getScheduleDOM(List<CalScheduleEntry> entries, String id){
		
		entriesInSchedule = entries;
		
		IWContext iwc = CoreUtil.getIWContext();
		BuilderService service = null;
		try { 
			service = BuilderServiceFactory.getBuilderService(iwc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if (htmlSchedules.get(id) == null){
			htmlSchedules.put(id, new HtmlSchedule());
		}
		ScheduleModel scheduleModel = null;
		
		if(entries == null){
			scheduleModel = htmlSchedules.get(id).getModel();
		}
		else{ 
			scheduleModel = new SimpleScheduleModel();
			
			for (int i = 0; i < entries.size(); i++) {
				CalScheduleEntry entry = entries.get(i);
//				DefaultScheduleEntry scheduleEntry = new DefaultScheduleEntry();
				DefaultScheduleEntry defaultScheduleEntry = new DefaultScheduleEntry();
				if (simpleDate == null){
					simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				}
				if (entry.getEntryDate() != null)
					defaultScheduleEntry.setStartTime(simpleDate.parse(entry.getEntryDate(), new ParsePosition(0)));
				if (entry.getEntryEndDate() != null)
					defaultScheduleEntry.setEndTime(simpleDate.parse(entry.getEntryEndDate(), new ParsePosition(0)));
				if (entry.getEntryName() != null)
					defaultScheduleEntry.setTitle(entry.getEntryName());
//				ScheduleEntry scheduleEntry = defaultScheduleEntry;
//				scheduleModel.addEntry(scheduleEntry);
				scheduleModel.addEntry(defaultScheduleEntry);
				
			}			
		}
		if(dateMode.get(id) == null){
			scheduleModel.setMode(ScheduleModel.DAY);
			dateMode.put(id, ScheduleModel.DAY);
		}
		else{
			scheduleModel.setMode(dateMode.get(id));
		}
		scheduleModel.refresh();
		htmlSchedules.get(id).setModel(scheduleModel);
		htmlSchedules.get(id).setReadonly(true);
		Layer scheduleLayer = new Layer();
//		HtmlSchedule scheduleToLayer = new HtmlSchedule();
//		scheduleToLayer = htmlSchedules.get(id);
//		ScheduleModel scheduleToLayerModel = scheduleToLayer.getModel();
		
		scheduleLayer.add(htmlSchedules.get(id));
		return service.getRenderedComponent(iwc, scheduleLayer, false);
//		return service.getRenderedComponent(iwc, htmlSchedules.get(id), false);
	}
	
	public int initializeSchedule(List<CalScheduleEntry> entries, String id){
//		System.out.println("initializeSchedule");
		entriesInSchedule = entries;
		if (htmlSchedules.get(id) == null){
			htmlSchedules.put(id, new HtmlSchedule());
		}
		ScheduleModel scheduleModel = null;
		
		if(entries == null){
			scheduleModel = htmlSchedules.get(id).getModel();
		}
		else{ 
			scheduleModel = new SimpleScheduleModel();
			
			for (int i = 0; i < entries.size(); i++) {
				CalScheduleEntry entry = entries.get(i);
//				DefaultScheduleEntry scheduleEntry = new DefaultScheduleEntry();
				DefaultScheduleEntry defaultScheduleEntry = new DefaultScheduleEntry();
				if (simpleDate == null){
					simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				}
				if (entry.getEntryDate() != null)
					defaultScheduleEntry.setStartTime(simpleDate.parse(entry.getEntryDate(), new ParsePosition(0)));
				if (entry.getEntryEndDate() != null)
					defaultScheduleEntry.setEndTime(simpleDate.parse(entry.getEntryEndDate(), new ParsePosition(0)));
				if (entry.getEntryName() != null)
					defaultScheduleEntry.setTitle(entry.getEntryName());
//				ScheduleEntry scheduleEntry = defaultScheduleEntry;
//				scheduleModel.addEntry(scheduleEntry);
				scheduleModel.addEntry(defaultScheduleEntry);
				
			}			
		}
		if(dateMode.get(id) == null){
			scheduleModel.setMode(ScheduleModel.DAY);
			dateMode.put(id, ScheduleModel.DAY);
		}
		else{
			scheduleModel.setMode(dateMode.get(id));
		}
		scheduleModel.refresh();
		htmlSchedules.get(id).setModel(scheduleModel);		
		return 0;
	}
	
}