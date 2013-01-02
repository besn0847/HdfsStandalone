package com.agorasoft.ubeeko.p2psockets;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Random;

import net.jxta.discovery.DiscoveryService;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.platform.NetworkManager;

import org.apache.log4j.Logger;

public class P2PNetwork {
	/**
	 * Copyright (c) 2010 by Agorasoft
	 *
	 * Redistributions in source code form must reproduce the above copyright and this condition.
	 *
	 * The contents of this file are subject to the Sun Project JXTA License Version 1.1 (the "License");
	 * you may not use this file except in compliance with the License. A copy of the License is available
	 * at http://www.jxta.org/jxta_license.html.
	 */

	/**
	 * This class signs us into the JXTA network.
	 * It is meant to model the JXTA network as a whole.
	 *
	 * By default, we re-profile this peer every time it starts
	 * up using the JXTA Profiler.  To turn off this behavior,
	 * call the signin() method with the fourth boolean argument
	 * set to false to indicate not to peform profiling.
	 *
	 * Threading: the entire object is locked if
	 * any methods are called to mediate access
	 * to the netPeerGroup.
	 * We are assuming net.jxta.peergroup.PeerGroup
	 * is thread-safe.
	 *
	 * This class has been revised to allow the user to specify what peer group to
	 * use. If no PeerGroup is specified, then this class will use the netPeerGroup.
	 *
	 * @author Franck Besnard
	 * @version 0.1
	 *
	 * @todo Doesn't seem to work correctly if the peer itself is a Net Peer Group rendezvous.
	 * @todo Create a constructor for initialize that takes an application string; this will
	 * hash the string into a peer group ID, and then find or create an application level
	 * peer group.  We should also have an initialize() method that takes a PeerGroup object,
	 * which will find or create the given peer group.  This first method is for those programmers
	 * who don't know anything about Jxta, while the second is for those who know more.
	 */
	
	private static final Logger log = Logger.getLogger(P2PNetwork.class.getName());
	    
	private static P2PSocketLooupManager lookupAddapter = null;
	    
	/** The system property name for setting the P2P network to sign into and initialize. */
	public static String P2PSOCKETS_NETWORK = "p2psockets.network";
	    
	 /** The system property name for the username of this Jxta peer.*/
    public static String JXTA_USERNAME = "net.jxta.tls.principal";
    /** The system property name for the password of this Jxta peer.*/
    public static String JXTA_PASSWORD = "net.jxta.tls.password";
    
    /** The system property name that specifies where to find the Jxta configuration * files. */
	public static String JXTA_HOME = "JXTA_HOME";
	    
	private static PeerGroup peerGroup;
	    
	/** The name to use for scoping all server sockets and sockets. */
	private static String applicationName;
	    
	private static boolean signedIn = false;
	    
    /**
     * Signin using complete peer configuration. No gui is shown to the user. Further
     * Tcp port confilcts are handled, and multiple apps may be run in the same dir.
     * @param peerName The virtual hostname of the configured peer.
     * @param appName 
     * @throws java.lang.Exception 
     */
    @SuppressWarnings("deprecation")
	public static synchronized void autoSignin(String peerName, String appName) throws Exception {
        // make sure we aren't already signed in
        if (signedIn) {
            return;
        }
        if(peerName == null || appName == null){
            throw new IllegalArgumentException("Neither peerName nor appName may be null");
        }
	        
        log.info("[Auto Signin]");
	        
        File dir = File.createTempFile("jxta",".d");
        dir.delete();
        System.setProperty("JXTA_HOME",dir.getAbsolutePath());
        System.setProperty("jxta.peer.name", peerName);
	        
        int tcpPort = 9700;
        while(true){
            try{
                tcpPort++;
                ServerSocket ss = new ServerSocket(tcpPort);
                ss.close();
                break;
            }catch(IOException ioe){}
        }

        System.setProperty("jxta.tcp.port", ""+tcpPort);
        System.setProperty(JXTA_USERNAME, "userName");
        System.setProperty(JXTA_PASSWORD, "password");
	        
        //PeerGroupFactory.setConfiguratorClass(P2PConfigurator.class);
        NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.EDGE,peerName,dir.toURI());
        manager.setConfigPersistent(true);
        manager = P2PNetworkConfigurator.setConfiguration(manager);
        
        applicationName = appName;
	    
        manager.startNetwork();
        
        log.info("Getting net peer group from network manager");
        peerGroup = manager.getNetPeerGroup();
        
        //wait around until we contact a rendezvous server
        log.info("Net peer group found:" + peerGroup.getPeerGroupName());
        
        
        log.info("Contacting remote RendezVous peer");
        if (peerGroup.isRendezvous() == false) manager.waitForRendezvousConnection(0);
        
        DiscoveryService rootPGdiscovery = peerGroup.getDiscoveryService();
        
        log.debug("Registering the new application within the PeerGroup");
        lookupAddapter = new P2PSocketLooupManager(peerGroup,applicationName);
        lookupAddapter.start();
	        
        signedIn = true;
	}
    
    //Ubeeko simple signIn
    public static synchronized void signIn(PeerGroup peerGrp, String appName) throws IllegalArgumentException {
        // make sure we aren't already signed in
        if (signedIn) {
            return;
        }
        if(peerGrp == null || appName == null){
            throw new IllegalArgumentException("Neither peerGroup nor appName may be null");
        }
             
        log.info("Signin into the Ubeeko P2P Sockets Network");
        
        applicationName = appName;
        peerGroup = peerGrp;
	    
        //wait around until we contact a rendezvous server
        log.info("Net peer group found:" + peerGroup.getPeerGroupName());
        
        
        lookupAddapter = new P2PSocketLooupManager(peerGroup,applicationName);
        lookupAddapter.start();
	        
        signedIn = true;
	}
	
	/** Signs us out of the P2P network.
	* FIXME: Actually do something here.
	*/
	public static void signOff() throws Exception {
		signedIn = false;
		throw new RuntimeException("not implemented");
	}
	    
	/**
	* Get the number of server:port nodes believed to be active on this network.
	*/
	public static int getNetworkNodeCount()throws IOException{
		if(lookupAddapter != null){
			return Collections.list(lookupAddapter.getAdvertisements()).size();
		}else{
			return -1;
		}
	}
	
	/**
	*This method returns the PeerGroup that was used to initialize P2PSockets.
	*If the version of signin() used was one that takes a PeerGroup param
	*then this method will return that PeerGroup.
	*Otherwise it will retun the netPeerGroup object from PeerGroupFactory.
	*/
	public static synchronized PeerGroup getPeerGroup(){
		if (peerGroup == null) {
			throw new RuntimeException("You must call P2PNetwork.signin() before " + " attempting to use this class");
		}
	        
		return peerGroup;
	}
	
	/**
	* This method can be used to change the networkName (also called the applicationName)
	* after the peer has been configured. Great care should be applied when using this method.
	*/
	public static synchronized void setApplicationName(String appName) {
		applicationName = appName;
	}
	    
	public static synchronized String getApplicationName() {
		return applicationName;
	}
}
