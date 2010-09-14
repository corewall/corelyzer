package org.chronos.util.j2k;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.media.imageioimpl.plugins.jpeg2000.J2KMetadata;
import com.sun.media.imageioimpl.plugins.jpeg2000.UUIDBox;
import com.sun.media.imageioimpl.plugins.jpeg2000.XMLBox;

/**
 * Various utility methods for working with JPEG 2000 files.
 * 
 * @author Doug Fils (fils@iastate.edu)
 * @author Josh Reed (jareed@psicat.org)
 */
public class J2KUtils {
	// some constants
	private static final String JPEG2000XMLBOX = "JPEG2000XMLBox";
	private static final String JPEG2000UUIDBOX = "JPEG2000UUIDBox";
	private static final String JPEG2000 = "JPEG2000";

	/**
	 * Adds any combination of XML and UUID boxes to a JPEG 2000 image.
	 * 
	 * @param in
	 *            the input file.This can be in any image format readable by
	 *            JAI.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @param list
	 *            the list of XML boxes.
	 * @param map
	 *            the map of UUID boxes.
	 * @throws IOException
	 *             thrown if any exception occurs.
	 */
	public static void addBoxes(final File in, final File out, final List<String> list, final Map<UUID, byte[]> map) throws IOException {
		// read in the image
		BufferedImage img = ImageIO.read(in);

		// get our writer
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(JPEG2000);
		if (!iter.hasNext()) {
			throw new IOException("Can't write image of type JPEG2000");
		}
		ImageWriter writer = iter.next(); // grab the first one
		ImageOutputStream ios = ImageIO.createImageOutputStream(out);
		writer.setOutput(ios);
		ImageTypeSpecifier typeSpec = ImageTypeSpecifier.createFromRenderedImage(img);

		// get our existing metadata if J2K or the default if not
		J2KMetadata metadata;
		if (isJ2K(in)) {
			metadata = getMetadata(in);
		} else {
			metadata = new J2KMetadata(typeSpec, writer.getDefaultWriteParam(), writer);
		}

		// add all of the XML boxes
		for (String s : list) {
			String foo = s.trim() + "\r\n";
			metadata.addNode(new XMLBox(foo.getBytes()));
		}

		// add all of the UUID boxes
		for (Entry<UUID, byte[]> entry : map.entrySet()) {
			// build our payload
			UUID u = entry.getKey();
			byte[] bytes = entry.getValue();
			byte[] payload = new byte[bytes.length + 16];
			System.arraycopy(uuidToBytes(u), 0, payload, 0, 16);
			System.arraycopy(bytes, 0, payload, 16, bytes.length);
			metadata.addNode(new UUIDBox(payload));
		}

		// write it out
		IIOImage iioImg = new IIOImage(img, null, metadata);
		writer.write(iioImg);
	}

	/**
	 * Adds a set of UUID boxes to a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This can be in any image format readable by
	 *            JAI.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @param map
	 *            the map of UUID boxes to add.
	 * @throws IOException
	 *             thrown if any exceptions occur.
	 */
	public static void addUUID(final File in, final File out, final Map<UUID, byte[]> map) throws IOException {
		addBoxes(in, out, new ArrayList<String>(), map);
	}

	/**
	 * Add a UUID box to a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file.This can be in any image format readable by
	 *            JAI.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @param uuid
	 *            the UUID of the box.
	 * @param bytes
	 *            the UUID data.
	 */
	public static void addUUID(final File in, final File out, final UUID uuid, final byte[] bytes) throws IOException {
		Map<UUID, byte[]> map = new HashMap<UUID, byte[]>();
		map.put(uuid, bytes);
		addBoxes(in, out, new ArrayList<String>(), map);
	}

	/**
	 * Append some XML to a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This can be in any image format readable by
	 *            JAI.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @param list
	 *            the XML payloads to append.
	 */
	public static void addXML(final File in, final File out, final List<String> list) throws IOException {
		addBoxes(in, out, list, new HashMap<UUID, byte[]>());
	}

	/**
	 * Append some XML to a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This can be in any image format readable by
	 *            JAI.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @param xml
	 *            the XML payload to append.
	 */
	public static void addXML(final File in, final File out, final String xml) throws IOException {
		List<String> list = new ArrayList<String>();
		list.add(xml);
		addBoxes(in, out, list, new HashMap<UUID, byte[]>());
	}

	private static List<Node> findChildren(final Node root, final String name) {
		List<Node> matching = new ArrayList<Node>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (name.equals(child.getNodeName())) {
				matching.add(child);
			}
		}
		return matching;
	}

	/**
	 * Get bytes from the specified file.
	 * 
	 * @param in
	 *            the file.
	 * @return the bytes.
	 * @throws IOException
	 *             thrown if any exceptions occur.
	 */
	public static byte[] getFileBytes(final File in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(in);
			byte[] buffer = new byte[5 * 1024];
			int len = -1;
			while ((len = fis.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			fis.close();
			return baos.toByteArray();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}
	}

	/**
	 * Gets the J2KMetadata associated with the specified JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file.
	 * @return the metadata object or null.
	 */
	public static J2KMetadata getMetadata(final File in) {
		// bail early if not J2K
		if (!isJ2K(in)) {
			return null;
		}

		// get our image
		RenderedOp img = JAI.create("ImageRead", in);

		// get our metadata object
		Object md = img.getProperty("JAI.ImageMetadata");
		if (md != null && md instanceof J2KMetadata) {
			return (J2KMetadata) md;
		} else {
			return null;
		}
	}

	/**
	 * Gets the UUID payloads of a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This must be a JPEG 2000 file.
	 * @return the maps of UUID payloads.
	 */
	public static Map<UUID, byte[]> getUUIDPayloads(final File in) {
		// our XML
		Map<UUID, byte[]> map = new HashMap<UUID, byte[]>();
		if (!isJ2K(in)) {
			return map;
		}

		// get our metadata object
		J2KMetadata metadata = getMetadata(in);
		if (metadata != null) {
			Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
			List<Node> other = findChildren(root, "OtherBoxes");
			if (other.size() > 0) {
				List<Node> uuids = findChildren(other.get(0), JPEG2000UUIDBOX);
				for (Node u : uuids) {
					List<Node> id = findChildren(u, "UUID");
					List<Node> data = findChildren(u, "Data");
					if (id.size() > 0 && data.size() > 0) {
						UUID uuid = uuidFromBytes(stringToBytes(id.get(0).getNodeValue()));
						map.put(uuid, stringToBytes(data.get(0).getNodeValue()));
					}
				}
			}
		}
		return map;
	}

	/**
	 * Gets the XML payloads of a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This must be a JPEG 2000 file.
	 * @return a list of XML payloads in the file.
	 */
	public static List<String> getXMLPayloads(final File in) {
		// our XML
		List<String> list = new ArrayList<String>();

		// get our metadata object
		J2KMetadata metadata = getMetadata(in);
		if (metadata != null) {
			Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
			List<Node> other = findChildren(root, "OtherBoxes");
			if (other.size() > 0) {
				List<Node> uuids = findChildren(other.get(0), JPEG2000XMLBOX);
				for (Node u : uuids) {
					List<Node> data = findChildren(u, "Content");
					if (data.size() > 0) {
						list.add(data.get(0).getNodeValue());
					}
				}
			}
		}
		return list;
	}

	/**
	 * Checks whether the file is a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file.
	 * @return true if the file is JPEG 2000, false otherwise.
	 */
	public static boolean isJ2K(final File in) {
		ImageInputStream iis = null;
		try {
			iis = ImageIO.createImageInputStream(in);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				return "jpeg 2000".equals(reader.getFormatName().toLowerCase());
			} else {
				return false;
			}
		} catch (IOException e) {
			return false;
		} finally {
			if (iis != null) {
				try {
					iis.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}
	}

	/**
	 * Checks whether JPEG 2000 is supported.
	 * 
	 * @return true if JPEG 2000 is supported, false otherwise.
	 */
	public static boolean isJ2KSupported() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(JPEG2000);
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG2000);
		return readers.hasNext() && writers.hasNext();
	}

	/**
	 * Print the metadata associated with the specified JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file.
	 * @param pw
	 *            the print writer.
	 */
	public static void printMetadata(final File in, final PrintWriter pw) {
		// bail early
		if (!isJ2K(in)) {
			return;
		}

		// get our metadata object
		RenderedOp img = JAI.create("ImageRead", in);
		Object md = img.getProperty("JAI.ImageMetadata");
		if (md != null && md instanceof J2KMetadata) {
			J2KMetadata metadata = (J2KMetadata) md;
			printNode(metadata.getAsTree(metadata.getNativeMetadataFormatName()), 0, pw);
		}
	}

	private static void printNode(final Node node, final int level, final PrintWriter pw) {
		String prefix = "";
		for (int i = 0; i < level; i++) {
			prefix = prefix + "    ";
		}

		// print out our name and value
		pw.println(prefix + node.getNodeName() + " = " + node.getNodeValue());

		// print out our attributes
		NamedNodeMap attrs = node.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Node a = attrs.item(i);
			pw.println(prefix + "  @" + a.getNodeName() + "=" + a.getNodeValue());
		}

		// print out our children
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			printNode(children.item(i), level + 1, pw);
		}

		// flush
		pw.flush();
	}

	private static byte[] stringToBytes(final String s) {
		String[] split = s.split(" ");
		byte[] bytes = new byte[split.length];
		for (int i = 0; i < split.length; i++) {
			bytes[i] = Byte.parseByte(split[i]);
		}
		return bytes;
	}

	/**
	 * Strip all UUID entries from a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file. This must be a JPEG 2000 file.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 * @throws IOException
	 *             thrown if any exception occurs.
	 */
	/*
	 * public static void stripUUID(final File in, final File out) throws
	 * IOException { // bail early if (!isJ2K(in)) { return; }
	 * 
	 * // read in the image BufferedImage img = ImageIO.read(in);
	 * 
	 * // get our writer Iterator<ImageWriter> iter = ImageIO
	 * .getImageWritersByFormatName(JPEG2000); if (!iter.hasNext()) { throw new
	 * IOException("Can't write image of type JPEG2000"); } ImageWriter writer =
	 * iter.next(); // grab the first one ImageOutputStream ios =
	 * ImageIO.createImageOutputStream(out); writer.setOutput(ios);
	 * ImageTypeSpecifier typeSpec = ImageTypeSpecifier
	 * .createFromRenderedImage(img);
	 * 
	 * // get our metadata J2KMetadata metadata; if (isJ2K(in)) { metadata =
	 * getMetadata(in); } else { metadata = new J2KMetadata(typeSpec,
	 * writer.getDefaultWriteParam(), writer); }
	 * 
	 * // Strip XML boxes Box box = metadata.getElement(JPEG2000UUIDBOX); while
	 * (box != null) { metadata.removeNode(box); box =
	 * metadata.getElement(JPEG2000UUIDBOX); }
	 * 
	 * // write it out IIOImage iioImg = new IIOImage(img, null, metadata);
	 * writer.write(iioImg); }
	 */

	/**
	 * Strip the XML payload from a JPEG 2000 file.
	 * 
	 * @param in
	 *            the input file.
	 * @param out
	 *            the output file. This will be written as JPEG 2000.
	 */
	/*
	 * public static void stripXML(final File in, final File out) throws
	 * IOException { // bail early if (!isJ2K(in)) { return; }
	 * 
	 * // read in the image BufferedImage img = ImageIO.read(in);
	 * 
	 * // get our writer Iterator<ImageWriter> iter = ImageIO
	 * .getImageWritersByFormatName(JPEG2000); if (!iter.hasNext()) { throw new
	 * IOException("Can't write image of type JPEG2000"); } ImageWriter writer =
	 * iter.next(); // grab the first one ImageOutputStream ios =
	 * ImageIO.createImageOutputStream(out); writer.setOutput(ios);
	 * ImageTypeSpecifier typeSpec = ImageTypeSpecifier
	 * .createFromRenderedImage(img);
	 * 
	 * // get our metadata J2KMetadata metadata; if (isJ2K(in)) { metadata =
	 * getMetadata(in); } else { metadata = new J2KMetadata(typeSpec,
	 * writer.getDefaultWriteParam(), writer); }
	 * 
	 * // Strip XML boxes Box box = metadata.getElement(JPEG2000XMLBOX); while
	 * (box != null) { metadata.removeNode(box); box =
	 * metadata.getElement(JPEG2000XMLBOX); }
	 * 
	 * // write it out IIOImage iioImg = new IIOImage(img, null, metadata);
	 * writer.write(iioImg); }
	 */

	private static UUID uuidFromBytes(final byte[] b) {
		long msb = ((long) b[7] & 0xFF) + (((long) b[6] & 0xFF) << 8) + (((long) b[5] & 0xFF) << 16) + (((long) b[4] & 0xFF) << 24)
				+ (((long) b[3] & 0xFF) << 32) + (((long) b[2] & 0xFF) << 40) + (((long) b[1] & 0xFF) << 48) + (((long) b[0] & 0xFF) << 56);

		long lsb = ((long) b[15] & 0xFF) + (((long) b[14] & 0xFF) << 8) + (((long) b[13] & 0xFF) << 16) + (((long) b[12] & 0xFF) << 24)
				+ (((long) b[11] & 0xFF) << 32) + (((long) b[10] & 0xFF) << 40) + (((long) b[9] & 0xFF) << 48) + (((long) b[8] & 0xFF) << 56);
		return new UUID(msb, lsb);
	}

	private static byte[] uuidToBytes(final UUID uuid) {
		String s = uuid.toString().replace("-", "");
		byte[] bytes = new byte[16];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}

	private J2KUtils() {
		// not intended to be instantiated
	}
}
