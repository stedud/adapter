package fr.kw.adapter.parser.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import fr.strs.pdf.jde.JdePdfConversion;
import fr.utils.Utils;

public class FileFilter {

	protected FilterType type = FilterType.COMMAND;

	protected String arguments;

	protected String resultExtension = null;

	protected String backFolder;

	public FileFilter() {
		// TODO Auto-generated constructor stub
	}

	public File filter(File inputFile) throws ParseProcessException {

		if (StringUtils.isNotBlank(backFolder)) {
			File backFolderFile = new File(backFolder);
			try {
				FileUtils.copyFileToDirectory(inputFile, backFolderFile);
			} catch (IOException e) {
				throw new ParseProcessException("Could not save " + inputFile.getPath() + " to folder "
						+ backFolderFile.getPath() + " : " + e.getMessage(), e);
			}
		}

		File dataFileResult = null;

		switch (type) {
		case XSLT:

			try {
				DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

				StreamSource stylesource = new StreamSource(new File(arguments));
				dataFileResult = new File(Utils.getTmpFolder(),
						inputFile.getName() + "." + (resultExtension != null ? resultExtension : "tmp"));

				Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
				transformer.transform(new StreamSource(inputFile), new StreamResult(dataFileResult));

			} catch (TransformerFactoryConfigurationError | TransformerException e) {

				if (dataFileResult != null)
					dataFileResult.delete();

				throw new ParseProcessException(
						"Could not convert " + inputFile.getName() + " using xslt " + arguments + ": " + e.getMessage(),
						e);
			}
			break;

		case COMMAND:
			OutputStream procIn = null;
			InputStream procOut = null;
			OutputStream errors = null;
			InputStream tmpIS = null;
			OutputStream errorOS = null;

			try {
				dataFileResult = new File(Utils.getTmpFolder(),
						inputFile.getName() + "." + (resultExtension != null ? resultExtension : "tmp"));

				Process process = Runtime.getRuntime().exec(arguments);
				procIn = process.getOutputStream();
				procOut = process.getInputStream();
				errors = process.getOutputStream();

				tmpIS = new FileInputStream(inputFile);
				Utils.pipeStream(tmpIS, procIn);

				Utils.pipeStream(procOut, new FileOutputStream(dataFileResult));
				File errorFile = File.createTempFile("errors_", ".txt");
				errorOS = new FileOutputStream(errorFile);
				Utils.pipeStream(procOut, errorOS);

				process.waitFor(5000 + inputFile.length(), TimeUnit.MILLISECONDS);

			} catch (InterruptedException | IOException e) {

				if (dataFileResult != null)
					dataFileResult.delete();

				throw new ParseProcessException("Time out with stdin/stdout filter : " + arguments, e);
			} finally {

				if (tmpIS != null)
					IOUtils.closeQuietly(tmpIS);
				if (procOut != null)
					IOUtils.closeQuietly(procOut);
				if (errorOS != null)
					IOUtils.closeQuietly(errorOS);
				if (procIn != null)
					IOUtils.closeQuietly(procIn);
				if (errors != null)
					IOUtils.closeQuietly(errors);

			}

			break;

		case JDE:

			FileInputStream fis = null;
			PrintStream ps = null;

			try {

				dataFileResult = new File(Utils.getTmpFolder(),
						inputFile.getName() + "." + (resultExtension != null ? resultExtension : "tmp"));
				String args[] = StringUtils.split(arguments);

				fis = new FileInputStream(inputFile);
				ps = new PrintStream(dataFileResult);
				JdePdfConversion.process(args, fis, ps);
			} catch (IOException e) {
				throw new ParseProcessException("IOException with PDFJDEFilter : " + arguments, e);
			} finally {
				if (fis != null)
					IOUtils.closeQuietly(fis);
				if (ps != null)
					IOUtils.closeQuietly(ps);

			}
			break;
		}

		return dataFileResult;
	}

	public FilterType getType() {
		return type;
	}

	public void setType(FilterType type) {
		this.type = type;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	public String getBackFolder() {
		return backFolder;
	}

	public void setBackFolder(String backFolder) {
		this.backFolder = backFolder;
	}

	public String getResultExtension() {
		return resultExtension;
	}

	public void setResultExtension(String resultExtension) {
		this.resultExtension = resultExtension;
	}

	@Override
	public String toString() {
		return "FileFilter [type=" + type + ", arguments=" + arguments + ", resultExtension=" + resultExtension
				+ ", backFolder=" + backFolder + "]";
	}

}
