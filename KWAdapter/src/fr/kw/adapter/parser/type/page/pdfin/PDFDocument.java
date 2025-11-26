package fr.kw.adapter.parser.type.page.pdfin;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.text.ShowText;
import org.apache.pdfbox.contentstream.operator.text.ShowTextAdjusted;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLine;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLineAndSpace;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;

import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.document.ADocument;
import fr.kw.adapter.parser.type.page.document.Field;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PointMm;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.utils.LogHelper;

public class PDFDocument extends ADocument {
	public PDFDocument(DocumentParserDefinition definition) {
		this.definition = definition;

	}

	@Override
	public void init(Object originalPDF) {
		this.document = new PDDocument();

		((PDDocument) document).getDocument().setVersion(((PDDocument) originalPDF).getVersion());
		((PDDocument) document).setDocumentInformation(((PDDocument) originalPDF).getDocumentInformation());
		((PDDocument) document).getDocumentCatalog()
				.setViewerPreferences(((PDDocument) originalPDF).getDocumentCatalog().getViewerPreferences());
	}

	@Override
	public void createFile(File target) throws IOException, FileProcessException {// TODO

		LogHelper.info("Creating PDF " + target);
		for (Field f : fields.values()) {
			LogHelper.debug(f.getName() + "=" + f.getValue());

		}

		for (int i = 1; i <= pages.size(); i++) {
			Page page = copyPage(i);
			((PDDocument) document).addPage((PDPage) page.getNativePage());
		}

		this.fileResult = target;
		((PDDocument) document).save(target);
		((PDDocument) document).close();
	}

	protected Page copyPage(int pageNum) throws IOException {

		float[][] tm = new float[3][3];
		tm[0][0] = 1;
		tm[0][1] = 0;
		tm[0][2] = 0;
		tm[1][0] = 0;
		tm[1][1] = 1;
		tm[1][2] = 0;
		tm[2][0] = 0;
		tm[2][1] = 0;
		tm[2][2] = 1;

		float[][] tm2 = null;

		Page page = this.pages.get(pageNum - 1);
		// float pageHeight = ((PDPage) page.getNativePage()).getMediaBox().getHeight();
		PDFCloneUtility cloner = new PDFCloneUtility((PDDocument) document);

		boolean first = pageNum == 1;
		boolean last = pageNum == pages.size();
		List<FieldDefinition> toRemove = new ArrayList<FieldDefinition>();
		List<FieldDefinition> toMove = new ArrayList<FieldDefinition>();

		for (FieldDefinition fDef : this.definition.getFields()) {
			boolean pageMatch = false;
			switch (fDef.getPage().getPageType()) {
			case ANY:
				pageMatch = true;
				break;
			case FIRST:
				pageMatch = first;
				break;
			case LAST:
				pageMatch = last;
				break;
			case NEXT:
				pageMatch = !first;
				break;
			case EXACT:
				pageMatch = pageNum == fDef.getPage().getExactPage();
				break;
			}
			if (pageMatch && fDef.isRemoveText()) {
				// System.out.println("Page " + pageNum + ", field Definition " + fDef.getName()
				// + "(" + fDef.getPage().getPageType() + ")");
				Rectangle2D rec = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
						PointMm.getPoints(fDef.getPosition().getY()), PointMm.getPoints(fDef.getDimension().getX()),
						PointMm.getPoints(fDef.getDimension().getY()));
				toRemove.add(fDef);
			} else if (pageMatch && fDef.getTranslate() != null && !fDef.getTranslate().equals(PointMm.ZERO)) {
				toMove.add(fDef);
			}

		}

		PDFStreamParser parser = new PDFStreamParser((PDPage) page.getNativePage());
		parser.parse();
		List<Object> tokens = parser.getTokens();

		// System.out.println("nb objects = " + tokens.size());
		boolean parsingTextObject = false;

		List<Object> textInstructions = new ArrayList<Object>();
		List<Object> newPageObjects = new ArrayList<Object>();
		StringBuffer text = new StringBuffer();
		List<String> textTokens = new ArrayList<String>();
		float x = Float.MIN_VALUE;
		float y = Float.MIN_VALUE;
		float pageHeight = ((PDPage) page.getNativePage()).getMediaBox().getHeight();

		for (int i = 0; i < tokens.size(); i++) {
			tm2 = null;
			Object next = tokens.get(i);
			// String currTokenText = (next instanceof COSString) ? ((COSString)
			// next).getString() : next.toString();
			// System.out.println("Page " + pageNum + ", object : " + next);
			if (next instanceof Operator) {
				Operator op = (Operator) next;

				switch (op.getName()) {

				case "Tm":

					// Les deux précédents objets étaient la position du texte
					// TODO : repérer x et y dans newPageObjects, les 2 derniers
					if (newPageObjects.size() >= 6) {
						// 1 2 3 4 5 6 tm
						tm[0][0] = getFloat(newPageObjects.get(newPageObjects.size() - 6));
						tm[0][1] = getFloat(newPageObjects.get(newPageObjects.size() - 5));
						tm[1][0] = getFloat(newPageObjects.get(newPageObjects.size() - 4));
						tm[1][1] = getFloat(newPageObjects.get(newPageObjects.size() - 3));
						tm[2][0] = getFloat(newPageObjects.get(newPageObjects.size() - 2));
						tm[2][1] = getFloat(newPageObjects.get(newPageObjects.size() - 1));

						x = tm[2][0];
						y = tm[2][1];

						tm2 = tm;

					}
					// System.out.println("(x;y)=(" + x + ";" + y + ")");
					break;
				case "Td":
				case "TD":

					float[][] position = new float[3][3];
					position[0][0] = 1;
					position[0][1] = 0;
					position[0][2] = 0;
					position[1][0] = 0;
					position[1][1] = 1;
					position[1][2] = 0;

					position[2][0] = getFloat(newPageObjects.get(newPageObjects.size() - 2));// x
					position[2][1] = getFloat(newPageObjects.get(newPageObjects.size() - 1));// y
					position[2][2] = 1;// pour produits matrice

					x = position[2][0] * tm[0][0] + position[2][1] * tm[1][0] + position[2][2] * tm[2][0];
					y = position[2][0] * tm[0][1] + position[2][1] * tm[1][1] + position[2][2] * tm[2][1];

					tm2 = matriceX(position, tm);

					break;
				default:
				}
			}
			if ((next instanceof COSString) || (next instanceof ShowTextLine || next instanceof ShowTextLineAndSpace)) {// (next
																														// instanceof
																														// ShowText
																														// ||
																														// next
																														// instanceof
																														// ShowTextAdjusted)
																														// {
				Point2D point = new Point2D.Float(x, pageHeight - y);
				boolean removeText = false;
				for (FieldDefinition fDef : toRemove) {
					Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
							PointMm.getPoints(fDef.getPosition().getY()), PointMm.getPoints(fDef.getDimension().getX()),
							PointMm.getPoints(fDef.getDimension().getY()));

					// StringBuffer tmpSt = new StringBuffer();
					// for (String s : textTokens) tmpSt.append(s);

					// System.out.println("(" + point.getX() + ";" + point.getY() + ") in
					// rect(x;y;w;h)=(" + rect.getX() + ";" + rect.getY() + ";" + rect.getWidth() +
					// ";" + rect.getHeight() + ") -> " + rect.contains(point));

					if (rect.contains(point)) {
						removeText = true;
						LogHelper.info("(" + point.getX() + ";" + point.getY() + ") in rect(x;y;w;h)=(" + rect.getX()
								+ ";" + rect.getY() + ";" + rect.getWidth() + ";" + rect.getHeight() + ") -> "
								+ rect.contains(point));

						LogHelper.info("Page " + pageNum + ", removing field " + fDef.getName() + "(" + next + ")");
					}
				}

				if (!removeText)
					for (FieldDefinition fDef : toMove) {
						Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
								PointMm.getPoints(fDef.getPosition().getY()),
								PointMm.getPoints(fDef.getDimension().getX()),
								PointMm.getPoints(fDef.getDimension().getY()));
						if (rect.contains(point) && x != Float.MIN_VALUE && y != Float.MIN_VALUE) {

							COSFloat newX = new COSFloat(getFloat(newPageObjects.get(newPageObjects.size() - 3))
									+ PointMm.getPoints(fDef.getTranslate().getX()) / tm[0][0]);

							COSFloat newY = new COSFloat(getFloat(newPageObjects.get(newPageObjects.size() - 2))
									- PointMm.getPoints(fDef.getTranslate().getY()) / tm[1][1]);

							newPageObjects.set(newPageObjects.size() - 3, newX);
							newPageObjects.set(newPageObjects.size() - 2, newY);

						}
					}

				if (!removeText) {
					newPageObjects.add(next);
				} else {

					if (next instanceof COSString) {
						((COSString) next).setValue("".getBytes());
					}
					newPageObjects.add(next);
				}
			} else {
				newPageObjects.add(next);
			}

			if (tm2 != null)
				tm = tm2;// mise à jour matrice coordonnées
		}

		PDPage newPage = new PDPage(((PDPage) page.getNativePage()).getMediaBox());
		PDStream newContents = new PDStream((PDDocument) document);
		OutputStream os = newContents.createOutputStream();
		ContentStreamWriter writer = new ContentStreamWriter(os);

		// System.out.println("nb objects in new page = " + newPageObjects.size());
		for (Object obj : newPageObjects) {
			if (obj instanceof COSBase) {
				writer.writeToken(cloner.cloneForNewDocument(obj));
			} else {
				writer.writeTokens(obj);
			}
		}
		os.flush();
		os.close();

		newPage.setContents(newContents);
		newPage.setResources(((PDPage) page.getNativePage()).getResources());

		Page newInterPage = new Page(newPage);
		newInterPage.setPageDocumentDefinition(page.getPageDocumentDefinition());

		return newInterPage;

	}

	protected Page copyPage_OBSOLETE2(int pageNum) throws IOException {

		Page page = this.pages.get(pageNum - 1);
		// float pageHeight = ((PDPage) page.getNativePage()).getMediaBox().getHeight();
		PDFCloneUtility cloner = new PDFCloneUtility((PDDocument) document);

		boolean first = pageNum == 1;
		boolean last = pageNum == pages.size();
		List<FieldDefinition> toRemove = new ArrayList<FieldDefinition>();
		List<FieldDefinition> toMove = new ArrayList<FieldDefinition>();

		for (FieldDefinition fDef : this.definition.getFields()) {
			boolean pageMatch = false;
			switch (fDef.getPage().getPageType()) {
			case ANY:
				pageMatch = true;
				break;
			case FIRST:
				pageMatch = first;
				break;
			case LAST:
				pageMatch = last;
				break;
			case NEXT:
				pageMatch = !first;
				break;
			case EXACT:
				pageMatch = pageNum == fDef.getPage().getExactPage();
				break;
			}
			if (pageMatch && fDef.isRemoveText()) {
				// System.out.println("Page " + pageNum + ", field Definition " + fDef.getName()
				// + "(" + fDef.getPage().getPageType() + ")");
				Rectangle2D rec = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
						PointMm.getPoints(fDef.getPosition().getY()), PointMm.getPoints(fDef.getDimension().getX()),
						PointMm.getPoints(fDef.getDimension().getY()));
				toRemove.add(fDef);
			} else if (pageMatch && fDef.getTranslate() != null && !fDef.getTranslate().equals(PointMm.ZERO)) {
				toMove.add(fDef);
			}

		}

		PDFStreamParser parser = new PDFStreamParser((PDPage) page.getNativePage());
		parser.parse();
		List<Object> tokens = parser.getTokens();

		// System.out.println("nb objects = " + tokens.size());
		boolean parsingTextObject = false;

		List<Object> textInstructions = new ArrayList<Object>();
		List<Object> newPageObjects = new ArrayList<Object>();
		StringBuffer text = new StringBuffer();
		List<String> textTokens = new ArrayList<String>();
		float x = -1;
		float y = -1;
		float factorX = 1;
		float factorY = 1;
		boolean relative = false;
		COSFloat cosX = null;
		COSFloat cosY = null;

		for (int i = 0; i < tokens.size(); i++) {
			Object next = tokens.get(i);
			// String currTokenText = (next instanceof COSString) ? ((COSString)
			// next).getString() : next.toString();
			// System.out.println("Page " + pageNum + ", object : " + next);
			if (next instanceof Operator) {
				Operator op = (Operator) next;

				switch (op.getName()) {

				case "Tm":
					relative = false;
					// Les deux précédents objets étaient la position du texte
					// TODO : repérer x et y dans newPageObjects, les 2 derniers
					if (newPageObjects.size() >= 6) {
						Object lastInstr = newPageObjects.get(newPageObjects.size() - 1);
						if (lastInstr instanceof COSFloat) {
							y = ((PDPage) page.getNativePage()).getMediaBox().getHeight()
									- ((COSFloat) lastInstr).floatValue();
							cosY = (COSFloat) lastInstr;

						}
						lastInstr = newPageObjects.get(newPageObjects.size() - 2);
						if (lastInstr instanceof COSFloat) {
							x = ((COSFloat) lastInstr).floatValue();
							cosX = (COSFloat) lastInstr;

						}
						lastInstr = newPageObjects.get(newPageObjects.size() - 3);
						if (lastInstr instanceof COSInteger) {
							factorY = ((COSInteger) lastInstr).floatValue();

						}
						lastInstr = newPageObjects.get(newPageObjects.size() - 6);
						if (lastInstr instanceof COSInteger) {
							factorX = ((COSInteger) lastInstr).floatValue();

						}
					}
					// System.out.println("(x;y)=(" + x + ";" + y + ")");
					break;
				case "Td":
				case "TD":
					relative = true;
					if (newPageObjects.size() >= 2) {
						Object lastInstr = newPageObjects.get(newPageObjects.size() - 1);
						if (lastInstr instanceof COSFloat) {
							y = (y + ((COSFloat) lastInstr).floatValue() * factorX);
							cosY = (COSFloat) lastInstr;
						}
						lastInstr = newPageObjects.get(newPageObjects.size() - 2);
						if (lastInstr instanceof COSFloat) {
							x = x + factorY * ((COSFloat) lastInstr).floatValue();
							cosX = (COSFloat) lastInstr;
						}
					}
					break;
				default:
				}
			}
			if ((next instanceof COSString) || (next instanceof ShowTextLine || next instanceof ShowTextLineAndSpace)) {// (next
																														// instanceof
																														// ShowText
																														// ||
																														// next
																														// instanceof
																														// ShowTextAdjusted)
																														// {
				Point2D point = new Point2D.Float(x, y);
				boolean removeText = false;
				for (FieldDefinition fDef : toRemove) {
					Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
							PointMm.getPoints(fDef.getPosition().getY()), PointMm.getPoints(fDef.getDimension().getX()),
							PointMm.getPoints(fDef.getDimension().getY()));

					// StringBuffer tmpSt = new StringBuffer();
					// for (String s : textTokens) tmpSt.append(s);

					// System.out.println("(" + point.getX() + ";" + point.getY() + ") in
					// rect(x;y;w;h)=(" + rect.getX() + ";" + rect.getY() + ";" + rect.getWidth() +
					// ";" + rect.getHeight() + ") -> " + rect.contains(point));

					if (rect.contains(point)) {
						removeText = true;
						LogHelper.info("(" + point.getX() + ";" + point.getY() + ") in rect(x;y;w;h)=(" + rect.getX()
								+ ";" + rect.getY() + ";" + rect.getWidth() + ";" + rect.getHeight() + ") -> "
								+ rect.contains(point));

						LogHelper.info("Page " + pageNum + ", removing field " + fDef.getName() + "(" + next + ")");
					}
				}

				if (!removeText)
					for (FieldDefinition fDef : toMove) {
						Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
								PointMm.getPoints(fDef.getPosition().getY()),
								PointMm.getPoints(fDef.getDimension().getX()),
								PointMm.getPoints(fDef.getDimension().getY()));
						if (rect.contains(point) && cosX != null && cosY != null) {

							COSFloat newX = new COSFloat(
									cosX.floatValue() + PointMm.getPoints(fDef.getTranslate().getX()) / factorX);

							COSFloat newY = new COSFloat(
									cosY.floatValue() + PointMm.getPoints(fDef.getTranslate().getY()) / factorY);

							int xIdx = newPageObjects.indexOf(cosX);
							int yIdx = newPageObjects.indexOf(cosY);

							if (xIdx >= 0 && yIdx > 0) {
								newPageObjects.set(xIdx, newX);
								newPageObjects.set(yIdx, newY);
							} else {
								LogHelper.error("Could not move text field " + fDef.getName());
							}
						}
					}

				if (!removeText) {
					newPageObjects.add(next);
				} else {

					if (next instanceof COSString) {
						((COSString) next).setValue("".getBytes());
					}
					newPageObjects.add(next);
				}
			} else {
				newPageObjects.add(next);
			}
		}

		PDPage newPage = new PDPage(((PDPage) page.getNativePage()).getMediaBox());
		PDStream newContents = new PDStream((PDDocument) document);
		OutputStream os = newContents.createOutputStream();
		ContentStreamWriter writer = new ContentStreamWriter(os);

		// System.out.println("nb objects in new page = " + newPageObjects.size());
		for (Object obj : newPageObjects) {
			if (obj instanceof COSBase) {
				writer.writeToken(cloner.cloneForNewDocument(obj));
			} else {
				writer.writeTokens(obj);
			}
		}
		os.flush();
		os.close();

		newPage.setContents(newContents);
		newPage.setResources(((PDPage) page.getNativePage()).getResources());

		Page newInterPage = new Page(newPage);
		newInterPage.setPageDocumentDefinition(page.getPageDocumentDefinition());

		return newInterPage;

	}

	protected Page copyPage_OBSOLETE(int pageNum) throws IOException {

		Page page = this.pages.get(pageNum - 1);
		// float pageHeight = ((PDPage) page.getNativePage()).getMediaBox().getHeight();
		PDFCloneUtility cloner = new PDFCloneUtility((PDDocument) document);

		boolean first = pageNum == 1;
		boolean last = pageNum == pages.size();
		List<FieldDefinition> toRemove = new ArrayList<FieldDefinition>();
		List<FieldDefinition> toMove = new ArrayList<FieldDefinition>();

		for (FieldDefinition fDef : this.definition.getFields()) {
			boolean pageMatch = false;
			switch (fDef.getPage().getPageType()) {
			case ANY:
				pageMatch = true;
				break;
			case FIRST:
				pageMatch = first;
				break;
			case LAST:
				pageMatch = last;
				break;
			case NEXT:
				pageMatch = !first;
				break;
			case EXACT:
				pageMatch = pageNum == fDef.getPage().getExactPage();
				break;
			}
			if (pageMatch && fDef.isRemoveText()) {
				Rectangle2D rec = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
						PointMm.getPoints(fDef.getPosition().getY()), PointMm.getPoints(fDef.getDimension().getX()),
						PointMm.getPoints(fDef.getDimension().getY()));
				toRemove.add(fDef);
			} else if (pageMatch && fDef.getTranslate() != null && !fDef.getTranslate().equals(PointMm.ZERO)) {
				toMove.add(fDef);
			}

		}

		PDFStreamParser parser = new PDFStreamParser((PDPage) page.getNativePage());
		parser.parse();
		List<Object> tokens = parser.getTokens();

		// System.out.println("nb objects = " + tokens.size());
		boolean parsingTextObject = false;

		List<Object> textInstructions = new ArrayList<Object>();
		List<Object> newPageObjects = new ArrayList<Object>();
		StringBuffer text = new StringBuffer();
		List<String> textTokens = new ArrayList<String>();
		float x = -1;
		float y = -1;
		COSFloat cosX = null;
		COSFloat cosY = null;

		for (int i = 0; i < tokens.size(); i++) {
			Object next = tokens.get(i);
			if (parsingTextObject)
				textInstructions.add(next);
			else
				newPageObjects.add(next);

			if (next instanceof Operator) {
				Operator op = (Operator) next;

				switch (op.getName()) {
				case "BT":
					// BT: Begin Text.
					x = -1;
					y = -1;
					parsingTextObject = true;
					text = new StringBuffer();
					textTokens.clear();
					textInstructions.clear();
					textInstructions.add(next);
					break;
				case "ET":
					parsingTextObject = false;
					Point2D point = new Point2D.Float(x, y);
					boolean remove = false;
					for (FieldDefinition fDef : toRemove) {
						Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
								PointMm.getPoints(fDef.getPosition().getY()),
								PointMm.getPoints(fDef.getDimension().getX()),
								PointMm.getPoints(fDef.getDimension().getY()));
						if (rect.contains(point)) {
							for (Object obj : textInstructions) {
								if (obj instanceof COSString) {
									textTokens.add(((COSString) obj).getString());
								} else if (obj instanceof ShowTextLine || obj instanceof ShowTextLineAndSpace) {
									textTokens.add(textTokens.size() - 1, "\n");
								} else if (obj instanceof ShowText || obj instanceof ShowTextAdjusted) {

								}
							}
							for (String s : textTokens)
								text.append(s);

							remove = true;
						}
					}
					for (FieldDefinition fDef : toMove) {
						Rectangle2D rect = new Rectangle2D.Float(PointMm.getPoints(fDef.getPosition().getX()),
								PointMm.getPoints(fDef.getPosition().getY()),
								PointMm.getPoints(fDef.getDimension().getX()),
								PointMm.getPoints(fDef.getDimension().getY()));
						if (rect.contains(point) && cosX != null && cosY != null) {
							for (Object obj : textInstructions) {
								if (obj instanceof COSString) {
									textTokens.add(((COSString) obj).getString());
								} else if (obj instanceof ShowTextLine || obj instanceof ShowTextLineAndSpace) {
									textTokens.add(textTokens.size() - 1, "\n");
								} else if (obj instanceof ShowText || obj instanceof ShowTextAdjusted) {

								}
							}
							for (String s : textTokens)
								text.append(s);

							COSFloat newX = new COSFloat(
									cosX.floatValue() + PointMm.getPoints(fDef.getTranslate().getX()));

							COSFloat newY = new COSFloat(
									cosY.floatValue() + PointMm.getPoints(fDef.getTranslate().getY()));

							int xIdx = textInstructions.indexOf(cosX);
							int yIdx = textInstructions.indexOf(cosY);

							if (xIdx < 0 || yIdx < 0) {
								for (int k = 0; k < textInstructions.size(); k++) {
									Object currObj = textInstructions.get(k);
									if (currObj instanceof Operator) {
										Operator operator = (Operator) currObj;
										if (operator.getName().compareToIgnoreCase("tf") == 0) {

											xIdx = i + 1;
											yIdx = i + 2;
											try {
												newX = new COSFloat(cosX.floatValue()
														+ PointMm.getPoints(fDef.getTranslate().getX()));
												newY = new COSFloat(cosY.floatValue()
														+ PointMm.getPoints(fDef.getTranslate().getY()));
												cosX = (COSFloat) textInstructions.get(xIdx);
												cosY = (COSFloat) textInstructions.get(yIdx);
											} catch (Exception e) {
												xIdx = -1;
												yIdx = -1;
											}

											break;
										}
									}

								}
							}
							if (xIdx >= 0 && yIdx > 0) {
								textInstructions.set(xIdx, newX);
								textInstructions.set(yIdx, newY);
							} else {
								LogHelper.error("Could not move text field " + fDef.getName());
							}
						}
					}

					if (!remove) {
						newPageObjects.addAll(textInstructions);
					} else {
						// System.out.println("removed " + textInstructions.size() + " objects");
					}
					text = new StringBuffer();
					textTokens.clear();
					textInstructions.clear();
					break;

				case "Tm":
				case "Td":
				case "TD":
					// Les deux précédents objets étaient la position du texte
					// TODO : repérer x et y dans textInstructions, les 2 derniers
					if (textInstructions.size() >= 3) {
						Object lastInstr = textInstructions.get(textInstructions.size() - 2);
						if (lastInstr instanceof COSFloat) {
							y = ((PDPage) page.getNativePage()).getMediaBox().getHeight()
									- ((COSFloat) lastInstr).floatValue();
							cosY = (COSFloat) lastInstr;
						}
						lastInstr = textInstructions.get(textInstructions.size() - 3);
						if (lastInstr instanceof COSFloat) {
							x = ((COSFloat) lastInstr).floatValue();
							cosX = (COSFloat) lastInstr;
						}
					}
					break;
				default:
					// System.out.println("unsupported operation " + op);

				}

			} else if (parsingTextObject) {

			} else {

			}
		}
		PDPage newPage = new PDPage(((PDPage) page.getNativePage()).getMediaBox());
		PDStream newContents = new PDStream((PDDocument) document);
		OutputStream os = newContents.createOutputStream();
		ContentStreamWriter writer = new ContentStreamWriter(os);

		// System.out.println("nb objects in new page = " + newPageObjects.size());
		for (Object obj : newPageObjects) {
			if (obj instanceof COSBase) {
				writer.writeToken(cloner.cloneForNewDocument(obj));
			} else {
				writer.writeTokens(obj);
			}
		}
		os.flush();
		os.close();

		newPage.setContents(newContents);
		newPage.setResources(((PDPage) page.getNativePage()).getResources());

		Page newInterPage = new Page(newPage);
		newInterPage.setPageDocumentDefinition(page.getPageDocumentDefinition());

		return newInterPage;

	}

	protected float[][] matriceX(float[][] m1, float[][] m2) {
		float[][] result = new float[m1.length][m2[0].length];
		for (int i = 0; i < m1.length; i++) {
			for (int j = 0; j < m2[0].length; j++) {
				result[i][j] = 0;
			}

			for (int j = 0; j < m2[0].length; j++) {
				for (int i2 = 0; i2 < m1[0].length; i2++) {
					result[i][j] += m1[i][i2] * m2[i2][j];
				}

			}
		}
		// System.out.println("result=" + result);

		return result;

	}

	protected float getFloat(Object o) {
		if (o instanceof COSInteger) {
			return ((COSInteger) o).floatValue();
		} else if (o instanceof COSFloat) {
			return ((COSFloat) o).floatValue();
		} else {
			return 0;
		}

	}

	@Override
	public void close() {
		if (document != null)
			if (document instanceof PDDocument)
				try {
					((PDDocument) document).close();
				} catch (IOException e) {
					LogHelper.warn("Could not close PDF " + this.fileResult + " : " + e.getMessage(), e);
				}
		
	}
}
