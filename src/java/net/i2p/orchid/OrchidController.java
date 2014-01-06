package net.i2p.orchid;
/*
 *  Copyright 2010 zzz (zzz@mail.i2p)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import net.i2p.I2PAppContext;
import net.i2p.app.*;
import static net.i2p.app.ClientAppState.*;
import net.i2p.data.Base32;
import net.i2p.data.DataHelper;
import net.i2p.util.FileUtil;
import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * This handles the starting and stopping of an eepsite tunnel and jetty
 * from a single static class so it can be called via clients.config.
 *
 * This makes installation of a new eepsite a turnkey operation -
 * the user is not required to configure a new tunnel in i2ptunnel manually.
 *
 * Usage: ZzzOTController -d $PLUGIN [start|stop]
 *
 * @author zzz
 */
public class OrchidController implements ClientApp, TorInitializationListener, Outproxy {
    private final Log _log;
    private volatile ClientAppState _state;
    private final I2PAppContext _context;
    private final ClientAppManager _mgr;
    private final File _configDir;
    private TorClient _client;
    private OrchidLogHandler _logger;

    private static final String DEFAULT_CONFIG_DIR = ".orchid";
    private static final String REGISTERED_NAME = Outproxy.NAME;


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
        _logger = new OrchidLogHandler(_context);
        _client = new TorClient();
        _client.getConfig().setDataDirectory(_configDir);
        _client.addInitializationListener(this);
        _client.start();
        if (_mgr != null)
            _mgr.register(this);
            // RouterAppManager registers its own shutdown hook
        else
            _context.addShutdownTask(new Shutdown());
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
            throw ioe;
        }
    }
}
    
