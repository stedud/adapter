package fr.sap.otf.parser.object.definition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class CommandDefinition {

	static Properties prop = new Properties();
	static {
		try {
			prop.load(CommandDefinition.class.getClassLoader()
					.getResourceAsStream("fr/sap/otf/parser/commands.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String ID;
	private List<ArgumentDefinition> arguments = new ArrayList<ArgumentDefinition>();

	public CommandDefinition() {
		// TODO Auto-generated constructor stub
	}

	public static CommandDefinition parse(String commandLine) {
		CommandDefinition description = new CommandDefinition();

		String id = StringUtils.left(commandLine, 2);
		String arguments = prop.getProperty(id);
		if (arguments == null) {
			id = StringUtils.left(commandLine, 4);
			arguments = prop.getProperty(id);
		}

		if (arguments == null) {
			description.ID = "";

		} else {
			description.ID = id;
			String[] tokens = StringUtils.split(arguments, ',');
			for (String token : tokens) {
				String[] argTokens = StringUtils.split(token, ':');
				ArgumentDefinition argDef = new ArgumentDefinition();
				argDef.setType(ArgumentType.valueOf(argTokens[0]));
				try {
					argDef.setLength(Integer.parseInt(argTokens[1]));
				} catch (NumberFormatException e) {
					argDef.setLength(-1);// No limit
				}
				description.arguments.add(argDef);
			}
		}

		return description;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public List<ArgumentDefinition> getArguments() {
		return arguments;
	}

}
