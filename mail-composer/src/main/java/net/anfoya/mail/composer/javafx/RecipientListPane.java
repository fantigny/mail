package net.anfoya.mail.composer.javafx;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.ComboBoxAutoShow;
import net.anfoya.javafx.scene.control.ComboField;
import net.anfoya.javafx.scene.control.RemoveLabel;
import net.anfoya.javafx.util.LabelHelper;
import net.anfoya.mail.service.Contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipientListPane<C extends Contact> extends HBox {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecipientListPane.class);

	private final Label title;
	private final FlowPane flowPane;
	private final ComboField<String> combo;
	private final Map<String, C> addressContacts;
	private final Set<String> selectedAdresses;

	private Task<Double> organiseTask;
	private long organiseTaskId;

	private EventHandler<ActionEvent> updateHandler;

	public RecipientListPane(final String title, final Map<String, C> addressContacts) {
		super(0);
		setPadding(new Insets(3, 0, 3, 0));
		getStyleClass().add("box-underline");

		this.addressContacts = addressContacts;

		this.title = new Label(title);
		this.title.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
		this.title.setStyle("-fx-text-fill: gray; -fx-padding: 2 0 0 0");
		getChildren().add(this.title);

		flowPane = new FlowPane(3,  2);
		flowPane.setMinWidth(150);
		getChildren().add(flowPane);
		HBox.setHgrow(flowPane, Priority.ALWAYS);

		organiseTask = null;
		organiseTaskId = -1;

		selectedAdresses = new LinkedHashSet<String>();

		combo = new ComboField<String>();
		combo.setPadding(new Insets(0));
		combo.setCellFactory(listView -> {
			return new ListCell<String>() {
				@Override
			    public void updateItem(final String address, final boolean empty) {
			        super.updateItem(address, empty);
			        if (!empty) {
			        	setText(addressContacts.get(address).getFullname() + " (" + addressContacts.get(address).getEmail() + ")");
			        }
				}
			};
		});
		combo.setOnFieldAction(e -> add(combo.getFieldValue()));
		combo.setOnBackspaceAction(e -> {
			final int lastAddressIndex = flowPane.getChildren().size() - 2;
			if (lastAddressIndex >= 0) {
				flowPane.getChildren().remove(lastAddressIndex);
			}
		});
		combo.getItems().addAll(addressContacts.keySet());
		new ComboBoxAutoShow(combo, address -> selectedAdresses.contains(address)
				? ""
				: addressContacts.get(address).getFullname() + " " + addressContacts.get(address).getEmail());
		flowPane.getChildren().add(combo);

		heightProperty().addListener((ov, o, n) -> organise(null));
		widthProperty().addListener((ov, o, n) -> organise(null));
	}

	public void add(final String address) {
		final String text = addressContacts.containsKey(address)? addressContacts.get(address).getFullname(): address;
		final RemoveLabel label = new RemoveLabel(text, address);
		label.getStyleClass().add("address-label");

		flowPane.getChildren().add(flowPane.getChildren().size() - 1, label);
		selectedAdresses.add(address);
		updateHandler.handle(null);
		organise(label);

		label.setOnRemove(e -> {
			flowPane.getChildren().remove(label);
			selectedAdresses.remove(label.getTooltip());
			updateHandler.handle(null);
			organise(null);
		});
	}

	public Set<String> getRecipients() {
		final Set<String> addresses = new LinkedHashSet<String>(selectedAdresses);
		if (combo.getValue() != null && !combo.getValue().isEmpty()) {
			addresses.add(combo.getValue());
		}

		return addresses;
	}

	public void setOnUpdateList(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
	}

	public ReadOnlyBooleanProperty textfocusedProperty() {
		return combo.getEditor().focusedProperty();
	}

	public String getTitle() {
		return title.getText();
	}

	public void setTitle(final String title) {
		this.title.setText(title);
	}

	private synchronized void organise(final Label lastAdded) {
		final long taskId = ++organiseTaskId;
		if (organiseTask != null && organiseTask.isRunning()) {
			organiseTask.cancel();
		}

		final double comboWidth = combo.getWidth();
		LOGGER.debug("combo width {}", comboWidth);

		combo.setFieldValue("");
		combo.hide();

		if (lastAdded != null) {
			final double tempWidth = comboWidth - LabelHelper.computeWidth(lastAdded) - flowPane.getHgap();
			LOGGER.debug("combo temp width {}", comboWidth);
			combo.setPrefWidth(tempWidth);
		}

		organiseTask = new Task<Double>() {
			@Override
			protected Double call() throws Exception {
				final double paneWidth = flowPane.getWidth();
				LOGGER.debug("pane width {}", paneWidth);
				if (paneWidth == 0) {
					return 0d;
				}

				double availableWidth = paneWidth;
				for(final Node node: flowPane.getChildren()) {
					if (node instanceof Label) {
						final Label l = (Label) node;
						while(l.getWidth() == 0) {
							try { Thread.sleep(50); }
							catch (final Exception e) { /* do nothing */ }
						}
						if (l.getWidth() > availableWidth) {
							availableWidth = paneWidth;
						}
						availableWidth -= l.getWidth() + flowPane.getHgap();
					}
				}
				LOGGER.debug("available width {}", availableWidth);
				return availableWidth;
			}
		};
		organiseTask.setOnFailed(event -> LOGGER.error("organizing labels and combo", event.getSource().getException()));
		organiseTask.setOnSucceeded(e -> {
			if (taskId != organiseTaskId) {
				return;
			}

			final double availableWidth = (double) e.getSource().getValue();
			combo.setPrefWidth(availableWidth < 150? flowPane.getWidth(): availableWidth);
		});
		ThreadPool.getInstance().submitHigh(organiseTask);
	}
}
