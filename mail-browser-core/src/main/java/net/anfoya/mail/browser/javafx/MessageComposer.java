package net.anfoya.mail.browser.javafx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class MessageComposer<M extends SimpleMessage> extends Stage {
	private static final Session SESSION = Session.getDefaultInstance(new Properties(), null);

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;
	private final M draft;

	private final HTMLEditor editor;
	private final ComboBox<String> fromCombo;
	private final TextField toField;
	private final TextField ccField;
	private final TextField bccField;
	private final TextField subjectField;
	private final GridPane headerPane;
	private final HBox toBox;
	private final BorderPane mainPane;
	private final Label toLabel;

	public MessageComposer(
			final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService)
			throws MailException {
		this(mailService, mailService.createDraft());
	}

	public MessageComposer(
			final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService,
			final M draft) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");
		getIcons().add(new Image(getClass().getResourceAsStream("entrypoint/Mail.png")));
		setScene(new Scene(new BorderPane(), 800, 600));

		this.mainPane = (BorderPane) getScene().getRoot();
		this.mailService = mailService;
		this.draft = draft;

		final ColumnConstraints widthConstraints = new ColumnConstraints(80);
		final ColumnConstraints growConstraints = new ColumnConstraints();
		growConstraints.setHgrow(Priority.ALWAYS);
		headerPane = new GridPane();
		headerPane.setVgap(5);
		headerPane.setHgap(5);
		headerPane.setPadding(new Insets(5));
		headerPane.prefWidthProperty().bind(widthProperty());
		headerPane.getColumnConstraints().add(widthConstraints);
		headerPane.getColumnConstraints().add(growConstraints);
		mainPane.setTop(headerPane);

		fromCombo = new ComboBox<String>();
		fromCombo.prefWidthProperty().bind(widthProperty());
		fromCombo.getItems().add("me");
		fromCombo.getSelectionModel().select(0);
		fromCombo.setDisable(true);

		toLabel = new Label("to");
		final Label moreLabel = new Label(" ...");
		toBox = new HBox(toLabel, moreLabel);
		toBox.setAlignment(Pos.CENTER_LEFT);
		toBox.setOnMouseClicked(event -> {
			if (headerPane.getChildren().contains(fromCombo)) {
				toMiniHeader();
			} else {
				toFullHeader();
			}
		});
		toField = new TextField();
		ccField = new TextField();
		bccField = new TextField();
		subjectField = new TextField("FisherMail - test");

		toMiniHeader();

		editor = new HTMLEditor();
		mainPane.setCenter(editor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> discardAndClose());

		final Button saveButton = new Button("save");
		saveButton.setOnAction(event -> saveAndClose());

		final Button sendButton = new Button("send");
		sendButton.setOnAction(event -> sendAndClose());

		final HBox buttonBox = new HBox(5, discardButton, saveButton, sendButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		show();
	}

	private MimeMessage buildMessage() throws MessagingException {
		final MimeMessage message = new MimeMessage(SESSION);
		message.setFrom(new InternetAddress("frederic.antigny@gmail.com"));
		message.setSubject(subjectField.getText());

		final MimeMultipart multipart = new MimeMultipart();
		message.setContent(multipart);

		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(editor.getHtmlText(), "text/html");
		mimeBodyPart.setHeader("Content-Type", "text/html; charset=\"UTF-8\"");
		multipart.addBodyPart(mimeBodyPart);

		InternetAddress to;
		try {
			to = new InternetAddress(toField.getText());
		} catch (final AddressException e) {
			to = new InternetAddress("frederic.antigny@gmail.com");
		}
		message.addRecipient(RecipientType.TO, to);

		return message;
	}

	private M buildDraft() throws MessagingException, IOException {
		final MimeMessage message = buildMessage();
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		message.writeTo(bos);
	    draft.setRaw(Base64.getUrlEncoder().encode(bos.toByteArray()));
		return draft;
	}

	private void sendAndClose() {
		try {
			mailService.send(buildDraft());
		} catch (final MessagingException | IOException | MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		close();
	}

	private void saveAndClose() {
	    try {
			mailService.save(buildDraft());
		} catch (IOException | MessagingException | MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    close();
	}

	private void discardAndClose() {
		try {
			mailService.remove(draft);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		close();
	}

	private void toMiniHeader() {
		headerPane.getChildren().clear();
		headerPane.addRow(0, toBox, toField);
		headerPane.addRow(1, new Label("subject"), subjectField);
	}

	private void toFullHeader() {
		headerPane.getChildren().clear();
//		headerPane.addRow(0, new Label("from"), fromCombo);
		headerPane.addRow(0, toLabel, toField);
		headerPane.addRow(1, new Label("cc"), ccField);
		headerPane.addRow(2, new Label("bcc"), bccField);
		headerPane.addRow(3, new Label("subject"), subjectField);
	}
}
