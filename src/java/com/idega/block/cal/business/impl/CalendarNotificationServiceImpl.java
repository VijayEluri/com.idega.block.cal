/**
 * @(#)CalendarNotificationServiceImpl.java    1.0.0 16:47:14
 *
 * Idega Software hf. Source Code Licence Agreement x
 *
 * This agreement, made this 10th of February 2006 by and between
 * Idega Software hf., a business formed and operating under laws
 * of Iceland, having its principal place of business in Reykjavik,
 * Iceland, hereinafter after referred to as "Manufacturer" and Agura
 * IT hereinafter referred to as "Licensee".
 * 1.  License Grant: Upon completion of this agreement, the source
 *     code that may be made available according to the documentation for
 *     a particular software product (Software) from Manufacturer
 *     (Source Code) shall be provided to Licensee, provided that
 *     (1) funds have been received for payment of the License for Software and
 *     (2) the appropriate License has been purchased as stated in the
 *     documentation for Software. As used in this License Agreement,
 *     Licensee shall also mean the individual using or installing
 *     the source code together with any individual or entity, including
 *     but not limited to your employer, on whose behalf you are acting
 *     in using or installing the Source Code. By completing this agreement,
 *     Licensee agrees to be bound by the terms and conditions of this Source
 *     Code License Agreement. This Source Code License Agreement shall
 *     be an extension of the Software License Agreement for the associated
 *     product. No additional amendment or modification shall be made
 *     to this Agreement except in writing signed by Licensee and
 *     Manufacturer. This Agreement is effective indefinitely and once
 *     completed, cannot be terminated. Manufacturer hereby grants to
 *     Licensee a non-transferable, worldwide license during the term of
 *     this Agreement to use the Source Code for the associated product
 *     purchased. In the event the Software License Agreement to the
 *     associated product is terminated; (1) Licensee's rights to use
 *     the Source Code are revoked and (2) Licensee shall destroy all
 *     copies of the Source Code including any Source Code used in
 *     Licensee's applications.
 * 2.  License Limitations
 *     2.1 Licensee may not resell, rent, lease or distribute the
 *         Source Code alone, it shall only be distributed as a
 *         compiled component of an application.
 *     2.2 Licensee shall protect and keep secure all Source Code
 *         provided by this this Source Code License Agreement.
 *         All Source Code provided by this Agreement that is used
 *         with an application that is distributed or accessible outside
 *         Licensee's organization (including use from the Internet),
 *         must be protected to the extent that it cannot be easily
 *         extracted or decompiled.
 *     2.3 The Licensee shall not resell, rent, lease or distribute
 *         the products created from the Source Code in any way that
 *         would compete with Idega Software.
 *     2.4 Manufacturer's copyright notices may not be removed from
 *         the Source Code.
 *     2.5 All modifications on the source code by Licencee must
 *         be submitted to or provided to Manufacturer.
 * 3.  Copyright: Manufacturer's source code is copyrighted and contains
 *     proprietary information. Licensee shall not distribute or
 *     reveal the Source Code to anyone other than the software
 *     developers of Licensee's organization. Licensee may be held
 *     legally responsible for any infringement of intellectual property
 *     rights that is caused or encouraged by Licensee's failure to abide
 *     by the terms of this Agreement. Licensee may make copies of the
 *     Source Code provided the copyright and trademark notices are
 *     reproduced in their entirety on the copy. Manufacturer reserves
 *     all rights not specifically granted to Licensee.
 *
 * 4.  Warranty & Risks: Although efforts have been made to assure that the
 *     Source Code is correct, reliable, date compliant, and technically
 *     accurate, the Source Code is licensed to Licensee as is and without
 *     warranties as to performance of merchantability, fitness for a
 *     particular purpose or use, or any other warranties whether
 *     expressed or implied. Licensee's organization and all users
 *     of the source code assume all risks when using it. The manufacturers,
 *     distributors and resellers of the Source Code shall not be liable
 *     for any consequential, incidental, punitive or special damages
 *     arising out of the use of or inability to use the source code or
 *     the provision of or failure to provide support services, even if we
 *     have been advised of the possibility of such damages. In any case,
 *     the entire liability under any provision of this agreement shall be
 *     limited to the greater of the amount actually paid by Licensee for the
 *     Software or 5.00 USD. No returns will be provided for the associated
 *     License that was purchased to become eligible to receive the Source
 *     Code after Licensee receives the source code.
 */
package com.idega.block.cal.business.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.cal.business.CalendarNotificationService;
import com.idega.block.cal.data.CalendarEntry;
import com.idega.block.cal.writer.ICalWriter;
import com.idega.block.calendar.business.GoogleEventService;
import com.idega.block.calendar.data.AttendeeEntity;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.file.data.bean.ICFile;
import com.idega.core.messaging.MessagingSettings;
import com.idega.data.IDOUtil;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.dao.UserDAO;
import com.idega.user.data.bean.Group;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * <p>You can report about problems to:
 * <a href="mailto:martynas@idega.is">Martynas Stakė</a></p>
 *
 * @version 1.0.0 2015 gruod. 28
 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CalendarNotificationServiceImpl extends DefaultSpringBean implements
		CalendarNotificationService {

	private UserBusiness userBusiness;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private GoogleEventService googleEventService;

	@Autowired
	private ICalWriter iCalWriter;

	private IWResourceBundle resourceBundle;

	private IWResourceBundle getResourceBundle() {
		if (this.resourceBundle == null) {
			this.resourceBundle = getResourceBundle(
					getBundle("com.idega.block.cal"));
		}

		return this.resourceBundle;
	}

	private GoogleEventService getGoogleEventService() {
		if (this.googleEventService == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.googleEventService;
	}

	private ICalWriter getICalWriter() {
		if (this.iCalWriter == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.iCalWriter;
	}

	private UserDAO getUserDAO() {
		if (this.userDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.userDAO;
	}

	private UserBusiness getUserBusiness() {
		if (this.userBusiness == null) {
			try {
				this.userBusiness = IBOLookup.getServiceInstance(
						IWMainApplication.getDefaultIWApplicationContext(),
						UserBusiness.class);
			} catch (IBOLookupException e) {
				java.util.logging.Logger.getLogger(getClass().getName()).log(
						Level.WARNING, "Failed to get " + UserBusiness.class +
						" cause of: ", e);
			}
		}

		return this.userBusiness;
	}

	private String localize(String key, String value) {
		return getResourceBundle().getLocalizedString(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notifyUser(com.idega.user.data.bean.User, java.lang.String)
	 */
	@Override
	public void notifyUser(User receiver, File attachment, String link) {
		Set<String> receiverEmails = getUserDAO().getEmailAddresses(receiver);
		if (ListUtil.isEmpty(receiverEmails)) {
			getLogger().warning(receiver + " (personal ID: " + receiver.getPersonalID() + ") does not have email, can not send link to calendar");
			return;
		}

		IWMainApplicationSettings settings = getSettings();
		String from = settings.getProperty(MessagingSettings.PROP_MESSAGEBOX_FROM_ADDRESS);
		String testReceiver = settings.getProperty("calendar_test_receiver");
		for (String receiverEmail: receiverEmails) {
			try {
				SendMail.send(
						from,
						StringUtil.isEmpty(testReceiver) ? receiverEmail : testReceiver,
						null,
						null,
						null,
						localize("calendar_update", "Calendar update"),
						localize("your_calendar_has_been_updated", "Your calendar has been updated: ") + link != null ? link : "",
						true,
						false,
						attachment
				);
			} catch (MessagingException e) {
				java.util.logging.Logger.getLogger(getClass().getName()).log(
						Level.WARNING,
								"Failed to send message to email: " + receiverEmail +
								", cause of: " , e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notify(java.util.Collection, java.util.Collection)
	 */
	@Override
	public void notify(
			Collection<User> receivers,
			Collection<CalendarEntry> entries
	) {
		if (!ListUtil.isEmpty(receivers)) {
			/*
			 * Convert events
			 */
			String name = entries.iterator().next().getName();
			name = name.replace(CoreConstants.SPACE, CoreConstants.UNDER);
			String filename = name + CoreConstants.UNDER + LocalDate.now() + CoreConstants.UNDER + System.currentTimeMillis() ;

			File attachment = null;
			try {
				attachment = File.createTempFile(filename, ".ics");
				OutputStream fileOutputStream = new FileOutputStream(attachment);
				getICalWriter().write(entries, fileOutputStream);
				fileOutputStream.close();
			} catch (IOException e) {
				getLogger().log(Level.WARNING, "Failed to create calendar file, cause of:", e);
			};

			String serverURL = null;
			try {
				ICFile databaseFile = getICalWriter().writeToDatabase(entries, filename + ".ics");
				String uuid = databaseFile.getMetaData("uuid");
				if (StringUtil.isEmpty(uuid)) {
					databaseFile = getICalWriter().getFileDAO().findById(databaseFile.getId());
					uuid = databaseFile.getMetaData("uuid");
				}

				if (databaseFile != null) {
					String path = "/file?id=" + uuid;

					IWContext iwc = CoreUtil.getIWContext();
					if (iwc != null) {
						serverURL = CoreUtil.getServerURL(iwc.getRequest());
					} else {
						serverURL = getApplication().getIWApplicationContext().getDomain().getURL();
					}

					if (serverURL.endsWith(CoreConstants.SLASH)) {
						path = path.substring(1);
					}

					serverURL = serverURL + path;
				}
			} catch (SQLException e) {
				getLogger().log(Level.WARNING, "Failed to create calendar file in database, cause of:", e);
			}

			/*
			 * Send e-mails
			 */
			for (User receiver : receivers) {
				notifyUser(receiver, attachment, serverURL);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notifyInvitees(java.util.Collection, java.util.Collection)
	 */
	@Override
	public void notifyInvitees(
			Collection<AttendeeEntity> receivers,
			Collection<CalendarEntry> entries) {
		List<User> users = new ArrayList<User>();

		if (!ListUtil.isEmpty(receivers)) {
			for (AttendeeEntity receiver: receivers) {
				users.add(receiver.getInvitee());
			}
		}

		notify(users, entries);
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notifyUsers(java.util.Collection, java.util.Collection)
	 */
	@Override
	public void notifyUsers(
			Collection<Integer> receivers,
			Collection<CalendarEntry> entries) {
		notify(getUserDAO().findAll(receivers), entries);
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notifyUser(java.lang.Integer, java.util.Collection)
	 */
	@Override
	public void notifyUser(Integer receiverId, Collection<CalendarEntry> entries) {
		if (receiverId != null) {
			notifyUsers(java.util.Arrays.asList(receiverId), entries);
		}
	}

	/* (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notify(com.idega.user.data.bean.User, java.util.Collection)
	 */
	@Override
	public void notifyUser(User receiver, Collection<CalendarEntry> entries) {
		if (receiver != null) {
			notifyUsers(java.util.Arrays.asList(receiver.getId()), entries);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notify(java.lang.Integer, java.util.Collection)
	 */
	@Override
	public void notifyGroup(
			Integer receiversGroupId,
			Collection<CalendarEntry> entries) {
		if (receiversGroupId != null) {
			Collection<com.idega.user.data.User> users = null;
			try {
				users = getUserBusiness().getUsersInGroup(receiversGroupId);
			} catch (RemoteException e) {
				java.util.logging.Logger.getLogger(getClass().getName()).log(
						Level.WARNING,
						"Failed to get users for group by id " + receiversGroupId);
			}

			notifyUsers(IDOUtil.getInstance().getIntegerPrimaryKeys(users), entries);
		}
	}

	/* (non-Javadoc)
	 * @see com.idega.block.cal.business.CalendarNotificationService#notify(com.idega.user.data.bean.Group, java.util.Collection)
	 */
	@Override
	public void notifyGroup(Group receivers, Collection<CalendarEntry> entries) {
		if (receivers != null) {
			notifyGroup(receivers.getID(), entries);
		}
	}
}
