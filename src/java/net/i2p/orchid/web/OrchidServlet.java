package net.i2p.orchid.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.subgraph.orchid.TorConfig;

import net.i2p.app.ClientAppManager;
import net.i2p.orchid.OrchidController;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Translate;

/**
 *  From base in i2psnark
 */
public class OrchidServlet extends BasicServlet {
    /** generally "/orchid" */
    private String _contextPath;
    /** generally "orchid" */
    private String _contextName;
    private volatile OrchidController _manager;
    private volatile boolean _isRunning;
    private static long _nonce;
    
    private static final String DEFAULT_NAME = "orchid";
    public static final String PROP_CONFIG_FILE = "orchid.configFile";
    private static final String DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n";
    private static final String FOOTER = "</div></center></body></html>";
    private static final String BUNDLE = "net.i2p.orchid.messages";

 
    public OrchidServlet() {
        super();
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
        String cpath = getServletContext().getContextPath();
        _contextPath = cpath == "" ? "/" : cpath;
        _contextName = cpath == "" ? DEFAULT_NAME : cpath.substring(1).replace("/", "_");
        _nonce = _context.random().nextLong();
        _isRunning = true;
        (new Starter()).start();
    }
    
    /**
     *  Wait for the ClientAppManager
     */
    private class Starter extends I2PAppThread {
        public void run() {
            File f = new File(_context.getAppDir(), "plugins");
            f = new File(f, _contextName);
            String[] args = new String[] { f.toString() };
            while (_isRunning) {
                ClientAppManager cam = _context.clientAppManager();
                if (cam != null) {
                    _manager = new OrchidController(_context, cam, args);
                    _manager.startup();
                    break;
                } else {
                    try {
                        Thread.sleep(10*1000);
                    } catch (InterruptedException ie) {}
                }
            }
        }
    }

    @Override
    public void destroy() {
        _isRunning = false;
        if (_manager != null)
            _manager.shutdown();
        super.destroy();
    }

    /**
     *  Handle what we can here, calling super.doGet() for the rest.
     *  @since 0.8.3
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGetAndPost(request, response);
    }

    /**
     *  Handle what we can here, calling super.doPost() for the rest.
     *  @since Jetty 7
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGetAndPost(request, response);
    }

    /**
     * Handle all here
     */
    private void doGetAndPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        // this is the part after /i2psnark
        String path = req.getServletPath();
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.write(DOCTYPE + "<html>\n" +
                  "<head>\n" +
                  "<title>");
        out.write(_("Orchid Controller"));
        out.write("</title>\n");
                                         
        out.write("</head>\n");
        out.write("<body><div><h3>Plugin Status</h3>");
        OrchidController c = _manager;
        if (c != null) {
            out.write("Status is: " + c.getState());
            ClientAppManager cam = _context.clientAppManager();
            if (cam != null)
                out.write("<br>Registered? " + (cam.getRegisteredApp("outproxy") != null));
            else
                out.write("<br>Not registered, no client manager");
            out.write("<h3>Circuit Status</h3><pre>");
            // not really in HTML for now
            c.renderStatusHTML(out);
            out.write("</pre>");
            TorConfig tc = c.getConfig();
            if (tc != null)
                out.write(getHTMLConfig(tc));
        } else {
            out.write("Status is: UNINITIALIZED");
        }
        out.write(FOOTER);
    }

    private static String getHTMLConfig(TorConfig tc) {
        StringBuilder buf = new StringBuilder(1024);
        buf.append("<h3>Configuration</h3>");
        buf.append("<table cellspacing=\"8\">");
        buf.append("<tr><th>Config</th><th>Value</th></tr>");
        buf.append("<tr><td>Bridges</td><td>").append(tc.getBridges()).append("</tr>");
        buf.append("<tr><td>Circuit Build Timeout</td><td>").append(tc.getCircuitBuildTimeout()).append("</tr>");
        buf.append("<tr><td>Circuit Idle Timeout</td><td>").append(tc.getCircuitIdleTimeout()).append("</tr>");
        buf.append("<tr><td>Circuit Stream Timeout</td><td>").append(tc.getCircuitStreamTimeout()).append("</tr>");
        buf.append("<tr><td>Client Reject Internal Address</td><td>").append(tc.getClientRejectInternalAddress()).append("</tr>");
        buf.append("<tr><td>Enforce Distinct Subnets</td><td>").append(tc.getEnforceDistinctSubnets()).append("</tr>");
        buf.append("<tr><td>Entry Guards</td><td>").append(tc.getNumEntryGuards()).append("</tr>");
        buf.append("<tr><td>Entry Nodes</td><td>").append(tc.getEntryNodes()).append("</tr>");
        buf.append("<tr><td>Exclude Exit Nodes</td><td>").append(tc.getExcludeExitNodes()).append("</tr>");
        buf.append("<tr><td>Exclude Nodes</td><td>").append(tc.getExcludeNodes()).append("</tr>");
        buf.append("<tr><td>Exit Nodes</td><td>").append(tc.getExitNodes()).append("</tr>");
        buf.append("<tr><td>Fascist Firewall</td><td>").append(tc.getFascistFirewall()).append("</tr>");
        buf.append("<tr><td>Firewall Ports</td><td>").append(tc.getFirewallPorts()).append("</tr>");
        buf.append("<tr><td>Handshake V2 Enabled</td><td>").append(tc.getHandshakeV2Enabled()).append("</tr>");
        buf.append("<tr><td>Handshake V3 Enabled</td><td>").append(tc.getHandshakeV3Enabled()).append("</tr>");
        buf.append("<tr><td>Long Lived Ports</td><td>").append(tc.getLongLivedPorts()).append("</tr>");
        buf.append("<tr><td>Max Circuit Dirtiness</td><td>").append(tc.getMaxCircuitDirtiness()).append("</tr>");
        buf.append("<tr><td>Max Client Circuits Pending</td><td>").append(tc.getMaxClientCircuitsPending()).append("</tr>");
        buf.append("<tr><td>New Circuit Period</td><td>").append(tc.getNewCircuitPeriod()).append("</tr>");
        buf.append("<tr><td>Safe Logging</td><td>").append(tc.getSafeLogging()).append("</tr>");
        buf.append("<tr><td>Safe Socks</td><td>").append(tc.getSafeSocks()).append("</tr>");
        buf.append("<tr><td>Strict Nodes</td><td>").append(tc.getStrictNodes()).append("</tr>");
        buf.append("<tr><td>Use Bridges</td><td>").append(tc.getUseBridges()).append("</tr>");
        buf.append("<tr><td>Use Microdescriptors</td><td>").append(tc.getUseMicrodescriptors()).append("</tr>");
        buf.append("<tr><td>Use NTor Handshake</td><td>").append(tc.getUseNTorHandshake()).append("</tr>");
        buf.append("<tr><td>Warn Unsafe Socks</td><td>").append(tc.getWarnUnsafeSocks()).append("</tr>");
        buf.append("</table>");
        return buf.toString();
    }

    /** translate */
    private String _(String s) {
        return Translate.getString(s, _context, BUNDLE);
    }

    /** translate */
    private String _(String s, Object o) {
        return Translate.getString(s, o, _context, BUNDLE);
    }

    /** translate */
    private String _(String s, Object o, Object o2) {
        return Translate.getString(s, o, o2, _context, BUNDLE);
    }

    /** translate (ngettext) @since 0.7.14 */
    private String ngettext(String s, String p, int n) {
        return Translate.getString(n, s, p, _context, BUNDLE);
    }

    /** dummy for tagging */
    private static String ngettext(String s, String p) {
        return null;
    }

}
