package com.agorasoft.ubeeko.p2pdatanode;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.security.cert.CertificateException;

import java.util.Random;

import net.jxta.exception.PeerGroupException;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.StringUtils;

import com.agorasoft.ubeeko.p2pcommon.GenerateJxtaID;
import com.agorasoft.ubeeko.p2pcommon.P2PConstants;
import com.agorasoft.ubeeko.p2psockets.P2PNetwork;
import com.agorasoft.ubeeko.p2psockets.P2PServerSocket;

public class PeerDataNode implements Runnable, P2PConstants {
	private static final Log LOG = LogFactory.getLog(PeerDataNode.class);
	
	private static final String NETWORK_MANAGER_NAME = "DataNode Peer Network Manager";
	private static final String NETWORK_CONFIGURATOR_NAME = "DataNode Peer Network Configurator";
	private static final String NETWORK_CONFIGURATOR_DESC = "Created by Agorasoft Ubeeko";
	
	private static String DATANODE_PEER_NAME;
	
	public NetworkManager nmanager;
	public NetworkConfigurator nconfig;
	
	public PeerGroup ubeekoPeerGroup;
	
	private Configuration conf;
	private File peerConfigFile;
	
	/**
	   * Construct Peer server.
	   * 
	   * @param conf the configuration
	   */
	public PeerDataNode(Configuration conf) throws IOException {
		LOG.info("Initializing P2P Peer");
		this.conf = conf;		

		peerConfigFile = new File(conf.get(BASE_DIRECTORY)+"/datanode");
		nmanager = new NetworkManager(NetworkManager.ConfigMode.EDGE,NETWORK_MANAGER_NAME,peerConfigFile.toURI());
		nmanager.setConfigPersistent(true);
		
		nconfig = nmanager.getConfigurator();
	}
	
	/**
	   * Initialize Peer server
	   * including the Network configurator
	   * and the network manager
	   * 
	   */
	public void init()  throws IOException {
		if (!nconfig.exists()) {
			conf.setIfUnset(DN_PEER_NAME,"Ubeeko-"+(new Random()).nextInt(MAX_DN_PEERS_NUM)+"-DataNode-Peer");			
			this.DATANODE_PEER_NAME = conf.get(DN_PEER_NAME);
			LOG.debug("DataNode peer name set to "+DATANODE_PEER_NAME);
			
			nconfig.setPeerID(GenerateJxtaID.getPeerID(INFRA_P2P_NAME, conf.get(DN_PEER_NAME)));
			
			nconfig.setName(DATANODE_PEER_NAME);
			nconfig.setDescription(NETWORK_CONFIGURATOR_DESC);
			
			nconfig.setMode(NetworkConfigurator.EDGE_NODE);
			
			nconfig.setHttpEnabled(false);
			
			nconfig.setTcpEnabled(true);
			InetSocketAddress isa = NetUtils.createSocketAddr(conf.get(PEER_DEFAULT_ADDRESS));
			LOG.debug("Peer port is: "+isa.getPort());
			nconfig.setTcpPort(isa.getPort());
			LOG.debug("Peer ip address is: "+isa.getAddress().toString());
			nconfig.setTcpInterfaceAddress(isa.getAddress().getHostAddress());
			
			nconfig.setTcpIncoming(true);
			nconfig.setTcpOutgoing(true);
			
			try {
				LOG.debug("Seed Rendez-Vous NameNode is at : tcp://"+conf.get(RDV_DEFAULT_ADDRESS));
				nconfig.addSeedRendezvous(new URI("tcp://"+conf.get(RDV_DEFAULT_ADDRESS)));
			} catch (URISyntaxException use) {
				LOG.error(StringUtils.stringifyException(use));
			}
			
			nconfig.setMulticastAddress(MULTICAST_IP_ADDRESS);
			nconfig.setMulticastPort(MULTICAST_IP_PORT);
			
			nconfig.save();
		} else {
			File pc = new File(nconfig.getHome(), "PlatformConfig");
			try {
				nconfig.load(pc.toURI());
			} catch (CertificateException ce) {}
		}	
	}
	
	public void start() {
		this.run();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		LOG.debug("Starting Peer Network Manager");
		try {
			ubeekoPeerGroup = nmanager.startNetwork();
		} catch(PeerGroupException pge) {
			LOG.error(StringUtils.stringifyException(pge));
		} catch(IOException ioe) {
			LOG.error(StringUtils.stringifyException(ioe));
		}
		
		if(ubeekoPeerGroup != null) {
			// Waiting for Rendez-Vous connection
			nmanager.waitForRendezvousConnection(MAX_WAIT_RDV);
			
			// Signing into the Ubeeko P2P socket network
			try {
				P2PNetwork.signIn(ubeekoPeerGroup,ADV_P2P_SOCKET_VALUE);
			} catch (IllegalArgumentException iae) {
				LOG.error(StringUtils.stringifyException(iae));
			}
		}
	}
	
	public void shutdown() {
		this.nmanager.registerShutdownHook();
		this.nmanager.stopNetwork();
	}
	
	public ServerSocket openP2PSocket() {
		ServerSocket server = null;
		
		if(this.ubeekoPeerGroup != null) {
			LOG.info("Starting P2P socket: p2p://"+this.DATANODE_PEER_NAME+":"+INFRA_P2P_PORT);
			try {
				server = new P2PServerSocket(this.DATANODE_PEER_NAME, INFRA_P2P_PORT);
			} catch (IOException ioe) {
				LOG.error(StringUtils.stringifyException(ioe));
			}
		} else {
			LOG.fatal("Not connected to Ubeeko P2P infrastructure");
		}
		
		return server;
	}
}
