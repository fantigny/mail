package net.anfoya.java.net.filtered;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import net.anfoya.java.net.EmptyUrlConnection;
import net.anfoya.java.net.filtered.engine.Matcher;
import sun.net.www.protocol.https.Handler;

public class FilteredHttpsHandler extends Handler {
	private final Matcher matcher;

	public FilteredHttpsHandler(final Matcher matcher) {
		this.matcher = matcher;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		return matcher.matches(url.toString())
				? new EmptyUrlConnection()
				: super.openConnection(url);
	}
}
