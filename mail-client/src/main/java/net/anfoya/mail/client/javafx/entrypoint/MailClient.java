package net.anfoya.mail.client.javafx.entrypoint;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.Notification.Notifier;
import net.anfoya.mail.browser.javafx.MailBrowser;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.settings.VersionChecker;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
import net.anfoya.mail.client.App;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.Message;

public class MailClient extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

	private GmailService gmail;
	private Stage stage;

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {
		Settings.getSettings().load();
		initGmail();
	}

	@Override
	public void start(final Stage stage) throws Exception {
		this.stage = stage;

		stage.setOnCloseRequest(e -> confirmClose(e));

		stage.initStyle(StageStyle.UNIFIED);
		initMacOs();

		showBrowser();

		initNotifier();
		checkVersion();
	}

	private void confirmClose(WindowEvent e) {
		if (Settings.getSettings().confirmOnQuit().get()) {
			final CheckBox checkBox = new CheckBox("don't ask for confirmation");
			checkBox.selectedProperty().addListener((ov, o, n) -> {
				Settings.getSettings().confirmOnQuit().set(!n);
				Settings.getSettings().save();
			});

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("FisherMail");
			alert.setHeaderText("closing this window will stop FisherMail\ryou will no longer receive new mail notification");
			alert.getDialogPane().contentProperty().set(checkBox);
			alert.showAndWait()
				.filter(response -> response == ButtonType.CANCEL)
				.ifPresent(response -> e.consume());
		}
	}

	private void signout() {
		boolean signout = false;
		if (Settings.getSettings().confirmOnSignout().get()) {
			final CheckBox checkBox = new CheckBox("don't ask for confirmation");
			checkBox.selectedProperty().addListener((ov, o, n) -> {
				Settings.getSettings().confirmOnSignout().set(!n);
				Settings.getSettings().save();
			});

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("FisherMail");
			alert.setHeaderText("sign-out from your e-mail account\rthis will close the mail browser");
			alert.getDialogPane().contentProperty().set(checkBox);
			final Optional<ButtonType> response = alert.showAndWait();
			signout = response.isPresent() && response.get() == ButtonType.OK;
		} else {
			signout = true;
		}
		if (signout) {
			stage.hide();
			gmail.disconnect();
			initGmail();
			showBrowser();
		}
	}

	private void initGmail() {
		if (gmail == null) {
			gmail = new GmailService();
		}
		try {
			gmail.connect(App.MAIL_CLIENT);
		} catch (final MailException e) {
			LOGGER.error("login failed", e);
		}
	}

	private void checkVersion() {
		final VersionChecker checker = new VersionChecker();
		if (checker.isDisconnected()) {
			return;
		}
		final Task<Boolean> isLatestTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				return checker.isLastVersion();
			}
		};
		isLatestTask.setOnSucceeded(e -> {
			if (!(boolean)e.getSource().getValue()) {
				Notifier.INSTANCE.notifyInfo(
						"FisherMail " + checker.getLastestVesion()
						, "available at " + Settings.URL
						, v -> {
							UrlHelper.open("http://" + Settings.URL);
							return null;
						});
			}
		});
		isLatestTask.setOnFailed(e -> LOGGER.error("getting latest version info", e));
		ThreadPool.getInstance().submitLow(isLatestTask, "checking version");
	}

	private void showBrowser() {
		if (gmail.disconnectedProperty().get()) {
			return;
		}

		MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> mailBrowser;
		try {
			mailBrowser = new MailBrowser<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact>(gmail);
		} catch (final MailException e) {
			LOGGER.error("loading mail browser", e);
			return;
		}
		mailBrowser.setOnSignout(e -> signout());

		initTitle(stage);

		stage.titleProperty().unbind();
		stage.setWidth(1400);
		stage.setHeight(800);
		stage.setScene(mailBrowser);
		stage.show();

		mailBrowser.initData();
	}

	private void initTitle(Stage stage) {
		final Task<String> titleTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				final Contact contact = gmail.getContact();
				if (contact.getFullname().isEmpty()) {
					return contact.getEmail();
				} else {
					return contact.getFullname() + " (" + contact.getEmail() + ")";
				}
			}
		};
		titleTask.setOnSucceeded(e -> stage.setTitle("FisherMail - " + e.getSource().getValue()));
		titleTask.setOnFailed(e -> LOGGER.error("loading user's name", e.getSource().getException()));
		ThreadPool.getInstance().submitLow(titleTask, "loading user's name");
	}

	private void initMacOs() {
		if (!System.getProperty("os.name").contains("OS X")) {
			return;
		}
//		final MenuItem aboutItem = new MenuItem("About FisherMail");
//		final MenuItem preferencesItem = new MenuItem("Preferences...");
//		final MenuItem browserItem = new MenuItem("Mail Browser");
//		browserItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
//		final MenuItem composerItem = new MenuItem("Mail Composer");
//		composerItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
//		final MenuItem tipsItem = new MenuItem("Tips and Tricks");
//		final MenuBar menuBar = new MenuBar(
//				new Menu("Window", (Node)null
//						, aboutItem
//						, new SeparatorMenuItem()
//						, preferencesItem
//						, new SeparatorMenuItem()
//						, composerItem
//						, browserItem)
//				, new Menu("Help", (Node)null
//						, tipsItem));
//		menuBar.setUseSystemMenuBar(true);
//		stage.sceneProperty().addListener(c -> {
//			stage.getScene().setRoot(new BorderPane(stage.getScene().getRoot(), menuBar, null, null, null));
//		});
//		LOGGER.info("initialize OS X stage behaviour");
//		Platform.setImplicitExit(false);
//		com.apple.eawt.Application.getApplication().addAppEventListener(new AppReOpenedListener() {
//			@Override
//			public void appReOpened(final AppReOpenedEvent e) {
//				LOGGER.info("OS X AppReOpenedListener");
//				if (!stage.isShowing()) {
//					LOGGER.debug("OS X show()");
//					Platform.runLater(() -> stage.show());
//				}
//				if (stage.isIconified()) {
//					LOGGER.debug("OS X setIconified(false)");
//					Platform.runLater(() -> stage.setIconified(false));
//				}
//				if (!stage.isFocused()) {
//					LOGGER.debug("OS X requestFocus()");
//					Platform.runLater(() -> stage.requestFocus());
//				}
//			}
//		});
//
//		final List<MenuBase> menus = new ArrayList<>();
//		menus.add(GlobalMenuAdapter.adapt(new Menu("java")));
//
//		final TKSystemMenu menu = Toolkit.getToolkit().getSystemMenu();
//		menu.setMenus(menus);
	}

	private void initNotifier() {
		if (gmail.disconnectedProperty().get()) {
			return;
		}
		gmail.addOnNewMessage(threads -> {
			LOGGER.debug("notifyAfterNewMessage");

			threads.forEach(t -> {
				ThreadPool.getInstance().submitLow(() -> {
					final String message;
					try {
						final Message m = gmail.getMessage(t.getLastMessageId());
						message = "from " + String.join(", ", MessageHelper.getNames(m.getMimeMessage().getFrom()))
								+ "\r\n" + m.getSnippet();
					} catch (final Exception e) {
						LOGGER.error("notifying new message for thread {}", t.getId(), e);
						return;
					}
					Platform.runLater(() -> Notifier.INSTANCE.notifySuccess(
							t.getSubject()
							, message
							, v -> {
								if (stage.isIconified()) {
									stage.setIconified(false);
								}
								if (!stage.isFocused()) {
									stage.requestFocus();
								}
								return null;
							}));
				}, "notifying new message");
			});

			return null;
		});
	}
}
