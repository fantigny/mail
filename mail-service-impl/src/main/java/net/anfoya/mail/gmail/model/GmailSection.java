package net.anfoya.mail.gmail.model;

import net.anfoya.tag.model.SimpleSection;

import com.google.api.services.gmail.model.Label;

public class GmailSection extends SimpleSection {
	public static final GmailSection NO_SECTION = new GmailSection(SimpleSection.NO_SECTION_NAME);
	public static final GmailSection GMAIL_SYSTEM = new GmailSection("GMail");
	public static final GmailSection GMAIL_CATEGORY = new GmailSection("Category");

	private final String string;
	private final boolean hidden;
	private final String path;

	private GmailSection(final String name) {
		super(name);

		string = name;
		hidden = false;
		path = name;
	}

	public GmailSection(final Label label) {
		super(label.getId(), label.getName());

		final String name = label.getName();
		if (name.contains("/")) {
			string = name.substring(0, name.lastIndexOf("/"));
		} else {
			string = name;
		}

		hidden = "labelHide".equals(label.getLabelListVisibility());
		path = label.getName();
	}

	@Override
	public String toString() {
		return string;
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getPath() {
		return path;
	}
}
