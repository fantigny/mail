package net.anfoya.mail.browser.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.anfoya.java.undo.UndoService;
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.javafx.scene.layout.FixedSplitPane;
import net.anfoya.mail.browser.controller.Controller;
import net.anfoya.mail.browser.javafx.css.StyleHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.section.SectionListPane;
import net.anfoya.tag.model.SpecialTag;

public class MailBrowser<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Scene {
	public enum Mode { FULL, MINI, MICRO }

//	private static final Logger LOGGER = LoggerFactory.getLogger(MailBrowser.class);

	private final MailService<S, T, H, M, C> mailService;
	private final Settings settings;

	private final FixedSplitPane splitPane;
	private final SectionListPane<S, T> sectionListPane;
	private final ThreadListPane<S, T, H, M, C> threadListPane;
	private final ThreadPane<S, T, H, M, C> threadPane;

	public MailBrowser(final MailService<S, T, H, M, C> mailService
			, final NotificationService notificationService
			, final Settings settings) throws MailException {
		super(new FixedSplitPane(), Color.TRANSPARENT);
		this.mailService = mailService;
		this.settings = settings;

		final UndoService undoService = new UndoService();

		StyleHelper.addCommonCss(this);
		StyleHelper.addCss(this, "/net/anfoya/javafx/scene/control/excludebox.css");

		splitPane = (FixedSplitPane) getRoot();
		splitPane.getStyleClass().add("background");

		sectionListPane = new SectionListPane<S, T>(mailService, undoService, settings.showExcludeBox().get());
		sectionListPane.setPrefWidth(settings.sectionListPaneWidth().get());
		sectionListPane.setFocusTraversable(false);
		sectionListPane.setSectionDisableWhenZero(false);
		sectionListPane.setLazyCount(true);
		splitPane.getPanes().add(sectionListPane);

		threadListPane = new ThreadListPane<S, T, H, M, C>(mailService, undoService, settings);
		threadListPane.setPrefWidth(settings.threadListPaneWidth().get());
		threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
		splitPane.getPanes().add(threadListPane);

		threadPane = new ThreadPane<S, T, H, M, C>(mailService, undoService, settings);
		threadPane.setPrefWidth(settings.threadPaneWidth().get());
		threadPane.setFocusTraversable(false);
		splitPane.getPanes().add(threadPane);

		splitPane.setOnKeyPressed(e -> toggleViewMode(e));

		final Controller<S, T, H, M, C> controller = new Controller<S, T, H, M, C>(
				mailService
				, notificationService
				, undoService
				, settings);
		controller.setMailBrowser(this);
		controller.setSectionListPane(sectionListPane);
		controller.addThreadPane(threadPane);
		controller.setThreadListPane(threadListPane);
		controller.init();

		windowProperty().addListener((ov, o, n) -> {
			if (n != null) {
				setMode(Mode.valueOf(settings.browserMode().get()));
			}
		});
	}

	public void setMode(Mode mode) {
		threadListPane.setMode(mode);

		switch(mode) {
		case FULL:
			splitPane.setVisiblePanes(sectionListPane, threadListPane, threadPane);
			break;
		case MINI:
			splitPane.setVisiblePanes(sectionListPane, threadListPane);
			break;
		case MICRO:
			splitPane.setVisiblePanes(threadListPane);
			break;
		}
		splitPane.setResizableWithParent(splitPane.getVisiblePanes().size() == 3? threadPane: threadListPane);
		if (getWindow() != null) {
			((Stage)getWindow()).setWidth(splitPane.computePrefWidth());
			((Stage)getWindow()).setMinWidth(splitPane.computeMinWidth());
		}
	}

	public void setOnSignout(final EventHandler<ActionEvent> handler) {
		threadPane.setOnSignout(handler);
	}

	public void initData() {
//		sectionListPane.init("Bank", "HK HSBC");
		sectionListPane.init(GmailSection.SYSTEM.getName(), mailService.getSpecialTag(SpecialTag.INBOX).getName());
	}

	private void toggleViewMode(final KeyEvent e) {
		final boolean left = e.getCode() == KeyCode.LEFT;
		final boolean right = e.getCode() == KeyCode.RIGHT;
		if (left || right) {
			e.consume();
		} else {
			return;
		}

		final int nbVisible = splitPane.getVisiblePanes().size();
		final Mode mode;
		if (right && nbVisible == 2) {
			mode = Mode.FULL;
		} else if (right && nbVisible == 1
				|| left && nbVisible == 3) {
			mode = Mode.MINI;
		} else if (left && nbVisible == 2) {
			mode = Mode.MICRO;
		} else {
			return;
		}
		setMode(mode);
		settings.browserMode().set(mode.toString());
	}

	public void saveSettings() {
		settings.sectionListPaneWidth().set(sectionListPane.getWidth());
		settings.threadListPaneWidth().set(threadListPane.getWidth());
		settings.threadPaneWidth().set(threadPane.getWidth());
	}

	public boolean isFull() {
		return splitPane.getVisiblePanes().size() == 3;
	}
}
