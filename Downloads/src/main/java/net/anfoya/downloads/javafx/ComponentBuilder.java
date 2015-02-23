package net.anfoya.downloads.javafx;

import net.anfoya.java.net.PersistentCookieStore;
import net.anfoya.java.net.filtered.easylist.EasyListFilterImpl;
import net.anfoya.java.net.filtered.engine.RuleSet;

public class ComponentBuilder {
	private final PersistentCookieStore cookieStore;
	private final RuleSet urlFilter;

	private final SearchTabs searchTabs;
	private final SearchPane searchPane;

	public ComponentBuilder() {
		cookieStore = new PersistentCookieStore();
		urlFilter = new EasyListFilterImpl(false);

		searchTabs = new SearchTabs();
		searchPane = new SearchPane();
	}

	public PersistentCookieStore buildCookieStore() {
		return cookieStore;
	}

	public SearchTabs buildSearchTabs() {
		return searchTabs;
	}

	public SearchPane buildSearchPane() {
		return searchPane;
	}

	public RuleSet buildUrlFilter() {
		return urlFilter;
	}
}