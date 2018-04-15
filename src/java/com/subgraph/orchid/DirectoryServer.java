package com.subgraph.orchid;

import java.util.List;

import com.subgraph.orchid.data.HexDigest;

/**
 * Represents a directory authority server or a directory cache.
 */
public interface DirectoryServer extends Router {
	int getDirectoryPort();
	boolean isV2Authority();
	boolean isV3Authority();
	HexDigest getV3Identity();
	boolean isHiddenServiceAuthority();
	boolean isBridgeAuthority();
	boolean isExtraInfoCache();
	
	/**
	 * https://github.com/geo-gs/Orchid/commit/22beeae1b881707491addaba6a7654e9de9f9db1
         */
	KeyCertificate getCertificateByAuthority(HexDigest fingerprint);
	KeyCertificate getCertificateByFingerprint(HexDigest fingerprint);
	List<KeyCertificate> getCertificates();
	void addCertificate(KeyCertificate certificate);
}
