package fr.kw.adapter.parser.type.page.pdfin.mfd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import de.kwsoft.moms.xf.XfXml2ControlJob;
import de.kwsoft.mtext.mffmfd.PageInfo;
import de.kwsoft.mtext.mffmfd.embed.MfdForEmbeddedDocument;
import fr.kw.api.rest.moms.MomsClientAPI;
import fr.kw.api.rest.mtext.Response;
import fr.utils.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "convert")
public class PDFConverter implements Callable<Boolean> {

	@Option(names = "-pdf")
	protected File pdf = null;

	@Option(names = "-mfd")
	protected File mfd = null;

	@Option(names = "-xml")
	protected File xml = null;

	@Option(names = "-mcj")
	protected File mcj = null;

	@Option(names = "-url")
	protected String url = null;

	@Option(names = "-user")
	protected String user = null;

	@Option(names = "-pwd")
	protected String pwd = null;

	@Option(names = "-pwdPlain")
	protected String pwdPlain = null;

	public static void main(String[] args) {
		PDFConverter converter = new PDFConverter();
		CommandLine commandLine = new CommandLine(converter);
		commandLine.execute(args);
	}

	@Override
	public Boolean call() throws Exception {

		InputStream is = null;
		if (pdf == null)
			is = System.in;
		else
			is = new FileInputStream(pdf);

		File tmpPdf = File.createTempFile("tmp", ".pdf");
		tmpPdf.deleteOnExit();
		Utils.copyInputStreamToFile(is, tmpPdf);

		File tmpMfd = File.createTempFile("tmp", ".mfd");
		tmpMfd.deleteOnExit();

		System.out.println("Converting pdf...");
		List<PageInfo> pageInfos = MfdForEmbeddedDocument.createMfdForEmbeddedDocument(tmpPdf, "mfd", tmpMfd);// Création
																												// du
																												// fichier
																												// MFD à
																												// partir
																												// du
																												// PDF

		System.out.println("Pdf converted.");

		if (xml != null) {
			if (mcj == null) {
				mcj = new File(FilenameUtils.removeExtension(xml.getPath()) + ".mcj");
			}
			System.out.println("Converting xml...");
			XfXml2ControlJob.convert(false, xml, pageInfos, mcj);// Création fichier MCJ
			System.out.println("xml converted.");

		}

		OutputStream os = null;
		if (mfd == null) {
			os = System.out;
		} else {
			os = new FileOutputStream(mfd);
		}

		FileInputStream mfdIS = new FileInputStream(tmpMfd);

		IOUtils.copy(mfdIS, os);

		mfdIS.close();
		os.close();
		tmpMfd.delete();
		tmpMfd.delete();

		if (url != null && mcj != null) {
			System.out.println("Sending document to M/OMS...");
			MomsClientAPI client = new MomsClientAPI(new String[] { url });
			Response response = client.send(mfd, mcj, user, pwd, pwdPlain);
			if (!response.isSuccess()) {
				System.out.println("Error during sending : " + response.getMessage());
				System.out.println("End.");
				return false;
			} else {
				System.out.println("Document sent.");
			}
		}

		System.out.println("End.");

		return true;
	}

}
