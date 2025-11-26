package fr.kw.adapter.parser.type.page.pdfin;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.document.Document;
import fr.kw.adapter.engine.ProcessFile;
import fr.kw.adapter.engine.ThreadManager;
import fr.kw.adapter.parser.type.page.FileProcessException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PDFScaler implements Callable<String> { 
	@Option(names = { "-in", "-input", "-fileName" }, required = false)
	protected File inputFile;
	
	@Option(names = "-pdfScale")
	protected double pdfScale = 1.0;
	
	
	public PDFScaler()
	{

		
	}
	
	public static void main(String[] args) {
		String tmp = System.getProperty("java.io.tmpdir");
		if (StringUtils.isNotBlank(tmp)) {
			File tmpFolder = new File(tmp);
			tmpFolder.mkdirs();
		}

		PDFScaler start = new PDFScaler();
		//ProcessFile.setDaemon(false);
		CommandLine cmdLine = new CommandLine(start);

		int result = cmdLine.execute(args);
		

		ThreadManager.stopAll();
		System.out.println("ProcessFile exiting with status " + result);

		System.exit(result);
	}
	
	public void scalePDF(File pdf, double scale) throws FileProcessException
	{
		PDFProcess.scalePDF(pdf, scale);
		
	}

	@Override
	public String call() throws Exception {
		this.scalePDF(inputFile, pdfScale);
		return inputFile.getPath() + " scaled to " + pdfScale;
	}

}
