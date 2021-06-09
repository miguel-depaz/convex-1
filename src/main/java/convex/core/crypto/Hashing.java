package convex.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.jcajce.provider.digest.Keccak;

import convex.core.data.Hash;

public class Hashing {

	/**
	 * Computes the SHA-256 hash of a string
	 * 
	 * @param message Message to hash (in UTF-8 encoding)
	 * @return Hash of UTF-8 encoded string
	 */
	public static Hash sha3(String message) {
		return sha3(message.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Computes the SHA3-256 hash of byte data
	 * 
	 * @param data
	 * @return SHA3-256 Hash value
	 */
	public static Hash sha3(byte[] data) {
		MessageDigest md = getSHA3Digest();
		byte[] hash = md.digest(data);
		return Hash.wrap(hash);
	}

	/**
	 * Gets a thread-local instance of a SHA3-256 MessageDigest
	 * 
	 * @return MessageDigest instance
	 */
	public static MessageDigest getSHA3Digest() {
		return sha3Store.get();
	}

	/**
	 * Gets the Convex default MessageDigest. 
	 * 
	 * Guaranteed thread safe, will be either a new or ThreadLocal instance.
	 * 
	 * @return MessageDigest
	 */
	public static MessageDigest getDigest() {
		return getSHA3Digest();
	}

	/**
	 * Gets a MessageDigest for Keccak256. 
	 * 
	 * Guaranteed thread safe, will be either a new or ThreadLocal instance.
	 * 
	 * @return MessageDigest
	 */
	public static MessageDigest getKeccak256Digest() {
		// MessageDigest md= KECCAK_DIGEST.get();
		// md.reset();
		MessageDigest md = new Keccak.Digest256();
		return md;
	}

	/**
	 * Gets a thread-local instance of a SHA256 MessageDigest
	 * 
	 * @return MessageDigest instance
	 */
	public static MessageDigest getSHA256Digest() {
		return sha256Store.get();
	}

	/**
	 * Computes the keccak256 hash of all byte data in an array
	 * 
	 * @param data
	 * @return Hash of byte data
	 */
	public static Hash keccak256(byte[] data) {
		return keccak256(data, 0, data.length);
	}

	/**
	 * Computes the keccak256 hash of a subset of byte data in an array
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @return Hash of byte data
	 */
	public static Hash keccak256(byte[] data, int offset, int length) {
		MessageDigest kecc = getKeccak256Digest();
		kecc.update(data, offset, length);
		return Hash.wrap(kecc.digest());
	}

	/**
	 * Compute the keccak256 hash of the UTF8 encoding of a string
	 * 
	 * @param string
	 * @return Hash of UTF-8 encoded string
	 */
	public static Hash keccak256(String string) {
		return keccak256(string.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Computes the SHA3-256 hash of byte data
	 * 
	 * @param data
	 * @return SHA3-256 Hash value
	 */
	public static Hash sha256(byte[] data) {
		MessageDigest md = getSHA256Digest();
		byte[] hash = md.digest(data);
		return Hash.wrap(hash);
	}

	/**
	 * Computes the SHA-256 hash of a string
	 * 
	 * @param message Message to Hash (in UTF8 encoding)
	 * @return Hash of UTF-8 encoded string
	 */
	public static Hash sha256(String message) {
		return sha256(message.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Private store for thread-local MessageDigent objects. Avoids cost of
	 * recreating these every time they are needed.
	 */
	private static final ThreadLocal<MessageDigest> sha256Store;
	/**
	 * Private store for thread-local MessageDigent objects. Avoids cost of
	 * recreating these every time they are needed.
	 */
	private static final ThreadLocal<MessageDigest> sha3Store;
	static {
		sha256Store = ThreadLocal.withInitial(() -> {
			try {
				return MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				throw new Error("SHA-256 algorithm not available", e);
			}
		});
		
		sha3Store = ThreadLocal.withInitial(() -> {
			try {
				return MessageDigest.getInstance("SHA3-256");
			} catch (NoSuchAlgorithmException e) {
				throw new Error("SHA3-256 algorithm not available", e);
			}
		});
	}
	/**
	 * Threadlocal store for MessageDigets instances. TODO: figure out if this is
	 * useful for performance. Probably not since digest initialisation is the
	 * bottleneck anyway?
	 */
	@SuppressWarnings("unused")
	private static final ThreadLocal<MessageDigest> KECCAK_DIGEST = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() {
			return new Keccak.Digest256();
		}
	};

}
