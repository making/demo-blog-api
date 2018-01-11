package com.example.blog.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class WebhookVerifier {
	private final Mac hmac;
	private static final String HMAC_SHA1 = "HmacSHA1";

	public WebhookVerifier(String secret)
			throws InvalidKeyException, NoSuchAlgorithmException {
		// initialize HMAC with SHA1 algorithm and secret
		SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA1);
		this.hmac = Mac.getInstance(HMAC_SHA1);
		this.hmac.init(signingKey);
	}

	public void verify(String payload, String signature) {
		String computedSignature = signature(payload);
		if (!computedSignature.equalsIgnoreCase(signature)) {
			throw new WebhookAuthenticationException(computedSignature, signature);
		}
	}

	String signature(String payload) {
		byte[] sig = hmac.doFinal(payload.getBytes());
		return "sha1=" + DatatypeConverter.printHexBinary(sig).toLowerCase();
	}
}
