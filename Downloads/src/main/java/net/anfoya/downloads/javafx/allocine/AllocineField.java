package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AllocineField extends ComboBox<AllocineQsResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	private volatile AllocineQsResult currentQs;
	private volatile AllocineQsResult searchedQs;
	private volatile AllocineQsResult requestQs;

	private final AtomicLong requestId;

	private Callback<AllocineQsResult, Void> searchCallback;

	public AllocineField() {
		setEditable(true);
		setPromptText("Key in a text and wait for quick search or type <Enter> for full search");
		setCellFactory(new Callback<ListView<AllocineQsResult>, ListCell<AllocineQsResult>>() {
			@Override
			public ListCell<AllocineQsResult> call(final ListView<AllocineQsResult> listView) {
				return new AllocineQsListCell();
			}
		});

		currentQs = null;
		searchedQs = null;
		requestQs = null;
		requestId = new AtomicLong(0);

		addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				debug("KEY_PRESSED " + event.getCode());
				switch(event.getCode()) {
				case ENTER:
					submitSearch();
					break;
				default:
				}
			}
		});

		getEditor().addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				debug("editor KEY_RELEASED " + event.getCode());
				switch(event.getCode()) {
				case RIGHT: case LEFT: case UP:
					break;
				case DOWN:
					if (!isShowing()) {
						showQuickSearch();
					}
					break;
				default:
					showQuickSearch();
				}
			}
		});
		getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> ov, final String oldVal, final String newVal) {
				debug("text property change " + newVal);
				currentQs = new AllocineQsResult(newVal);
			}
		});

		setConverter(new StringConverter<AllocineQsResult>() {
			@Override
			public String toString(final AllocineQsResult qsResult) {
				debug("converting qsResult " + qsResult);
				return qsResult == null? "": qsResult.toString();
			}
			@Override
			public AllocineQsResult fromString(final String string) {
				debug("converting string " + string);
				return string == null? AllocineQsResult.getEmptyResult(): new AllocineQsResult(string);
			}
		});
	}

	private void submitSearch() {
		debug("submit search");
		cancelQuickSearch();
		if (searchCallback != null
				&& currentQs != null
				&& !currentQs.toString().isEmpty()
				&& !currentQs.equals(searchedQs)) {
			final AllocineQsResult qs = getValue();
			if (currentQs.equals(qs)) {
				// actual value of the combo hold a full ResultQs loaded from json data
				currentQs = qs;
			}
			searchedQs = currentQs;
			searchCallback.call(currentQs);
		}
	}

	private void cancelQuickSearch() {
		debug("cancel quick search");
		hide();
		requestId.incrementAndGet();
	}

	private synchronized void showQuickSearch() {
		debug("show quick search");

		if (currentQs == null) {
			// nothing to display
			cancelQuickSearch();
			return;
		}

		if (currentQs.equals(requestQs) && !getItems().isEmpty()) {
			// quick search already done
			show();
			return;
		}

		cancelQuickSearch();
		requestQs = currentQs;
		final long requestId = this.requestId.incrementAndGet();
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					requestQuickSearch(requestId, requestQs.toString());
				} catch (final InterruptedException e) {
					return;
				} catch (final Exception e) {
					LOGGER.error("loading {}", requestQs.toString(), e);
					return;
				}
			}
		});
	}

	private void requestQuickSearch(final long requestId, final String title) throws InterruptedException, MalformedURLException, IOException {
		debug("request quick search (" + requestId + ") " + title);

		if (title.length() < 3) {
			// need more characters
			return;
		}

		// allow user to type more characters
		Thread.sleep(500);
		if (requestId != this.requestId.get()) {
			debug("cancelled quick search (" + requestId + ") " + title);
			return;
		}

		// get a connection
		final String url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		final List<AllocineQsResult> qsResults = new ArrayList<AllocineQsResult>();
		LOGGER.info("request ({}) {}", requestId, url);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		if (requestId != this.requestId.get()) {
			debug("cancelled quick search (" + requestId + ") " + title);
			return;
		}

		// read / parse json data
		final JsonArray jsonQsResults = new JsonParser().parse(reader).getAsJsonArray();
		for (final JsonElement jsonElement : jsonQsResults) {
			qsResults.add(new AllocineQsResult(jsonElement.getAsJsonObject()));
		}

		// launch GUI update
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (requestId != AllocineField.this.requestId.get()) {
					debug("cancelled quick search (" + requestId + ") " + title);
					return;
				}
				LOGGER.info("displayed ({}) {}", requestId, url);
				getItems().clear();
				getItems().addAll(qsResults);
				if (!getItems().isEmpty()) {
					show();
				}
			}
		});
	}

	public void setSearchedText(final String searched) {
		debug("set searched " + searched);
		final AllocineQsResult searchedQs = new AllocineQsResult(searched);
		if (!searchedQs.equals(this.searchedQs)) {
			this.searchedQs = searchedQs;
			setValue(searchedQs);
		}
	}

	public void setOnSearch(final Callback<AllocineQsResult, Void> callback) {
		searchCallback = callback;
	}

	private void debug(final String s) {
		LOGGER.debug(s + " ({}) ({}) ({}) ({}) ({})"
				, getValue()
				, currentQs
				, searchedQs
				, requestQs
				, requestId);
	}
}
