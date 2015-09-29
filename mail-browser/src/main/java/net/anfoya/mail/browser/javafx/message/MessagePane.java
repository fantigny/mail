package net.anfoya.mail.browser.javafx.message;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.web.WebViewFitContent;
import net.anfoya.mail.browser.javafx.thread.ThreadDropPane;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.mime.DateHelper;
import net.anfoya.mail.mime.MessageHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePane<M extends Message, C extends Contact> extends VBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagePane.class);
	private static final String CSS_DATA = "<style> body {"
			+ " margin: 7;"
			+ " padding: 0;"
			+ " font-family: Arial, Helvetica, sans-serif;"
			+ " font-size: 12px;"
			+ "} </style>";

	private final String messageId;
	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService;
	private final BooleanProperty expanded;
	private final BooleanProperty collapsible;

	private final MessageHelper helper;

	private final Text titleText;
	private final Text dateText;
	private final FlowPane attachmentPane;
	private final WebView snippetView;
	private final VBox titlePane;
	private final WebViewFitContent messageView;

	private M message;
	private Task<String> loadTask;

	private EventHandler<ActionEvent> updateHandler;
	private volatile boolean mouseOver;

	private Timeline showSnippetTimeline;
	private Timeline showMessageTimeline;

	public MessagePane(final String messageId, final MailService<? extends Section, ? extends Tag, ? extends Thread, M, C> mailService) {
		this.mailService = mailService;
		this.messageId = messageId;
		expanded = new SimpleBooleanProperty(true);
		collapsible = new SimpleBooleanProperty(true);
		helper = new MessageHelper();

		messageView = new WebViewFitContent();
		messageView.getEngine().setUserStyleSheetLocation(getClass().getResource("default.css").toExternalForm());
		messageView.getEngine().setCreatePopupHandler(handler -> messageView.getEngine());
		messageView.getEngine().locationProperty().addListener((ov, o, n) -> {
			if (o != null) {
				Platform.runLater(() -> messageView.getEngine().getLoadWorker().cancel());
				handleHyperlink(n);
			}
		});

		titleText = new Text("loading...");
		dateText = new Text();

		final HBox hSpace = new HBox();
		HBox.setHgrow(hSpace, Priority.ALWAYS);

		snippetView = new WebView();
		snippetView.prefWidthProperty().bind(widthProperty());
		snippetView.setPrefHeight(30);
		snippetView.setMinHeight(0);
		snippetView.setMaxHeight(0);
		snippetView.toBack();

		attachmentPane = new FlowPane(Orientation.HORIZONTAL, 5, 0);
		attachmentPane.setPadding(new Insets(0, 10, 0, 10));

		final HBox title = new HBox(titleText, hSpace, dateText);
		title.getStyleClass().add("message-title-text");
		title.setOnMouseClicked(event -> expanded.set(!expanded.get()));
		title.setCursor(Cursor.HAND);

		titlePane = new VBox(title, attachmentPane, snippetView);
		titlePane.setAlignment(Pos.CENTER_LEFT);

		titlePane.setOnMouseEntered(e ->{
			 setShowSnippet(expanded.not().get());
			 if (expanded.not().get()) {
				 setShowAttachment(true);
			 }
			 mouseOver = true;
		});
		titlePane.setOnMouseExited(e ->{
			 setShowSnippet(false);
			 if (expanded.not().get()) {
				 setShowAttachment(false);
			 }
			 mouseOver = false;
		});

		expanded.addListener((ov, o, n) -> {
			if (collapsible.not().get()) {
				return;
			}
			setShowMessage(n);
			if (!o && n) {
				snippetView.setMaxHeight(0);
			} else {
				setShowSnippet(mouseOver && !n);
				setShowAttachment(mouseOver && !n);
			}
		});

		collapsible.addListener((ov, o, n) -> {
			if (n) {
				titleText.setCursor(Cursor.HAND);
			} else {
				titleText.setCursor(Cursor.DEFAULT);
			}
		});

		getChildren().addAll(titlePane, messageView);
		setOnDragDetected(event -> {
	        final ClipboardContent content = new ClipboardContent();
	        content.put(ThreadDropPane.MESSAGE_DATA_FORMAT, message);
	        final Dragboard db = startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
	}

	private void setShowSnippet(final boolean show) {
		if (showSnippetTimeline != null) {
			showSnippetTimeline.stop();
		}

		final KeyValue values = new KeyValue(snippetView.maxHeightProperty(), show? snippetView.getPrefHeight(): 0);
		final KeyFrame frame = new KeyFrame(Duration.millis(100 * (show? 1: .5)), values);
		showSnippetTimeline = new Timeline(frame);
		showSnippetTimeline.play();
	}

	private void setShowAttachment(final boolean show) {
		if (show && !titlePane.getChildren().contains(attachmentPane)) {
			titlePane.getChildren().add(1, attachmentPane);
		} else if (!show && titlePane.getChildren().contains(attachmentPane)) {
			titlePane.getChildren().remove(attachmentPane);
		}
	}

	private void setShowMessage(final boolean show) {
		if (showMessageTimeline != null) {
			showMessageTimeline.stop();
		}

		final KeyValue values = new KeyValue(messageView.maxHeightProperty(), show? messageView.getPrefHeight(): 0);
		final KeyFrame frame = new KeyFrame(Duration.millis(50 * (show? 1: .5)), values);
		showMessageTimeline = new Timeline(frame);
		showMessageTimeline.play();
	}

	public synchronized void load() {
		if (loadTask != null) {
			//already loading;
			return;
		}
		loadTask = new Task<String>() {
			@Override
			protected String call() throws MailException, MessagingException, IOException, URISyntaxException {
				message = mailService.getMessage(messageId);
			    return helper.toHtml(message.getMimeMessage());
			}
		};
		loadTask.setOnFailed(event -> {
			LOGGER.error("loading message id {}", messageId, event.getSource().getException());
		});
		loadTask.setOnSucceeded(event -> {
			refresh();
			messageView.getEngine().loadContent(loadTask.getValue());
		});
		ThreadPool.getInstance().submitHigh(loadTask, "loading message id " + messageId);
	}

	public void setScrollHandler(final EventHandler<ScrollEvent> handler) {
		messageView.setScrollHandler(handler);
	}

	public boolean isExpanded() {
		return expanded.get();
	}

	public void setExpanded(final boolean expanded) {
		this.expanded.set(expanded);
	}

	public BooleanProperty expandedProperty() {
		return expanded;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setCollapsible(final boolean collapsible) {
		this.collapsible.set(collapsible);
	}

	public boolean isCollapsible() {
		return collapsible.get();
	}

	public BooleanProperty collapsibleProperty() {
		return collapsible;
	}

	public void setUpdateHandler(final EventHandler<ActionEvent> handler) {
		this.updateHandler = handler;
	}

	private void handleHyperlink(final String link) {
		URI uri;
		try {
			uri = new URI(link);
		} catch (final URISyntaxException e) {
			LOGGER.error("reading address {}", link, e);
			return;
		}
		final String scheme = uri.getScheme();
		if (scheme.equals("mailto")) {
			try {
				new MailComposer<M, C>(mailService, updateHandler).newMessage(uri.getSchemeSpecificPart());
			} catch (final MailException e) {
				LOGGER.error("creating new mail to {}", link, e);
			}
		} else {
			ThreadPool.getInstance().submitHigh(() -> {
				try {
					Desktop.getDesktop().browse(uri);
				} catch (final Exception e) {
					LOGGER.error("handling link {}", link, e);
				}
			}, "handling link " + link);
		}
	}

	private void refresh() {
		final StringBuilder title = new StringBuilder();
		final MimeMessage mimeMessage = message.getMimeMessage();
		try {
			title.append(String.join(", ", helper.getNames(mimeMessage.getFrom())));
			title.append(" to ").append(String.join(", ", helper.getNames(mimeMessage.getRecipients(MimeMessage.RecipientType.TO))));
		} catch (final MessagingException e) {
			LOGGER.error("loading title data", e);
		}
		titleText.setText(title.toString());

		String date = "";
		try {
			date = new DateHelper(mimeMessage.getSentDate()).format();
		} catch (final Exception e) {
			LOGGER.warn("loading sent date", e);
		}
		dateText.setText(date);

		snippetView.getEngine().loadContent(CSS_DATA + message.getSnippet() + "...");

		attachmentPane.getChildren().clear();
		final Set<String> attachNames = helper.getAttachmentNames();
		if (!attachNames.isEmpty()) {
			final Image image = new Image(getClass().getResourceAsStream("attachment.png"));
			for(final String name: attachNames) {
				final HBox attachment = new HBox(3, new ImageView(image), new Label(name));
				attachment.setPadding(new Insets(5));
				attachment.setCursor(Cursor.HAND);
				attachment.setOnMouseClicked(e -> {
					try {
						new AttachmentLoader<M>(mailService, message.getId()).start(name);
					} catch (final Exception ex) {
						LOGGER.error("loading ", ex);
					}
				});
				attachmentPane.getChildren().add(attachment);
			}
		}
	}
}
