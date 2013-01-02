package com.agorasoft.ubeeko.p2pnamenode;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.StringUtils;

import com.agorasoft.ubeeko.p2pcommon.P2PConstants;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

/**********************************************************
 * This class is the master server hosting the P2P connexions
 * from remote datanode.
 * 2 properties are used to define the P2P config:
 * 	dfs.rdv.p2p.address - the interface on which the RDV is litening
 * 	hadoop.p2p.dir - the directory where all the JXTA P2P config is dumped
 * The RDV PeerID is hardcoded which is only suitable for 
 * a single RDV peer deployment.
 * This server listens for incoming peer join request. 
 * If a peer joins, the services it provides are scanned to find
 * whether an Ubeeko peer is available aka a peer delivering 
 * P2P socket services.
 * *********************************************************
*/

public class RendezVousNameNode implements Runnable, RendezvousListener, DiscoveryListener, P2PConstants {
	private static final Log LOG = LogFactory.getLog(RendezVousNameNode.class);
	
	private static final String NETWORK_MANAGER_NAME = "NameNode Rendez-Vous Network Manager";
	private static final String NETWORK_CONFIGURATOR_NAME = "NameNode Rendez-Vous Network Configurator";
	private static final String NETWORK_CONFIGURATOR_DESC = "Created by Agorasoft Ubeeko";
	public static final String RDV_PEER_ID = "urn:jxta:uuid-59616261646162614E50472050325033C859EFFDE1DB493F986FB8788CB1635A03";
			
	public NetworkManager nmanager;
	public NetworkConfigurator nconfig;
	public PeerGroup ubeekoPeerGroup;
	public RendezVousService ubeekoRendezVousService;
	public DiscoveryService ndservice;
	
	private Configuration conf;
	private File rdvConfigFile;
	
	Map<PeerID, String> P2PSocketPeers = Collections.synchronizedMap(new HashMap<PeerID,String>());
	
	/**
	   * Construct rendez-Vous server.
	   * 
	   * @param conf the configuration
	   */
	public RendezVousNameNode(Configuration conf) throws IOException {
		LOG.info("Initializing P2P rendez-Vous server");
		
		this.conf = conf;		
		
		rdvConfigFile = new File(conf.get(BASE_DIRECTORY)+"/namenode");
		nmanager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS,NETWORK_MANAGER_NAME,rdvConfigFile.toURI());
		nmanager.setConfigPersistent(true);
		
		nconfig = nmanager.getConfigurator();
	}
	
	/**
	   * Initialize RendezVous server
	   * including the Network configurator
	   * and the network manager
	   * 
	   */
	public void init()  throws IOException {
		if (!nconfig.exists()) {
			nconfig.setName(NETWORK_CONFIGURATOR_NAME);
			nconfig.setDescription(NETWORK_CONFIGURATOR_DESC);
			
			nconfig.setMode(NetworkConfigurator.RDV_SERVER);
			
			nconfig.setHttpEnabled(false);
			
			nconfig.setTcpEnabled(true);
			InetSocketAddress isa = NetUtils.createSocketAddr(conf.get(RDV_DEFAULT_ADDRESS));
			LOG.debug("Rendez Vous port is: "+isa.getPort());
			nconfig.setTcpPort(isa.getPort());
			LOG.debug("Rendez Vous ip address is: "+isa.getAddress().toString());
			nconfig.setTcpInterfaceAddress(isa.getAddress().getHostAddress());
			
			nconfig.setTcpIncoming(true);
			nconfig.setTcpOutgoing(true);
			
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
	/**
	   * Run the Rendez-Vous server
	   * 
	   */
	public void run()  {
		LOG.debug("Starting Rendez-Vous Network Manager");
		try {
			ubeekoPeerGroup = nmanager.startNetwork();
		} catch(PeerGroupException pge) {
			LOG.error(StringUtils.stringifyException(pge));
		} catch(IOException ioe) {
			LOG.error(StringUtils.stringifyException(ioe));
		}

		if ( ubeekoPeerGroup != null) {
			ubeekoRendezVousService = ubeekoPeerGroup.getRendezVousService();
			ubeekoRendezVousService.addListener(this);
			ndservice = ubeekoPeerGroup.getDiscoveryService();
			ndservice.addDiscoveryListener(this);
		}
		
	}

	@Override
	/**
	   * When peers joins the group, a service scan is requested
	   * by the Rendez-Vous server
	   * 
	   * @param rdve the rendez-vous event detected by the listener
	   */
	public void rendezvousEvent(RendezvousEvent rdve) {
		if (rdve.getType() == RendezvousEvent.CLIENTCONNECT) {
			LOG.info("Client connected with peer ID: "+rdve.getPeer());	
			ndservice.getRemoteAdvertisements(rdve.getPeer(),DiscoveryService.ADV,null,null,5,this);
		} else if (rdve.getType() == RendezvousEvent.CLIENTDISCONNECT) {
			LOG.info("Client disconnected with peer ID: "+rdve.getPeer());
		} else if (rdve.getType() == RendezvousEvent.CLIENTFAILED) {
			
		} else if (rdve.getType() == RendezvousEvent.CLIENTRECONNECT) {
			
		}
	}

	public void shutdown() {
		this.nmanager.stopNetwork();
	}

	@Override
	/**
	   * When peer scan ends, the advertisements are published to the rendez-vous server.
	   * It then browses through the advertisements to find one matching the Ubeeko
	   * P2P socket services.  
	   * It then updates the Ubeeko P2P peers tracking list 
	   * 
	   * @param de the discovery event detected by the listener
	   */
	public void discoveryEvent(DiscoveryEvent de) {
		// TODO Auto-generated method stub
		PeerID DatanodeP2PPeerID;
		String DatanodeP2PHostname;
		int DatanodeP2PPort;
		
		DiscoveryResponseMsg msg = de.getResponse();
		Enumeration<Advertisement> enumAdv = msg.getAdvertisements(); 
			
		if (enumAdv != null) 
		{ 
			while (enumAdv.hasMoreElements())
			{
				DatanodeP2PPeerID = null;
				
				Advertisement adv = (Advertisement) enumAdv.nextElement();
				Boolean isUbeekoPipeAdv = false;
				
				if ( adv.getAdvType() == ADV_P2P_TYPE ) {
					LOG.debug("Received a pipe advertisement: "+adv.getID());
					String peerId = "urn:" + ((EndpointAddress)de.getSource()).getProtocolName() +":" + ((EndpointAddress)de.getSource()).getProtocolAddress();
					
					try {
						DatanodeP2PPeerID = (PeerID) IDFactory.fromURI(new URI(peerId));
					} catch (URISyntaxException use) {
						LOG.error(StringUtils.stringifyException(use));
					}
					
					StructuredDocument advDoc = (StructuredDocument)adv.getDocument(MimeMediaType.XMLUTF8);
					Enumeration<Element> enumElem = advDoc.getChildren();
					
					if (enumElem != null) 
					{
						while (enumElem.hasMoreElements())
						{
							isUbeekoPipeAdv = false;
							DatanodeP2PHostname = null;
							DatanodeP2PPort = 0;
											
							Element elem = (Element)enumElem.nextElement();
							
							if (elem.getKey().toString().contentEquals(ADV_P2P_SOCKET_NAME)  && elem.getValue().toString().contains(ADV_P2P_SOCKET_VALUE) ) {
								// This is an ubeeko pipe advertisement
								isUbeekoPipeAdv = true;
							}
							
							if (isUbeekoPipeAdv) {
								LOG.debug("This is a Ubeeko pipe adv"+adv.getID());
								String pipeName = elem.getValue().toString();
								
								if(pipeName.matches(ADV_P2P_SOCKET_REGEX)) {
										Pattern pat = Pattern.compile(ADV_P2P_SOCKET_REGEX_EXTRACT);
										Matcher mat = pat.matcher(pipeName);
										
										if (mat.find()) {
											DatanodeP2PHostname = mat.group(1);
											
											try {
												DatanodeP2PPort = Integer.parseInt(mat.group(2));
											} catch (NumberFormatException nfe) {
												LOG.error(StringUtils.stringifyException(nfe));
											} 
										}
								}
								
								if (DatanodeP2PPort > 0 && DatanodeP2PHostname != null && DatanodeP2PPeerID != null) {																		
									if (!P2PSocketPeers.containsValue(DatanodeP2PHostname+":"+DatanodeP2PPort)) {
										LOG.info("A new P2P Ubeeko Socket service has been found.");	
										LOG.info("Adding new mapping: "+DatanodeP2PPeerID.toString()+" / "+DatanodeP2PHostname+":"+DatanodeP2PPort);
										P2PSocketPeers.put(DatanodeP2PPeerID,DatanodeP2PHostname+":"+DatanodeP2PPort);
									}
								}
							}
						}
					}
				}
			} 
		} 
	}	
}
