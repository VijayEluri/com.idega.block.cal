/*
 * Created on Mar 18, 2004
 */
package com.idega.block.cal.presentation;

import java.util.Collection;
import java.util.Iterator;

import com.idega.block.cal.business.CalBusiness;
import com.idega.block.cal.data.CalendarLedger;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.presentation.StyledIWAdminWindow;
import com.idega.presentation.IWContext;
import com.idega.presentation.Table;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.CloseButton;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.HiddenInput;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextInput;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;

/**
 * Description: <br>
 * Copyright: Idega Software 2004 <br>
 * Company: Idega Software <br>
 * @author <a href="mailto:birna@idega.is">Birna Iris Jonsdottir</a>
 */
public class CreateUserInLedger extends StyledIWAdminWindow{
	private final static String IW_BUNDLE_IDENTIFIER = "com.idega.block.cal";
	
	
	public static String NEW_USER_IN_LEDGER = "user_new_in_ledger_";
	
	//parameterNames
	private static String nameFieldParameterName = "cul_name";
	private static String ssnFieldParameterName = "cul_ssn";
	private static String submitButtonParameterName = "submit";
	private static String submitButtonParameterValue ="save";
	private static String icelandicSSNErrorMsg = "cul_icelandicSSNErrorMsg";
	
	//texts
	private Text nameText;
	private Text ssnText;

	//fields
	private TextInput nameField;
	private TextInput ssnField;
	private SubmitButton submitButton;
	private CloseButton closeButton;
	
	private Table table;
	private Form form;
	
	public CreateUserInLedger() {
		super();
		setHeight(250);
		setWidth(380);
		setScrollbar(false);
		setResizable(true);
	}
	public void initializeTexts() {
		IWContext iwc = IWContext.getInstance();
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		nameText = new Text(iwrb.getLocalizedString(nameFieldParameterName,"Name"));
		ssnText = new Text(iwrb.getLocalizedString(ssnFieldParameterName,"SSN"));
	}
	public void initializeFields() {
		IWContext iwc = IWContext.getInstance();
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		nameField = new TextInput(nameFieldParameterName);		
		ssnField = new TextInput(ssnFieldParameterName);
		ssnField.setAsIcelandicSSNumber(iwrb.getLocalizedString(icelandicSSNErrorMsg,"SSN is not a valid icelandic ssn"));
		
		submitButton = new SubmitButton(iwrb.getLocalizedString("save","Save"),submitButtonParameterName,submitButtonParameterValue);
		//closes the window
		closeButton = new CloseButton(iwrb.getLocalizedString("close","Close"));
	}
	public void lineUp() {
		table = new Table();
		table.setCellspacing(0);
		table.setCellpadding(0);
		table.add(nameText,1,1);
		table.add(nameField,2,1);
		table.add(ssnText,1,2);
		table.add(ssnField,2,2);
		table.setAlignment(2,3,"right");
		table.add(submitButton,2,3);
		table.add(Text.NON_BREAKING_SPACE,2,3);
		table.add(closeButton,2,3);
		form.add(table);
	}
	public void main(IWContext iwc) throws Exception {
		form = new Form();
		initializeTexts();
		initializeFields();
		lineUp();
		

		String ledgerString = iwc.getParameter(LedgerWindow.LEDGER);
		Integer ledgerID = new Integer(ledgerString);
		HiddenInput hi = new HiddenInput(LedgerWindow.LEDGER,ledgerString);
		form.add(hi);
		
		add(form,iwc);
		
		CalendarLedger ledger = null;
		
		String ssn = iwc.getParameter(ssnFieldParameterName);
		String name = iwc.getParameter(nameFieldParameterName);
		
		Collection groups = null;
		boolean isInGroup = false;
		
		String save = iwc.getParameter("submit");
		if(save != null && !save.equals("")){
			User user = null;
			try {
				user = getUserBusiness(iwc).getUser(ssn);	
				ledger = getCalendarBusiness(iwc).getLedger(ledgerID.intValue());
				groups = getUserBusiness(iwc).getUserGroupsDirectlyRelated(user);
				
			}catch (Exception e){
				e.printStackTrace();
			}
			if(groups != null) {
				Iterator groupIter = groups.iterator();
				//go through the groupIDs to see if the user is in the ledgerGroup
				while(groupIter.hasNext()) {
					Group g = (Group) groupIter.next();
					Integer groupID = (Integer) g.getPrimaryKey();
					if(groupID.intValue() == ledger.getGroupID()) {
						isInGroup = true;
					}
				}
			}
			
						
			if(user!=null) {
				//user exists in a group but not the ledgerGroup
				if(isInGroup == false || user.getPrimaryGroup() == null) {
					//TODO: make adding user available in calbusiness
					user.setMetaData(NEW_USER_IN_LEDGER,NEW_USER_IN_LEDGER);//user.getPrimaryKey().toString());
					user.store();
					ledger.addUser(user);	
				}	
				else {
					ledger.addUser(user);
				}
			}
			else {
				try {
					user = getUserBusiness(iwc).createUserByPersonalIDIfDoesNotExist(name,ssn,null,null);
					user.store();
					ledger.addUser(user);
				}catch (Exception e){
					e.printStackTrace();
				}
				
			}
			Link l = new Link();
			l.setWindowToOpen(LedgerWindow.class);
			l.addParameter(LedgerWindow.LEDGER, ledgerString);
			String script = "window.opener." + l.getWindowToOpenCallingScript(iwc);
			setOnLoad(script);
			close();
		}
	}
	public String getBundleIdentifier() {
		return IW_BUNDLE_IDENTIFIER;
	}
	
	public CalBusiness getCalendarBusiness(IWApplicationContext iwc) {
		CalBusiness calBiz = null;
		if (calBiz == null) {
			try {
				calBiz = (CalBusiness) com.idega.business.IBOLookup.getServiceInstance(iwc, CalBusiness.class);
			}
			catch (java.rmi.RemoteException rme) {
				throw new RuntimeException(rme.getMessage());
			}
		}
		return calBiz;
	}
	public UserBusiness getUserBusiness(IWApplicationContext iwc) {
		UserBusiness userBiz = null;
		if (userBiz == null) {
			try {
				userBiz = (UserBusiness) com.idega.business.IBOLookup.getServiceInstance(iwc, UserBusiness.class);
			}
			catch (java.rmi.RemoteException rme) {
				throw new RuntimeException(rme.getMessage());
			}
		}
		return userBiz;
	}

}
