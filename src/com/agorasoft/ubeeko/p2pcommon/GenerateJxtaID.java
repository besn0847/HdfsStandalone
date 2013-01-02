package com.agorasoft.ubeeko.p2pcommon;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

public final class GenerateJxtaID {
	private static final String SEED = "UbeekoJxtaP2P";
	
	private static byte[] hash(final String expression) {
		byte[] result;
		
		MessageDigest digest;
		
		if (expression == null) {
			throw new IllegalArgumentException("Invalid null expression");
		}
		
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException failed) {
			failed.printStackTrace(System.err);
			RuntimeException failure = new IllegalStateException("Could not get SHA-1 Message");
			failure.initCause(failed);
			throw failure;
		}
		
		try {
			byte[] expressionBytes = expression.getBytes("UTF-8");
			result = digest.digest(expressionBytes);
		} catch (UnsupportedEncodingException impossible) {
			RuntimeException failure =
			new IllegalStateException("Could not encode expression as UTF8");
			failure.initCause(impossible);
			throw failure;
		}
		
		return result;
	}
	
	public static PeerID createNewPeerID(PeerGroupID pgID) {
		return IDFactory.newPeerID(pgID);
	}
	
	public static PeerID createPeerID(PeerGroupID pgID, String peerName) {
		String seed = peerName + SEED;
		return IDFactory.newPeerID(pgID, hash(seed.toLowerCase()));
	}
	
	public static PeerGroupID createNewPeerGroupID(PeerGroupID pgID) {
		return IDFactory.newPeerGroupID(pgID);
	}
	
	public static PeerGroupID createPeerGroupID(final String groupName) {
		//Use lower case to avoid any locale conversion inconsistencies
		return IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID,hash(SEED + groupName.toLowerCase()));
	}
	
	public static PeerGroupID createInfraPeerGroupID(String groupName) {
		return createPeerGroupID(groupName);
	}
	
	public static PeerID getPeerID(String infraName, String peerName) {
		PeerGroupID infra = createInfraPeerGroupID(infraName);
		PeerID peerID = createPeerID(infra, peerName);
		
		return  peerID;
	}
	
	public static void main(String args[]) {
		PeerGroupID infra = createInfraPeerGroupID("ubeeko");
		PeerID peerID = createPeerID(infra, "node");
		
		System.out.println(MessageFormat.format("\n\nAn infrastucture PeerGroupID: {0}", infra.toString()));
		System.out.println(MessageFormat.format("PeerID with the above infra ID encoding: {0}", peerID.toString()));
		
		peerID = createNewPeerID(PeerGroupID.defaultNetPeerGroupID);
		PeerGroupID pgid = createNewPeerGroupID(PeerGroupID.defaultNetPeerGroupID);
		
		System.out.println(MessageFormat.format("\n\nNew PeerID created : {0}", peerID.toString()));
		System.out.println(MessageFormat.format("New PeerGroupID created : {0}", pgid.toString()));
	}
}
