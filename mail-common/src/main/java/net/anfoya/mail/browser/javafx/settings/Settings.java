package net.anfoya.mail.browser.javafx.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.anfoya.java.io.SerializedFile;

@SuppressWarnings("serial")
public class Settings implements Serializable {
	public static final String DOWNLOAD_URL = "https://fishermail.wordpress.com/download/";

	public static final String VERSION_TXT_RESOURCE = "/version.txt";
	public static final String VERSION_TXT_URL = "https://www.dropbox.com/s/tpknt8yxfhnlwhm/version.txt?dl=1";

	private static final String SND_PATH = "/net/anfoya/mail/snd/";
	public static final String MP3_NEW_MAIL = Settings.class.getClass().getResource(SND_PATH + "new_mail.mp3").toExternalForm();
	public static final String MP3_TRASH = Settings.class.getClass().getResource(SND_PATH + "trash.mp3").toExternalForm();

	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String FILENAME = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-settings";

	private static final Settings SETTINGS = new Settings();
	public static Settings getSettings() {
		return SETTINGS;
	}

	private final BooleanProperty showToolbar;
	private final BooleanProperty showExcludeBox;
	private final BooleanProperty archiveOnDrop;
	private final IntegerProperty popupLifetime;
	private final BooleanProperty replyAllDblClick;
	private final StringProperty htmlSignature;
	private final BooleanProperty confirmOnQuit;
	private final BooleanProperty confirmOnSignout;
	private final BooleanProperty mute;

	public Settings() {
		showToolbar = new SimpleBooleanProperty(true);
		showExcludeBox = new SimpleBooleanProperty(false);
		archiveOnDrop = new SimpleBooleanProperty(true);
		popupLifetime = new SimpleIntegerProperty(20);
		replyAllDblClick = new SimpleBooleanProperty(false);
		htmlSignature = new SimpleStringProperty("");
		confirmOnQuit = new SimpleBooleanProperty(true);
		confirmOnSignout = new SimpleBooleanProperty(true);
		mute = new SimpleBooleanProperty(false);
	}

	public void load() {
		final List<Object> list;
		try {
			list = new SerializedFile<List<Object>>(FILENAME).load();
		} catch (final FileNotFoundException e) {
			LOGGER.warn("no settings found {}", FILENAME);
			return;
		} catch (final Exception e) {
			LOGGER.error("loading settings {}", FILENAME, e);
			return;
		}

		final Iterator<Object> i = list.iterator();
		if (i.hasNext()) {
			showToolbar.set((boolean) i.next());
		}
		if (i.hasNext()) {
			showExcludeBox.set((boolean) i.next());
		}
		if (i.hasNext()) {
			archiveOnDrop.set((boolean) i.next());
		}
		if (i.hasNext()) {
			popupLifetime.set((int) i.next());
		}
		if (i.hasNext()) {
			htmlSignature.set((String) i.next());
		}
		if (i.hasNext()) {
			replyAllDblClick.set((Boolean) i.next());
		}
		if (i.hasNext()) {
			confirmOnQuit.set((Boolean) i.next());
		}
		if (i.hasNext()) {
			confirmOnSignout.set((Boolean) i.next());
		}
		if (i.hasNext()) {
			mute.set((Boolean) i.next());
		}
	}

	public void save() {
		final List<Object> list = new ArrayList<Object>();

		list.add(showToolbar.get());
		list.add(showExcludeBox.get());
		list.add(archiveOnDrop.get());
		list.add(popupLifetime.get());
		list.add(htmlSignature.get());
		list.add(replyAllDblClick.get());
		list.add(confirmOnQuit.get());
		list.add(confirmOnSignout.get());
		list.add(mute.get());

		try {
			new SerializedFile<List<Object>>(FILENAME).save(list);
		} catch (final IOException e) {
			LOGGER.error("saving settings {}", FILENAME, e);
		}
	}

	public void reset() {
		new Settings().save();
		load();
	}

	public BooleanProperty showToolbar() {
		return showToolbar;
	}

	public BooleanProperty showExcludeBox() {
		return showExcludeBox;
	}

	public BooleanProperty archiveOnDrop() {
		return archiveOnDrop;
	}

	public IntegerProperty popupLifetime() {
		return popupLifetime;
	}

	public BooleanProperty replyAllDblClick() {
		return replyAllDblClick;
	}

	public StringProperty htmlSignature() {
		return htmlSignature;
	}

	public BooleanProperty confirmOnQuit() {
		return confirmOnQuit;
	}

	public BooleanProperty confirmOnSignout() {
		return confirmOnSignout;
	}

	public BooleanProperty mute() {
		return mute;
	}
}
