package net.anfoya.mail.service;

import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.util.Callback;
import net.anfoya.tag.model.SpecialTag;
import net.anfoya.tag.service.TagService;

public interface MailService<
		S extends Section
		, T extends Tag
		, H extends Thread
		, M extends Message
		, C extends Contact>
		extends TagService<S, T> {

	public void connect(String appName) throws MailException;
	public void reconnect() throws MailException;
	public void disconnect();
	public ReadOnlyBooleanProperty disconnectedProperty();

	public void start();
	public void stop();

	public void clearCache();

	public void addOnUpdateMessage(final Runnable callback);
	public void addOnNewMessage(Callback<Set<H>, Void> callback);

	public Set<S> getHiddenSections() throws MailException;

	public H getThread(String id) throws MailException;
	public Set<H> findThreads(Set<T> includes, Set<T> excludes, String pattern, int pageMax) throws MailException;
	public void addTagForThreads(T tag, Set<H> threads) throws MailException;
	public void removeTagForThreads(T tag, Set<H> thread) throws MailException;

	public void archive(Set<H> threads) throws MailException;
	public void trash(Set<H> threads) throws MailException;

	public M getMessage(String id) throws MailException;
	public void remove(M message) throws MailException;

	public M createDraft(M message) throws MailException;
	public M getDraft(String id) throws MailException;
	public void send(M draft) throws MailException;
	public void save(M draft) throws MailException;

	public C getContact();
	public Set<C> getContacts() throws MailException;

	public void persistBytes(String id, byte[] bytes) throws MailException;
	public byte[] readBytes(String id) throws MailException;

	@Override public Set<S> getSections() throws MailException;
	@Override public long getCountForSection(S section, Set<T> includes, Set<T> excludes, String itemPattern) throws MailException;

	@Override public S addSection(String name) throws MailException;
	@Override public void remove(S Section) throws MailException;
	@Override public S rename(S Section, String name) throws MailException;
	@Override public void hide(S Section) throws MailException;
	@Override public void show(S Section) throws MailException;

	@Override public T findTag(String name) throws MailException;
	@Override public T getTag(String id) throws MailException;
	@Override public Set<T> getTags(S section) throws MailException;
	@Override public Set<T> getTags(String pattern) throws MailException;
	@Override public long getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws MailException;

	@Override public Set<T> getHiddenTags() throws MailException;
	@Override public T getSpecialTag(SpecialTag specialTag);

	@Override public T addTag(String name) throws MailException;
	@Override public void remove(T tag) throws MailException;
	@Override public T rename(T tag, String name) throws MailException;
	@Override public void hide(T tag) throws MailException;
	@Override public void show(T tag) throws MailException;

	@Override public T moveToSection(T tag, S section) throws MailException;

	@Override public void addOnUpdateTagOrSection(Runnable callback);
}
