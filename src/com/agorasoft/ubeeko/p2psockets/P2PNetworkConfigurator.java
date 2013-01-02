package com.agorasoft.ubeeko.p2psockets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class P2PNetworkConfigurator extends NetworkConfigurator {
	
	private final transient static Logger LOG = Logger.getLogger(P2PNetworkConfigurator.class.getName());

	public static NetworkManager setConfiguration(NetworkManager manager) {
		NetworkManager nm = manager;
		NetworkConfigurator nc = null;
		
		try {
			nc = manager.getConfigurator();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		nc.setHttpEnabled(false);
		nc.setTcpEnabled(true);
		nc.clearRendezvousSeedingURIs();
		nc.clearRendezvousSeeds();
	
		try {
			nc.addSeedRendezvous(new URI("tcp://mybinccloud.dynsdns.org:26420"));
		} catch (URISyntaxException use) {
			use.printStackTrace();
		}
		
		PlatformConfig advertisement = (PlatformConfig) nc.getPlatformConfig();
		advertisement.setDescription("Platform Config Advertisement created by : " + P2PNetworkConfigurator.class.getName() );
		 
        String peerName = advertisement.getName();
        if ((null == peerName) || (0 == peerName.trim().length())) {
            String jpn = System.getProperty("jxta.peer.name", "" );
            
            if(0 != jpn.trim().length()) {
                peerName = jpn;
                advertisement.setName(jpn);
            }
        }
		
		int port = 9701;
        // get the port from a property
        String tcpPort = System.getProperty("jxta.tcp.port");
        if (tcpPort != null) {
            try {
                int propertyPort = Integer.parseInt( tcpPort );
                if( (propertyPort < 65536) && ( propertyPort >= 0 ) )
                    port = propertyPort;
                else {
                    if( LOG.isEnabledFor(Level.WARN) )
                        LOG.warn( "Property 'jxta.tcp.port' is not a valid port number : " + propertyPort );
                }
                
            } catch ( NumberFormatException ignored ) {
                if( LOG.isEnabledFor(Level.WARN) )
                    LOG.warn( "Property 'jxta.tcp.port' was not an integer : " + tcpPort );
            }
        }
		     
        TCPAdv tcpAdv = (TCPAdv) AdvertisementFactory.newAdvertisement( TCPAdv.getAdvertisementType() );
        tcpAdv.setProtocol( "tcp" );
        tcpAdv.setInterfaceAddress( null );
        tcpAdv.setPort( port );
        tcpAdv.setMulticastAddr( "224.0.1.85" );
        tcpAdv.setMulticastPort( 1234 );
        tcpAdv.setMulticastSize( 16384 );
        tcpAdv.setMulticastState( true );
        tcpAdv.setServer( null );
        
        StructuredDocument tcp = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
        
        StructuredDocumentUtils.copyElements(tcp, tcp, (StructuredDocument) tcpAdv.getDocument(MimeMediaType.XMLUTF8));
       	
        advertisement.putServiceParam(PeerGroup.tcpProtoClassID, tcp);		
        
        try {
        	nc.save();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
        
		return nm;
	}

}
