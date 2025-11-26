import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;

import de.kwsoft.mtext.util.misc.PasswordEncryptor;

public class Dummy {

	public Dummy() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {


		String crypted = PasswordEncryptor.encode(args[0]);
		String uncrypted = PasswordEncryptor.decode(args[1]);

		System.out.println(crypted);
		System.out.println(uncrypted);

		new java.lang.String("").getBytes();
		

	}

}
