package com.agorasoft.ubeeko.utils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.server.common.HdfsConstants;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.Tool;

import com.agorasoft.ubeeko.p2pdatanode.PeerDataNode;

public class DataNodeUtils implements Tool, FSConstants {	
	Configuration dnconfig = null;;
	InetSocketAddress isatarget = null;
	Socket dnsocket = null;
	DataOutputStream dos = null;
	
	public DataNodeUtils() {
		super();
		dnconfig = new Configuration();
		dnconfig.addDefaultResource("hdfs-default.xml");
	}
	
	public void init() {
		System.out.println("Configuration is: "+dnconfig.toString());
		
		String target = this.dnconfig.get("dfs.datanode.address");
		isatarget = NetUtils.createSocketAddr(target);
		if(isatarget.getHostName().contentEquals("0.0.0.0")) isatarget = NetUtils.createSocketAddr("127.0.0.1:"+isatarget.getPort());
		
		dnsocket = new Socket();
		try {
			NetUtils.connect(dnsocket, isatarget, HdfsConstants.READ_TIMEOUT);
			 OutputStream baseStream = NetUtils.getOutputStream(dnsocket, HdfsConstants.WRITE_TIMEOUT);
		     dos = new DataOutputStream(new BufferedOutputStream(baseStream, SMALL_BUFFER_SIZE));
		} catch (IOException ioe) {
			System.out.println("Cannot connect to socket; Exiting...");
			System.exit(8);
		}
		
		try {
			dos.writeShort(DataTransferProtocol.DATA_TRANSFER_VERSION);
			
		} catch (IOException ioe) {
			System.out.println("Cannot send packets to datanode; Exiting...");
			System.exit(8);
		}
		
	}
	
	@Override
	public int run(String[] args) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Configuration getConf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConf(Configuration conf) {
		// TODO Auto-generated method stub

	}
	
	public static void main(String args[]) {
		DataNodeUtils dnutils = new DataNodeUtils();
		dnutils.init();
	}

}
