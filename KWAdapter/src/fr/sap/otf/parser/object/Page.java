package fr.sap.otf.parser.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import fr.sap.otf.parser.geometry.Box;
import fr.sap.otf.parser.geometry.Dimension;
import fr.sap.otf.parser.geometry.Position;
import fr.sap.otf.parser.object.definition.CommandDefinition;

public class Page {

	private List<Command> contents = new ArrayList<Command>();

	static Properties charsetMapping = new Properties();
	static {
		try {
			charsetMapping.load(CommandDefinition.class.getClassLoader()
					.getResourceAsStream("fr/sap/otf/parser/charsetMapping.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Page() {
		// TODO Auto-generated constructor stub
	}

	public String getText(Box box, boolean remove) {
		// TODO

		List<Command> textContained = new ArrayList<Command>();
		Map<Command, Box> textBoxes = new HashMap<Command, Box>();
		for (Command command : getContents()) {
			if (StringUtils.compare(command.getID(), "ST") == 0) {
				Box textBox = findBox(command);
				if (intersection(box, textBox)) {
					textContained.add(command);
					textBoxes.put(command, textBox);
				}
			}
		}

		textContained.sort(new Comparator<Command>() {

			@Override
			public int compare(Command arg0, Command arg1) {

				Box box0 = textBoxes.get(arg0);
				Box box1 = textBoxes.get(arg1);

				if (box0.getPosition().getY() > box1.getPosition().getY())
					return 1;
				if (box0.getPosition().getY() < box1.getPosition().getY())
					return -1;
				if (box0.getPosition().getX() < box1.getPosition().getX())
					return -1;
				if (box0.getPosition().getX() > box1.getPosition().getX())
					return 1;

				return 0;
			}
		});

		StringBuffer sb = new StringBuffer();
		long lastY = -1;
		for (Command textCommand : textContained) {
			Box textBox = textBoxes.get(textCommand);
			String text = getTextIntersection(box, textCommand, textBox, remove);
			if (textBox.getPosition().getY() != lastY && lastY != -1 && StringUtils.isNoneBlank(text)) {
				sb.append("\n");
			} else if (lastY != -1) {
				// sb.append(" ");
			}

			sb.append(text);
			lastY = textBox.getPosition().getY();
		}

		return sb.toString();
	}

	protected boolean contains(Box bigBox, Box smallBox) {
		boolean point1_OK = false;
		boolean point2_OK = false;
		boolean point3_OK = false;
		boolean point4_OK = false;

		Position pos = new Position();
		pos.setX(smallBox.getPosition().getX());
		pos.setY(smallBox.getPosition().getY());

		if (inside(bigBox, pos))
			point1_OK = true;

		pos.setX(smallBox.getPosition().getX());
		pos.setY(smallBox.getPosition().getY() + smallBox.getDimension().getH());
		if (inside(bigBox, pos))
			point2_OK = true;
		;

		pos.setX(smallBox.getPosition().getX() + smallBox.getDimension().getW());
		pos.setY(smallBox.getPosition().getY() + smallBox.getDimension().getH());
		if (inside(bigBox, pos))
			point3_OK = true;

		pos.setX(smallBox.getPosition().getX() + smallBox.getDimension().getW());
		pos.setY(smallBox.getPosition().getY());
		if (inside(bigBox, pos))
			point4_OK = true;

		return point1_OK && point2_OK && point3_OK && point4_OK;
	}

	protected boolean intersection(Box box1, Box box2) {

		Position pos = new Position();
		pos.setX(box1.getPosition().getX());
		pos.setY(box1.getPosition().getY());

		if (inside(box2, pos))
			return true;

		pos.setX(box2.getPosition().getX());
		pos.setY(box2.getPosition().getY());

		if (inside(box1, pos))
			return true;

		pos.setX(box1.getPosition().getX());
		pos.setY(box1.getPosition().getY() + box1.getDimension().getH());
		if (inside(box2, pos))
			return true;

		pos.setX(box2.getPosition().getX());
		pos.setY(box2.getPosition().getY() + box2.getDimension().getH());
		if (inside(box1, pos))
			return true;

		pos.setX(box1.getPosition().getX() + box1.getDimension().getW());
		pos.setY(box1.getPosition().getY() + box1.getDimension().getH());
		if (inside(box2, pos))
			return true;

		pos.setX(box2.getPosition().getX() + box2.getDimension().getW());
		pos.setY(box2.getPosition().getY() + box1.getDimension().getH());
		if (inside(box1, pos))
			return true;

		pos.setX(box1.getPosition().getX() + box1.getDimension().getW());
		pos.setY(box1.getPosition().getY());
		if (inside(box2, pos))
			return true;

		pos.setX(box2.getPosition().getX() + box2.getDimension().getW());
		pos.setY(box2.getPosition().getY());
		if (inside(box1, pos))
			return true;

		return false;
	}

	protected boolean inside(Box box, Position pos) {
		long x1 = box.getPosition().getX();
		long x2 = box.getPosition().getX() + box.getDimension().getW();
		if (x1 > x2) {
			long x = x1;
			x1 = x2;
			x2 = x;
		}

		long y1 = box.getPosition().getY();
		long y2 = box.getPosition().getY() + box.getDimension().getH();
		if (y1 > y2) {
			long y = y1;
			y1 = y2;
			y2 = y;
		}

		if (pos.getX() >= x1)
			if (pos.getX() <= x2)
				if (pos.getY() >= y1)
					if (pos.getY() <= y2)
						return true;

		return false;

	}

	public String getTextIntersection(Box box, Command textCommand, Box textCommandBox, boolean removeText) {
		String text = textCommand.getParameters().get(2).getValue();
		StringBuffer textResult = new StringBuffer();
		// if (! removeText) textResult.append(text);

		int nbsapCode = StringUtils.countMatches(text, '#');

		int nbChars = text.length() - 3 * nbsapCode;

		int width = Integer.parseInt(textCommand.getParameters().get(0).getValue());

		StringBuffer textIntersection = new StringBuffer();

		double meanCharWidth = ((double) width) / ((double) nbChars);

		int charCounter = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String sapCode = null;
			if (c == '#') {
				sapCode = StringUtils.substring(text, i + 1, i + 4);
				String decoded = decodeSapCode(sapCode);
				c = decoded.charAt(0);
				i = i + 3;

			}

			long x = textCommandBox.getPosition().getX() + Math.round(charCounter * meanCharWidth);
			Position p = new Position();
			p.setX(x);
			p.setY(textCommandBox.getPosition().getY());

			Box charBox = new Box();
			charBox.setPosition(p);
			charBox.getDimension().setW(Math.round(meanCharWidth - 0.5));
			charBox.getDimension().setH(textCommandBox.getDimension().getH());

			charCounter++;
			if (contains(box, charBox)) {// char dans le cadre
				if (removeText) {
					textResult.append(" ");
				} else {
					textResult.append(c);
					if (c == '#') {
						textResult.append(sapCode);
					}
				}

				textIntersection.append(c);

			} else {
				textResult.append(c);
				if (c == '#') {
					textResult.append(sapCode);
				}
			}
		}
		if (removeText) {
			textCommand.getParameters().get(2).setValue(textResult.toString());
			textCommand.setLine(StringUtils.substring(textCommand.getLine(), 0, 9) + textResult.toString());
		}
		return textIntersection.toString();
	}

	public static String decodeSapCode(String sapCode) {// sapcode sans le #
		String uncCode = charsetMapping.getProperty(sapCode);
		if (uncCode != null) {
			return uncCode;
		}
		return sapCode;
	}

	public Box findBox(Command textCommand) {
		Dimension dim = findDimension(textCommand);
		Position pos = findPosition(textCommand);
		Box box = new Box();
		box.setPosition(pos);
		box.setDimension(dim);
		return box;

	}

	protected Dimension findDimension(Command textCommand) {

		Command previous = textCommand.getPreviousCommand();
		if (StringUtils.compare(textCommand.getID(), "ST") == 0)
			while (previous != null) {
				if (StringUtils.compare(previous.getID(), "FC") == 0) {
					// 1pt = 20 twips --> 0.1pt = 2twip
					int fontSize1_10_Pt = Integer.parseInt(previous.getParameters().get(1).getValue());// 1/10 pt
					int fontHeight = fontSize1_10_Pt * 2 / 10;// twip
					int w = Integer.parseInt(textCommand.getParameters().get(0).getValue());
					Dimension dim = new Dimension();
					dim.setH(-fontHeight);
					dim.setW(w);
					return dim;
				}
				previous = previous.getPreviousCommand();

			}
		return new Dimension();
	}

	protected Position findPosition(Command textCommand) {
		Command previous = textCommand.getPreviousCommand();

		while (previous != null) {
			if (StringUtils.compare(previous.getID(), "ST") == 0) {
				Position lastPosition = findPosition(previous);
				Position newPosition = lastPosition;
				newPosition.setX(newPosition.getX() + Integer.parseInt(previous.getParameters().get(0).getValue()));
				return newPosition;
			} else if (StringUtils.compare(previous.getID(), "MT") == 0) {
				Position position = new Position();
				position.setX(Integer.parseInt(previous.getParameters().get(0).getValue()));
				position.setY(Integer.parseInt(previous.getParameters().get(1).getValue()));
				return position;
			}
			previous = previous.getPreviousCommand();

		}
		return new Position();
	}

	public static long pointToTwip(long nbPoints) {
		// 1pt = 20 twip
		return nbPoints * 20;
	}

	public static long twipToPoint(long nbTwips) {
		// 1pt = 20 twip

		return Math.round((double) nbTwips / 20.0);
	}

	public List<Command> getContents() {
		return contents;
	}

	public void sortBoxes() {
		List<Command> boxes = new ArrayList<Command>();
		int insertIndex = -1;
		int index = 0;
		for (Command c : contents) {

			if (c.getID().equals("BX")) {
				boxes.add(c);
			} else if (insertIndex < 0) {
				if (c.getID().startsWith("MT") || c.getID().startsWith("FC"))
					insertIndex = index;
			}

			index++;

		}

		if (insertIndex > 0) {
			for (Command box : boxes) {
				contents.remove(box);
			}

			contents.addAll(insertIndex, boxes);
		}

	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Command c : contents) {
			sb.append(c);
			sb.append("\n");

		}

		return "Page [contents=" + sb.toString() + "]";
	}
}
