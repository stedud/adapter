package fr.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class SerializableMarshallingAdapter extends XmlAdapter<String, Serializable> {
	private HexBinaryAdapter hexAdapter = new HexBinaryAdapter();

	@Override
	public String marshal(Serializable v) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(v);
		oos.close();
		byte[] serializedBytes = baos.toByteArray();
		return hexAdapter.marshal(serializedBytes);
	}

	@Override
	public Serializable unmarshal(String v) throws Exception {
		byte[] serializedBytes = hexAdapter.unmarshal(v);
		ByteArrayInputStream bais = new ByteArrayInputStream(serializedBytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Serializable result = (Serializable) ois.readObject();
		return result;
	}
}