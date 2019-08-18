package net.i2p.orchid.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.TorConfig;

import net.i2p.app.ClientAppManager;
import net.i2p.orchid.OrchidController;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Translate;

import net.i2p.I2PAppContext;

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
    private static final String DOCTYPE = "<!DOCTYPE HTML>\n";
    private static final String FOOTER = "</div>\n<span id=\"endOfPage\" data-iframe-height></span>\n</body>\n</html>";
    private static final String BUNDLE = "net.i2p.orchid.messages";
    private static final String RESOURCES = "/orchid/resources/";
 
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
        public Starter() {
            super("Orchid Starter");
        }

        public void run() {
            try {
                run2();
            } catch (Throwable t) {
                // class problems, old router version, ...
                _log.error("Unable to start Orchid", t);
                _isRunning = false;
            }
        }

        private void run2() {
            File f = new File(_context.getConfigDir(), "plugins");
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
        // this is the part after /orchid
        String path = req.getServletPath();
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.write(DOCTYPE + "<html>\n<head>\n<title>");
        out.write(_t("Orchid Tor Client"));
        out.write("</title>\n");
        out.write("<meta http-equiv=\"Content-Security-Policy\" content=\"script-src \'self\' \'unsafe-inline\';\">\n");
        out.write("<link rel=\"icon\" href=\"" + RESOURCES + "images/favicon.png\">\n");
        out.write("<link href=\"" + RESOURCES + "orchid.css\" rel=\"stylesheet\" type=\"text/css\">\n");
        out.write("<noscript><style>.script, #expand, #collapse {display: none !important;} " +
                   "#configuration {display: table !important;} *::selection {color: #fff; background: #77f;}" +
                   ".node:active {cursor: text;}</style></noscript>\n");
        out.write("</head>\n");
        out.write("<body id=\"orchid\" onload=\"hideConfig();\">\n<div id=\"container\">\n<table id=\"main\" width=\"100%\">\n" +
                  "<thead><tr><th id=\"title\" align=\"left\">Orchid Tor Client</th></tr></thead>\n");
        out.write("<tbody>\n<tr><td>\n<hr>\n<table id=\"status\" width=\"100%\">\n<tr class=\"subtitle\">" +
                  "<th width=\"33%\">Status</th><th width=\"34%\">Registered with I2P</th><th width=\"33%\">Plugin Version</th></tr>\n");
        out.write("<tr><td align=\"center\">");
        OrchidController c = _manager;
        String status = c.getState().toString();
        if (c != null) {
            if (status.equals("RUNNING"))
                out.write("<span id=\"running\">Running</span>");
            else if (status.equals("STARTING"))
                out.write("<span id=\"starting\">Starting...</span>");
            else
                out.write("" + c.getState());
        } else {
            out.write("Not initialized");
        }
        out.write("</td><td align=\"center\">");
        ClientAppManager cam = _context.clientAppManager();
        if (c != null && cam != null) {
            if (cam.getRegisteredApp("outproxy") != null) {
//                out.write("<span id=\"registered\">" + (cam.getRegisteredApp("outproxy") != null) + "</span>");
                out.write("<span id=\"registered\">Yes</span>");
            } else {
                out.write("<span id=\"starting\">In progress...</span>");
            }
        } else {
            out.write("<span id=\"notregistered\" title=\"Not registered, no client manager\">Not registered, no client manager</span>");
        }
        out.write("</td>" +
                  "<td  align=\"center\">" +
                  Tor.getFullVersion() + "</td></tr>\n</table>\n");
        if (c != null) {
            out.write("<hr>\n<!-- Circuit Status -->\n<tr><th id=\"circuitstatus\" align=\"left\">Circuit Status&nbsp;" +
                      "<span id=\"refresh\" style=\"float: right;\"><a href=\".\">Refresh</a></span></th></tr>\n");
            out.write("<tr><td>\n<hr>\n");
            // yes, really in HTML now!
            c.renderStatusHTML(out);
            out.write("</table>\n</td></tr>\n<!-- end Circuit Status -->\n");
            TorConfig tc = c.getConfig();
            if (tc != null)
                out.write("<tr id=\"configsection\"><td>\n<hr>\n<div id=\"configtitle\"><b>Configuration Parameters</b>&nbsp;\n" +
                          "<a class=\"script\" id=\"expand\" href=\"#\" onclick=\"clean();expand();\"><img alt=\"Expand\" src=\"/orchid/resources/images/expand.png\" title=\"Expand\"></a>\n" +
                          "<a class=\"script\" id=\"collapse\" href=\"#\" onclick=\"clean();collapse();\"><img alt=\"Collapse\" src=\"/orchid/resources/images/collapse.png\" title=\"Collapse\"></a></div>\n");
                out.write(getHTMLConfig(tc));
        }
        out.write("<script src=\"" + RESOURCES + "ajaxRefresh.js\" type=\"application/javascript\"></script>\n");
        out.write("<script src=\"" + RESOURCES + "toggleConfig.js\" type=\"application/javascript\"></script>\n");
        out.write(FOOTER);
    }

    private static String getHTMLConfig(TorConfig tc) {
        File configPath = I2PAppContext.getGlobalContext().getConfigDir();
        String slash = System.getProperty("file.separator");
        StringBuilder buf = new StringBuilder(1024);
        buf.append("<hr>\n<table id=\"configuration\" width=\"100%\">\n");
        buf.append("<tr><td class=\"notice\" colspan=\"3\">");
        buf.append("The configuration is stored at <code>" + configPath + slash + "plugins" + slash + "orchid" + slash + "orchid.config</code>. " +
                   "Any changes will require a restart of the plugin to take effect. " +
                   "For more information on the configuration options, see <a href=\"https://www.torproject.org/docs/tor-manual.html.en\" target=\"_blank\">Tor's Online Manual</a>.");
        buf.append("</td></tr>\n");
        buf.append("<tr><th align=\"left\">Setting Name</th><th align=\"left\">Value <i>(hint)</i></th><th align=\"left\" width=\"50%\">Notes</th></tr>\n");
        buf.append("<tr><td>Bridges</td><td><code>");
        if (!tc.getBridges().isEmpty())
            buf.append(tc.getBridges());
        buf.append("</code></td><td>See <a href=\"https://bridges.torproject.org\" target=\"_blank\">bridges.torproject.org</a></td></tr>\n");
        buf.append("<tr><td>Circuit Build Timeout</td><td><code>").append(tc.getCircuitBuildTimeout()).append("</code> <span class=\"nowrap\">(")
           .append(tc.getCircuitBuildTimeout() / 1000).append(" seconds)").append("</span></td><td>Time limit for new circuit build</td></tr>\n");
        buf.append("<tr><td>Circuit Idle Timeout</td><td><code>").append(tc.getCircuitIdleTimeout()).append("</code> <span class=\"nowrap\">(")
           .append((tc.getCircuitIdleTimeout() / 1000) / 60).append(" minutes)</span>").append("</td><td>Expire circuit if unused for period</td></tr>\n");
        buf.append("<tr><td>Circuit Stream Timeout</td><td><code>").append(tc.getCircuitStreamTimeout()).append("</code> <span class=\"nowrap\">(")
           .append(tc.getCircuitStreamTimeout() / 1000).append(" seconds)</span>").append("</td><td>Timeout for trying new circuit if stream fails</td></tr>\n");
        buf.append("<tr><td>Client Reject Internal Address</td><td><code>").append(tc.getClientRejectInternalAddress()).append("</code></td><td>Reject connection attempts to internal addresses</td></tr>\n");
        buf.append("<tr><td>Enforce Distinct Subnets</td><td><code>").append(tc.getEnforceDistinctSubnets()).append("</code></td><td>Don't use nodes from the same /16 in a single circuit</td></tr>\n");
        buf.append("<tr><td>Entry Nodes</td><td><code>");
        if (!tc.getEntryNodes().isEmpty())
            buf.append(tc.getEntryNodes());
        buf.append("</code></td><td>List of desired entry nodes</td></tr>\n");
        buf.append("<tr><td>Exclude Exit Nodes</td><td><code>");
        if (!tc.getExcludeExitNodes().isEmpty())
            buf.append(tc.getExcludeExitNodes());
        buf.append("</code></td><td>Don't use specified nodes as Exits</td></tr>\n");
        buf.append("<tr><td>Exclude Nodes</td><td><code>");
        if (!tc.getExcludeNodes().isEmpty())
            buf.append(tc.getExcludeNodes());
        buf.append("</code></td><td>Don't use specified nodes in any circuit</td></tr>\n");
        buf.append("<tr><td>Exit Nodes</td><td><code>");
        if (!tc.getExitNodes().isEmpty())
            buf.append(tc.getExitNodes());
        buf.append("</code></td><td>Limit Exit nodes to those specified</td></tr>\n");
        buf.append("<tr><td>Fascist Firewall</td><td><code>").append(tc.getFascistFirewall()).append("</code></td><td>Only connect to nodes using &lt;Firewall Ports&gt;</td></tr>\n");
        buf.append("<tr><td>Firewall Ports</td><td><code>").append(tc.getFirewallPorts()).append("</code></td><td>Specify outgoing ports if behind strict firewall</td></tr>\n");
        buf.append("<tr><td>Handshake V2 Enabled</td><td><code>").append(tc.getHandshakeV2Enabled()).append("</code></td><td>Use version 2 of the Tor handshake</td></tr>\n");
        buf.append("<tr><td>Handshake V3 Enabled</td><td><code>").append(tc.getHandshakeV3Enabled()).append("</code></td><td>Use version 3 of the Tor handshake</td></tr>\n");
        buf.append("<tr><td>Long Lived Ports</td><td><code>").append(tc.getLongLivedPorts()).append("</code></td><td>Map these ports to high-uptime nodes</td></tr>\n");
        buf.append("<tr><td>Max Circuit Dirtiness</td><td><code>").append(tc.getMaxCircuitDirtiness()).append("</code> <span class=\"nowrap\">(")
           .append((tc.getMaxCircuitDirtiness() / 1000) / 60).append(" minutes)</span>").append("</td><td>Max time to use circuit before cycling</td></tr>\n");
        buf.append("<tr><td>Max Client Circuits Pending</td><td><code>").append(tc.getMaxClientCircuitsPending()).append("</code></td><td>Max number of circuit builds in progess</td></tr>\n");
        buf.append("<tr><td>New Circuit Period</td><td><code>").append(tc.getNewCircuitPeriod()).append("</code> <span class=\"nowrap\">(")
           .append(tc.getNewCircuitPeriod() / 1000).append(" seconds)</span>").append("</td><td>Period of new circuit build consideration</td></tr>\n");
        buf.append("<tr><td>Number of Entry Guards</td><td><code>").append(tc.getNumEntryGuards()).append("</code></td><td>Number of long-term entry guards to use if &lt;Use Entry Guards&gt; is set to <i>true</i></td></tr>\n");
        buf.append("<tr><td>Safe Logging</td><td><code>").append(tc.getSafeLogging()).append("</code></td><td>Scrub sensitive strings (eg. addresses) from logs</td></tr>\n");
        buf.append("<tr><td>Safe Socks</td><td><code>").append(tc.getSafeSocks()).append("</code></td><td>Reject requests that only provide ip address</td></tr>\n");
        buf.append("<tr><td>Strict Nodes</td><td><code>").append(tc.getStrictNodes()).append("</code></td><td>Use excluded nodes if necessary for network task</td></tr>\n");
        buf.append("<tr><td>Use Bridges</td><td><code>").append(tc.getUseBridges()).append("</code></td><td>Use &lt;bridges&gt; to connect to network</td></tr>\n");
        buf.append("<tr><td>Use Entry Guards</td><td><code>").append(tc.getUseEntryGuards()).append("</code></td><td>Select a few long-term entry nodes and stick with them</td></tr>\n");
        buf.append("<tr><td>Use Microdescriptors</td><td><code>").append(tc.getUseMicrodescriptors()).append("</code></td><td>Use bandwidth-saving info to build circuits</td></tr>\n");
        buf.append("<tr><td>Use NTor Handshake</td><td><code>").append(tc.getUseNTorHandshake()).append("</code></td><td>Use the ntor circuit-creation handshake</td></tr>\n");
        buf.append("<tr><td>Warn Unsafe Socks</td><td><code>").append(tc.getWarnUnsafeSocks()).append("</code></td><td>Warn when requests only provide ip address</td></tr>\n");
        buf.append("</table>\n</td></tr>\n</table>\n");
        String useMds = String.valueOf(tc.getUseMicrodescriptors());
        if (useMds.equals("AUTO") || useMds.equals("TRUE") || useMds.equals("true"))
            buf.append("<style>#conncache .nickname:hover::after {display: none !important;}</style>\n");
        return buf.toString();
    }

    /** translate */
    private String _t(String s) {
        return Translate.getString(s, _context, BUNDLE);
    }

    /** translate */
    private String _t(String s, Object o) {
        return Translate.getString(s, o, _context, BUNDLE);
    }

    /** translate */
    private String _t(String s, Object o, Object o2) {
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
