package fr.sap.otf.parser.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.sap.otf.parser.object.definition.ArgumentDefinition;
import fr.sap.otf.parser.object.definition.CommandDefinition;

public class Command {

	private Command previousCommand;
	private String ID;
	private List<Argument> parameters = new ArrayList<Argument>();
	private String line;

	public static Map<String, CommandDefinition> descriptions = new HashMap<String, CommandDefinition>();

	public Command() {
		// TODO Auto-generated constructor stub
	}

	public static Command parse(String line) {

		if (line == null)
			return null;
		if (StringUtils.compare(line, "//") == 0)
			return null;

		CommandDefinition description = CommandDefinition.parse(line);
		if (descriptions.containsKey(description.getID())) {
			description = descriptions.get(description.getID());
		} else {
			descriptions.put(description.getID(), description);
		}

		Command command = new Command();
		command.ID = description.getID();
		command.line = line;

		if (StringUtils.isNoneBlank(description.getID())) {
			String argumentsToParse = StringUtils.substring(line, description.getID().length());
			if (StringUtils.isNoneBlank(argumentsToParse)) {
				int position = 0;

				for (ArgumentDefinition argDef : description.getArguments()) {
					Argument arg = new Argument();
					arg.setDefinition(argDef);
					if (argDef.getLength() > 0) {
						String argument = StringUtils.substring(argumentsToParse, position,
								argDef.getLength() + position);
						position = position + argDef.getLength();
						arg.setValue(argument);
					} else {
						String argument = StringUtils.substring(argumentsToParse, position);
						position = position + argument.length();
						arg.setValue(argument);
					}
					command.parameters.add(arg);
				}
			}
		} else {

		}

		return command;
	}

	public Command getPreviousCommand() {
		return previousCommand;
	}

	public void setPreviousCommand(Command previousCommand) {
		this.previousCommand = previousCommand;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public List<Argument> getParameters() {
		return parameters;
	}

	public void rebuildLine() {
		StringBuffer sb = new StringBuffer(getID());
		for (Argument arg : parameters) {
			sb.append(arg.getValue());
		}
		this.setLine(sb.toString());
	}

	@Override
	public String toString() {
		return "Command [line=" + line + "]";
	}

}
