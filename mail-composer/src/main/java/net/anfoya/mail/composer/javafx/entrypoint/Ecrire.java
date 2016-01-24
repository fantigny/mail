package net.anfoya.mail.composer.javafx.entrypoint;

import javafx.application.Application;
import javafx.stage.Stage;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.client.App;
import net.anfoya.mail.composer.javafx.MailComposer;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;

public class Ecrire extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {

		final GmailService mailService = new GmailService();
		mailService.connect(App.MAIL_CLIENT);

		final Settings settings = new Settings(mailService);
		settings.load();

		new MailComposer<GmailMessage, GmailContact>(mailService, settings).newMessage("frederic.antigny+ecrire@gmail.com");

		Thread.sleep(2000);
	}
}
