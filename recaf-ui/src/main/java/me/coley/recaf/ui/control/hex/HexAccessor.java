package me.coley.recaf.ui.control.hex;

import me.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper of a {@code byte[]} for hex operations.
 *
 * @author Matt Coley
 */
public class HexAccessor {
	/**
	 * Base for hex operations.
	 */
	public static final int HEX_RADIX = 16;
	private final HexView view;
	private byte[] data;

	/**
	 * New accessor with no backing data.
	 *
	 * @param view
	 * 		Associated hex viewer component.
	 */
	public HexAccessor(HexView view) {
		this(view, new byte[0]);
	}

	/**
	 * @param data
	 * 		Initial backing array.
	 * @param view
	 * 		Associated hex viewer component.
	 */
	public HexAccessor(HexView view, byte[] data) {
		this.view = view;
		this.data = data;
	}

	/**
	 * @param data
	 * 		New backing array.
	 */
	public void setBacking(byte[] data) {
		this.data = data;
	}

	/**
	 * @param hex
	 * 		Hex string.
	 *
	 * @return Value casted to byte.
	 */
	public int toHex(String hex) {
		return Integer.parseInt(hex, HEX_RADIX);
	}

	/**
	 * Text of hex at offset.
	 *
	 * @param offset
	 * 		Offset to peek.
	 *
	 * @return Hex string at offset.
	 */
	public String getHexStringAtOffset(int offset) {
		if (offset >= data.length || offset < 0)
			return "  ";
		else
			return StringUtil.fillLeft(2, "0", HexView.caseHex(Integer.toHexString(data[offset] & 0xff)));
	}

	/**
	 * @param offset
	 * 		Offset to peek.
	 *
	 * @return Value at offset, or {@link Integer#MIN_VALUE} if the value is not inside the data bounds.
	 */
	public int getHexAtOffset(int offset) {
		if (offset >= data.length || offset < 0)
			return Integer.MIN_VALUE;
		else
			return data[offset];
	}

	/**
	 * @param offset
	 * 		Offset to write to.
	 * @param value
	 * 		Value to write.
	 */
	public void setHexAtOffset(int offset, int value) {
		if (offset >= 0 && offset < data.length)
			data[offset] = (byte) (value & 0xFF);
	}

	/**
	 * This is used for the right-most column in the {@link HexView}.
	 *
	 * @param offset
	 * 		Offset to start from.
	 *
	 * @return Text representation of the hex data starting at the given offset.
	 */
	public String getPreviewAtOffset(int offset) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < view.getHexColumns(); j++) {
			int i = offset + j;
			if (i >= data.length)
				sb.append(' ');
			else {
				char c = (char) data[i];
				if (c >= 32 && c <= 126) {
					sb.append(c);
				} else {
					sb.append('.');
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Used to create the offsets shown on the left-most column of {@link HexView}.
	 *
	 * @return List of integers starting from 0, incrementing by {@link HexView#getHexColumns()} until the data length is reached.
	 */
	public List<Integer> computeOffsetsInRange() {
		List<Integer> newOffsets = new ArrayList<>();
		int offset = 0;
		while (offset <= data.length) {
			newOffsets.add(offset);
			offset += view.getHexColumns();
		}
		return newOffsets;
	}
}