package net.anfoya.mail.browser.mime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MimeMessageHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MimeMessageHelper.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + File.separatorChar;
	private static final String ATTACH_ICON_PATH = TEMP + "fishermail-attachment.png";

	private static boolean copied = false;
	/* prepare attachment icon */ {
		if (!copied) {
			copied = true;
			try {
				Files.copy(getClass().getResourceAsStream("attachment.png"), new File(ATTACH_ICON_PATH).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final Map<String, String> cidFilenames;
	private final List<String> attachments;

	public MimeMessageHelper() {
		cidFilenames = new HashMap<String, String>();
		attachments = new ArrayList<String>();
	}

	public String toHtml(final MimeMessage message) throws IOException, MessagingException {
		cidFilenames.clear();
		String html = toHtml(message, false).toString();
		html = replaceCids(html, cidFilenames);
		html = addAttachments(html, attachments);
		return html;
	}

	private String addAttachments(final String html, final List<String> attachNames) {
		if (attachNames.isEmpty()) {
			return html;
		}

		String attHtml = "";
		attHtml += "<br>";
		attHtml += "<div style='POSITION: absolute; BOTTOM: 0;'><table style='cursor: hand'><tr>";
		for(final String name: attachNames) {
			attHtml += "<td align='center' onClick='attLoader.start(\"" + name + "\")'><img src='file://" + ATTACH_ICON_PATH + "'></td>";
		}
		attHtml += "</tr><tr>";
		for(final String name: attachNames) {
			attHtml += "<td align='center' onClick='attLoader.start(\"" + name + "\")'>" + name + "</td>";
		}
		attHtml += "</tr></table></div>";
		LOGGER.debug(attHtml);

		final String start, end;
		final int pos = Math.max(html.lastIndexOf("</body>"), html.lastIndexOf("</BODY>"));
		if (pos == -1) {
			start = html;
			end = "";
		} else {
			start = html.substring(0, pos);
			end = html.substring(pos);
		}

		return start + attHtml + end;
	}

	private String replaceCids(String html, final Map<String, String> cidFilenames) {
		for(final Entry<String, String> entry: cidFilenames.entrySet()) {
			html = html.replaceAll("cid:" + entry.getKey(), "file://" + entry.getValue());
		}
		return html;
	}

	private StringBuilder toHtml(final Part part, boolean isHtml) throws IOException, MessagingException {
		final String type = part.getContentType().replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		isHtml = isHtml || type.contains("multipart/alternative");
		if (part.getContent() instanceof String && type.contains("text/html")) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder((String) part.getContent());
		} else if (part.getContent() instanceof String && type.contains("text/plain") && !isHtml) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder("<pre>").append(part.getContent()).append("</pre>");
		} else if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) part.getContent();
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html;
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& ((MimeBodyPart)part).getContentID() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String cid = bodyPart.getContentID().replaceAll("<", "").replaceAll(">", "");
			final String tempFilename = TEMP + (part.getFileName() == null? cid: MimeUtility.decodeText(bodyPart.getFileName()));
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			cidFilenames.put(cid, tempFilename);
			return new StringBuilder();
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			return new StringBuilder("<img src='file://").append(tempFilename).append("'>");
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() == null || MimeBodyPart.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String filename = MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ keep {}", filename);
			attachments.add(filename);
			return new StringBuilder();
		} else {
			LOGGER.warn("---- type {}", type);
			return new StringBuilder();
		}
	}
}
