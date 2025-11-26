package fr.kw.api.rest.mtext;

public class Response {

	protected boolean success = false;
	protected String message = null;
	protected byte[] body;
	protected Object response;

	public Response() {
		// TODO Auto-generated constructor stub
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
