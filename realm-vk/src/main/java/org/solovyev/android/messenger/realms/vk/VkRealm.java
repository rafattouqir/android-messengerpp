package org.solovyev.android.messenger.realms.vk;

import android.app.Application;
import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import org.solovyev.android.http.ImageLoader;
import org.solovyev.android.messenger.App;
import org.solovyev.android.messenger.accounts.Account;
import org.solovyev.android.messenger.accounts.AccountBuilder;
import org.solovyev.android.messenger.accounts.AccountState;
import org.solovyev.android.messenger.icons.HttpRealmIconService;
import org.solovyev.android.messenger.icons.RealmIconService;
import org.solovyev.android.messenger.notifications.Notification;
import org.solovyev.android.messenger.notifications.NotificationService;
import org.solovyev.android.messenger.notifications.Notifications;
import org.solovyev.android.messenger.realms.AbstractRealm;
import org.solovyev.android.messenger.realms.vk.http.VkResponseErrorException;
import org.solovyev.android.messenger.users.Gender;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.properties.AProperty;
import org.solovyev.common.msg.MessageType;
import org.solovyev.common.security.Cipherer;
import org.solovyev.common.security.CiphererException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.solovyev.android.messenger.notifications.Notifications.newNotification;
import static org.solovyev.android.messenger.notifications.Notifications.newOpenAccountConfSolution;
import static org.solovyev.android.properties.Properties.newProperty;

/**
 * User: serso
 * Date: 8/12/12
 * Time: 10:34 PM
 */
@Singleton
public class VkRealm extends AbstractRealm<VkAccountConfiguration> {

    /*
	**********************************************************************
    *
    *                           CONSTANTS
    *
    **********************************************************************
    */

	@Nonnull
	private static final String REALM_ID = "vk";

    /*
    **********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */

	@Inject
	@Nonnull
	private Application context;

	@Inject
	@Nonnull
	private ImageLoader imageLoader;

	@Inject
	@Nonnull
	private NotificationService notificationService;

    /*
    **********************************************************************
    *
    *                           FIELDS
    *
    **********************************************************************
    */

	@Nonnull
	private final HttpRealmIconService.UrlGetter iconUrlGetter = new VkIconUrlGetter();

	@Nonnull
	private final HttpRealmIconService.UrlGetter photoUrlGetter = new VkPhotoUrlGetter();

	/*@Nonnull*/
	private volatile HttpRealmIconService iconService;

    /*
    **********************************************************************
    *
    *                           CONSTRUCTORS
    *
    **********************************************************************
    */

	public VkRealm() {
		super(REALM_ID, R.string.mpp_vk_realm_name, R.drawable.mpp_vk_icon, VkAccountConfigurationFragment.class, VkAccountConfiguration.class, true, null);
	}

    /*
    **********************************************************************
    *
    *                           METHODS
    *
    **********************************************************************
    */

	@Nonnull
	@Override
	public Account<VkAccountConfiguration> newAccount(@Nonnull String accountId, @Nonnull User user, @Nonnull VkAccountConfiguration configuration, @Nonnull AccountState state) {
		return new VkAccount(accountId, this, user, configuration, state);
	}

	@Nonnull
	@Override
	public AccountBuilder newAccountBuilder(@Nonnull VkAccountConfiguration configuration, @Nullable Account editedAccount) {
		return new VkAccountBuilder(this, (VkAccount) editedAccount, configuration);
	}

	@Nonnull
	@Override
	public List<AProperty> getUserDisplayProperties(@Nonnull User user, @Nonnull Context context) {
		final List<AProperty> result = new ArrayList<AProperty>(user.getPropertiesCollection().size());

		for (AProperty property : user.getPropertiesCollection()) {
			final String name = property.getName();
			if (name.equals(User.PROPERTY_NICKNAME)) {
				addUserProperty(context, result, R.string.mpp_nickname, property.getValue());
			} else if (name.equals(User.PROPERTY_SEX)) {
				result.add(newProperty(context.getString(R.string.mpp_sex), context.getString(Gender.valueOf(property.getValue()).getCaptionResId())));
			} else if (name.equals("bdate")) {
				final String birthDate = formatBirthDate(property.getValue());
				if (birthDate != null) {
					result.add(newProperty(context.getString(R.string.mpp_birth_date), birthDate));
				}
			}

		}

		return result;
	}

	private String formatBirthDate(String value) {
		int dateParts = 1;
		for (int i = 0; i < value.length(); i++) {
			if(value.charAt(i) == '.') {
				dateParts++;
			}
		}


		if(dateParts > 1) {
			final SimpleDateFormat dt;
			if(dateParts > 2) {
				dt = new SimpleDateFormat("dd.MM.yyyy");
			} else {
				dt = new SimpleDateFormat("dd.MM");
			}
			try {
				final DateFormat df;
				if (dateParts > 2) {
					df = SimpleDateFormat.getDateInstance();
				} else {
					df = new SimpleDateFormat("dd.MM");
				}
				return df.format(dt.parse(value));
			} catch (ParseException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public void init(@Nonnull Context context) {
		super.init(context);
	}

	@Nonnull
	@Override
	public synchronized RealmIconService getRealmIconService() {
		if (iconService == null) {
			iconService = new HttpRealmIconService(context, imageLoader, R.drawable.mpp_icon_user, R.drawable.mpp_icon_users, iconUrlGetter, photoUrlGetter);
		}
		return iconService;
	}

	@Nullable
	@Override
	public Cipherer<VkAccountConfiguration, VkAccountConfiguration> getCipherer() {
		return new VkRealmConfigurationCipherer(App.getSecurityService().getStringSecurityService().getCipherer());
	}

	@Override
	public boolean handleException(@Nonnull Throwable e, @Nonnull Account account) {
		boolean handled = super.handleException(e, account);
		if (!handled) {
			if (e instanceof VkResponseErrorException) {
				final VkResponseErrorException cause = (VkResponseErrorException) e;
				if ("5".equals(cause.getError().getErrorId())) {
					notificationService.add(newNotification(R.string.mpp_vk_notification_auth_token_expired, MessageType.error).solvedBy(newOpenAccountConfSolution(account)));
				} else {
					notificationService.add(newVkNotification(cause));
				}

				handled = true;
			}
		}
		return handled;
	}

	@Nonnull
	private static Notification newVkNotification(@Nonnull VkResponseErrorException e) {
		return Notifications.newNotification(R.string.mpp_vk_notification_error, MessageType.error, e.getError().getErrorDescription()).causedBy(e);
	}

	/*
    **********************************************************************
    *
    *                           STATIC
    *
    **********************************************************************
    */

	private static final class VkPhotoUrlGetter implements HttpRealmIconService.UrlGetter {

		@Nullable
		@Override
		public String getUrl(@Nonnull User user) {
			String result = user.getPropertyValueByName("photoRec");

			if (result == null) {
				result = user.getPropertyValueByName("photoBig");
			}

			if (result == null) {
				result = user.getPropertyValueByName("photo");
			}

			return result;
		}
	}

	private static final class VkIconUrlGetter implements HttpRealmIconService.UrlGetter {

		@Nullable
		@Override
		public String getUrl(@Nonnull User user) {
			String result = user.getPropertyValueByName("photo");

			if (result == null) {
				result = user.getPropertyValueByName("photoRec");
			}

			if (result == null) {
				result = user.getPropertyValueByName("photoBig");
			}

			return result;
		}
	}

	private static class VkRealmConfigurationCipherer implements Cipherer<VkAccountConfiguration, VkAccountConfiguration> {

		@Nonnull
		private final Cipherer<String, String> stringCipherer;

		private VkRealmConfigurationCipherer(@Nonnull Cipherer<String, String> stringCipherer) {
			this.stringCipherer = stringCipherer;
		}

		@Nonnull
		public VkAccountConfiguration encrypt(@Nonnull SecretKey secret, @Nonnull VkAccountConfiguration decrypted) throws CiphererException {
			final VkAccountConfiguration encrypted = decrypted.clone();
			encrypted.setAccessParameters(stringCipherer.encrypt(secret, decrypted.getAccessToken()), decrypted.getUserId());
			return encrypted;
		}

		@Nonnull
		public VkAccountConfiguration decrypt(@Nonnull SecretKey secret, @Nonnull VkAccountConfiguration encrypted) throws CiphererException {
			final VkAccountConfiguration decrypted = encrypted.clone();
			decrypted.setAccessParameters(stringCipherer.decrypt(secret, encrypted.getAccessToken()), encrypted.getUserId());
			return decrypted;
		}
	}

	private static class AuthTokenExpiredSolver implements Runnable {
		@Override
		public void run() {

		}
	}
}
