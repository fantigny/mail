package net.anfoya.mail.browser.javafx;

import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadPane extends BorderPane {
	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread> mailService;

	private final TextField subjectField;
	private final Accordion messageAcc;

	public ThreadPane(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread> mailService) {
		this.mailService = mailService;

		setPadding(new Insets(5));

		subjectField = new TextField("select a thread");
		setTop(subjectField);

		messageAcc = new Accordion();
		setCenter(messageAcc);
	}

	public void load(final SimpleThread thread) {
		messageAcc.getPanes().clear();
		if (thread == null) {
			subjectField.setText("no thread selected");
			return;
		}

		subjectField.setText(thread.getSubject());

		for(final String id: thread.getMessageIds()) {
			final MessagePane pane = new MessagePane(mailService);
			messageAcc.getPanes().add(0, pane);
			pane.load(id);
		}

		if (!messageAcc.getPanes().isEmpty()) {
			messageAcc.setExpandedPane(messageAcc.getPanes().get(0));
		}
	}
}
