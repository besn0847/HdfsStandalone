package com.agorasoft.ubeeko.p2pcommon;

public interface P2PConstants {
	public static final String RDV_DEFAULT_ADDRESS = "dfs.rdv.p2p.address";
	public static final String PEER_DEFAULT_ADDRESS = "dfs.datanode.p2p.address";
	public static final String BASE_DIRECTORY = "hadoop.p2p.dir";
	public static final String DN_PEER_NAME = "dfs.datanode.p2p.peername";
	
	public static final String MULTICAST_IP_ADDRESS = "224.0.1.85";
	public static final int MULTICAST_IP_PORT = 1234;
	
	public static final String INFRA_P2P_NAME = "UbeekoInfrastructure";
	public static final int INFRA_P2P_PORT = 26421;
	public static final int MAX_DN_PEERS_NUM = 1000;
	public static final int MAX_WAIT_RDV = 10000; // 10 seconds
	
	public static final String ADV_P2P_SOCKET_NAME = "Name";
	public static final String ADV_P2P_SOCKET_VALUE = "UbeekoNetwork";
	public static final String ADV_P2P_TYPE = "jxta:PipeAdvertisement";
	public static final String ADV_P2P_SOCKET_REGEX = "^"+ADV_P2P_SOCKET_VALUE+"/[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+,[^:]+:[0-9]+$";
	public static final String ADV_P2P_SOCKET_REGEX_EXTRACT = "^"+ADV_P2P_SOCKET_VALUE+"/[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+,([^:]+):([0-9]+)$";
	
}
