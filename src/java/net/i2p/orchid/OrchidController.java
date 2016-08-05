package net.i2p.orchid;
/*
 *  Copyright 2014 zzz (zzz@mail.i2p)
 *
 *  BSD License
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorConfig;
// import static com.subgraph.orchid.TorConfig.ConfigVarType.*;
import com.subgraph.orchid.TorConfig.AutoBoolValue;
import com.subgraph.orchid.TorInitializationListener;
import com.subgraph.orchid.config.TorConfigBridgeLine;
import com.subgraph.orchid.config.TorConfigInterval;
import com.subgraph.orchid.config.TorConfigParser;
import com.subgraph.orchid.dashboard.Dashboard;

import net.i2p.I2PAppContext;
import net.i2p.app.*;
import static net.i2p.app.ClientAppState.*;
import net.i2p.data.Base32;
import net.i2p.data.DataHelper;
import net.i2p.util.FileUtil;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * This handles the starting and stopping of Orchid itself.
 *
 * This is instantiated and started by the servlet to avoid class loader issues later.
 * Starting from client.config caused class loader problems.
 *
 * We implement ClientApp so we may register with the ClientAppManager,
 * as this is how i2ptunnel finds us.
 *
 * @author zzz
 */
public class OrchidController implements ClientApp, TorInitializationListener, Outproxy {
    private final Log _log;
    private volatile ClientAppState _state;
    private final I2PAppContext _context;
    private final ClientAppManager _mgr;
    private final File _configDir;
    private final File _configFile;
    private TorClient _client;
    private OrchidLogHandler _logger;

    private static final String DEFAULT_CONFIG_DIR = ".orchid";
    private static final String REGISTERED_NAME = Outproxy.NAME;
    private static final String CONFIG_FILE = "orchid.config";

    /**
     *  Instantiation only. Caller must call startup().
     *  Config file problems will not throw exception until startup().
     *
     *  @param mgr may be null
     *  @param args one arg, the config file, if not absolute will be relative to the context's config dir,
     *              if empty or null, the default is i2ptunnel.config
     */
    public OrchidController(I2PAppContext context, ClientAppManager mgr, String[] args) {
        _state = UNINITIALIZED;
        _context = context;
        _mgr = mgr;
        _log = _context.logManager().getLog(OrchidController.class);
        if (args == null || args.length <= 0)
            _configDir = new File(System.getProperty("user.home"), DEFAULT_CONFIG_DIR);
        else if (args.length >= 1)
            _configDir = new File(args[0], "data");
        else
            throw new IllegalArgumentException("Usage: OrchidController [configDir]");
        _configFile = new File(_configDir.getParentFile(), CONFIG_FILE);
        _state = INITIALIZED;
    }		

    public void initializationProgress(String message, int percent) {
        if (_log.shouldLog(Log.INFO))
            _log.info(message + ' ' + percent + '%');
    }

    public void initializationCompleted() {
        changeState(RUNNING);
        if (_mgr != null)
            _mgr.register(this);
        if (_log.shouldLog(Log.INFO))
            _log.info("Orchid ready");
    }

    /**
     *  ClientApp interface
     *  @throws IllegalArgumentException if unable to load config from file
     */
    public synchronized void startup() {
        if (_state != INITIALIZED && _state != STOPPED)
            throw new IllegalStateException();
        changeState(STARTING);
        if (_log.shouldLog(Log.INFO))
            _log.info("Starting Orchid");
        // TODO config dir
        try {
            _logger = new OrchidLogHandler(_context);
            _client = new TorClient();
            _client.getConfig().setDataDirectory(_configDir);
            loadConfig(_client.getConfig());
            _client.addInitializationListener(this);
            _client.start();
        } catch (RuntimeException t) {
            // TorException extends RuntimeException,
            // unlimited strength policy files not installed
            changeState(START_FAILED, t);
            throw t;
        }
        if (_mgr != null) {
            // Don't register until initializationCompleted()
            //_mgr.register(this);
            // RouterAppManager registers its own shutdown hook
        } else {
            _context.addShutdownTask(new Shutdown());
        }
    }

    /**
     *  ClientApp interface
     */
    public ClientAppState getState() {
        return _state;
    }

    /**
     *  ClientApp interface
     */
    public String getName() {
        return REGISTERED_NAME;
    }

    /**
     *  ClientApp interface
     */
    public String getDisplayName() {
        return REGISTERED_NAME;
    }

    /**
     *
     */
    private void changeState(ClientAppState state) {
        changeState(state, null);
    }

    /**
     *
     */
    private synchronized void changeState(ClientAppState state, Exception e) {
        _state = state;
        if (_mgr != null)
            _mgr.notify(this, state, null, e);
    }

    /**
     *
     */
    private class Shutdown implements Runnable {
        public void run() {
            shutdown();
        }
    }

    /**
     *  ClientApp interface
     */
    public void shutdown(String[] args) {
        shutdown();
    }

    /**
     *  Stop everything
     */
    public synchronized void shutdown() {
        if (_state != STARTING && _state != RUNNING)
            return;
        changeState(STOPPING);
        if (_log.shouldLog(Log.INFO))
            _log.info("Stopping Orchid");
        if (_mgr != null)
            _mgr.unregister(this);
        if (_client != null) {
            _client.stop();
            _client = null;
        }
        if (_logger != null) {
            _logger.close();
            _logger = null;
        }
        changeState(STOPPED);
        if (_log.shouldLog(Log.INFO))
            _log.info("Orchid stopped");
    }

    public Socket connect(String host, int port) throws IOException {
        if (host.equals("127.0.0.1") || host.equals("localhost") ||
            host.toLowerCase(Locale.US).endsWith(".i2p"))
            throw new IOException("unsupported host " + host);
        ClientAppState state = _state;
        if (state != RUNNING)
            throw new IOException("Cannot connect in state " + state);
        if (_log.shouldLog(Log.INFO))
            _log.info("Connecting to " + host + ':' + port);
        try {
            return new TorStreamSocket(_client, host, port);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException ioe = new IOException("connect error");
            ioe.initCause(e);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("Error Connecting to " + host + ':' + port, ioe);
            throw ioe;
        }
    }

    public synchronized TorConfig getConfig() {
        if (_client != null)
            return _client.getConfig();
        // else load from file
        return null;
    }

    public synchronized void saveConfig() {
        // ...
    }

    public synchronized void loadConfig(TorConfig tc) {
        Properties p = new Properties();
        try {
            DataHelper.loadProps(p, _configFile);
        } catch (IOException ioe) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("error loading config file", ioe);
            return;
        }
        if (_log.shouldLog(Log.INFO))
            _log.info("Loading " + p.size() + " configuations");
        TorConfigParser tcp = new TorConfigParser();
        for (Map.Entry e : p.entrySet()) {
            String k = (String) e.getKey();
            String v = (String) e.getValue();
            if (k.equals("bridges")) {
                // unimplemented in parser, will throw IAE
                List<TorConfigBridgeLine> list = (List<TorConfigBridgeLine>) tcp.parseValue(v, "BRIDGE_LINE");
                for (TorConfigBridgeLine tcbl : list) {
                    tc.addBridge(tcbl.getAddress(), tcbl.getPort(), tcbl.getFingerprint());
                }
            } else if (k.equals("circuitBuildTimeout")) {
                TorConfigInterval tci = (TorConfigInterval) tcp.parseValue(v, "INTERVAL");
                tc.setCircuitBuildTimeout(tci.getMilliseconds(), TimeUnit.MILLISECONDS);
            } else if (k.equals("circuitIdleTimeout")) {
                TorConfigInterval tci = (TorConfigInterval) tcp.parseValue(v, "INTERVAL");
                tc.setCircuitIdleTimeout(tci.getMilliseconds(), TimeUnit.MILLISECONDS);
            } else if (k.equals("circuitStreamTimeout")) {
                TorConfigInterval tci = (TorConfigInterval) tcp.parseValue(v, "INTERVAL");
                tc.setCircuitStreamTimeout(tci.getMilliseconds(), TimeUnit.MILLISECONDS);
            } else if (k.equals("clientRejectInternalAddress")) {
                tc.setClientRejectInternalAddress((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("enforceDistinctSubnets")) {
                tc.setEnforceDistinctSubnets((Boolean) tcp.parseValue(v, "INTEGER"));
            } else if (k.equals("numEntryGuards")) {
                tc.setNumEntryGuards((Integer) tcp.parseValue(v, "INTEGER"));
            } else if (k.equals("entryNodes")) {
                tc.setEntryNodes((List<String>) tcp.parseValue(v, "STRINGLIST"));
            } else if (k.equals("excludeExitNodes")) {
                tc.setExcludeExitNodes((List<String>) tcp.parseValue(v, "STRINGLIST"));
            } else if (k.equals("excludeNodes")) {
                tc.setExcludeNodes((List<String>) tcp.parseValue(v, "STRINGLIST"));
            } else if (k.equals("exitNodes")) {
                tc.setExitNodes((List<String>) tcp.parseValue(v, "STRINGLIST"));
            } else if (k.equals("fascistFirewall")) {
                tc.setFascistFirewall((Boolean) tcp.parseValue(v, "INTEGER"));
            } else if (k.equals("firewallPorts")) {
                tc.setFirewallPorts((List<Integer>) tcp.parseValue(v, "PORTLIST"));
            } else if (k.equals("handshakeV2Enabled")) {
                tc.setHandshakeV2Enabled((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("handshakeV3Enabled")) {
                tc.setHandshakeV3Enabled((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("longLivedPorts")) {
                tc.setLongLivedPorts((List<Integer>) tcp.parseValue(v, "PORTLIST"));
            } else if (k.equals("maxCircuitDirtiness")) {
                TorConfigInterval tci = (TorConfigInterval) tcp.parseValue(v, "INTERVAL");
                tc.setMaxCircuitDirtiness(tci.getMilliseconds(), TimeUnit.MILLISECONDS);
            } else if (k.equals("maxClientCircuitsPending")) {
                tc.setMaxClientCircuitsPending((Integer) tcp.parseValue(v, "INTEGER"));
            } else if (k.equals("newCircuitPeriod")) {
                TorConfigInterval tci = (TorConfigInterval) tcp.parseValue(v, "INTERVAL");
                tc.setNewCircuitPeriod(tci.getMilliseconds(), TimeUnit.MILLISECONDS);
            } else if (k.equals("safeLogging")) {
                tc.setSafeLogging((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("safeSocks")) {
                tc.setSafeSocks((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("strictNodes")) {
                tc.setStrictNodes((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("useBridges")) {
                tc.setUseBridges((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else if (k.equals("useMicrodescriptors")) {
                tc.setUseMicrodescriptors((AutoBoolValue) tcp.parseValue(v, "AUTOBOOL"));
            } else if (k.equals("useNTorHandshake")) {
                tc.setUseNTorHandshake((AutoBoolValue) tcp.parseValue(v, "AUTOBOOL"));
            } else if (k.equals("warnUnsafeSocks")) {
                tc.setWarnUnsafeSocks((Boolean) tcp.parseValue(v, "BOOLEAN"));
            } else {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Unknown config entry " + k + " = " + v);
            }
        }
    }

    public synchronized void renderStatusHTML(PrintWriter out) throws IOException {
        if (_client == null)
            return;
        // can't get to TorConfig's Dashboard from here so make a new one
        // FIXME strip HTML
        (new Dashboard()).renderComponent(out, 0xff, _client.getCircuitManager());
   }
}
    
