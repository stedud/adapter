import de.kwsoft.mtext.converter.gof.OtfConverter;
import de.kwsoft.mtext.converter.gof.OtfConverterException;
import de.kwsoft.mtext.format.intermediate.MTextIFRendererFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public final class ConvertFile
{
    /**
     *
     */
    private ConvertFile()
    {
    }

    /**
     *
     */
    public static void main(final String... args) throws OtfConverterException
    {
        final Path inputFile = Paths.get("CandidateLetter.gof");
        final Path outputFile = Paths.get("CandidateLetter.mfd");

        // Only if you are interested in the header metadata, otherwise null is allowed.
        final Path headerOutputFile = Paths.get("CandidateLetter.xml");

        final String format = "mfd";

        // Only for performance reasons get the factory once.
        // Otherwise it's also possible to pass null as factory.
        final MTextIFRendererFactory rendererFactory = OtfConverter.getRendererFactory();

        OtfConverter.convertFile(inputFile, outputFile, headerOutputFile,
                format, rendererFactory, null);
    }
}
