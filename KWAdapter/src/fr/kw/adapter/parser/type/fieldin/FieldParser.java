package fr.kw.adapter.parser.type.fieldin;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.IParser;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.structure.EventStructure;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class FieldParser implements IParser<Field, String> {

	protected FieldParserConfiguration config;
	protected String prefix = "";
	protected String charset = StandardCharsets.UTF_8.name();

	public FieldParser(FieldParserConfiguration config) {
		this.config = config;
	}

	@Override
	public Event parseEvent(Event currentEvent, String line, ScriptEngine scriptEngine) throws DataDescriptionException {
		if (StringUtils.isBlank(line))
			return null;

		Event event = null;
		String labelStartEvent = getLabelStartEvent(line);
		if (labelStartEvent != null && labelStartEvent.startsWith(config.labelStartEvent)) {
			String eventName = getEventName(line);
			if (StringUtils.isNotBlank(eventName)) {
				eventName = Utils.normalizeXmlName(eventName);
				event = new Event(eventName);
				// Search for EventStructure
				File eventStructureFile = this.config.getRootConfiguration().getEventStructure(event.getName());
				LogHelper.info("Event " + event.getName() + ", structure file:" + eventStructureFile.getAbsolutePath());
				if (eventStructureFile.canRead()) {
					LogHelper.info("Loading structure file:" + eventStructureFile.getPath());
					EventStructure eventStructure = EventStructure.load(eventStructureFile);
					event.setStructure(eventStructure);
				}

			}
			return event;
		}
		return null;
	}

	@Override
	public Field parseFields(Event currentEvent, Field currentField, String line, ScriptEngine scriptEngine) throws DataDescriptionException {

		Field result = null;
		if (config.labelPrefix != null) {
			String utilLine = StringUtils.substring(line, Math.max(0, config.posLabelPrefix - 1));
			if (utilLine.startsWith(config.labelPrefix) && StringUtils.isNotEmpty(config.labelPrefix)) {
				if (StringUtils.isNotEmpty(config.chrSepPrefix)) {
					prefix = StringUtils.substringAfter(utilLine, config.chrSepPrefix);
				} else if (config.posPrefix > 0) {
					StringUtils.substring(line, Math.max(0, config.posPrefix - 1));
				} else {
					throw new DataDescriptionException(
							"Field prefix in " + config.getId() + " must have ChrSepPrefix OR posPrefix >= 1");
				}
				return result;// no field to return but the prefix has been set for next field
			}
		}
		String fieldLabel = (config.fixLenField > 0)
				? StringUtils.substring(line, Integer.max(0, config.posField - 1),
						Integer.max(0, config.posField + config.fixLenField - 1))
				: StringUtils.substring(line, Integer.max(0, config.posField - 1));
		fieldLabel = fieldLabel.trim();

		if (config.chrSepField != null && config.chrSepField.length() > 0) {
			fieldLabel = StringUtils.substringBefore(fieldLabel, config.chrSepField);
		}
		if (config.posField > 0) {
			fieldLabel = StringUtils.substring(fieldLabel, Math.max(0, config.posField - 1));
		}
		if (StringUtils.isNotBlank(prefix)) {
			fieldLabel = prefix + "_" + fieldLabel;
		}

		if (StringUtils.isNotBlank(fieldLabel) || (currentField != null && currentField.isInclude())) {

			fieldLabel = Utils.normalizeXmlName(fieldLabel);
			Field field = new Field(fieldLabel);

			String value = "";
			if (config.chrSepField != null && config.chrSepField.length() > 0) {
				value = StringUtils.substringAfter(line, config.chrSepField);
			} else if (config.posStartValue > 0) {
				value = (config.posEndValue > 0)
						? StringUtils.substring(line, Math.max(0, config.posStartValue - 1),
								Math.max(0, config.posEndValue - 1))
						: StringUtils.substring(line, Math.max(0, config.posStartValue - 1));
				// field.value =
				// (if (appendNextField && lastField != null) lastField!!.value +
				// fieldContString else "") + currValue
			}

			// TODO
			// include mode ?
			// YES
			// End include ? YES --> close field, NO --> append line
			// NO
			// Start include ? YES --> include mode "on", NO --> take value and put on field

			if (currentField != null && currentField.isInclude()) {
				if (isEndInclude(line)) {
					currentField.setInclude(false);
					return currentField;
				} else {
					field = currentField;
					if (StringUtils.isNotEmpty(field.getValue())) {
						if (config.keepSpace) {
							field.setValue(field.getValue() + config.includeContString + line);
						} else {
							field.setValue(field.getValue() + config.includeContString + line.trim());
						}
					} else {
						if (config.keepSpace) {
							field.setValue(line);
						} else {
							field.setValue(line.trim());
						}
					}
					return field;
				}
			} else {
				if (isStartInclude(line)) {
					// TODO : le startinclude s'applique-t'il au nouveau champ (start include et
					// nouveau champ sur même ligne ?) ou bien au champ précédent ?
					// CAS 1 :
					// field textName STARTINCLUDE
					// line1...
					// line2...
					// field textName ENDINCLUDE

					// CAS 2:
					// field textName
					// STARTINCLUDE
					// line1...
					// line2...
					// ENDINCLUDE
					if (value.trim().compareTo(config.labelStartInclude) == 0) {// CAS 1
						field.setInclude(true);
						return field;
					} else {// CAS 2
						if (currentField != null) {
							field = currentField;
						}

						field.setInclude(true);
						return field;
					}

				} else {// Normal field (no start/end include
						// fieldContinue ?
					if (config.labelFieldCont != null && config.labelFieldCont.length() > 0) {
						if (config.posLabelFieldCont > 0) {
							String labelFieldContCandidate = StringUtils.substring(line, config.posLabelFieldCont);
							if (labelFieldContCandidate.startsWith(config.labelFieldCont)) {
								field.setValue(((currentField != null && currentField.isAppend())
										? currentField.getValue() + config.fieldContString
										: "") + (config.keepSpace ? value : value.trim()));

								if (currentField != null) {
									currentField.setValue(field.getValue());
									currentField.setAppend(true);
									result = currentField;
								} else {
									field.setAppend(true);
									result = field;
								}
								return result;
							}
						} else if (StringUtils.startsWith(value.trim(), config.labelFieldCont)
								&& StringUtils.isNotEmpty(config.labelFieldCont)) {
							value = StringUtils.substringAfter(value, config.labelFieldCont);
							field.setValue(((currentField != null && currentField.isAppend())
									? currentField.getValue() + config.fieldContString
									: "") + (config.keepSpace ? value : value.trim()));

							if (currentField != null) {
								currentField.setValue(field.getValue());
								currentField.setAppend(true);
								result = currentField;
							} else {
								field.setAppend(true);
								result = field;
							}
							return result;
						}
					}

					field.setValue((config.keepSpace ? value : value.trim()));

					result = field;
					return result;

				}
			}

		}
		return result;
	}

	protected String getLabelStartEvent(String line) {
		String labelStartEvent = (config.posLabelEndEvent > config.posLabelEndEvent)
				? StringUtils.substring(line, Math.max(0, config.posLabelStartEvent - 1),
						Math.max(0, config.posLabelEndEvent - 1))
				: StringUtils.substring(line, Math.max(0, config.posLabelStartEvent - 1));
		return labelStartEvent;
	}

	protected boolean isStartInclude(String line) {

		if (StringUtils.isNotEmpty(config.labelStartInclude)) {
			String label = StringUtils.substring(line, Math.max(0, config.posLabelStartInclude - 1)).trim();
			if (label.compareTo(config.labelStartInclude) == 0)
				return true;
		}

		return false;
	}

	protected boolean isEndInclude(String line) {

		if (StringUtils.isNotEmpty(config.labelEndInclude)) {
			String label = StringUtils.substring(line, Math.max(0, config.posLabelEndInclude - 1)).trim();
			if (label.compareTo(config.labelEndInclude) == 0)
				return true;
		}

		return false;
	}

	protected String getEventName(String line) throws DataDescriptionException {

		String eventLabel = getLabelStartEvent(line);

		if (eventLabel.startsWith(config.labelStartEvent) && StringUtils.isNotEmpty(config.labelStartEvent)) {// New
																												// event

			if (StringUtils.isNotBlank(config.labelEndEvent)) {
				eventLabel = StringUtils.substringBefore(eventLabel, config.labelEndEvent);
			}
			String eventName = null;
			if (config.chrSepEvent != null && config.chrSepEvent.length() > 0) {
				eventName = StringUtils.substringAfter(line, config.chrSepEvent);
			} else if (config.posEvent > 0) {
				eventName = StringUtils.substring(line, Math.max(0, config.posEvent - 1));
			} else {

				throw new DataDescriptionException(
						"Field descriptor " + config.getId() + " has no posLabelStartEvent or ChrSepEvent");
			}
			return eventName;
		}
		return null;
	}

	@Override
	public String getCharset() {
		return charset;
	}

	@Override
	public void setCharset(String charsetName) {
		this.charset = charsetName;

	}

}
