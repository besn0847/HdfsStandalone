package com.agorasoft.ubeeko.p2psockets;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class P2PInetAddress implements Serializable {
    private static final Logger log = Logger.getLogger(P2PInetAddress.class.getName());
    
    /** If we have generated an IP address for the local host then we
     * cache its value so that the value will remain the same during a single
     * session. All access to this variable should be done with the
     * methods getLocalHostAddress() and setLocalHostAddress() to ensure
     * thread-safety (these two methods are synchronized). */
    private static byte[] localHostAddress;
    
    /** Holds our P2PInetAddress instance host name. */
    private String hostName;
    
    /** Holds our P2PInetAddress instance IP address. */
    private int address = -1;
    
    P2PInetAddress(String hostName, int address) {
        this(hostName, getAddress(address));
    }
    
    P2PInetAddress() {
        this(null, getAddress(0));
    }
    
    P2PInetAddress(String hostName, byte address[]) {
        if (hostName == null && address == null) {
            hostName = getLocalHostName();
            address = getIPAddress(hostName);
        }
        
        if (isLocalHost(hostName)) {
            hostName = getLocalHostName();
        }
        
        // is the address null and the host an IP string?
        if (address == null && isIPAddress(hostName)) {
            address = fromIPString(hostName);
        }
        
        // if the host is just an IP string than throw its value away
        if (isIPAddress(hostName)) {
            hostName = null;
        }
        
        if (address != null) {
            // convert the IP bytes into an integer to store
            int intAddress = getIPAddressAsInteger(address);
            this.address = intAddress;
        } else {
            this.address = getIPAddressAsInteger(generateIPAddress(hostName));
        }
        
        if (toIPString(getAddress(this.address)).equals("127.0.0.1") ||
                toIPString(getAddress(this.address)).equals("0.0.0.0")) {
            if (hostName == null) {
                hostName = getLocalHostName();
            }
            
            this.address = getIPAddressAsInteger(generateIPAddress(hostName));
        }
        
        this.hostName = hostName;
    }
    
    public static boolean isLocalHost(String host){
        try{
        if(
                host == null ||
                host.equals("localhost")||
                host.equals("127.0.0.1")||
                host.equals("0.0.0.0") ||
                host.equals(getLocalHostName())
                ){
            return true;
        }else if(host.equals(InetAddress.getLocalHost().getHostAddress())){
            log.warn("Warning: specified host "+host+"" +
                                    " is the local host ip address. We are going " +
                                    "to interpret this as the p2p localhost " +
                                    "and not a p2p remote address.");
            return true;
        }else{
            return false;
        }
        }catch(UnknownHostException uhe){
            throw new RuntimeException(uhe);
        }
    }
    /**
     * Create an InetAddress based on the provided host name and IP address
     * No name service is checked for the validity of the address.
     *
     * <p> The host name can either be a machine name, such as
     * "<code>java.sun.com</code>", or a textual representation of its IP
     * address.
     *
     * <p> IPv4 address byte array must be 4 bytes long.
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     * @return  an InetAddress object created from the raw IP address
     * and hostname.
     */
    public static InetAddress getByAddress(String host, byte[] addr)
    throws P2PInetAddressException {
        P2PInetAddress p2pAddr = new P2PInetAddress(host, addr);
        
        return p2pAddr.toInetAddress();
    }
    
    /**
     * Determines the IP address of a host, given the host's name.
     *
     * <p> The host name can either be a machine name, such as
     * "<code>java.sun.com</code>", or a textual representation of its
     * IP address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * @param      host   the specified host, or <code>null</code> for the
     *                    local host.
     * @return     an IP address for the given host name.
     * @exception  UnknownHostException  if no IP address for the
     *               <code>host</code> could be found.
     */
    public static InetAddress getByName(String host) throws UnknownHostException {
        try {
            byte[] addr = null;
            
            // make sure someone didn't pass in an IP address as a string
            if (isIPAddress(host)) {
                // check to see if this is the local host or the "any" address; if so
                // use our local peer name to generate the required info
                if (host.equals("127.0.0.1") || host.equals("0.0.0.0")) {
                    host = getLocalHostName();
                    addr = getIPAddress(host);
                } else {
                    InetAddress hostInetAddr = P2PInetAddress.getByAddress(host, null);
                    
                    addr = hostInetAddr.getAddress();
                }
            }
            
            if (isLocalHost(host))
                host = getLocalHostName();
            
            // search the network to see if there is an ip address
            // for the given hostname
            if (addr == null)
                addr = P2PNameService.findIPAddress(host);
            
            // if there is no pre-existing advertisement, just generate an IP address
            if (addr == null)
                addr = getIPAddress(host);
            
            P2PInetAddress p2pAddr = new P2PInetAddress(host, addr);
            
            return p2pAddr.toInetAddress();
        } catch (Exception e) {
            log.error("Exception resolveing address of "+host,e);
            throw new UnknownHostException(e.toString());
        }
    }
    
    /**
     * Given the name of a host, returns an array of its IP addresses,
     * based on the configured name service on the system.
     *
     * <p> The host name can either be a machine name, such as
     * "<code>java.sun.com</code>", or a textual representation of its IP
     * address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * @param      host   the name of the host.
     * @return     an array of all the IP addresses for a given host name.
     *
     * @exception  UnknownHostException  if no IP address for the
     *               <code>host</code> could be found.
     */
    public static InetAddress[] getAllByName(String host)
    throws UnknownHostException {
        InetAddress results[] = new InetAddress[1];
        
        results[0] = P2PInetAddress.getByName(host);
        
        return results;
    }
    
    /**
     * Returns an <code>InetAddress</code> object given the raw IP address .
     * The argument is in network byte order: the highest order
     * byte of the address is in <code>getAddress()[0]</code>.
     *
     * <p> This method doesn't block, i.e. no reverse name service lookup
     * is performed.
     *
     * <p> IPv4 address byte array must be 4 bytes long.
     *
     * @param addr the raw IP address in network byte order
     * @return  an InetAddress object created from the raw IP address.
     * @exception  UnknownHostException  if IP address is of illegal length
     */
    public static InetAddress getByAddress(byte[] addr)
    throws P2PInetAddressException {
        P2PInetAddress p2pAddr = new P2PInetAddress(null, addr);
        
        return p2pAddr.toInetAddress();
    }
    
    /**
     * Returns the local host.
     *
     * @return     the IP address of the local host.
     *
     * @exception  UnknownHostException  if no IP address for the
     *               <code>host</code> could be found.
     */
    public static InetAddress getLocalHost() throws P2PInetAddressException {
        P2PInetAddress p2pAddr = new P2PInetAddress(null, null);
        
        return p2pAddr.toInetAddress();
    }
    
    public static InetAddress anyLocalAddress() throws P2PInetAddressException {
        InetAddress results = P2PInetAddress.getByAddress("0.0.0.0", null);
        
        return results;
    }
    
    public static InetAddress loopbackAddress() throws P2PInetAddressException {
        InetAddress results = P2PInetAddress.getByAddress("localhost", null);
        
        return results;
    }
    
    /** Converts this P2PInetAddress into a java.net.InetAddress object. */
    protected InetAddress toInetAddress() throws P2PInetAddressException {
        try {
            // make sure we have all the information we need:
            // both a host name and an IP address
            if (hostName != null && address == -1) {
                address = getIPAddressAsInteger(getIPAddress(hostName));
            } else if (hostName == null && address != -1) {
                String ipAddressString = toIPString(getAddress(address));
                hostName = P2PNameService.findHostName(ipAddressString);
            }
            
            if (hostName == null && address == -1) {
                String message = "Not enough information was provided for " +
                        "P2PInetAddress: hostName=" + hostName +
                        ", address=" + address;
                log.error(message);
                throw new UnknownHostException(message);
            }
            
            // if no host name at this point then just "stringify" the IP address
            if (hostName == null) {
                hostName = getIPString(getAddress(address));
            }
            
            InetAddress results = InetAddress.getByAddress(hostName, getAddress(address));
            
            // set this InetAddress object's canonicalHostName field using reflection
            Field canonicalHostNameField = null;
            Field fields[] = InetAddress.class.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                if (fields[i].getName().equals("canonicalHostName")) {
                    fields[i].setAccessible(true);
                    canonicalHostNameField = fields[i];
                    break;
                }
            }
            
            if (canonicalHostNameField == null) {
                throw new UnknownHostException("canonicalHostName could not be found through " +
                        "Java reflection.");
            }
            
            canonicalHostNameField.set(results, hostName);
            
            return results;
        } catch (Exception e) {
            throw new P2PInetAddressException(e);
        }
    }
    
    /** Returns the IP address for a given host name
     * as a series of bytes.
     *
     * If host name is null, the it is assumed that
     * the "any"/wildcard interface is being requested
     * (0.0.0.0).  We resolve this to being the host name
     * and IP address for the localhost.  The same thing
     * is returned for the loop back interface (i.e.
     * the host "localhost" or the address "127.0.0.1").
     */
    protected static byte[] getIPAddress(String hostName) {
        if (isLocalHost(hostName)) {
            hostName = getLocalHostName();
            if (getLocalHostAddress() == null) {
                setLocalHostAddress(generateIPAddress(hostName));
            }
            
            return getLocalHostAddress();
        }
        
        return generateIPAddress(hostName);
    }
    
    /** Returns the IP address for a given host name
     * as a formatted string.
     *
     * If host name is null, the it is assumed that
     * the "any"/wildcard interface is being requested
     * (0.0.0.0).  We resolve this to being the host name
     * and IP address for the localhost.  The same thing
     * is returned for the loop back interface (i.e.
     * the host "localhost" or the address "127.0.0.1").
     */
    protected static String getIPString(String hostName) {
        return toIPString(getIPAddress(hostName));
    }
    
    /** Converts an IP address from a series of bytes to a string. */
    protected static String getIPString(byte[] addr) {
        StringBuffer results = new StringBuffer();
        
        try {
            results.append(new Byte(addr[0]).toString());
            results.append(".");
            results.append(new Byte(addr[1]).toString());
            results.append(".");
            results.append(new Byte(addr[2]).toString());
            results.append(".");
            results.append(new Byte(addr[3]).toString());
            
            return results.toString();
        } catch (NumberFormatException e) {
            // should never happen
            log.error("Exception formating address", e);
            throw new RuntimeException(e.toString());
        }
    }
    
    
    /** Hashes hostnames into an IPv4 format (i.e. four bytes).
     * We generate this value by cycling over each of the bytes
     * in the hostName string.  At each stop, we add the byte
     * value to the byte value in the 4 byte IP array, also cycling
     * through the 4 byte IP array.
     *
     * None of the bytes can have the values 0, 127, or 255.  To protected
     * against this, we check to see if the generated IP address has the
     * bytes 0, 127, or 255.  If any byte is 0 or 127, we add one to it;
     * if any byte is 255, then we subtract one from it.
     */
    protected static byte[] generateIPAddress(String hostName) {
        byte results[] = new byte[4];
        results[0] = 0;
        results[1] = 0;
        results[2] = 0;
        results[3] = 0;
        
        byte hostNameBytes[] = hostName.getBytes();
        
        for (int i = 0; i < hostNameBytes.length; i++) {
            results[i % 4] += hostNameBytes[i];
        }
        
        // make sure we don't have the values 0, 127, or 255
        for (int i = 0; i < 4; i++) {
            if (results[i] == 0 || results[i] == 127)
                results[i] += (byte)1;
            else if (results[i] == 255)
                results[i] -= - (byte)1;
        }
        
        return results;
    }
    
    /** Converts a series of bytes expressing an IP address
     * into a string, such as "33.22.44.12".
     */
    protected static String toIPString(byte[] value) {
        String results;
        if (value == null) {
            results = "0.0.0.0";
        } else {
            results = (value[0] & 0xff) + "." +
                    (value[1] & 0xff) + "." +
                    (value[2] & 0xff) + "." +
                    (value[3] & 0xff);
        }
        
        return results;
    }
    
    /** Converts a dotted IP string into an array of 4 bytes.
     */
    protected static byte[] fromIPString(String ipString) {
        // this is a tricky process; we are using bytes to hold
        // our values, but in Java bytes are signed (i.e. from -127 to 127).
        // when we convert the byte into a string they come out to the correct
        // values because we use boolean arithmetic.
        // we need to convert the string into a short (16 bits), then throw
        // away the high-order 8 bits and turn it back into a byte
        
        byte[] results = new byte[4];
        
        StringTokenizer tk = new StringTokenizer(ipString, ".", false);
        try {
            // go over each character of each byte
            for (int i = 0; i < 4; i++) {
                String ipByte = tk.nextToken();
                results[i] = new Short(ipByte).byteValue();
            }
        } catch (NumberFormatException e) {
            log.error("Exception parsing byte address", e);
            throw new RuntimeException(e.toString());
        }
        
        return results;
    }
    
    /** Checks to see if the given value in 'checkMe' is
     * an IP address.
     */
    protected static boolean isIPAddress(String checkMe) {
        if (checkMe == null)
            return false;
        
        boolean results = false;
        
        StringTokenizer tk = new StringTokenizer(checkMe, ".", false);
        if (tk.countTokens() != 4)
            return false;
        
        // go over each character of each byte; make sure that it
        // is actually a number
        for (int i = 0; i < 4; i++) {
            String ipByte = tk.nextToken();
            
            if (ipByte.length() > 3) // bytes can't be longer than 3 numbers
                return false;
            
            for (int j = 0; j < ipByte.length(); j++) {
                char ipChar = ipByte.charAt(j);
                switch (ipChar) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': break;
                    default: return false;
                }
            }
        }
        
        return true;
    }
    
    protected static int getIPAddressAsInteger(byte[] addr) {
        int results = 0;
        
        results = addr[3] & 0xFF;
        results |= (addr[2] << 8) & 0xFF00;
        results |= (addr[1] << 16) & 0xFF0000;
        results |= (addr[0] << 24) & 0xFF000000;
        
        return results;
    }
    
    /** Gets the local host name for this peer, such as "Brad GNUberg".
     */
    protected static String getLocalHostName() {
        return P2PNameService.PEER_SERVICE_PREFIX +
                P2PNetwork.getPeerGroup().getPeerName() +
                P2PNameService.PEER_SERVICE_SUFFIX;
    }
    
    /** This method mediates access to the static localHostAddress variable
     * to help with thread-safety. */
    protected static synchronized byte[] getLocalHostAddress() {
        return localHostAddress;
    }
    
    /** This method mediates access to the static localHostAddress variable
     * to help with thread-safety. */
    protected static synchronized void setLocalHostAddress(byte[] addr) {
        localHostAddress = addr;
    }
    
    /**
     * Converts the IP address as the integer intAddr to a
     * byte array.
     */
    protected static byte[] getAddress(int intAddr) {
        byte[] results = new byte[4];
        
        results[0] = (byte)((intAddr >>> 24) & 0xFF);
        results[1] = (byte)((intAddr >>> 16) & 0xFF);
        results[2] = (byte)((intAddr >>> 8) & 0xFF);
        results[3] = (byte)(intAddr & 0xFF);
        
        return results;
    }
    
    //start jxl modification
    public int hashCode(){
        return this.hostName.hashCode()^this.address;
    }
    public boolean equals(Object o){
        if( this.getClass().isInstance(o)){
            return hashCode() == o.hashCode();
        }//else
        return false;
    }
    //end jxl modificatoin
}